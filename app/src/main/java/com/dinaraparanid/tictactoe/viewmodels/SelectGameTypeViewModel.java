package com.dinaraparanid.tictactoe.viewmodels;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;

import com.dinaraparanid.tictactoe.MainActivity;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.fragments.SelectGameRoomTypeFragment;
import com.dinaraparanid.tictactoe.fragments.SelectGameTypeFragment;

public final class SelectGameTypeViewModel extends BaseObservable {

    @NonNull
    public final MainActivity activity;

    public SelectGameTypeViewModel(@NonNull final MainActivity activity) {
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
