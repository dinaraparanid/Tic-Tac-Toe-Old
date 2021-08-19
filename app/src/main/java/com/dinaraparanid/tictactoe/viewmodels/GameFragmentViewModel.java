package com.dinaraparanid.tictactoe.viewmodels;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.dinaraparanid.tictactoe.BR;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.utils.polymorphism.Player;

import org.jetbrains.annotations.Contract;

public class GameFragmentViewModel extends BaseObservable {

    private static final int MAX_TIME = 20;

    @NonNull
    @Bindable
    public final Player player;

    @NonNull
    @Bindable
    private byte[][] gameTable;

    @Bindable
    private byte timeLeft = MAX_TIME;

    public GameFragmentViewModel(@NonNull final Player player, @NonNull final byte[][] gameTable) {
        this.player = player;
        this.gameTable = gameTable;

        new Thread(() -> {
            while (true) {
                updateTimeLeft();
                try { Thread.sleep(1000); }
                catch (final InterruptedException e) { break; }
            }
        }).start();
    }

    @Contract(pure = true)
    public final byte getTimeLeft() { return timeLeft; }

    @Contract(pure = true)
    public final int getButtonImage(final int buttonNumber) {
        final int y = buttonNumber / 3;
        final int x = buttonNumber % 3;

        switch (gameTable[y][x]) {
            case 0: return android.R.color.transparent;
            case 1: return R.drawable.cross;
            default: return R.drawable.zero;
        }
    }

    private final void updateTimeLeft() {
        timeLeft = timeLeft - 1 < 0 ? MAX_TIME : (byte) (timeLeft - 1);
        notifyPropertyChanged(BR.timeLeft);
    }

    public final void updateGameTable(@NonNull final byte[][] gameTab) {
        System.arraycopy(gameTab, 0, gameTable, 0, gameTab.length);
        notifyPropertyChanged(BR.gameTable);
    }

    public final void onButtonClicked(@NonNull final View view) {

    }
}
