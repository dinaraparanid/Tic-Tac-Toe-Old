package com.dinaraparanid.tictactoe;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dinaraparanid.tictactoe.ServerPlayer;
import com.dinaraparanid.tictactoe.nativelibs.ServerNative;
import com.dinaraparanid.tictactoe.utils.Coordinate;
import com.dinaraparanid.tictactoe.utils.polymorphism.State;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Server extends Service {

    /** Game states after a correct move */
    public enum GameState { CONTINUE, SERVER_VICTORY, CLIENT_VICTORY }

    @NonNls
    @NonNull
    private static final String TAG = "Server";

    public static final int gameTableSize = 3; // TODO: create customizable table

    @NonNull
    // 0 -> null, 1 -> server, 2 -> client
    protected byte[][] gameTable = new byte[gameTableSize][gameTableSize];

    @NonNull
    protected final AtomicReference<String> hostName = new AtomicReference<>();

    @NonNull
    protected final AtomicBoolean isGameEnded = new AtomicBoolean();

    @NonNull
    protected final ExecutorService executor = Executors.newCachedThreadPool();

    // -------------------------------- Receive broadcasts --------------------------------

    @NonNls
    @NonNull
    static final String BROADCAST_CREATE_GAME = "create_game";

    @NonNls
    @NonNull
    static final String BROADCAST_CANCEL_GAME = "cancel_game";

    @NonNls
    @NonNull
    static final String BROADCAST_SERVER_PLAYER_MOVED = "first_server_moved";

    @NonNls
    @NonNull
    static final String BROADCAST_SERVER_PLAYER_DISCONNECTED = "server_player_disconnected";

    // -------------------------------- Send broadcasts --------------------------------

    @NonNls
    @NonNull
    static final String BROADCAST_SHOW_IP = "show_ip";

    @NonNls
    @NonNull
    static final String BROADCAST_GET_IP = "get_ip";

    @NonNls
    @NonNull
    static final String BROADCAST_NO_PLAYER_FOUND = "no_player_found";

    @NonNls
    @NonNull
    static final String BROADCAST_ROLE = "role";

    @NonNls
    @NonNull
    static final String BROADCAST_GET_ROLE = "get_role";

    @NonNls
    @NonNull
    static final String BROADCAST_CORRECT_MOVE = "update_table";

    @NonNls
    @NonNull
    static final String BROADCAST_GET_UPDATE_TABLE = "get_update_table";

    @NonNls
    @NonNull
    static final String BROADCAST_INVALID_MOVE = "invalid_move";

    @NonNls
    @NonNull
    static final String BROADCAST_ENDING_STATE = "ending_state";

    @NonNls
    @NonNull
    static final String BROADCAST_GAME_FINISHED = "game_finished";

    private static final long NO_PLAYER_FOUND = Long.MAX_VALUE;

    @NonNull
    protected AtomicBoolean isClientPlayerConnected = new AtomicBoolean();

    protected final class LocalBinder extends Binder {
        @NonNull
        @Contract(pure = true)
        public final Server getServer() { return Server.this; }
    }

    @NonNull
    private final IBinder iBinder = new LocalBinder();

    @NonNull
    protected ServerNative serverNative;

    @NonNull
    private final BroadcastReceiver createGameReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(BROADCAST_CREATE_GAME)) {
                Log.d(TAG, "Create game");
                sendIp(hostName.get());

                // TODO: Incorrect server
                serverNative = Objects.requireNonNull(
                        ServerNative.create(
                                Server.this, hostName.get()
                        )
                );

                isGameEnded.set(false);
                isClientPlayerConnected.set(false);

                System.arraycopy(
                        new byte[gameTableSize][gameTableSize],
                        0,
                        gameTable,
                        0,
                        gameTable.length
                );

                new Thread(serverNative::runBFSM).start();

                executor.submit(() -> {
                    final Lock lock = new ReentrantLock();
                    final Condition noClientCondition = lock.newCondition();

                    lock.lock();

                    try {
                        long await = 0;

                        if(!isClientPlayerConnected.get())
                            await = noClientCondition.awaitNanos(NO_PLAYER_FOUND);

                        if (await <= 0)
                            sendNoPlayerFound();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                });
            }
        }
    };

    @NonNull
    private final BroadcastReceiver cancelGameReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(BROADCAST_CANCEL_GAME))
                Log.d(TAG, "Cancel game");
        }
    };

    @NonNull
    private final BroadcastReceiver serverPlayerMovedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            if (intent.getAction().equals(BROADCAST_SERVER_PLAYER_MOVED)) {
                Log.d(TAG, "Server player moved");

                final Coordinate coordinate = (Coordinate) intent
                        .getSerializableExtra(ServerPlayer.COORDINATE_KEY);

                if (isMoveCorrect(coordinate)) {
                    gameTable[coordinate.getY()][coordinate.getX()] = 1;

                    executor.execute(() -> {
                        final GameState gs = getGameState();

                        if (gs != GameState.CONTINUE) {
                            try {
                                sendGameFinished(gs);
                            } catch (final ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }

                            return;
                        }

                        try { sendCorrectMove(); }
                        catch (ExecutionException | InterruptedException ignored) {}

                        boolean isFilled = true;

                        for (final byte[] row : gameTable) {
                            for (final byte cell : row) {
                                if (cell == 0) {
                                    isFilled = false;
                                    break;
                                }
                            }
                        }

                        if (isFilled) {
                            isGameEnded.set(true);
                            try { sendGameFinished(GameState.CONTINUE); }
                            catch (final ExecutionException | InterruptedException ignored) {}
                        }
                    });
                } else {
                    sendServerPlayerInvalidMove();
                }
            }
        }
    };

    @NonNull
    private final BroadcastReceiver serverPlayerDisconnectedReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            // TODO: Show to second player that first player is disconnected
        }
    };

    private final class ClientPlayerIsFoundState extends State {
        ClientPlayerIsFoundState() {
            super(() -> {
                Log.d(TAG, "Client player connected");
                isClientPlayerConnected.set(true);
            });
        }
    }

    private final class SendRolesState extends State {
        SendRolesState() {
            super(() -> {
                Log.d(TAG, "Send roles");
                try {
                    sendRoles();
                } catch (final ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private final class ClientPlayerIsMovedState extends State {
        ClientPlayerIsMovedState() {
            super(() -> {
                Log.d(TAG, "Client player is moved");

                final byte[] buf = serverNative.readMove();
                final byte y = buf[0];
                final byte x = buf[1];

                if (isMoveCorrect(y, x)) {
                    gameTable[y][x] = 2;

                    final GameState gs = getGameState();

                    if (gs != GameState.CONTINUE) {
                        try {
                            sendGameFinished(gs);
                        } catch (final ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }

                        return;
                    }

                    try {
                        sendCorrectMove();
                    } catch (final ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    boolean isFilled = true;

                    for (final byte[] row : gameTable) {
                        for (final byte cell : row) {
                            if (cell == 0) {
                                isFilled = false;
                                break;
                            }
                        }
                    }

                    if (isFilled) {
                        isGameEnded.set(true);

                        try {
                            sendGameFinished(GameState.CONTINUE);
                        } catch (final ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        sendClientPlayerInvalidMove();
                    } catch (final ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @NonNull
    protected final Future<?> createServerSocketAsync() {
       return executor.submit(() -> {
            hostName.set(
                    Formatter.formatIpAddress(
                            ((WifiManager) getApplicationContext()
                                    .getSystemService(WIFI_SERVICE))
                                    .getConnectionInfo()
                                    .getIpAddress()
                    )
            );

            // TODO: incorrect server
            serverNative = Objects.requireNonNull(
                    ServerNative.create(this, hostName.get())
            );
            sendIp(hostName.get());
        });
    }

    @NonNull
    @Override
    @Contract(pure = true)
    public final IBinder onBind(@Nullable final Intent intent) { return iBinder; }

    @Override
    public final void onCreate() {
        super.onCreate();

        registerCreateGameReceiver();
        registerCancelGameReceiver();
        registerServerPlayerMovedReceiver();
        registerServerPlayerDisconnectedReceiver();

        try {
            createServerSocketAsync().get();
        } catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final int onStartCommand(
            @NonNull final Intent intent,
            final int flags,
            final int startId
    ) {
        new Thread(serverNative::runBFSM).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
    }

    private final void registerCreateGameReceiver() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        createGameReceiver,
                        new IntentFilter(BROADCAST_CREATE_GAME)
                );
    }

    private final void registerCancelGameReceiver() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        cancelGameReceiver,
                        new IntentFilter(BROADCAST_CANCEL_GAME)
                );
    }

    private final void registerServerPlayerMovedReceiver() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        serverPlayerMovedReceiver,
                        new IntentFilter(BROADCAST_SERVER_PLAYER_MOVED)
                );
    }

    private final void registerServerPlayerDisconnectedReceiver() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(
                        serverPlayerDisconnectedReceiver,
                        new IntentFilter(BROADCAST_SERVER_PLAYER_DISCONNECTED)
                );
    }

    protected final void unregisterReceivers() {
        final LocalBroadcastManager manager = LocalBroadcastManager
                .getInstance(getApplicationContext());

        manager.unregisterReceiver(createGameReceiver);
        manager.unregisterReceiver(cancelGameReceiver);
        manager.unregisterReceiver(serverPlayerMovedReceiver);
        manager.unregisterReceiver(serverPlayerDisconnectedReceiver);
    }

    public final void runClientPlayerIsFoundState() { new ClientPlayerIsFoundState().run(); }
    public final void runSendRolesState() { new SendRolesState().run(); }
    public final void runClientPlayerIsMovedState() { new ClientPlayerIsMovedState().run(); }

    protected final boolean isMoveCorrect(@NonNull final Coordinate coordinate) {
        return gameTable[coordinate.getY()][coordinate.getX()] == 0;
    }

    @Contract(pure = true)
    protected final boolean isMoveCorrect(final byte y, final byte x) {
        return gameTable[y][x] == 0;
    }

    protected final GameState getGameState() {
        final byte[][] rows = new byte[8][3];

        // vertical
        System.arraycopy(gameTable, 0, rows, 0, 3);

        // diagonal
        for (int i = 0; i < 3; i++) {
            rows[3][i] = gameTable[i][i];
            rows[4][i] = gameTable[i][2 - i];
        }

        // horizontal
        for (int i = 0; i < 3; i++)
            for (int q = 0; q < 3; q++)
                rows[5 + i][q] = gameTable[q][i];

        // Fucking Java 7, there are no streams...

        final byte[] serverVic = { 1, 1, 1 };
        final byte[] clientVic = { 2, 2, 2 };

        for (final byte[] row : rows) {
            if (Arrays.equals(serverVic, row))
                return GameState.SERVER_VICTORY;
            if (Arrays.equals(clientVic, row))
                return GameState.CLIENT_VICTORY;
        }

        return GameState.CONTINUE;
    }

    protected final void sendIp(@NonNull final String ip) {
        Log.d(TAG, "Send IP");

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_SHOW_IP)
                                .putExtra(BROADCAST_GET_IP, ip)
                );
    }

    protected final void sendNoPlayerFound() {
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(new Intent(BROADCAST_NO_PLAYER_FOUND));
    }

    protected final void sendRoles()
            throws ExecutionException, InterruptedException {
        final byte serverPlayerRole = (byte) new SecureRandom().nextInt(2);
        final byte clientPlayerRole = (byte) (1 - serverPlayerRole);

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_ROLE)
                                .putExtra(BROADCAST_GET_ROLE, serverPlayerRole)
                );

        executor.submit(() -> serverNative.sendRole(clientPlayerRole)).get();
    }

    protected final void sendCorrectMove()
            throws ExecutionException, InterruptedException {
        Log.d(TAG, "Send correct move");

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_CORRECT_MOVE)
                                .putExtra(BROADCAST_GET_UPDATE_TABLE, gameTable)
                );

        executor.submit(() -> serverNative.sendCorrectMove(gameTable)).get();
    }

    protected final void sendServerPlayerInvalidMove() {
        Log.d(TAG, "Send server invalid move");

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BROADCAST_INVALID_MOVE)
                );
    }

    protected void sendClientPlayerInvalidMove()
            throws ExecutionException, InterruptedException {
        Log.d(TAG, "Send client invalid move");
        executor.submit(serverNative::sendInvalidMove).get();
    }

    protected void sendGameFinished(@NonNull final GameState state)
            throws ExecutionException, InterruptedException {
        Log.d(TAG, "Game finished");

        final Intent finishIntent = new Intent(BROADCAST_GAME_FINISHED);
        finishIntent.putExtra(BROADCAST_ENDING_STATE, state.ordinal());

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(finishIntent);

        executor.submit(() -> serverNative.sendGameFinished((byte) state.ordinal())).get();
    }
}