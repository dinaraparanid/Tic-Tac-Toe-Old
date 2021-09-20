package com.dinaraparanid.tictactoe.native_libs;

public final class ServerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    private long ptr = 0;

    private ServerNative() {}

    public static final ServerNative create(final String ip) {
        final ServerNative server = new ServerNative();
        server.ptr = init(ip);
        return server.ptr == 0 ? null : server;
    }

    private static final native long init(final String ip);

    public final native byte[] readMove();

    public final native void runBFSM();

    public final native void sendCorrectMove(final byte[][] table);

    public final native void sendInvalidMove();

    public final native void sendGameFinished();

    public final native void sendRole(final byte clientPlayerRole);
}
