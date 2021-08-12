package com.dinaraparanid.tictactoe.utils.polymorphism;

import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;

public abstract class DataBindingFragment<T extends ViewDataBinding> extends Fragment {
    @Nullable
    protected T binding;

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
