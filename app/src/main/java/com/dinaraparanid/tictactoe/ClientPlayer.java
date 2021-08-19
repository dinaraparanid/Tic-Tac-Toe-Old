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
    static final byte UPDATE_TABLE_COMMAND = 2;
    static final byte INCORRECT_MOVE_COMMAND = 3;
    static final byte FIRST_PLAYER_MOVED_COMMAND = 4;
    static final byte COMMAND_GAME_FINISH = 5;

    @NonNull
    final SocketChannel client;

    @NonNull
    private final State[] states = {
            new ShowRoleState(),
            new GetTurnState(),
            new UpdateTableState(),
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

    private final class ShowRoleState extends State {
        ShowRoleState() {
            super(() -> {
                setRole();
                showRole(activity);
                initGame();
                startGame();
            });
        }
    }

    private final class GetTurnState extends State {
        GetTurnState() {
            super(() -> gameFragment.updatePlayer());
        }
    }

    private final class UpdateTableState extends  State {
        UpdateTableState() {
            super(() -> {
                final ByteBuffer buffer = ByteBuffer.allocate(9);

                try { client.read(buffer); }
                catch (final IOException e) { e.printStackTrace(); }

                buffer.flip();

                final byte[][] gameTable = new byte[Server.gameTableSize][Server.gameTableSize];

                for (int i = 0; i < Server.gameTableSize; i++)
                    for (int q = 0; q < Server.gameTableSize; q++)
                        gameTable[i][q] = buffer.get();

                gameFragment.updateTable(gameTable);
            });
        }
    }

    private final class IncorrectMoveState extends State {
        IncorrectMoveState() {
            super(() -> {}); // TODO: Say that move was wrong
        }
    }

    private final class FirstPlayerMovedState extends State {
        FirstPlayerMovedState() {
            super(() -> {}); // TODO: Redraw table, start move
        }
    }

    private final class GameFinishedState extends State {
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

    @Override
    public final void sendMove(final int y, final int x) {
        final ByteBuffer sendMoveBuffer = ByteBuffer.allocate(3);
        sendMoveBuffer.put(Server.PLAYER_MOVED);
        sendMoveBuffer.put(Server.PLAYER_MOVED_Y, (byte) y);
        sendMoveBuffer.put(Server.PLAYER_MOVED_X, (byte) x);
        sendMoveBuffer.flip();

        try { client.write(sendMoveBuffer); }
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
