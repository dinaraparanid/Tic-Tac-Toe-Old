package com.dinaraparanid.tictactoe;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dinaraparanid.tictactoe.utils.Coordinate;
import com.dinaraparanid.tictactoe.utils.polymorphism.State;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Server extends Service {

    @NonNls
    @NonNull
    private static final String TAG = "Server";

    static final int PORT = 1337;

    static final int gameTableSize = 3; // TODO: create customizable table

    @NonNull
    protected final AtomicBoolean isGameEnded = new AtomicBoolean(false);

    @NonNull
    protected byte[][] gameTable = new byte[gameTableSize][gameTableSize]; // 0 -> null, 1 -> x, 2 -> 0

    @NonNull
    protected final AtomicBoolean needToSendTableUpdate = new AtomicBoolean(false);

    // -------------------------------- Receive broadcasts --------------------------------

    @NonNls
    @NonNull
    static final String BROADCAST_CREATE_GAME = "create_game";

    @NonNls
    @NonNull
    static final String BROADCAST_CANCEL_GAME = "cancel_game";

    @NonNls
    @NonNull
    static final String BROADCAST_SERVER_PLAYER_MOVED = "first_server_moved";

    @NonNls
    @NonNull
    static final String BROADCAST_SERVER_PLAYER_DISCONNECTED = "server_player_disconnected";

    @NonNls
    @NonNull
    static final String BROADCAST_KILL = "kill";

    // -------------------------------- Send broadcasts --------------------------------

    @NonNls
    @NonNull
    static final String BROADCAST_NO_PLAYER_FOUND = "no_player_found";

    @NonNls
    @NonNull
    static final String BROADCAST_ROLE = "role";

    @NonNls
    @NonNull
    static final String BROADCAST_GET_ROLE = "get_role";

    @NonNls
    @NonNull
    static final String BROADCAST_CORRECT_MOVE = "update_table";

    @NonNls
    @NonNull
    static final String BROADCAST_GET_UPDATE_TABLE = "get_update_table";

    @NonNls
    @NonNull
    static final String BROADCAST_INVALID_MOVE = "invalid_move";

    @NonNls
    @NonNull
    static final String BROADCAST_GAME_FINISHED = "game_finished";

    // -------------------------------- ClientPlayer callbacks --------------------------------

    static final byte PLAYER_IS_FOUND = 0;

    static final byte PLAYER_MOVED = 1;
    static final byte PLAYER_MOVED_Y = 2;
    static final byte PLAYER_MOVED_X = 3;

    // -------------------------------- ClientPlayer send commands --------------------------------

    static final byte COMMAND_SHOW_ROLE = 0;
    static final byte COMMAND_CORRECT_MOVE = 1;
    static final byte COMMAND_INVALID_MOVE = 2;
    static final byte COMMAND_GAME_FINISH = 3;

    private static final int NO_PLAYER_FOUND = (int) 1e9;

    protected boolean isClientPlayerConnected = false;

    protected final class LocalBinder extends Binder {
        @NonNull
        @Contract(pure = true)
        public final Server getServer() { return Server.this; }
    }

    @NonNull
    private final IBinder iBinder = new LocalBinder();

    @NonNull
    protected ServerSocketChannel server;

    @NonNull
    protected Selector selector;

    @NonNull
    private final BroadcastReceiver createGameReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(BROADCAST_CREATE_GAME)) {
                Log.d(TAG, "Create game");

                int wait = 0;

                while (!isClientPlayerConnected && wait < NO_PLAYER_FOUND)
                    wait++;

                if (wait == NO_PLAYER_FOUND)
                    sendNoPlayerFound();
            }
        }
    };

    @NonNull
    private final BroadcastReceiver cancelGameReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(BROADCAST_CANCEL_GAME)) {
                Log.d(TAG, "Cancel game");
                unregisterReceivers();
                stopSelf();
            }
        }
    };

    @NonNull
    private final BroadcastReceiver serverPlayerMovedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(BROADCAST_SERVER_PLAYER_MOVED)) {
                Log.d(TAG, "Server player moved");

                final Coordinate coordinate = (Coordinate) intent
                        .getSerializableExtra(ServerPlayer.COORDINATE_KEY);

                if (checkMovement(coordinate)) {
                    gameTable[coordinate.getY()][coordinate.getX()] = 1;

                    boolean filled = true;

                    for (final byte[] row : gameTable) {
                        for (final byte cell : row) {
                            if (cell == 0) {
                                filled = false;
                                break;
                            }
                        }
                    }

                    if (filled)
                        isGameEnded.set(true);

                    needToSendTableUpdate.set(true);
                } else {
                    sendServerPlayerInvalidMove();
                }
            }
        }
    };

    @NonNull
    private final BroadcastReceiver serverPlayerDisconnectedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            // TODO: Show to second player that first player is disconnected
        }
    };

    @NonNull
    private final BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(BROADCAST_KILL)) {
                Log.d(TAG, "Kill");
                unregisterReceivers();
                stopSelf();
            }
        }
    };

    private final class ClientPlayerIsFoundState extends State {
        ClientPlayerIsFoundState() {
            super(new Runnable() {
                @Override
                public final void run() {
                    Log.d(TAG, "Client player connected");
                    isClientPlayerConnected = true;
                }
            });
        }
    }

    private final class SendRolesState extends State {
        SendRolesState(@NonNull final WritableByteChannel client) {
            super(new Runnable() {
                @Override
                public final void run() {
                    Log.d(TAG, "Send roles");
                    sendRoles(client);
                }
            });
        }
    }

    private final class ClientPlayerIsMovedState extends State {
        ClientPlayerIsMovedState(
                @NonNull final SocketChannel client
        ) {
            super(new Runnable() {
                @Override
                public final void run() {
                    Log.d(TAG, "Client player is moved");

                    final ByteBuffer buffer = ByteBuffer.allocate(2);

                    try { client.read(buffer); }
                    catch (final IOException e) { e.printStackTrace(); }

                    buffer.flip();

                    final byte y = buffer.get(PLAYER_MOVED_Y);
                    final byte x = buffer.get(PLAYER_MOVED_X);

                    if (checkMovement(y, x)) {
                        gameTable[y][x] = 2;

                        boolean filled = true;

                        for (final byte[] row : gameTable) {
                            for (final byte cell : row) {
                                if (cell == 0) {
                                    filled = false;
                                    break;
                                }
                            }
                        }

                        if (filled)
                            isGameEnded.set(true);

                        sendCorrectMove(client);
                    } else {
                        sendClientPlayerInvalidMove(client);
                    }
                }
            });
        }
    }

    public final void createServerSocket() throws InterruptedException {
        final String[] hostName = new String[1];

        final Thread getHostName = new Thread(new Runnable() {
            @Override
            public final void run() {
                hostName[0] = Formatter.formatIpAddress(
                        ((WifiManager) getApplicationContext()
                                .getSystemService(WIFI_SERVICE))
                                .getConnectionInfo()
                                .getIpAddress()
                );
            }
        });

        getHostName.start();
        getHostName.join();

        try { server = ServerSocketChannel.open(); }
        catch (final IOException e) { e.printStackTrace(); }

        final Thread init = new Thread(new Runnable() {
            @Override
            public final void run() {
                try {
                    server.socket().bind(new InetSocketAddress(hostName[0], PORT));
                    server.configureBlocking(false);

                    selector = Selector.open();
                    server.register(selector, SelectionKey.OP_ACCEPT);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        });

        init.start();
        init.join();
    }

    @NonNull
    @Override
    @Contract(pure = true)
    public final IBinder onBind(@Nullable final Intent intent) { return iBinder; }

    @Override
    public final void onCreate() {
        super.onCreate();

        registerCreateGameReceiver();
        registerCancelGameReceiver();
        registerServerPlayerMovedReceiver();
        registerServerPlayerDisconnectedReceiver();
        registerKillReceiver();

        try {
            createServerSocket();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final int onStartCommand(
            @NonNull final Intent intent,
            final int flags,
            final int startId
    ) {
        new Thread(new Runnable() {
            @Override
            public final void run() {
                try { runServer(); }
                catch (final IOException e) { e.printStackTrace(); }
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
    }

    @NonNull
    private final Intent registerCreateGameReceiver() {
        return registerReceiver(
                createGameReceiver,
                new IntentFilter(BROADCAST_CREATE_GAME)
        );
    }

    @NonNull
    private final Intent registerCancelGameReceiver() {
        return registerReceiver(
                cancelGameReceiver,
                new IntentFilter(BROADCAST_CANCEL_GAME)
        );
    }

    @NonNull
    private final Intent registerServerPlayerMovedReceiver() {
        return registerReceiver(
                serverPlayerMovedReceiver,
                new IntentFilter(BROADCAST_SERVER_PLAYER_MOVED)
        );
    }

    @NonNull
    private final Intent registerServerPlayerDisconnectedReceiver() {
        return registerReceiver(
                serverPlayerDisconnectedReceiver,
                new IntentFilter(BROADCAST_SERVER_PLAYER_DISCONNECTED)
        );
    }

    @NonNull
    private final Intent registerKillReceiver() {
        return registerReceiver(killReceiver, new IntentFilter(BROADCAST_KILL));
    }

    final void unregisterReceivers() {
        unregisterReceiver(createGameReceiver);
        unregisterReceiver(cancelGameReceiver);
        unregisterReceiver(serverPlayerMovedReceiver);
        unregisterReceiver(serverPlayerDisconnectedReceiver);
        unregisterReceiver(killReceiver);
    }

    protected final void runServer() throws IOException {
        while (true) {
            selector.select();
            final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

            while (iter.hasNext()) {
                final SelectionKey key = iter.next();

                if (key.isAcceptable()) {
                    final SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    final ByteBuffer readerBuffer = ByteBuffer.allocate(1);
                    final SocketChannel client = (SocketChannel) key.channel();

                    if (client.read(readerBuffer) < 0) {
                        client.close();
                    } else {
                        readerBuffer.flip();

                        if (isGameEnded.get()) {
                            sendGameFinish(client);
                            stopSelf();
                            return;
                        }

                        final byte command = readerBuffer.get();

                        if (needToSendTableUpdate.compareAndSet(true, false))
                            sendCorrectMove(client);

                        switch (command) {
                            case PLAYER_IS_FOUND:
                                new ClientPlayerIsFoundState().run();
                                new SendRolesState(client).run();
                                break;

                            case PLAYER_MOVED:
                                new ClientPlayerIsMovedState(client).run();
                                break;

                            default:
                                break;
                        }
                    }
                }

                iter.remove();
            }
        }
    }

    final boolean checkMovement(@NonNull final Coordinate coordinate) {
        return gameTable[coordinate.getY()][coordinate.getX()] != 0;
    }

    @Contract(pure = true)
    final boolean checkMovement(final byte y, final byte x) {
        return gameTable[y][x] != 0;
    }

    final void sendNoPlayerFound() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(new Intent(BROADCAST_NO_PLAYER_FOUND));
    }

    final void sendRoles(@NonNull final WritableByteChannel client) {
        final byte serverPlayerRole = (byte) new SecureRandom().nextInt(2);
        final byte clientPlayerRole = (byte) (1 - serverPlayerRole);

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_ROLE)
                                .putExtra(BROADCAST_GET_ROLE, serverPlayerRole)
                );

        final Thread sendRole = new Thread(new Runnable() {
            @Override
            public final void run() {
                final ByteBuffer buffer = ByteBuffer.allocate(2);
                buffer.put(COMMAND_SHOW_ROLE);
                buffer.put(clientPlayerRole);
                buffer.flip();

                try { client.write(buffer); }
                catch (final IOException e) { e.printStackTrace(); }
            }
        });

        sendRole.start();
        try { sendRole.join(); }
        catch (final InterruptedException e) { e.printStackTrace(); }
    }

    final void sendCorrectMove(@NonNull final WritableByteChannel client) {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_CORRECT_MOVE)
                                .putExtra(BROADCAST_GET_UPDATE_TABLE, gameTable)
                );

        final Thread sendCorrcetMove = new Thread(new Runnable() {
            @Override
            public final void run() {
                final ByteBuffer buffer = ByteBuffer.allocate(10);
                buffer.put(COMMAND_CORRECT_MOVE);

                // Damn, streams requires API 24 :(
                for (final byte[] r : gameTable)
                    for (final byte b : r)
                        buffer.put(b);

                try { client.write(buffer); }
                catch (final IOException e) { e.printStackTrace(); }
            }
        });

        sendCorrcetMove.start();
        try { sendCorrcetMove.join(); }
        catch (final InterruptedException e) { e.printStackTrace(); }
    }

    final void sendServerPlayerInvalidMove() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_INVALID_MOVE)
                );
    }

    static void sendClientPlayerInvalidMove(@NonNull final WritableByteChannel client) {
        final Thread sendInvalid = new Thread(new Runnable() {
            @Override
            public final void run() {
                final ByteBuffer buffer = ByteBuffer.allocate(1);
                buffer.put(COMMAND_INVALID_MOVE);
                try { client.write(buffer); }
                catch (final IOException e) { e.printStackTrace(); }
            }
        });

        sendInvalid.start();
        try { sendInvalid.join(); }
        catch (final InterruptedException e) { e.printStackTrace(); }
    }

    void sendGameFinish(@NonNull final WritableByteChannel client) {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(new Intent(BROADCAST_GAME_FINISHED));

        final Thread finish = new Thread(new Runnable() {
            @Override
            public final void run() {
                final ByteBuffer buffer = ByteBuffer.allocate(1);
                buffer.put(COMMAND_GAME_FINISH);
                try { client.write(buffer); }
                catch (final IOException e) { e.printStackTrace(); }
            }
        });

        finish.start();
        try { finish.join(); }
        catch (final InterruptedException e) { e.printStackTrace(); }
    }
}