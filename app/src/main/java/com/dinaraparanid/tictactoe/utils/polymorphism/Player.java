package com.dinaraparanid.tictactoe.utils.polymorphism;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.R;

public abstract class Player {

    private byte role;

    protected final void showRole(@NonNull final Context context) {
        new AlertDialog.Builder(context)
                .setCancelable(true)
                .setMessage(role == 0 ? R.string.cross_role : R.string.zero_role)
                .show();
    }
}
