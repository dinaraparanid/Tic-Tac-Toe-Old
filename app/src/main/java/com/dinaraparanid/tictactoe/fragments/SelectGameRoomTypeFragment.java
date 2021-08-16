package com.dinaraparanid.tictactoe.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.databinding.FragmentSelectGameRoomTypeBinding;
import com.dinaraparanid.tictactoe.utils.polymorphism.DataBindingFragment;
import com.dinaraparanid.tictactoe.viewmodels.SelectGameRoomViewModel;

public final class SelectGameRoomTypeFragment
        extends DataBindingFragment<FragmentSelectGameRoomTypeBinding> {

    @NonNull
    public static final SelectGameRoomTypeFragment newInstance() {
        return new SelectGameRoomTypeFragment();
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_select_game_room_type, container, false);

        binding.setSelectGameRoomViewModel(new SelectGameRoomViewModel());

        return binding.getRoot();
    }
}
