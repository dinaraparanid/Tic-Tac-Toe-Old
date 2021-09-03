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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Server extends Service {

    @NonNls
    @NonNull
    private static final String TAG = "Server";

    static final int PORT = 1337;

    public static final int gameTableSize = 3; // TODO: create customizable table

    @NonNull
    // 0 -> null, 1 -> server, 2 -> client
    protected byte[][] gameTable = new byte[gameTableSize][gameTableSize];

    @NonNull
    protected final AtomicReference<String> hostName = new AtomicReference<>();

    @NonNull
    protected final AtomicBoolean isGameEnded = new AtomicBoolean();

    @NonNull
    protected final AtomicReference<SocketChannel> client = new AtomicReference<>();

    @NonNull
    protected final ExecutorService executor = Executors.newCachedThreadPool();

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
    static final String BROADCAST_SHOW_IP = "show_ip";

    @NonNls
    @NonNull
    static final String BROADCAST_GET_IP = "get_ip";

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

    // -------------------------------- ClientPlayer send commands --------------------------------

    static final byte COMMAND_SHOW_ROLE = 0;
    static final byte COMMAND_CORRECT_MOVE = 1;
    static final byte COMMAND_INVALID_MOVE = 2;
    static final byte COMMAND_GAME_FINISH = 3;

    private static final long NO_PLAYER_FOUND = Long.MAX_VALUE;

    protected AtomicBoolean isClientPlayerConnected = new AtomicBoolean();

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
                sendIp(hostName.get());

                isGameEnded.set(false);
                isClientPlayerConnected.set(false);

                System.arraycopy(
                        new byte[gameTableSize][gameTableSize],
                        0,
                        gameTable,
                        0,
                        gameTable.length
                );

                new Thread(() -> {
                    try { runBFSM(); }
                    catch (final IOException e) { e.printStackTrace(); }
                }).start();

                executor.submit(() -> {
                    final Lock lock = new ReentrantLock();
                    final Condition noClientCondition = lock.newCondition();

                    lock.lock();

                    try {
                        long await = 0;

                        if(!isClientPlayerConnected.get())
                            await = noClientCondition.awaitNanos(NO_PLAYER_FOUND);

                        if (await <= 0) {
                            sendNoPlayerFound();
                            stop();
                        }
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                });
            }
        }
    };

    @NonNull
    private final BroadcastReceiver cancelGameReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(BROADCAST_CANCEL_GAME)) {
                Log.d(TAG, "Cancel game");
                stop();
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

                    executor.execute(() -> {
                        try {
                            sendCorrectMove(client.get());
                        } catch (final ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }

                        boolean isFilled = true;

                        for (final byte[] row : gameTable) {
                            for (final byte cell : row) {
                                if (cell == 0) {
                                    isFilled = false;
                                    break;
                                }
                            }
                        }

                        if (isFilled) {
                            isGameEnded.set(true);

                            try {
                                sendGameFinish(client.get());
                            } catch (final ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
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
                stop();
            }
        }
    };

    private final class ClientPlayerIsFoundState extends State {
        ClientPlayerIsFoundState() {
            super(() -> {
                Log.d(TAG, "Client player connected");
                isClientPlayerConnected.set(true);
            });
        }
    }

    private final class SendRolesState extends State {
        SendRolesState(@NonNull final WritableByteChannel client) {
            super(() -> {
                Log.d(TAG, "Send roles");
                try {
                    sendRoles(client);
                } catch (final ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private final class ClientPlayerIsMovedState extends State {
        ClientPlayerIsMovedState(@NonNull final SocketChannel client) {
            super(() -> {
                Log.d(TAG, "Client player is moved");

                final ByteBuffer buffer = ByteBuffer.allocate(2);

                try { client.read(buffer); }
                catch (final IOException e) { e.printStackTrace(); }

                buffer.flip();

                final byte y = buffer.get();
                final byte x = buffer.get();

                if (checkMovement(y, x)) {
                    gameTable[y][x] = 2;

                    try {
                        sendCorrectMove(client);
                    } catch (final ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    boolean isFilled = true;

                    for (final byte[] row : gameTable) {
                        for (final byte cell : row) {
                            if (cell == 0) {
                                isFilled = false;
                                break;
                            }
                        }
                    }

                    if (isFilled) {
                        isGameEnded.set(true);

                        try {
                            sendGameFinish(client);
                        } catch (final ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }

                        stop();
                    }
                } else {
                    try {
                        sendClientPlayerInvalidMove(client);
                    } catch (final ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    protected final void createServerSocket() throws ExecutionException, InterruptedException {
        executor.submit(() -> hostName.set(
                Formatter.formatIpAddress(
                        ((WifiManager) getApplicationContext()
                                .getSystemService(WIFI_SERVICE))
                                .getConnectionInfo()
                                .getIpAddress()
                )
        )).get();

        sendIp(hostName.get());

        try { server = ServerSocketChannel.open(); }
        catch (final IOException e) { e.printStackTrace(); }

        executor.submit(() -> {
            try {
                server.socket().bind(new InetSocketAddress(hostName.get(), PORT));
                server.configureBlocking(false);

                selector = Selector.open();
                server.register(selector, SelectionKey.OP_ACCEPT);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }).get();
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
        } catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final int onStartCommand(
            @NonNull final Intent intent,
            final int flags,
            final int startId
    ) {
        new Thread(() -> {
            try { runBFSM(); }
            catch (final IOException e) { e.printStackTrace(); }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
    }

    private final void registerCreateGameReceiver() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        createGameReceiver,
                        new IntentFilter(BROADCAST_CREATE_GAME)
                );
    }

    private final void registerCancelGameReceiver() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        cancelGameReceiver,
                        new IntentFilter(BROADCAST_CANCEL_GAME)
                );
    }

    private final void registerServerPlayerMovedReceiver() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        serverPlayerMovedReceiver,
                        new IntentFilter(BROADCAST_SERVER_PLAYER_MOVED)
                );
    }

    private final void registerServerPlayerDisconnectedReceiver() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        serverPlayerDisconnectedReceiver,
                        new IntentFilter(BROADCAST_SERVER_PLAYER_DISCONNECTED)
                );
    }

    private final void registerKillReceiver() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        killReceiver,
                        new IntentFilter(BROADCAST_KILL)
                );
    }

    protected final void unregisterReceivers() {
        final LocalBroadcastManager manager = LocalBroadcastManager
                .getInstance(getApplicationContext());

        manager.unregisterReceiver(createGameReceiver);
        manager.unregisterReceiver(cancelGameReceiver);
        manager.unregisterReceiver(serverPlayerMovedReceiver);
        manager.unregisterReceiver(serverPlayerDisconnectedReceiver);
        manager.unregisterReceiver(killReceiver);
    }

    /** Like BFG in DOOM, but it's State Machine */

    protected final void runBFSM() throws IOException {
        while (!isGameEnded.get()) {
            selector.select();
            final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

            while (iter.hasNext()) {
                final SelectionKey key = iter.next();

                if (key.isAcceptable()) {
                    final SocketChannel clientRegister = server.accept();
                    clientRegister.configureBlocking(false);
                    clientRegister.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    final ByteBuffer readerBuffer = ByteBuffer.allocate(1);
                    client.set((SocketChannel) key.channel());

                    if (client.get().read(readerBuffer) < 0) {
                        client.get().close();
                    } else {
                        readerBuffer.flip();
                        final byte command = readerBuffer.get();

                        Log.d(TAG, "Command: " + command);

                        switch (command) {
                            case PLAYER_IS_FOUND:
                                new ClientPlayerIsFoundState().run();
                                new SendRolesState(client.get()).run();
                                break;

                            case PLAYER_MOVED:
                                new ClientPlayerIsMovedState(client.get()).run();
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

    protected final boolean checkMovement(@NonNull final Coordinate coordinate) {
        return gameTable[coordinate.getY()][coordinate.getX()] == 0;
    }

    @Contract(pure = true)
    protected final boolean checkMovement(final byte y, final byte x) { return gameTable[y][x] == 0; }

    protected final void sendIp(@NonNull final String ip) {
        Log.d(TAG, "Send IP");

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_SHOW_IP)
                                .putExtra(BROADCAST_GET_IP, ip)
                );
    }

    protected final void sendNoPlayerFound() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(new Intent(BROADCAST_NO_PLAYER_FOUND));
    }

    protected final void sendRoles(@NonNull final WritableByteChannel client)
            throws ExecutionException, InterruptedException {
        final byte serverPlayerRole = (byte) new SecureRandom().nextInt(2);
        final byte clientPlayerRole = (byte) (1 - serverPlayerRole);

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_ROLE)
                                .putExtra(BROADCAST_GET_ROLE, serverPlayerRole)
                );

        executor.submit(() -> {
            final ByteBuffer buffer = ByteBuffer.allocate(2);
            buffer.put(COMMAND_SHOW_ROLE);
            buffer.put(clientPlayerRole);
            buffer.flip();

            try { client.write(buffer); }
            catch (final IOException e) { e.printStackTrace(); }
        }).get();
    }

    protected final void sendCorrectMove(@NonNull final WritableByteChannel client)
            throws ExecutionException, InterruptedException {
        Log.d(TAG, "Send correct move");

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_CORRECT_MOVE)
                                .putExtra(BROADCAST_GET_UPDATE_TABLE, gameTable)
                );

        executor.submit(() -> {
            final ByteBuffer buffer = ByteBuffer.allocate(1 + gameTableSize * gameTableSize);
            buffer.put(COMMAND_CORRECT_MOVE);

            // Damn, streams requires API 24 :(
            for (final byte[] r : gameTable)
                for (final byte b : r)
                    buffer.put(b);

            buffer.flip();

            try { client.write(buffer); }
            catch (final IOException e) { e.printStackTrace(); }
        }).get();
    }

    protected final void sendServerPlayerInvalidMove() {
        Log.d(TAG, "Send server invalid move");

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_INVALID_MOVE)
                );
    }

    protected void sendClientPlayerInvalidMove(@NonNull final WritableByteChannel client)
            throws ExecutionException, InterruptedException {
        Log.d(TAG, "Send client invalid move");

        executor.submit(() -> {
            final ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put(COMMAND_INVALID_MOVE);
            buffer.flip();

            try { client.write(buffer); }
            catch (final IOException e) { e.printStackTrace(); }
        }).get();
    }

    protected void sendGameFinish(@NonNull final WritableByteChannel client)
            throws ExecutionException, InterruptedException {
        Log.d(TAG, "Game finished");

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(new Intent(BROADCAST_GAME_FINISHED));

        executor.submit(() -> {
            final ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put(COMMAND_GAME_FINISH);
            buffer.flip();

            try { client.write(buffer); }
            catch (final IOException e) { e.printStackTrace(); }
        }).get();
    }

    protected final void stop() {
        unregisterReceivers();
        stopSelf();
    }
}