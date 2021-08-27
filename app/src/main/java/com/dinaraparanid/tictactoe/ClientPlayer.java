package com.dinaraparanid.tictactoe;

import android.util.Log;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.utils.polymorphism.Player;
import com.dinaraparanid.tictactoe.utils.polymorphism.State;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public final class ClientPlayer extends Player {

    @NonNls
    @NonNull
    private static final String TAG = "ClientPlayer";

    @NonNull
    SocketChannel client;

    @NonNull
    private final State[] states = {
            new ShowRoleState(),
            new CorrectMoveState(),
            new InvalidMoveState(),
            new GameFinishedState()
    };

    public ClientPlayer(@NonNull final MainActivity activity) {
        this.activity = activity;

        final Thread init = new Thread(new Runnable() {
            @Override
            public final void run() {
                try {
                    client = SocketChannel.open();

                    // TODO: dialog with host name
                    client.connect(new InetSocketAddress("192.168.1.127", Server.PORT));

                    new Thread(ClientPlayer.this::startCycle).start();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        });

        init.start();
        try { init.join(); }
        catch (final InterruptedException e) { e.printStackTrace(); }
    }

    private final class ShowRoleState extends State {
        ShowRoleState() {
            super(new Runnable() {
                @Override
                public final void run() {
                    Log.d(TAG, "Show role");

                    setRole();
                    activity.runOnUiThread(() -> showRole(activity));
                    initGame();
                    startGame();
                }
            });
        }
    }

    private final class CorrectMoveState extends State {
        CorrectMoveState() {
            super(new Runnable() {
                @Override
                public final void run() {
                    Log.d(TAG, "Correct move");

                    updateTurn();
                    gameFragment.get().updateTable(readTable());
                }
            });
        }
    }

    private final class InvalidMoveState extends State {
        InvalidMoveState() {
            super(new Runnable() {
                @Override
                public final void run() {
                    Log.d(TAG, "Invalid move");
                    gameFragment.get().showInvalidMove();
                }
            });
        }
    }

    private final class GameFinishedState extends State {
        GameFinishedState() {
            super(new Runnable() {
                @Override
                public final void run() {
                    Log.d(TAG, "Game Finished");
                    gameFragment.get().gameFinished();
                }
            });
        }
    }

    @Override
    public final void sendReady() {
        final Thread sendReady = new Thread(new Runnable() {
            @Override
            public final void run() {
                final ByteBuffer sayHelloBuffer = ByteBuffer.allocate(1);
                sayHelloBuffer.put(Server.PLAYER_IS_FOUND);
                sayHelloBuffer.flip();

                try { client.write(sayHelloBuffer); }
                catch (final IOException e) { e.printStackTrace(); }
                catch (final NullPointerException e) {
                    // TODO: No service running
                }
            }
        });

        sendReady.start();
        try { sendReady.join(); }
        catch (final InterruptedException e) { e.printStackTrace(); }
    }

    @Override
    public final void sendMove(final int y, final int x) {
        final Thread sendMove = new Thread(new Runnable() {
            @Override
            public final void run() {
                final ByteBuffer sendMoveBuffer = ByteBuffer.allocate(3);
                sendMoveBuffer.put(Server.PLAYER_MOVED);
                sendMoveBuffer.put(Server.PLAYER_MOVED_Y, (byte) y);
                sendMoveBuffer.put(Server.PLAYER_MOVED_X, (byte) x);
                sendMoveBuffer.flip();

                try { client.write(sendMoveBuffer); }
                catch (final IOException e) { e.printStackTrace(); }
            }
        });

        sendMove.start();
        try { sendMove.join(); }
        catch (final InterruptedException e) { e.printStackTrace(); }
    }

    protected final void startCycle() {
        byte command = -1;

        try {
            while (command != Server.COMMAND_GAME_FINISH) {
                final ByteBuffer readBuffer = ByteBuffer.allocate(1);

                if (client.read(readBuffer) < 0)
                    continue;

                readBuffer.flip();
                command = readBuffer.get();

                Log.d(TAG + " COMMAND", Integer.toString(command));

                states[command].run();
            }

            client.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    protected final void setRole() {
        final Thread setRole = new Thread(new Runnable() {
            @Override
            public void run() {
                final ByteBuffer readBuffer = ByteBuffer.allocate(1);

                try { client.read(readBuffer); }
                catch (final IOException e) { e.printStackTrace(); }

                readBuffer.flip();
                role = readBuffer.get();
            }
        });

        setRole.start();
        try { setRole.join(); }
        catch (final InterruptedException e) { e.printStackTrace(); }
    }

    @NonNull
    protected final byte[][] readTable() {
        final ByteBuffer buffer = ByteBuffer.allocate(9);

        try { client.read(buffer); }
        catch (final IOException e) { e.printStackTrace(); }

        buffer.flip();

        final byte[][] gameTable = new byte[Server.gameTableSize][Server.gameTableSize];

        for (int i = 0; i < Server.gameTableSize; i++)
            for (int q = 0; q < Server.gameTableSize; q++)
                gameTable[i][q] = buffer.get();

        return gameTable;
    }
}
