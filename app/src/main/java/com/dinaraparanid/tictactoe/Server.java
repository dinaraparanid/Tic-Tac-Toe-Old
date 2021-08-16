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
import java.util.Iterator;

public final class Server extends Service {
    private static final int PORT = 1337;
    private static final String HOST_NAME = "127.0.0.1";

    @NonNull
    private final State[] states = {
            new SecondPlayerIsFoundState(),
            new SecondPlayerIsMovedState()
    };

    private final int deskSize = 3;
    byte[][] desk = new byte[deskSize][deskSize]; // 0 -> null, 1 -> x, 2 -> 0
    int turn = 0;

    // -------------------------------- Receive broadcasts --------------------------------

    @NonNls
    static final String BROADCAST_CREATE_GAME = "create_game";

    @NonNls
    static final String BROADCAST_CANCEL_GAME = "cancel_game";

    @NonNls
    static final String BROADCAST_FIRST_PLAYER_MOVED = "first_player_moved";

    @NonNls
    static final String BROADCAST_FIRST_PLAYER_DISCONNECTED = "first_player_disconnected";

    @NonNls
    static final String BROADCAST_KILL = "kill";

    // -------------------------------- Send broadcasts --------------------------------

    @NonNls
    static final String BROADCAST_NO_PLAYER_FOUND = "no_player_found";

    @NonNls
    static final String BROADCAST_PLAYER_FOUND = "player_found";

    @NonNls
    static final String BROADCAST_SECOND_PLAYER_MOVED = "second_player_moved";

    @NonNls
    static final String BROADCAST_GAME_FINISHED = "game_finished";

    // -------------------------------- ClientPlayer callbacks --------------------------------

    static final byte PLAYER_IS_FOUND = 0;

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
                desk[coordinate.getY()][coordinate.getX()] = 1;
                turn = (turn + 1) % 2;
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

    private class SecondPlayerIsFoundState extends State {
        SecondPlayerIsFoundState() {
            super(() -> isClientPlayerConnected = true);
        }
    }

    private class SecondPlayerIsMovedState extends State {
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
                        states[readerBuffer.get()].run();
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
                coordinate.getX() >= deskSize ||
                coordinate.getY() >= deskSize
        ) return false;

        return desk[coordinate.getY()][coordinate.getX()] != 0;
    }

    final void sendNoPlayerFound() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(new Intent(BROADCAST_NO_PLAYER_FOUND));
    }
}