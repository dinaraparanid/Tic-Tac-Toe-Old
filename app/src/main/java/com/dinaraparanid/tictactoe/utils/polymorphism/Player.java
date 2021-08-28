package com.dinaraparanid.tictactoe.utils.polymorphism;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.MainActivity;
import com.dinaraparanid.tictactoe.MainApplication;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.fragments.GameFragment;

import org.jetbrains.annotations.Contract;

import java.lang.ref.WeakReference;

public abstract class Player implements Parcelable {

    protected byte role;
    protected byte turn = 0;
    public byte number;
    protected String hostName;

    @NonNull
    protected WeakReference<GameFragment> gameFragment;

    @Contract(pure = true)
    @Override
    public final int describeContents() { return 0; }

    @Override
    public final void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeByte(role);
        dest.writeByte(turn);
        dest.writeString(hostName);
    }

    @Contract(pure = true)
    public final byte getRole() { return role; }

    @Contract(pure = true)
    public final byte getNumber() { return number; }

    public abstract void sendReady();

    public abstract void sendMove(final int y, final int x);

    protected final void showRole(@NonNull final Context context) {
        new AlertDialog.Builder(context)
                .setCancelable(true)
                .setMessage(role == 0 ? R.string.cross_role : R.string.zero_role)
                .show();
    }

    protected final void startGame() {
        gameFragment = new WeakReference<>(GameFragment.newInstance(this));

        ApplicationAccessor.activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, gameFragment.get())
                .addToBackStack(null)
                .commit();
    }

    @Contract(pure = true)
    public final boolean isMoving() { return role == turn; }

    protected final void updateTurn() {
        turn = (byte)(1 - turn);
        gameFragment.get().updatePlayer();
    }

    public static final class ApplicationAccessor {
        @NonNull
        public static MainApplication application;

        @NonNull
        public static MainActivity activity;

        private ApplicationAccessor() {}
    }
}
