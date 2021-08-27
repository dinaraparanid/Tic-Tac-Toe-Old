package com.dinaraparanid.tictactoe.utils;

import org.jetbrains.annotations.Contract;

import java.io.Serializable;

public final class Coordinate implements Serializable {
    private final int x;
    private final int y;

    public Coordinate(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    @Contract(pure = true)
    public final int getX() { return x; }

    @Contract(pure = true)
    public final int getY() { return y; }
}
