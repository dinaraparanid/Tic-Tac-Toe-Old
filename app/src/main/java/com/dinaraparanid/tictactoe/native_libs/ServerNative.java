package com.dinaraparanid.tictactoe.native_libs;

public final class ServerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    private long ptr = 0;
}
