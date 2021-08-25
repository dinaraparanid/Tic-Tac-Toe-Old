package com.dinaraparanid.tictactoe.utils.polymorphism;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.MainActivity;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.fragments.GameFragment;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class Player implements Serializable {

    private static final long serialVersionUID = 3929149860943205098L;

    @NonNls
    @NonNull
    private static final String LOCATION = "com.dinaraparanid.tictactoe.utils.polymorphism.Player";

    protected byte role;
    private byte turn = 0;

    @NonNull
    protected MainActivity activity;

    @NonNull
    protected GameFragment gameFragment;

    public abstract void sendReady();

    public abstract void sendMove(final int y, final int x);

    protected final void showRole(@NonNull final Context context) {
        new AlertDialog.Builder(context)
                .setCancelable(true)
                .setMessage(role == 0 ? R.string.cross_role : R.string.zero_role)
                .show();
    }

    protected final void initGame() {
        gameFragment = GameFragment.newInstance(this);
    }

    protected final void startGame() {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, gameFragment)
                .addToBackStack(null)
                .commit();
    }

    @Contract(pure = true)
    public final byte getRole() {
        return role;
    }

    @Contract(pure = true)
    public final boolean isMoving() {
        return role == turn;
    }

    protected final void updateTurn() {
        turn = (byte)(1 - turn);
        gameFragment.updatePlayer();
    }

    @Contract("_ -> fail")
    private final void readObject(final ObjectInputStream in) throws
            ClassNotFoundException, NotSerializableException {
        throw new NotSerializableException(LOCATION);
    }

    @Contract("_ -> fail")
    private final void writeObject(final ObjectOutputStream out) throws NotSerializableException {
        throw new NotSerializableException(LOCATION);
    }
}
