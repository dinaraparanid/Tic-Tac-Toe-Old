package com.dinaraparanid.tictactoe.native_libs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

public final class ServerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    private ByteBuffer ptr = null;

    private ServerNative() {}

    @Nullable
    public static final ServerNative create(@NonNull final String ip) {
        final ServerNative server = new ServerNative();
        server.ptr = init(ip);
        return server.ptr == null ? null : server;
    }

    @Nullable
    private static final native ByteBuffer init(@NonNull final String ip);

    @NonNull
    public final native byte[] readMove();

    public final native void runBFSM();

    @Nullable
    public final native String sendCorrectMove(@NonNull final byte[][] table);

    @Nullable
    public final native String sendInvalidMove();

    @Nullable
    public final native String sendGameFinished();

    @Nullable
    public final native String sendRole(final byte clientPlayerRole);
}
