package com.dinaraparanid.tictactoe.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.dinaraparanid.tictactoe.BR;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.databinding.FragmentGameBinding;
import com.dinaraparanid.tictactoe.utils.polymorphism.DataBindingFragment;
import com.dinaraparanid.tictactoe.utils.polymorphism.Player;
import com.dinaraparanid.tictactoe.viewmodels.GameFragmentViewModel;

import org.jetbrains.annotations.NonNls;

public final class GameFragment extends DataBindingFragment<FragmentGameBinding> {

    @NonNls
    @NonNull
    private static final String PLAYER_KEY = "player";

    @NonNls
    @NonNull
    private static final String TABLE_KEY = "table";

    @NonNull
    private Player player;

    @NonNull
    private GameFragmentViewModel mvvmViewModel;

    @NonNull
    private AndroidXGameFragmentViewModel androidxViewModel;

    @NonNull
    public static final GameFragment newInstance(@NonNull final Player player) {
        final GameFragment gameFragment = new GameFragment();
        final Bundle args = new Bundle();

        args.putParcelable(PLAYER_KEY, player);
        gameFragment.setArguments(args);
        return gameFragment;
    }

    @Override
    public final void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        player = requireArguments().getParcelable(PLAYER_KEY);
    }

    @NonNull
    @Override
    public final View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_game,
                container,
                false
        );

        mvvmViewModel = new GameFragmentViewModel(player, new byte[3][3]);
        binding.setViewModel(mvvmViewModel);

        androidxViewModel = new ViewModelProvider(this).get(AndroidXGameFragmentViewModel.class);

        androidxViewModel.load(
                savedInstanceState == null ? null :
                (byte[][]) savedInstanceState.getSerializable(TABLE_KEY)
        );

        binding.getViewModel().updateGameTable(
                androidxViewModel.gameTableLiveData.getValue()
        );

        return binding.getRoot();
    }

    @Override
    public final void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putSerializable(TABLE_KEY, mvvmViewModel.gameTable);
        super.onSaveInstanceState(outState);
    }

    @Override
    public final void onResume() {
        super.onResume();
        player.initGame(this);
    }

    public final void updatePlayer() {
        mvvmViewModel.notifyPropertyChanged(BR.player);
    }

    public final void updateTable(@NonNull final byte[][] gameTable) {
        mvvmViewModel.updateGameTable(gameTable);
    }

    public final void showInvalidMove() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.invalid_move)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public final void gameFinished() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
