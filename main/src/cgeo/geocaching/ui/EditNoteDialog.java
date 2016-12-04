package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import butterknife.ButterKnife;
import org.apache.commons.lang3.StringUtils;

public class EditNoteDialog extends DialogFragment {

    public static final String ARGUMENT_INITIAL_NOTE = "initialNote";

    private EditText mEditText;

    public interface EditNoteDialogListener {
        void onFinishEditNoteDialog(final String inputText);
    }

    /**
     * Create a new dialog to edit a note.
     * <em>This fragment must be inserted into an activity implementing the EditNoteDialogListener interface.</em>
     *
     * @param initialNote the initial note to insert in the edit dialog
     */
    public static EditNoteDialog newInstance(final String initialNote) {
        final EditNoteDialog dialog = new EditNoteDialog();

        final Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_INITIAL_NOTE, initialNote);
        dialog.setArguments(arguments);

        return dialog;
    }

    @Override
    @android.support.annotation.NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        final View view = View.inflate(activity, R.layout.fragment_edit_note, null);
        mEditText = ButterKnife.findById(view, R.id.note);
        String initialNote = getArguments().getString(ARGUMENT_INITIAL_NOTE);
        if (initialNote != null) {
            // add a new line when editing existing text, to avoid accidental overwriting of the last line
            if (StringUtils.isNotBlank(initialNote)) {
                initialNote = StringUtils.appendIfMissing(initialNote, "\n");
            }
            mEditText.setText(initialNote);
            Dialogs.moveCursorToEnd(mEditText);
            getArguments().remove(ARGUMENT_INITIAL_NOTE);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final TextView title = ButterKnife.findById(view, R.id.dialog_title_title);
        title.setText(R.string.cache_personal_note);
        title.setVisibility(View.VISIBLE);

        final ImageButton cancel = ButterKnife.findById(view, R.id.dialog_title_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                dialog.dismiss();
            }
        });
        cancel.setVisibility(View.VISIBLE);

        final ImageButton done = ButterKnife.findById(view, R.id.dialog_title_done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                // trim note to avoid unnecessary uploads for whitespace only changes
                final String personalNote = StringUtils.trim(mEditText.getText().toString());
                ((EditNoteDialogListener) getActivity()).onFinishEditNoteDialog(personalNote);
                dialog.dismiss();
            }
        });
        done.setVisibility(View.VISIBLE);

        new Keyboard(activity).showDelayed(mEditText);
        return dialog;
    }
}
