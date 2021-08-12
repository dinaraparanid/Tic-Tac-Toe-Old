package com.dinaraparanid.tictactoe;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public final class Client {
    private static final int PORT = 1337;

    @NonNull
    private final SocketChannel client;

    public Client() throws IOException {
        client = SocketChannel.open();
        client.connect(new InetSocketAddress("127.0.0.1", PORT));
    }
}
