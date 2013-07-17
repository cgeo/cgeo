package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class EditNoteDialog extends DialogFragment {

    public interface EditNoteDialogListener {
        void onFinishEditNoteDialog(final String inputText);
    }

    public static final String ARGUMENT_INITIAL_NOTE = "initialNote";

    private EditText mEditText;
    private String initialNote;

    public static EditNoteDialog newInstance(final String initialNote) {
        EditNoteDialog dialog = new EditNoteDialog();

        Bundle arguments = new Bundle();
        arguments.putString(EditNoteDialog.ARGUMENT_INITIAL_NOTE, initialNote);
        dialog.setArguments(arguments);

        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_edit_note, null);
        mEditText = (EditText) view.findViewById(R.id.note);
        initialNote = getArguments().getString(ARGUMENT_INITIAL_NOTE);
        if (initialNote != null) {
            mEditText.setText(initialNote);
            initialNote = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.cache_personal_note);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final EditNoteDialogListener activity = (EditNoteDialogListener) getActivity();
                        activity.onFinishEditNoteDialog(mEditText.getText().toString());
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
