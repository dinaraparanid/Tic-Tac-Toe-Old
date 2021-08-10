package com.dinaraparanid.tictactoe.viewmodels;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;

import com.dinaraparanid.tictactoe.MainActivity;
import com.dinaraparanid.tictactoe.R;
import com.dinaraparanid.tictactoe.fragments.SelectGameTypeFragment;

public final class MainActivityViewModel extends BaseObservable {

    public MainActivity activity;

    public MainActivityViewModel(@NonNull final MainActivity activity) {
        this.activity = activity;
    }

    public final void showGameTypeFragment() {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                )
                .replace(
                        R.id.fragment_container,
                        SelectGameTypeFragment.newInstance()
                )
                .addToBackStack(null)
                .commit();
    }
}
