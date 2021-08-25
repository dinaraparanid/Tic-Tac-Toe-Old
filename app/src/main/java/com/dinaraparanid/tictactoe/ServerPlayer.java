package com.dinaraparanid.tictactoe;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.utils.Coordinate;
import com.dinaraparanid.tictactoe.utils.polymorphism.Player;

import org.jetbrains.annotations.NonNls;

public final class ServerPlayer extends Player {

    @NonNls
    @NonNull
    static final String COORDINATE_KEY = "coordinate_key";

    @NonNull
    final MainApplication application;

    public ServerPlayer(
            @NonNull final MainApplication application,
            @NonNull final MainActivity activity
    ) {
        this.application = application;
        this.activity = activity;
    }

    @NonNull
    private final BroadcastReceiver noPlayerFoundReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_NO_PLAYER_FOUND)) {
                activity.sendBroadcast(new Intent(Server.BROADCAST_CANCEL_GAME));

                new AlertDialog.Builder(activity)
                        .setMessage(R.string.player_not_found)
                        .setPositiveButton(R.string.ok, (dialog, which) ->
                            activity.getSupportFragmentManager().popBackStack()
                        )
                        .show();
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
                initGame();
                startGame();
            }
        }
    };

    @NonNull
    private final BroadcastReceiver correctMoveReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_CORRECT_MOVE)) {
                updateTurn();

                gameFragment.updateTable(
                        (byte[][]) intent.getSerializableExtra(
                                Server.BROADCAST_GET_UPDATE_TABLE
                        )
                );
            }
        }
    };

    @NonNull
    private final BroadcastReceiver invalidMoveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_INVALID_MOVE))
                gameFragment.showInvalidMove();
        }
    };

    @NonNull
    private final BroadcastReceiver gameFinishedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_GAME_FINISHED))
                gameFragment.gameFinished();
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
    private final Intent registerGetRoleReceiver() {
        return activity.registerReceiver(
                getRoleReceiver,
                new IntentFilter(Server.BROADCAST_ROLE)
        );
    }

    @NonNull
    private final Intent registerCorrectMoveReceiver() {
        return activity.registerReceiver(
                correctMoveReceiver,
                new IntentFilter(Server.BROADCAST_CORRECT_MOVE)
        );
    }

    @NonNull
    private final Intent registerInvalidMoveReceiver() {
        return activity.registerReceiver(
                invalidMoveReceiver,
                new IntentFilter(Server.BROADCAST_INVALID_MOVE)
        );
    }

    @NonNull
    private final Intent registerGameFinishedReceiver() {
        return activity.registerReceiver(
                gameFinishedReceiver,
                new IntentFilter(Server.BROADCAST_GAME_FINISHED)
        );
    }

    @Override
    public final void sendReady() {
        if (application.serviceBound) {
            sendCreateGame();
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

    @Override
    public final void sendMove(final int y, final int x) {
        application.sendBroadcast(
                new Intent(Server.BROADCAST_SERVER_PLAYER_MOVED)
                        .putExtra(COORDINATE_KEY, new Coordinate(x, y))
        );
    }

    public final void registerReceivers() {
        registerNoPlayerFoundReceiver();
        registerGetRoleReceiver();
        registerCorrectMoveReceiver();
        registerInvalidMoveReceiver();
        registerGameFinishedReceiver();
    }

    private final void unregisterReceivers() {
        activity.unregisterReceiver(noPlayerFoundReceiver);
        activity.unregisterReceiver(getRoleReceiver);
        activity.unregisterReceiver(correctMoveReceiver);
        activity.unregisterReceiver(invalidMoveReceiver);
        activity.unregisterReceiver(gameFinishedReceiver);
    }

    private final void sendCreateGame() {
        application.sendBroadcast(new Intent(Server.BROADCAST_CREATE_GAME));
    }

    private final void sendCancelGame() {
        application.sendBroadcast(new Intent(Server.BROADCAST_CANCEL_GAME));
        unregisterReceivers();
    }

    private final void sendPlayerDisconnected() {
        application.sendBroadcast(new Intent(Server.BROADCAST_SERVER_PLAYER_DISCONNECTED));
    }
}