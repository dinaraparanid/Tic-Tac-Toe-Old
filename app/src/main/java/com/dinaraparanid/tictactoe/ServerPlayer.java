package com.dinaraparanid.tictactoe;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dinaraparanid.tictactoe.utils.Coordinate;
import com.dinaraparanid.tictactoe.utils.polymorphism.Player;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

public final class ServerPlayer extends Player {

    public static final Parcelable.Creator<ServerPlayer> CREATOR = new Parcelable.Creator<ServerPlayer>() {
        @NonNull
        @Contract("_ -> new")
        @Override
        public final ServerPlayer createFromParcel(@NonNull final Parcel source) {
            return new ServerPlayer(
                    source.readByte(),
                    source.readByte(),
                    source.readString()
            );
        }

        @NonNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public ServerPlayer[] newArray(final int size) {
            return new ServerPlayer[0];
        }
    };

    @NonNls
    @NonNull
    private static final String TAG = "ServerPlayer";

    @NonNls
    @NonNull
    static final String COORDINATE_KEY = "coordinate_key";

    public ServerPlayer() {
        number = 1;
        registerReceivers();
    }

    public ServerPlayer(final byte role, final byte turn, @NonNull final String hostName) {
        number = 1;
        this.role = role;
        this.turn = turn;
        this.hostName = hostName;
        registerReceivers();
    }

    @NonNull
    private final BroadcastReceiver noPlayerFoundReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_NO_PLAYER_FOUND)) {

                Log.d(TAG, "No player found");

                sendCancelGame();

                new AlertDialog.Builder(Player.ApplicationAccessor.activity)
                        .setMessage(R.string.player_not_found)
                        .setPositiveButton(R.string.ok, (dialog, which) ->
                                Player.ApplicationAccessor.activity
                                        .getSupportFragmentManager()
                                        .popBackStack()
                        )
                        .show();
            }
        }
    };

    @NonNull
    private final BroadcastReceiver showIPReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(Server.BROADCAST_SHOW_IP)) {
                Log.d(TAG, "Show IP");

                new AlertDialog.Builder(Player.ApplicationAccessor.activity)
                        .setMessage(
                                String.format(
                                        "%s: %s",
                                        Player.ApplicationAccessor.activity
                                                .getResources()
                                                .getString(R.string.your_ip),
                                        intent.getStringExtra(Server.BROADCAST_GET_IP)
                                )
                        )
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                        .setCancelable(true)
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
                showRole(Player.ApplicationAccessor.activity);
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

                gameFragment.get().updateTable((byte[][]) intent.getSerializableExtra(
                        Server.BROADCAST_GET_UPDATE_TABLE
                ));
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

    private final void registerShowIpReceiver() {
        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.application.getApplicationContext())
                .registerReceiver(
                        showIPReceiver,
                        new IntentFilter(Server.BROADCAST_SHOW_IP)
                );
    }

    private final void registerNoPlayerFoundReceiver() {
        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.application.getApplicationContext())
                .registerReceiver(
                        noPlayerFoundReceiver,
                        new IntentFilter(Server.BROADCAST_NO_PLAYER_FOUND)
                );
    }

    private final void registerGetRoleReceiver() {
        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.application.getApplicationContext())
                .registerReceiver(
                        getRoleReceiver,
                        new IntentFilter(Server.BROADCAST_ROLE)
                );
    }

    private final void registerCorrectMoveReceiver() {
        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.application.getApplicationContext())
                .registerReceiver(
                        correctMoveReceiver,
                        new IntentFilter(Server.BROADCAST_CORRECT_MOVE)
                );
    }

    private final void registerInvalidMoveReceiver() {
        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.application.getApplicationContext())
                .registerReceiver(
                        invalidMoveReceiver,
                        new IntentFilter(Server.BROADCAST_INVALID_MOVE)
                );
    }

    private final void registerGameFinishedReceiver() {
        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.application.getApplicationContext())
                .registerReceiver(
                        gameFinishedReceiver,
                        new IntentFilter(Server.BROADCAST_GAME_FINISHED)
                );
    }

    protected final void registerReceivers() {
        registerShowIpReceiver();
        registerNoPlayerFoundReceiver();
        registerGetRoleReceiver();
        registerCorrectMoveReceiver();
        registerInvalidMoveReceiver();
        registerGameFinishedReceiver();
    }

    @Override
    public final void sendReady() {
        final MainApplication app =
                (MainApplication) Player.ApplicationAccessor.activity.getApplication();

        if (app.serviceBound) {
            sendCreateGame();
        } else {
            final Intent runServerIntent = new Intent(
                    Player.ApplicationAccessor.activity.getApplicationContext(),
                    Server.class
            );

            Player.ApplicationAccessor.activity
                    .getApplicationContext()
                    .startService(runServerIntent);

            Player.ApplicationAccessor.activity.getApplicationContext().bindService(
                    runServerIntent,
                    app.serviceConnection,
                    Context.BIND_AUTO_CREATE
            );
        }
    }

    @Override
    public final void sendMove(final int y, final int x) {
        Log.d(TAG, "Send move");

        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.activity.getApplicationContext())
                .sendBroadcast(
                        new Intent(Server.BROADCAST_SERVER_PLAYER_MOVED)
                                .putExtra(COORDINATE_KEY, new Coordinate(x, y))
                );
    }

    @NonNls
    @NonNull
    @Contract(pure = true)
    @Override
    public final String toString() {
        return "ServerPlayer{" +
                "noPlayerFoundReceiver=" + noPlayerFoundReceiver +
                ", showIPReceiver=" + showIPReceiver +
                ", getRoleReceiver=" + getRoleReceiver +
                ", correctMoveReceiver=" + correctMoveReceiver +
                ", invalidMoveReceiver=" + invalidMoveReceiver +
                ", gameFinishedReceiver=" + gameFinishedReceiver +
                '}';
    }

    private final void unregisterReceivers() {
        final LocalBroadcastManager manager = LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.application.getApplicationContext());

        manager.unregisterReceiver(showIPReceiver);
        manager.unregisterReceiver(noPlayerFoundReceiver);
        manager.unregisterReceiver(getRoleReceiver);
        manager.unregisterReceiver(correctMoveReceiver);
        manager.unregisterReceiver(invalidMoveReceiver);
        manager.unregisterReceiver(gameFinishedReceiver);
    }

    private final void sendCreateGame() {
        Log.d(TAG, "Send create game");

        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.activity.getApplicationContext())
                .sendBroadcast(new Intent(Server.BROADCAST_CREATE_GAME));
    }

    protected final void sendCancelGame() {
        Log.d(TAG, "Send cancel game");

        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.activity.getApplicationContext())
                .sendBroadcast(new Intent(Server.BROADCAST_CANCEL_GAME));
        unregisterReceivers();
    }

    private final void sendPlayerDisconnected() {
        Log.d(TAG, "Send player disconnected");

        LocalBroadcastManager
                .getInstance(Player.ApplicationAccessor.activity.getApplicationContext())
                .sendBroadcast(new Intent(Server.BROADCAST_SERVER_PLAYER_DISCONNECTED));
        unregisterReceivers();
    }
}