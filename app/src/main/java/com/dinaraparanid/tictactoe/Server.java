package com.dinaraparanid.tictactoe;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dinaraparanid.tictactoe.utils.Coordinate;
import com.dinaraparanid.tictactoe.utils.polymorphism.State;

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
import java.util.Arrays;
import java.util.Iterator;

import kotlin.collections.ArraysKt;

public final class Server extends Service {
    private static final int PORT = 1337;
    private static final String HOST_NAME = "127.0.0.1";

    static final int gameTableSize = 3; // TODO: create customizable table
    byte[][] gameTable = new byte[gameTableSize][gameTableSize]; // 0 -> null, 1 -> x, 2 -> 0
    byte turn = 0;

    // -------------------------------- Receive broadcasts --------------------------------

    @NonNls
    @NonNull
    static final String BROADCAST_CREATE_GAME = "create_game";

    @NonNls
    @NonNull
    static final String BROADCAST_CANCEL_GAME = "cancel_game";

    @NonNls
    @NonNull
    static final String BROADCAST_FIRST_PLAYER_MOVED = "first_player_moved";

    @NonNls
    @NonNull
    static final String BROADCAST_FIRST_PLAYER_DISCONNECTED = "first_player_disconnected";

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
    static final String BROADCAST_TURN = "turn";

    @NonNls
    @NonNull
    static final String BROADCAST_GET_TURN = "get_turn";

    @NonNls
    @NonNull
    static final String BROADCAST_UPDATE_TABLE = "update_table";

    @NonNls
    @NonNull
    static final String BROADCAST_GET_UPDATE_TABLE = "get_update_table";

    @NonNls
    @NonNull
    static final String BROADCAST_SECOND_PLAYER_MOVED = "second_player_moved";

    @NonNls
    @NonNull
    static final String BROADCAST_GAME_FINISHED = "game_finished";

    // -------------------------------- ClientPlayer callbacks --------------------------------

    static final byte PLAYER_IS_FOUND = 0;

    static final byte PLAYER_MOVED = 1;
    static final byte PLAYER_MOVED_Y = 2;
    static final byte PLAYER_MOVED_X = 3;

    private static final int NO_PLAYER_FOUND = (int) 1e9;

    boolean isClientPlayerConnected = false;

    final class LocalBinder extends Binder {
        @NonNull
        public final Server getServer() { return Server.this; }
    }

    @NonNull
    private final IBinder iBinder = new LocalBinder();

    @NonNull
    private ServerSocketChannel server;

    @NonNull
    private Selector selector;

    @NonNull
    private final BroadcastReceiver createGameReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            int wait = 0;

            while (!isClientPlayerConnected && wait < NO_PLAYER_FOUND)
                wait++;

            if (wait == NO_PLAYER_FOUND) {
                sendNoPlayerFound();
            } else {
                // TODO: clientPlayer is found
            }
        }
    };

    @NonNull
    private final BroadcastReceiver cancelGameReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            unregisterReceivers();
            stopSelf();
        }
    };

    @NonNull
    private final BroadcastReceiver firstPlayerMovedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            final Coordinate coordinate = (Coordinate) intent
                    .getSerializableExtra(ServerPlayer.COORDINATE_KEY);

            if (checkMovement(0, coordinate)) {
                gameTable[coordinate.getY()][coordinate.getX()] = 1;
                turn = (byte)((turn + 1) % 2);
            }

            // TODO: send info to players
        }
    };

    @NonNull
    private final BroadcastReceiver firstPlayerDisconnectedReceiver = new BroadcastReceiver() {
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
                unregisterReceivers();
                stopSelf();
            }
        }
    };

    private final class SecondPlayerIsFoundState extends State {
        SecondPlayerIsFoundState() {
            super(() -> isClientPlayerConnected = true);
        }
    }

    private final class SendRolesState extends State {
        SendRolesState(@NonNull final WritableByteChannel client) {
            super(() -> sendRoles(client));
        }
    }

    private final class SendTurnState extends State {
        SendTurnState(@NonNull final WritableByteChannel client) {
            super(() -> sendTurn(client));
        }
    }

    private final class SecondPlayerIsMovedState extends State {
        SecondPlayerIsMovedState() {
            // TODO: Second player is moved
            super(() -> {});
        }
    }

    public final void createServerSocket() throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(HOST_NAME, PORT));
        server.configureBlocking(false);

        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    @NonNull
    @Override
    public final IBinder onBind(@Nullable final Intent intent) { return iBinder; }

    @Override
    public final void onCreate() {
        super.onCreate();

        registerCreateGameReceiver();
        registerCancelGameReceiver();
        registerFirstPlayerMovedReceiver();
        registerFirstPlayerDisconnectedReceiver();
        registerKillReceiver();

        try {
            createServerSocket();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final int onStartCommand(
            @NonNull final Intent intent,
            final int flags,
            final int startId
    ) {
        try {
            runServer();
        } catch (final IOException e) {
            e.printStackTrace();
        }

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
    private final Intent registerFirstPlayerMovedReceiver() {
        return registerReceiver(
                firstPlayerMovedReceiver,
                new IntentFilter(BROADCAST_FIRST_PLAYER_MOVED)
        );
    }

    @NonNull
    private final Intent registerFirstPlayerDisconnectedReceiver() {
        return registerReceiver(
                firstPlayerMovedReceiver,
                new IntentFilter(BROADCAST_FIRST_PLAYER_DISCONNECTED)
        );
    }

    @NonNull
    private final Intent registerKillReceiver() {
        return registerReceiver(killReceiver, new IntentFilter(BROADCAST_KILL));
    }

    final void unregisterReceivers() {
        unregisterReceiver(createGameReceiver);
        unregisterReceiver(cancelGameReceiver);
        unregisterReceiver(firstPlayerMovedReceiver);
        unregisterReceiver(firstPlayerDisconnectedReceiver);
        unregisterReceiver(killReceiver);
    }

    private final void runServer() throws IOException {
        final State secondPlayerIsFoundState = new SecondPlayerIsFoundState();
        final State secondPlayerIsMovedState = new SecondPlayerIsMovedState();

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

                        switch (readerBuffer.get()) {
                            case PLAYER_IS_FOUND:
                                new SendRolesState(client).run();
                                new SendTurnState(client).run();
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

    final boolean checkMovement(final int player, @NonNull final Coordinate coordinate) {
        if (turn != player) return false;

        if (coordinate.getX() < 0 ||
                coordinate.getY() < 0 ||
                coordinate.getX() >= gameTableSize ||
                coordinate.getY() >= gameTableSize
        ) return false;

        return gameTable[coordinate.getY()][coordinate.getX()] != 0;
    }

    final void sendNoPlayerFound() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(new Intent(BROADCAST_NO_PLAYER_FOUND));
    }

    final void sendRoles(@NonNull final WritableByteChannel client) {
        final byte serverPlayerRole = (byte) new SecureRandom().nextInt(2);
        final byte clientPlayerRole = (byte) (1 - serverPlayerRole);

        final ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put(ClientPlayer.SHOW_ROLE_COMMAND);
        buffer.put(clientPlayerRole);
        buffer.flip();

        try { client.write(buffer); }
        catch (final IOException e) { e.printStackTrace(); }

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_ROLE)
                                .putExtra(BROADCAST_GET_ROLE, serverPlayerRole)
                );
    }

    final void sendTurn(@NonNull final WritableByteChannel client) {
        final ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put(ClientPlayer.TURN_COMMAND);
        buffer.put(turn);
        buffer.flip();

        try { client.write(buffer); }
        catch (final IOException e) { e.printStackTrace(); }

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_TURN)
                                .putExtra(BROADCAST_GET_TURN, turn)
                );
    }

    final void sendUpdatedTable(@NonNull final WritableByteChannel client) {
        final ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put(ClientPlayer.UPDATE_TABLE_COMMAND);

        // Damn, streams requires API 24 :(
        for (final byte[] r : gameTable)
            for (final byte b : r)
                buffer.put(b);

        try { client.write(buffer); }
        catch (final IOException e) { e.printStackTrace(); }

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_UPDATE_TABLE)
                        .putExtra(BROADCAST_GET_UPDATE_TABLE, gameTable)
                );
    }
}