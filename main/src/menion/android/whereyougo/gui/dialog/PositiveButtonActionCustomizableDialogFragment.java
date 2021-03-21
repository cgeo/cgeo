package menion.android.whereyougo.gui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import cgeo.geocaching.R;

public class PositiveButtonActionCustomizableDialogFragment extends DialogFragment {

    private PositiveButtonActionCustomizableDialogListener listener;
    private int message;

    public interface PositiveButtonActionCustomizableDialogListener {
        void onPositiveClick(DialogFragment dialog);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            message = getArguments().getInt("message");
        } else {
            throw new IllegalArgumentException("This dialog needs to be handed a Bundle as an "
                    + "argument with the key \"message\" which is a Android string resource int.");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (PositiveButtonActionCustomizableDialogListener) context;
        } catch (ClassCastException e) {
            String exceptionDescription = String.format("%s must implement "
                            + "PositiveButtonActionCustomizableDialogListener.",
                    getActivity().toString());
            throw new ClassCastException(exceptionDescription);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, id) ->
                        listener.onPositiveClick(
                                PositiveButtonActionCustomizableDialogFragment.this))
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    // User cancelled the dialog --> Do nothing
                });
        return builder.create();
    }
}
