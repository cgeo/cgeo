package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;

public class EditTextDialog extends DialogFragment {

    public interface EditTextDialogListener {
        void onFinishEditTextDialog(final String inputText);
    }

    public static final String ARGUMENT_TEXT = "text";

    private EditText mEditText;
    private EditTextDialogListener listener;
    private int layoutResourceId;
    private int titleResourceId;

    public static EditTextDialog newInstance(final String initialText, final int layoutResourceId, final int titleResourceId, EditTextDialogListener listener) {
        EditTextDialog dialog = new EditTextDialog();

        Bundle arguments = new Bundle();
        arguments.putString(EditTextDialog.ARGUMENT_TEXT, initialText);
        dialog.setArguments(arguments);
        dialog.listener = listener;
        dialog.layoutResourceId = layoutResourceId;
        dialog.titleResourceId = titleResourceId;

        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(new ContextThemeWrapper(getActivity(), R.style.dark), this.layoutResourceId, null);
        mEditText = (EditText) view.findViewById(R.id.text);
        String initialNote = getArguments().getString(ARGUMENT_TEXT);
        if (initialNote != null) {
            mEditText.setText(initialNote);
            getArguments().remove(ARGUMENT_TEXT);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(this.titleResourceId);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        listener.onFinishEditTextDialog(mEditText.getText().toString());
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }
}
