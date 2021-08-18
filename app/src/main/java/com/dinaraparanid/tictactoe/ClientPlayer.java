package com.dinaraparanid.tictactoe;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.utils.polymorphism.Player;
import com.dinaraparanid.tictactoe.utils.polymorphism.State;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public final class ClientPlayer extends Player {
    private static final int PORT = 1337;
    private static final byte NO_COMMAND = -1;
    static final byte SHOW_ROLE_COMMAND = 0;
    static final byte TURN_COMMAND = 1;
    static final byte INCORRECT_MOVE_COMMAND = 2;
    static final byte FIRST_PLAYER_MOVED_COMMAND = 3;
    static final byte COMMAND_GAME_FINISH = 4;

    @NonNull
    private final SocketChannel client;

    private byte role;

    @NonNull
    private final State[] states = {
            new ShowRoleState(),
            new GetTurnState(),
            new IncorrectMoveState(),
            new FirstPlayerMovedState(),
            new GameFinishedState()
    };

    public ClientPlayer(@NonNull final MainActivity activity) throws IOException {
        this.activity = activity;
        client = SocketChannel.open();
        client.connect(new InetSocketAddress("127.0.0.1", PORT));

        sendReady();
        startCycle();
    }

    private class ShowRoleState extends State {
        ShowRoleState() {
            super(() -> {
                setRole();
                showRole(activity);
                startGame();
            });
        }
    }

    private class GetTurnState extends State {
        GetTurnState() {
            super(() -> {}); // TODO: change label on R.string.your_turn, start move if needed
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

    @Override
    public final void sendReady() {
        final ByteBuffer sayHelloBuffer = ByteBuffer.allocate(1);
        sayHelloBuffer.put(Server.PLAYER_IS_FOUND);
        sayHelloBuffer.flip();

        try { client.write(sayHelloBuffer); }
        catch (final IOException e) { e.printStackTrace(); }
    }

    private final void startCycle() {
        byte command = NO_COMMAND;

        try {
            while (command != COMMAND_GAME_FINISH) {
                final ByteBuffer readBuffer = ByteBuffer.allocate(1);
                client.read(readBuffer);
                readBuffer.flip();

                command = readBuffer.get();
                states[command].run();
            }

            client.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    final void setRole() {
        final ByteBuffer readBuffer = ByteBuffer.allocate(1);

        try { client.read(readBuffer); }
        catch (final IOException e) { e.printStackTrace(); }

        readBuffer.flip();
        role = readBuffer.get();
    }
}
