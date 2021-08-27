package com.dinaraparanid.tictactoe.utils.polymorphism;

public interface Command<P, R> {
    R run(final P param);
}
