package com.dinaraparanid.tictactoe;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.utils.polymorphism.State;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public final class ClientPlayer {
    private static final int PORT = 1337;
    private static final byte NO_COMMAND = 0;
    static final byte SHOW_ROLE_COMMAND = 1;
    static final byte INCORRECT_MOVE_COMMAND = 2;
    static final byte FIRST_PLAYER_MOVED_COMMAND = 3;
    static final byte COMMAND_GAME_FINISH = -1;

    @NonNull
    private final SocketChannel client;

    @NonNull
    private final State[] states = {
            new ShowRoleState(),
            new IncorrectMoveState(),
            new FirstPlayerMovedState(),
            new GameFinishedState()
    };

    public ClientPlayer() throws IOException {
        client = SocketChannel.open();
        client.connect(new InetSocketAddress("127.0.0.1", PORT));

        final ByteBuffer sayHelloBuffer = ByteBuffer.allocate(1);
        sayHelloBuffer.put(Server.PLAYER_IS_FOUND);
        sayHelloBuffer.flip();
        client.write(sayHelloBuffer);
        runBFSM();
    }

    private class ShowRoleState extends State {
        ShowRoleState() {
            super(() -> {}); // TODO: Show role
        }
    }

    private class IncorrectMoveState extends State {
        IncorrectMoveState() {
            super(() -> {}); // TODO: Say that move was wrong
        }
    }

    private class FirstPlayerMovedState extends State {
        FirstPlayerMovedState() {
            super(() -> {}); // TODO: Redraw table, start move
        }
    }

    private class GameFinishedState extends State {
        GameFinishedState() {
            super(() -> {}); // TODO: Show game results
        }
    }

    private final void runBFSM() throws IOException {
        byte command = NO_COMMAND;

        while (command != COMMAND_GAME_FINISH) {
            final ByteBuffer readBuffer = ByteBuffer.allocate(1);
            client.read(readBuffer);
            readBuffer.flip();

            command = readBuffer.get();
            states[command].run();
        }

        client.close();
    }
}
