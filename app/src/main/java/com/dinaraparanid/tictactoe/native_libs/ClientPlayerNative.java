package com.dinaraparanid.tictactoe.native_libs;

public final class ClientPlayerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    private long ptr = 0;

    public static final ClientPlayerNative create(final String ip) {
        final ClientPlayerNative player = new ClientPlayerNative();
        player.ptr = init(ip);
        return player.ptr == 0 ? null : player;
    }

    private static final native long init(final String ip);
    public final native void sendReady();
    public final native void sendMove(final byte y, final  byte x);
    public final native byte readCommand();
    public final native byte readRole();
    public final native byte[][] readTable();
    public final native void drop();
}
