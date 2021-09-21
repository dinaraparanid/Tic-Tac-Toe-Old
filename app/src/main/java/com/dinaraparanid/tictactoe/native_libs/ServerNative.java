package com.dinaraparanid.tictactoe.native_libs;

import java.nio.ByteBuffer;

public final class ServerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    private ByteBuffer ptr = null;

    private ServerNative() {}

    public static final ServerNative create(final String ip) {
        final ServerNative server = new ServerNative();
        server.ptr = init(ip);
        return server.ptr == null ? null : server;
    }

    private static final native ByteBuffer init(final String ip);

    public final native byte[] readMove();

    public final native void runBFSM();

    public final native void sendCorrectMove(final byte[][] table);

    public final native void sendInvalidMove();

    public final native void sendGameFinished();

    public final native void sendRole(final byte clientPlayerRole);
}
