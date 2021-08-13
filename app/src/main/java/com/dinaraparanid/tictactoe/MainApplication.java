package com.dinaraparanid.tictactoe;

import android.app.Application;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;

public final class MainApplication extends Application {

    public final boolean isServiceBound() { return serviceBound; }

    boolean serviceBound = false;

    @NonNull
    public final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public final void onServiceConnected(
                @NonNull final ComponentName name,
                @NonNull final IBinder service
        ) { serviceBound = true; }

        @Override
        public final void onServiceDisconnected(@NonNull final ComponentName name) {
            serviceBound = false;
        }
    };
}
