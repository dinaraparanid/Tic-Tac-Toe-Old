package com.dinaraparanid.tictactoe.viewmodels;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.ClientPlayer;
import com.dinaraparanid.tictactoe.MainActivity;
import com.dinaraparanid.tictactoe.MainApplication;
import com.dinaraparanid.tictactoe.ServerPlayer;

import org.jetbrains.annotations.Contract;

public final class SelectGameRoomViewModel {

    @NonNull
    private final MainActivity activity;

    public SelectGameRoomViewModel(@NonNull final MainActivity activity) {
        this.activity = activity;
    }

    public final void startNewGame() {
        new ServerPlayer(activity).sendReady();
    }

    public final void connectToGame() {
        new ClientPlayer(activity).sendReady();
    }
}
