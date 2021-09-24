package com.dinaraparanid.tictactoe.native_libs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dinaraparanid.tictactoe.Server;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public final class ServerNative {

    static {
        System.loadLibrary("tictactoe");
    }

    @Nullable
    private ByteBuffer ptr;

    @NonNull
    private WeakReference<Server> server;

    private ServerNative() {}

    @Nullable
    public static final ServerNative create(
            @NonNull final Server server,
            @NonNull final String ip
    ) {
        final ServerNative serverNative = new ServerNative();
        serverNative.ptr = init(ip);

        if (serverNative.ptr == null)
            return null;

        serverNative.server = new WeakReference<>(server);
        return serverNative;
    }

    @Nullable
    private static final native ByteBuffer init(@NonNull final String ip);

    @NonNull
    private static final native byte[] readMove(@NonNull final ByteBuffer pointerBuffer);

    private final native void runBFSM(@NonNull final ByteBuffer pointerBuffer);

    private static final native void sendCorrectMove(
            @NonNull final ByteBuffer pointerBuffer,
            @NonNull final byte[][] table
    );

    private static final native void sendInvalidMove(@NonNull final ByteBuffer pointerBuffer);

    private static final native void sendGameFinished(@NonNull final ByteBuffer pointerBuffer);

    private static final native void sendRole(
            @NonNull final ByteBuffer pointerBuffer,
            final byte clientPlayerRole
    );

    @NonNull
    public final byte[] readMove() {
        return readMove(ptr);
    }

    public final void runBFSM() { runBFSM(ptr); }

    public final void sendCorrectMove(@NonNull final byte[][] table) {
        sendCorrectMove(ptr, table);
    }

    public final void sendInvalidMove() { sendInvalidMove(ptr); }

    public final void sendGameFinished() { sendGameFinished(ptr); }

    public final void sendRole(final byte clientPlayerRole) {
        sendRole(ptr, clientPlayerRole);
    }

    private final void runClientPlayerIsFoundState() {
        server.get().runClientPlayerIsFoundState();
    }

    private final void runSendRolesState() {
        server.get().runSendRolesState();
    }

    private final void runClientPlayerIsMovedState() {
        server.get().runClientPlayerIsMovedState();
    }
}
