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

import com.dinaraparanid.tictactoe.utils.Coordinate;
import com.dinaraparanid.tictactoe.utils.polymorphism.State;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

public final class Server extends Service {
    private static final int PORT = 1337;
    private static final String HOST_NAME = "127.0.0.1";
    private static final State[] states = {};

    static final String BROADCAST_CREATE_GAME = "create_game";
    static final String BROADCAST_CANCEL_GAME = "cancel_game";
    static final String BROADCAST_FIRST_PLAYER_MOVED = "first_player_moved";
    static final String BROADCAST_FIRST_PLAYER_DISCONNECTED = "first_player_disconnected";

    final class LocalBinder extends Binder {
        @NonNull
        public final Server getServer() { return Server.this; }
    }

    @NonNull
    private final IBinder iBinder = new LocalBinder();

    @NonNull
    private final ServerSocketChannel server;

    @NonNull
    private final Selector selector;

    @NonNull
    private final BroadcastReceiver createGameReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
            // TODO: create game
        }
    };

    @NonNull
    private final BroadcastReceiver cancelGameReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
            // TODO: cancel game
        }
    };

    @NonNull
    private final BroadcastReceiver firstPlayerMovedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            final Coordinate coordinate = (Coordinate) intent
                    .getSerializableExtra(ServerPlayer.COORDINATE_KEY);

            if (checkMovement((byte) 0, coordinate)) {
                desk[coordinate.getY()][coordinate.getX()] = 1;
                turn = (byte) ((turn + 1) % 2);
            }

            // TODO: send info to players
        }
    };

    @NonNull
    private final BroadcastReceiver firstPlayerDisconnected = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
            // TODO: show to second player that first player is disconnected
        }
    };

    private final int deskSize = 3;
    byte[][] desk = new byte[deskSize][deskSize]; // 0 -> null, 1 -> x, 2 -> 0
    byte turn = 0;

    public Server() throws IOException {
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
        registerFirstPlayerMoved();
        registerFirstPlayerDisconnected();
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
        unregisterReceiver(createGameReceiver);
        unregisterReceiver(cancelGameReceiver);
        unregisterReceiver(firstPlayerMovedReceiver);
        unregisterReceiver(firstPlayerDisconnected);
    }

    @NonNull
    private final Intent registerCreateGameReceiver() {
        return registerReceiver(createGameReceiver, new IntentFilter(BROADCAST_CREATE_GAME));
    }

    @NonNull
    private final Intent registerCancelGameReceiver() {
        return registerReceiver(cancelGameReceiver, new IntentFilter(BROADCAST_CANCEL_GAME));
    }

    @NonNull
    private final Intent registerFirstPlayerMoved() {
        return registerReceiver(
                firstPlayerMovedReceiver,
                new IntentFilter(BROADCAST_FIRST_PLAYER_MOVED)
        );
    }

    @NonNull
    private final Intent registerFirstPlayerDisconnected() {
        return registerReceiver(
                firstPlayerMovedReceiver,
                new IntentFilter(BROADCAST_FIRST_PLAYER_DISCONNECTED)
        );
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
                    final ByteBuffer readerBuffer = ByteBuffer.allocate(4);
                    final SocketChannel client = (SocketChannel) key.channel();

                    if (client.read(readerBuffer) < 0) {
                        client.close();
                    } else {
                        readerBuffer.flip();
                        final int command = readerBuffer.getInt();
                    }
                }

                iter.remove();
            }
        }
    }

    final boolean checkMovement(final byte player, @NonNull final Coordinate coordinate) {
        if (turn != player) return false;

        if (coordinate.getX() < 0 ||
                coordinate.getY() < 0 ||
                coordinate.getX() >= deskSize ||
                coordinate.getY() >= deskSize
        ) return false;

        return desk[coordinate.getY()][coordinate.getX()] != 0;
    }
}