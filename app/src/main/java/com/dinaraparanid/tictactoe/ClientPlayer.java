package com.dinaraparanid.tictactoe;

import android.content.Context;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.utils.polymorphism.Player;
import com.dinaraparanid.tictactoe.utils.polymorphism.State;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public final class ClientPlayer extends Player {
    private static final int PORT = 1337;
    private static final byte NO_COMMAND = 0;
    static final byte SHOW_ROLE_COMMAND = 1;
    static final byte INCORRECT_MOVE_COMMAND = 2;
    static final byte FIRST_PLAYER_MOVED_COMMAND = 3;
    static final byte COMMAND_GAME_FINISH = -1;

    @NonNull
    private final SocketChannel client;

    private byte role;

    @NonNull
    final Context context;

    @NonNull
    private final State[] states = {
            new ShowRoleState(),
            new IncorrectMoveState(),
            new FirstPlayerMovedState(),
            new GameFinishedState()
    };

    public ClientPlayer(@NonNull final Context context) throws IOException {
        this.context = context;
        client = SocketChannel.open();
        client.connect(new InetSocketAddress("127.0.0.1", PORT));

        final ByteBuffer sayHelloBuffer = ByteBuffer.allocate(1);
        sayHelloBuffer.put(Server.PLAYER_IS_FOUND);
        sayHelloBuffer.flip();
        client.write(sayHelloBuffer);
        run();
    }

    private class ShowRoleState extends State {
        ShowRoleState() {
            super(() -> {
                setRole();
                showRole(context);
            });
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

    private final void run() throws IOException {
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

    final void setRole() {
        final ByteBuffer readBuffer = ByteBuffer.allocate(1);

        try { client.read(readBuffer); }
        catch (final IOException e) { e.printStackTrace(); }

        readBuffer.flip();
        role = readBuffer.get();
    }
}
