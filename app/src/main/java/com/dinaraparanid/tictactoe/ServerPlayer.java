package com.dinaraparanid.tictactoe;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.utils.Coordinate;
import com.dinaraparanid.tictactoe.utils.polymorphism.Player;

import org.jetbrains.annotations.NonNls;

public final class ServerPlayer extends Player {

    @NonNls
    @NonNull
    private static final String TAG = "ServerPlayer";

    @NonNls
    @NonNull
    static final String COORDINATE_KEY = "coordinate_key";

    public ServerPlayer(@NonNull final MainActivity activity) {
        this.activity = activity;
        registerNoPlayerFoundReceiver();
        registerGetRoleReceiver();
        registerCorrectMoveReceiver();
        registerInvalidMoveReceiver();
        registerGameFinishedReceiver();
    }

    @NonNull
    private final BroadcastReceiver noPlayerFoundReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_NO_PLAYER_FOUND)) {

                Log.d(TAG, "No player found");

                sendCancelGame();

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

                Log.d(TAG, "Get role");

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

                Log.d(TAG, "Correct move");

                updateTurn();

                gameFragment.get().updateTable(
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
            if (intent.getAction().equals(Server.BROADCAST_INVALID_MOVE)) {
                Log.d(TAG, "Invalid move");
                gameFragment.get().showInvalidMove();
            }
        }
    };

    @NonNull
    private final BroadcastReceiver gameFinishedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_GAME_FINISHED)) {
                Log.d(TAG, "Game finished");
                gameFragment.get().gameFinished();
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
        final MainApplication app = (MainApplication) activity.getApplication();

        if (app.serviceBound) {
            sendCreateGame();
        } else {
            final Intent runServerIntent = new Intent(activity, Server.class);

            activity.startService(runServerIntent);

            activity.bindService(
                    runServerIntent,
                    app.serviceConnection,
                    Context.BIND_AUTO_CREATE
            );
        }
    }

    @Override
    public final void sendMove(final int y, final int x) {
        activity.sendBroadcast(
                new Intent(Server.BROADCAST_SERVER_PLAYER_MOVED)
                        .putExtra(COORDINATE_KEY, new Coordinate(x, y))
        );
    }

    private final void unregisterReceivers() {
        activity.unregisterReceiver(noPlayerFoundReceiver);
        activity.unregisterReceiver(getRoleReceiver);
        activity.unregisterReceiver(correctMoveReceiver);
        activity.unregisterReceiver(invalidMoveReceiver);
        activity.unregisterReceiver(gameFinishedReceiver);
    }

    private final void sendCreateGame() {
        activity.sendBroadcast(new Intent(Server.BROADCAST_CREATE_GAME));
    }

    protected final void sendCancelGame() {
        activity.sendBroadcast(new Intent(Server.BROADCAST_CANCEL_GAME));
        unregisterReceivers();
    }

    private final void sendPlayerDisconnected() {
        activity.sendBroadcast(new Intent(Server.BROADCAST_SERVER_PLAYER_DISCONNECTED));
    }
}