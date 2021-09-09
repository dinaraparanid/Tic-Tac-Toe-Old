package com.dinaraparanid.tictactoe;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dinaraparanid.tictactoe.utils.polymorphism.Player;
import com.dinaraparanid.tictactoe.utils.polymorphism.State;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ClientPlayer extends Player {

    @NonNls
    @NonNull
    private static final String TAG = "ClientPlayer";

    public static final Parcelable.Creator<ClientPlayer> CREATOR = new Creator<ClientPlayer>() {
        @NonNull
        @Contract("_ -> new")
        @Override
        public final ClientPlayer createFromParcel(@NonNull final Parcel source) {
            return new ClientPlayer(
                    source.readByte(),
                    source.readByte(),
                    source.readString()
            );
        }

        @NonNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public ClientPlayer[] newArray(final int size) { return new ClientPlayer[0]; }
    };

    @NonNull
    protected ClientPlayerNative clientPlayerNative;

    @NonNull
    private String hostName;

    @NonNull
    private final State[] states = {
            new ShowRoleState(),
            new CorrectMoveState(),
            new InvalidMoveState(),
            new GameFinishedState()
    };

    @NonNull
    protected final ExecutorService executor = Executors.newCachedThreadPool();

    public ClientPlayer() {
        number = 2;

        new InputDialog.Builder()
                .setMessage(R.string.host_ip)
                .setOkAction(param -> {
                    hostName = param;

                    try {
                        establishConnection();
                    } catch (final ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    return null;
                })
                .build()
                .show(Player.ApplicationAccessor.activity.getSupportFragmentManager(), null);
    }

    public ClientPlayer(final byte role, final byte turn, @NonNull final String hostName) {
        number = 2;
        this.role = role;
        this.turn = turn;
        this.hostName = hostName;

        try {
            establishConnection();
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final class ShowRoleState extends State {
        ShowRoleState() {
            super(new Runnable() {
                @Override
                public final void run() {
                    Log.d(TAG, "Show role");

                    try {
                        setRole();
                    } catch (final ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    Player.ApplicationAccessor.activity.runOnUiThread(
                            () -> showRole(Player.ApplicationAccessor.activity)
                    );

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
                    gameFragment.get().updateTable(clientPlayerNative.readTable());
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
                    isPlaying.set(false);
                    clientPlayerNative.drop();
                    gameFragment.get().gameFinished();
                }
            });
        }
    }

    @Override
    public final void sendReady() {
        Log.d(TAG, "Send ready");

        try {
            executor.submit(() -> clientPlayerNative.sendReady()).get();
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void sendMove(final int y, final int x) {
        Log.d(TAG, "Send move");

        try {
            executor.submit(() -> clientPlayerNative.sendMove((byte) y, (byte) x)).get();
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return "ClientPlayer{" +
                "states=" + Arrays.toString(states) +
                '}';
    }

    protected final void startCycle() {
        byte command = -1;

        while (command != Server.COMMAND_GAME_FINISH) {
            command = clientPlayerNative.readCommand();
            Log.d(TAG + " COMMAND", Integer.toString(command));
            states[command].run();
        }
    }

    private final void establishConnection() throws ExecutionException, InterruptedException {
        executor.submit(() -> {
            final ClientPlayerNative player = ClientPlayerNative.create(hostName);

            if (player == null) {
                Toast.makeText(
                        ApplicationAccessor.activity,
                        R.string.invalid_ip,
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            clientPlayerNative = player;
            new Thread(this::startCycle).start();
            sendReady();
        }).get();
    }

    protected final void setRole() throws ExecutionException, InterruptedException {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                role = clientPlayerNative.readRole();
            }
        }).get();
    }
}
