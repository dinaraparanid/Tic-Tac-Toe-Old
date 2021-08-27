package com.dinaraparanid.tictactoe.viewmodels;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.fragment.app.FragmentActivity;

import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.fragments.SelectGameRoomTypeFragment;

public final class SelectGameTypeViewModel extends BaseObservable {

    @NonNull
    private final FragmentActivity activity;

    public SelectGameTypeViewModel(@NonNull final FragmentActivity activity) {
        this.activity = activity;
    }

    public final void showSelectGameRoomTypeFragment() {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragment_container,
                        SelectGameRoomTypeFragment.newInstance()
                )
                .addToBackStack(null)
                .commit();
    }
}
