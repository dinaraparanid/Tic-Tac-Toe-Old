package com.dinaraparanid.tictactoe;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.dinaraparanid.tictactoe.utils.polymorphism.Command;

import org.jetbrains.annotations.Contract;

public final class InputDialog extends DialogFragment {

    private final int message;
    protected final int errorMessage;

    @NonNull
    protected final Command<String, Void> okAction;

    protected InputDialog(
            final int message,
            final int errorMessage,
            @NonNull final Command<String, Void> okAction
    ) {
        this.message = message;
        this.errorMessage = errorMessage;
        this.okAction = okAction;
    }

    @NonNull
    @Override
    public final Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final EditText input = constructInput();

        return new AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    try {
                        okAction.run(input.getText().toString());
                    } catch (final Exception e) {
                        dialog.cancel();

                        Toast.makeText(
                                requireContext(),
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .setCancelable(false)
                .create();
    }

    @NonNull
    private final EditText constructInput() {
        final EditText input = new EditText(requireContext());
        input.setPadding(15, 15, 15, 15);
        input.setTextColor(Color.BLACK);
        return input;
    }

    public static final class Builder {
        private int message = -1;
        private int errorMessage = -1;

        @NonNull
        private Command<String, Void> okAction = param -> null;

        @Contract("_ -> this")
        public final Builder setMessage(final int message) {
            this.message = message;
            return this;
        }

        @Contract("_ -> this")
        public final Builder setErrorMessage(final int errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        @Contract("_ -> this")
        public final Builder setOkAction(@NonNull final Command<String, Void> okAction) {
            this.okAction = okAction;
            return this;
        }

        @NonNull
        @Contract(" -> new")
        public final InputDialog build() {
            return new InputDialog(message, errorMessage, okAction);
        }
    }
}
