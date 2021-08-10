package com.dinaraparanid.tictactoe.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dinaraparanid.tictactoe.R;

import org.jetbrains.annotations.Contract;

import carbon.widget.Button;

public final class SelectGameTypeFragment extends Fragment {
    @NonNull
    public Button singlePlayer;

    @NonNull
    public Button multiPlayer;

    @NonNull
    @Contract(" -> new")
    public static final SelectGameTypeFragment newInstance() {
        return new SelectGameTypeFragment();
    }

    @Override
    public final void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public final View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        final View view = inflater.inflate(R.layout.fragment_select_game_type, container, false);
        singlePlayer = view.findViewById(R.id.singleplayer_game);
        multiPlayer = view.findViewById(R.id.multiplayer_game);
        return view;
    }
}
