package com.dinaraparanid.tictactoe.fragments;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dinaraparanid.tictactoe.Server;

import org.jetbrains.annotations.Contract;

public final class AndroidXGameFragmentViewModel extends ViewModel {
    final MutableLiveData<byte[][]> gameTableLiveData = new MutableLiveData<>();

    @Contract(pure = true)
    final void load(@Nullable final byte[][] gameTable) {
        gameTableLiveData.setValue(
                gameTable == null ? new byte[Server.gameTableSize][Server.gameTableSize] : gameTable
        );
    }
}
