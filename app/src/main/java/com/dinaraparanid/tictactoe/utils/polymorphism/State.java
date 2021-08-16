package com.dinaraparanid.tictactoe.utils.polymorphism;

import androidx.annotation.NonNull;

public abstract class State {

    @NonNull
    protected Runnable action;

    protected State(@NonNull final Runnable action) { this.action = action; }

    public void run() { action.run(); }
}
