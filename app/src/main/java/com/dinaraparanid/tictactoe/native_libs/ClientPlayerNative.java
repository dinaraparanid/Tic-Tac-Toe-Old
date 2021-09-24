package com.dinaraparanid.tictactoe.native_libs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

public final class ClientPlayerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    @Nullable
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
    private static final native String sendReady(@NonNull final ByteBuffer pointerBuffer);

    @Nullable
    private static final native String sendMove(
            @NonNull final ByteBuffer pointerBuffer,
            final byte y,
            final  byte x
    );

    @NonNull
    private static final native byte[][] readTable(@NonNull final ByteBuffer pointerBuffer);

    private static final native byte readCommand(@NonNull final ByteBuffer pointerBuffer);
    private static final native byte readRole(@NonNull final ByteBuffer pointerBuffer);
    private static final native void drop(@NonNull final ByteBuffer pointerBuffer);

    public final void sendReady() { sendReady(ptr); }

    public final void sendMove(final byte y, final  byte x) { sendMove(ptr, y, x); }

    @Nullable
    public final byte[][] readTable() { return readTable(ptr); }

    public final byte readCommand() { return readCommand(ptr); }
    public final byte readRole() { return readRole(ptr); }
    public final void drop() { drop(ptr); }
}
