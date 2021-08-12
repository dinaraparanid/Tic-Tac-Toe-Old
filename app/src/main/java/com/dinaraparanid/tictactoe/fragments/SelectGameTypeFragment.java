package com.dinaraparanid.tictactoe.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.dinaraparanid.tictactoe.MainActivity;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.databinding.FragmentSelectGameTypeBinding;
import com.dinaraparanid.tictactoe.utils.polymorphism.DataBindingFragment;
import com.dinaraparanid.tictactoe.viewmodels.SelectGameTypeViewModel;

import org.jetbrains.annotations.Contract;

import carbon.widget.Button;

public final class SelectGameTypeFragment
        extends DataBindingFragment<FragmentSelectGameTypeBinding> {

    @Nullable
    private FragmentSelectGameTypeBinding binding;

    @NonNull
    @Contract(" -> new")
    public static final SelectGameTypeFragment newInstance() {
        return new SelectGameTypeFragment();
    }

    @NonNull
    @Override
    public final View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        binding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_select_game_type, container, false);

        binding.setSelectGameTypeViewModel(
                new SelectGameTypeViewModel((MainActivity) requireActivity())
        );

        return binding.getRoot();
    }
}
