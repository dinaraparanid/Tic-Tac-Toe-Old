package com.dinaraparanid.tictactoe.viewmodels;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.ClientPlayer;
import com.dinaraparanid.tictactoe.MainActivity;
import com.dinaraparanid.tictactoe.MainApplication;
import com.dinaraparanid.tictactoe.ServerPlayer;

import java.io.IOException;

public final class SelectGameRoomViewModel {

    @NonNull
    private final MainActivity activity;

    @NonNull
    private final MainApplication application;

    public SelectGameRoomViewModel(
            @NonNull final MainActivity activity,
            @NonNull final MainApplication application
    ) {
        this.activity = activity;
        this.application = application;
    }

    public final void startNewGame() {
        final ServerPlayer player = new ServerPlayer(application, activity);
        player.registerReceivers();
        player.sendReady();
    }

    public final void connectToGame() throws IOException {
        new ClientPlayer(activity).sendReady();
    }
}
