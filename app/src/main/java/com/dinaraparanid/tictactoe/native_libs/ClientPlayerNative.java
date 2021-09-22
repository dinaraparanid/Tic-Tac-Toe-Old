package com.dinaraparanid.tictactoe.native_libs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

public final class ClientPlayerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    private ByteBuffer ptr = null;

    private ClientPlayerNative() {}

    @Nullable
    public static final ClientPlayerNative create(@NonNull final String ip) {
        final ClientPlayerNative player = new ClientPlayerNative();
        player.ptr = init(ip);
        return player.ptr == null ? null : player;
    }

    @Nullable
    private static final native ByteBuffer init(@NonNull final String ip);

    @Nullable
    public final native String sendReady();

    @Nullable
    public final native String sendMove(final byte y, final  byte x);

    @NonNull
    public final native byte[][] readTable();

    public final native byte readCommand();
    public final native byte readRole();
    public final native void drop();
}
