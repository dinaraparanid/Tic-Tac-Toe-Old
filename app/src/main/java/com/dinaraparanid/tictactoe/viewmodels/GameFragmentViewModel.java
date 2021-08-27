package com.dinaraparanid.tictactoe.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.dinaraparanid.tictactoe.BR;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.Server;
import com.dinaraparanid.tictactoe.utils.polymorphism.Player;

import org.jetbrains.annotations.Contract;

import java.util.Arrays;

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

    @NonNull
    @Contract(pure = true)
    public final String getTimeLeft() { return Byte.toString(timeLeft); }

    @Contract(pure = true)
    public final int getButtonImage(final int buttonNumber) {
        final int y = buttonNumber / Server.gameTableSize;
        final int x = buttonNumber % Server.gameTableSize;

        if (gameTable[y][x] == 0)
            return android.R.color.transparent;

        return player.getNumber() == gameTable[y][x] ?
                player.getRole() == 0 ? R.drawable.cross : R.drawable.zero :
                player.getRole() == 0 ? R.drawable.zero : R.drawable.cross;
    }

    private final void updateTimeLeft() {
        timeLeft = timeLeft - 1 < 0 ? MAX_TIME : (byte) (timeLeft - 1);
        notifyPropertyChanged(BR.timeLeft);
    }

    public final void updateGameTable(@NonNull final byte[][] gameTab) {
        System.arraycopy(gameTab, 0, gameTable, 0, gameTab.length);

        final StringBuilder builder = new StringBuilder();

        for (final byte[] row : gameTable)
            builder.append(Arrays.toString(row) + " | ");

        Log.d("TEST", "New table: " + builder);

        notifyPropertyChanged(BR._all);
    }
}
