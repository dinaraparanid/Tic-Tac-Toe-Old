package com.dinaraparanid.tictactoe;

import androidx.annotation.Nullable;

final class ClientPlayerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    private long ptr = 0;

    @Nullable
    static final ClientPlayerNative create(final String ip) {
        final ClientPlayerNative player = new ClientPlayerNative();
        player.ptr = init(ip);
        return player.ptr == 0 ? null : player;
    }

    private static final native long init(final String ip);
    final native void sendReady();
    final native void sendMove(final byte y, final  byte x);
    final native byte readCommand();
    final native byte readRole();
    final native byte[][] readTable();
    final native void drop();
}
