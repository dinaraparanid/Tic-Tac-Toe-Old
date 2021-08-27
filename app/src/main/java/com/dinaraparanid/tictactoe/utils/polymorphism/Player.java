package com.dinaraparanid.tictactoe.utils.polymorphism;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.MainActivity;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.fragments.GameFragment;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import java.io.Serializable;
import java.lang.ref.WeakReference;

public abstract class Player implements Serializable {

    @NonNls
    @NonNull
    private static final String LOCATION = "com.dinaraparanid.tictactoe.utils.polymorphism.Player";

    protected byte role;
    private byte turn = 0;

    @NonNull
    protected MainActivity activity;

    @NonNull
    protected WeakReference<GameFragment> gameFragment;

    public abstract void sendReady();

    public abstract void sendMove(final int y, final int x);

    protected final void showRole(@NonNull final Context context) {
        new AlertDialog.Builder(context)
                .setCancelable(true)
                .setMessage(role == 0 ? R.string.cross_role : R.string.zero_role)
                .show();
    }

    protected final void initGame() {
        gameFragment = new WeakReference<>(GameFragment.newInstance(this));
    }

    protected final void startGame() {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, gameFragment.get())
                .addToBackStack(null)
                .commit();
    }

    @Contract(pure = true)
    public final byte getRole() { return role; }

    @Contract(pure = true)
    public final boolean isMoving() { return role == turn; }

    protected final void updateTurn() {
        turn = (byte)(1 - turn);
        gameFragment.get().updatePlayer();
    }
}
