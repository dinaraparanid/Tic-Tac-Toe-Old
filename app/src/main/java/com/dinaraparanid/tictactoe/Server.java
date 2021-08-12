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

import com.dinaraparanid.tictactoe.utils.polymorphism.State;

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
    private static final State[] states = {};

    static final String BROADCAST_CREATE_ROOM = "create_room";
    static final String BROADCAST_CANCEL_GAME = "cancel_game";
    static final String BROADCAST_SECOND_PLAYER_CONNECTED = "second_player_connected";
    static final String BROADCAST_FIRST_PLAYER_MOVED = "first_player_moved";
    static final String BROADCAST_FIRST_PLAYER_DISCONNECTED = "first_player_disconnected";

    final class LocalBinder extends Binder {
        @NonNull
        public final Server getServer() {
            return Server.this;
        }
    }

    private final IBinder iBinder = new LocalBinder();

    @NonNull
    private final ServerSocketChannel server;

    @NonNull
    private final Selector selector;

    @NonNull
    private final BroadcastReceiver createRoomReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
            // TODO: create room
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
    private final BroadcastReceiver secondPlayerConnectedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
            // TODO: show 2-nd player connected
        }
    };

    @NonNull
    private final BroadcastReceiver firstPlayerMovedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
            // TODO: handle 1-st player move
        }
    };

    @NonNull
    private final BroadcastReceiver firstPlayerDisconnected = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
            // TODO: show to second player that first player is disconnected
        }
    };

    public Server() throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(HOST_NAME, PORT));
        server.configureBlocking(false);

        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    @NonNull
    @Override
    public final IBinder onBind(@Nullable final Intent intent) {
        return iBinder;
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        registerCreateRoomReceiver();
        registerCancelGameReceiver();
        registerSecondPlayerConnectedReceiver();
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
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(createRoomReceiver);
        unregisterReceiver(cancelGameReceiver);
        unregisterReceiver(secondPlayerConnectedReceiver);
        unregisterReceiver(firstPlayerMovedReceiver);
        unregisterReceiver(firstPlayerDisconnected);
    }

    @NonNull
    private final Intent registerCreateRoomReceiver() {
        return registerReceiver(createRoomReceiver, new IntentFilter(BROADCAST_CREATE_ROOM));
    }

    @NonNull
    private final Intent registerCancelGameReceiver() {
        return registerReceiver(cancelGameReceiver, new IntentFilter(BROADCAST_CANCEL_GAME));
    }

    @NonNull
    private final Intent registerSecondPlayerConnectedReceiver() {
        return registerReceiver(
                secondPlayerConnectedReceiver,
                new IntentFilter(BROADCAST_SECOND_PLAYER_CONNECTED)
        );
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
}
