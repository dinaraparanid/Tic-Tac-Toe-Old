package com.dinaraparanid.tictactoe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.utils.Coordinate;
import com.dinaraparanid.tictactoe.utils.polymorphism.Player;

public final class ServerPlayer extends Player {

    static final String COORDINATE_KEY = "coordinate_key";

    @NonNull
    final MainApplication application;

    @NonNull
    final MainActivity activity;

    byte role;

    public ServerPlayer(
            @NonNull final MainApplication application,
            @NonNull final MainActivity activity
    ) {
        this.application = application;
        this.activity = activity;
        sendReady();
    }

    @NonNull
    private final BroadcastReceiver noPlayerFoundReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_NO_PLAYER_FOUND)) {
                activity.sendBroadcast(new Intent(Server.BROADCAST_CANCEL_GAME));
                // TODO: Tell that player is not found
            }
        }
    };

    @NonNull
    private final BroadcastReceiver playerFoundReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_PLAYER_FOUND)) {
                // TODO: Show that game is started
            }
        }
    };

    @NonNull
    private final BroadcastReceiver getRoleReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_ROLE)) {
                role = intent.getByteExtra(Server.BROADCAST_GET_ROLE, (byte) 0);
                showRole(activity);
            }
        }
    };

    @NonNull
    private final BroadcastReceiver secondPlayerMovedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_SECOND_PLAYER_MOVED)) {
                // TODO: Update table, start move
            }
        }
    };

    @NonNull
    private final BroadcastReceiver gameFinishedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_GAME_FINISHED)) {
                // TODO: Show results, kill server
            }
        }
    };

    @NonNull
    private final Intent registerNoPlayerFoundReceiver() {
        return activity.registerReceiver(
                noPlayerFoundReceiver,
                new IntentFilter(Server.BROADCAST_NO_PLAYER_FOUND)
        );
    }

    @NonNull
    private final Intent registerPlayerFoundReceiver() {
        return activity.registerReceiver(
                playerFoundReceiver,
                new IntentFilter(Server.BROADCAST_PLAYER_FOUND)
        );
    }

    @NonNull
    private final Intent registerSecondPlayerMovedReceiver() {
        return activity.registerReceiver(
                secondPlayerMovedReceiver,
                new IntentFilter(Server.BROADCAST_SECOND_PLAYER_MOVED)
        );
    }

    @NonNull
    private final Intent registerGameFinishedReceiver() {
        return activity.registerReceiver(
                gameFinishedReceiver,
                new IntentFilter(Server.BROADCAST_GAME_FINISHED)
        );
    }

    final void registerReceivers() {
        registerNoPlayerFoundReceiver();
        registerPlayerFoundReceiver();
        registerSecondPlayerMovedReceiver();
        registerGameFinishedReceiver();
    }

    final void unregisterReceivers() {
        activity.unregisterReceiver(noPlayerFoundReceiver);
        activity.unregisterReceiver(playerFoundReceiver);
        activity.unregisterReceiver(secondPlayerMovedReceiver);
        activity.unregisterReceiver(gameFinishedReceiver);
    }

    private final void sendReady() {
        if (application.serviceBound) {
            application.sendBroadcast(new Intent(Server.BROADCAST_CREATE_GAME));
        } else {
            final Intent runServerIntent = new Intent(application, Server.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                application.startForegroundService(runServerIntent);
            else
                application.startService(runServerIntent);

            application.bindService(
                    runServerIntent,
                    application.serviceConnection,
                    Context.BIND_AUTO_CREATE
            );
        }
    }

    private final void sendCreateGame() {
        application.sendBroadcast(new Intent(Server.BROADCAST_CREATE_GAME));
    }

    private final void sendCancelGame() {
        application.sendBroadcast(new Intent(Server.BROADCAST_CANCEL_GAME));
    }

    private final void sendPlayerMoved(final int x, final int y) {
        application.sendBroadcast(
                new Intent(Server.BROADCAST_FIRST_PLAYER_MOVED)
                        .putExtra(COORDINATE_KEY, new Coordinate(x, y))
        );
    }

    private final void sendPlayerDisconnected() {
        application.sendBroadcast(new Intent(Server.BROADCAST_FIRST_PLAYER_DISCONNECTED));
    }
}