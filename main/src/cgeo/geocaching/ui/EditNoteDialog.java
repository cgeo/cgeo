package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.R.string;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class EditNoteDialog extends DialogFragment implements OnEditorActionListener {

    public interface EditNoteDialogListener {
        void onFinishEditNoteDialog(final String inputText);
    }

    public static final String ARGUMENT_INITIAL_NOTE = "initialNote";

    private EditText mEditText;
    private String initialNote;

    public EditNoteDialog() {
        // Empty constructor required for DialogFragment
    }

    public static EditNoteDialog newInstance(final String initialNote) {
        EditNoteDialog dialog = new EditNoteDialog();

        Bundle arguments = new Bundle();
        arguments.putString(EditNoteDialog.ARGUMENT_INITIAL_NOTE, initialNote);
        dialog.setArguments(arguments);

        return dialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_note, container);
        mEditText = (EditText) view.findViewById(R.id.note);
        initialNote = getArguments().getString(ARGUMENT_INITIAL_NOTE);
        if (initialNote != null) {
            mEditText.setText(initialNote);
            initialNote = null;
        }
        getDialog().setTitle(string.cache_personal_note);
        mEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mEditText.setOnEditorActionListener(this);

        return view;
    }

    @Override
    public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            final EditNoteDialogListener activity = (EditNoteDialogListener) getActivity();
            activity.onFinishEditNoteDialog(mEditText.getText().toString());
            dismiss();
            return true;
        }
        return false;
    }


}
