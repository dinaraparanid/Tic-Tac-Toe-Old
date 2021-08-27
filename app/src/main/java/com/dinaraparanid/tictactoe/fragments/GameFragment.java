package com.dinaraparanid.tictactoe.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

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

    @NonNull
    private Player player;

    @NonNull
    private GameFragmentViewModel viewModel;

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

        viewModel = new GameFragmentViewModel(player, new byte[3][3]);
        binding.setViewModel(viewModel);

        return binding.getRoot();
    }

    public final void updatePlayer() {
        viewModel.notifyPropertyChanged(BR.player);
    }

    public final void updateTable(@NonNull final byte[][] gameTable) {
        viewModel.updateGameTable(gameTable);
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
