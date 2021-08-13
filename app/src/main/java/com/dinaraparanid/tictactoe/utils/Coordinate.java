package com.dinaraparanid.tictactoe.utils;

import java.io.Serializable;

public final class Coordinate implements Serializable {
    private final int x;
    private final int y;

    public Coordinate(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    public final int getX() { return x; }

    public final int getY() { return y; }
}
