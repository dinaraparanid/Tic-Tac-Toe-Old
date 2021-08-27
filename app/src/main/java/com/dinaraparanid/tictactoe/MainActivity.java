package com.dinaraparanid.tictactoe;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;

import com.dinaraparanid.tictactoe.databinding.ActivityMainBinding;
import com.dinaraparanid.tictactoe.utils.polymorphism.Player;
import com.dinaraparanid.tictactoe.viewmodels.MainActivityViewModel;

public final class MainActivity extends AppCompatActivity {

    @Override
    protected final void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Player.ApplicationAccessor.activity = this;

        final ActivityMainBinding binding = DataBindingUtil
                .setContentView(this, R.layout.activity_main);

        binding.setViewModel(new MainActivityViewModel(this));
        binding.executePendingBindings();
    }
}