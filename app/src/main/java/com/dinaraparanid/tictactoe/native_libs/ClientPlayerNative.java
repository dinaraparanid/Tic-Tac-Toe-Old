package com.dinaraparanid.tictactoe.native_libs;

import java.nio.ByteBuffer;

public final class ClientPlayerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    private ByteBuffer ptr = null;

    private ClientPlayerNative() {}

    public static final ClientPlayerNative create(final String ip) {
        final ClientPlayerNative player = new ClientPlayerNative();
        player.ptr = init(ip);
        return player.ptr == null ? null : player;
    }

    private static final native ByteBuffer init(final String ip);
    public final native void sendReady();
    public final native void sendMove(final byte y, final  byte x);
    public final native byte readCommand();
    public final native byte readRole();
    public final native byte[][] readTable();
    public final native void drop();
}
