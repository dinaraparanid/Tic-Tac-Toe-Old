package com.dinaraparanid.tictactoe;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.utils.Coordinate;

public final class ServerPlayer {

    public static final String COORDINATE_KEY = "coordinate_key";

    private final MainApplication application;

    public ServerPlayer(@NonNull final MainApplication application) {
        this.application = application;
        sendReady();
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