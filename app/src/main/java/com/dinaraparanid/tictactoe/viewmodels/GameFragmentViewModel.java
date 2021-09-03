package com.dinaraparanid.tictactoe.viewmodels;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.dinaraparanid.tictactoe.BR;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.Server;
import com.dinaraparanid.tictactoe.utils.polymorphism.Player;

import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.Objects;

public class GameFragmentViewModel extends BaseObservable {

    private static final int MAX_TIME = 20;

    @NonNull
    @Bindable
    public final Player player;

    @NonNull
    @Bindable
    public byte[][] gameTable;

    @Bindable
    private byte timeLeft = MAX_TIME;

    public GameFragmentViewModel(@NonNull final Player player, @NonNull final byte[][] gameTable) {
        this.player = player;
        this.gameTable = new byte[gameTable.length][gameTable.length];
        System.arraycopy(gameTable, 0, this.gameTable, 0, gameTable.length);

        new Thread(() -> {
            while (player.isPlaying()) {
                updateTimeLeft();
                try { Thread.sleep(1000); }
                catch (final InterruptedException e) { break; }
            }
        }).start();
    }

    @NonNull
    @Contract(pure = true)
    public final String getTimeLeft() { return Byte.toString(timeLeft); }

    @NonNull
    @Contract(pure = true)
    public final Drawable getButtonImage(final int buttonNumber) {
        final int y = buttonNumber / Server.gameTableSize;
        final int x = buttonNumber % Server.gameTableSize;
        final Resources resources = Player.ApplicationAccessor.application.getResources();


        if (gameTable[y][x] == 0)
            return Objects.requireNonNull(ResourcesCompat.getDrawable(
                    resources,
                    android.R.color.transparent,
                    null
            ));

        final byte role = player.getRole();

        return Objects.requireNonNull(ResourcesCompat.getDrawable(
                resources,
                player.getNumber() == gameTable[y][x] ?
                        role == 0 ? R.drawable.cross : R.drawable.zero :
                        role == 0 ? R.drawable.zero : R.drawable.cross,
                null
        ));
    }

    private final void updateTimeLeft() {
        timeLeft = timeLeft - 1 < 0 ? MAX_TIME : (byte) (timeLeft - 1);
        notifyPropertyChanged(BR.timeLeft);
    }

    public final void updateGameTable(@NonNull final byte[][] gameTab) {
        System.arraycopy(gameTab, 0, gameTable, 0, gameTab.length);
        notifyPropertyChanged(BR._all);
    }
}
