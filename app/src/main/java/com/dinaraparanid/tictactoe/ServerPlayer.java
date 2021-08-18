package com.dinaraparanid.tictactoe;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.fragments.GameFragment;
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

                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                GameFragment.newInstance(ServerPlayer.this)
                        )
                        .addToBackStack(null)
                        .commit();
            }
        }
    };

    @NonNull
    private final BroadcastReceiver getTurnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_TURN)) {
                // TODO: change label on R.string.your_turn, start move if needed
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
    private final Intent registerGetRoleReceiver() {
        return activity.registerReceiver(
                getRoleReceiver,
                new IntentFilter(Server.BROADCAST_ROLE)
        );
    }

    @NonNull
    private final Intent registerGetTurnReceiver() {
        return activity.registerReceiver(
                getTurnReceiver,
                new IntentFilter(Server.BROADCAST_TURN)
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

    public final void registerReceivers() {
        registerNoPlayerFoundReceiver();
        registerGetRoleReceiver();
        registerGetTurnReceiver();
        registerSecondPlayerMovedReceiver();
        registerGameFinishedReceiver();
    }

    private final void unregisterReceivers() {
        activity.unregisterReceiver(noPlayerFoundReceiver);
        activity.unregisterReceiver(getRoleReceiver);
        activity.unregisterReceiver(getTurnReceiver);
        activity.unregisterReceiver(secondPlayerMovedReceiver);
        activity.unregisterReceiver(gameFinishedReceiver);
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

    private final void sendCreateGame() {
        application.sendBroadcast(new Intent(Server.BROADCAST_CREATE_GAME));
    }

    private final void sendCancelGame() {
        application.sendBroadcast(new Intent(Server.BROADCAST_CANCEL_GAME));
        unregisterReceivers();
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