package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import org.apache.commons.lang3.StringUtils;

public class EditNoteDialog extends DialogFragment {

    public static final String ARGUMENT_INITIAL_NOTE = "initialNote";
    public static final String ARGUMENT_INITIAL_PREVENT = "initialPrevent";

    private EditText mEditText;
    private CheckBox mPreventCheckbox;

    public interface EditNoteDialogListener {
        void onFinishEditNoteDialog(String inputText, boolean preventWaypointsFromNote);
    }

    /**
     * Create a new dialog to edit a note.
     * <em>This fragment must be inserted into an activity implementing the EditNoteDialogListener interface.</em>
     *
     * @param initialNote the initial note to insert in the edit dialog
     */
    public static EditNoteDialog newInstance(final String initialNote, final boolean preventWaypointsFromNote) {
        final EditNoteDialog dialog = new EditNoteDialog();

        final Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_INITIAL_NOTE, initialNote);
        arguments.putBoolean(ARGUMENT_INITIAL_PREVENT, preventWaypointsFromNote);
        dialog.setArguments(arguments);

        return dialog;
    }

    @Override
    @androidx.annotation.NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        final View view = View.inflate(activity, R.layout.fragment_edit_note, null);
        mEditText = view.findViewById(R.id.note);
        String initialNote = getArguments().getString(ARGUMENT_INITIAL_NOTE);
        if (initialNote != null) {
            // add a new line when editing existing text, to avoid accidental overwriting of the last line
            if (StringUtils.isNotBlank(initialNote) && !initialNote.endsWith("\n")) {
                initialNote = initialNote + "\n";
            }
            mEditText.setText(initialNote);
            Dialogs.moveCursorToEnd(mEditText);
            getArguments().remove(ARGUMENT_INITIAL_NOTE);
        }
        mPreventCheckbox = view.findViewById(R.id.preventWaypointsFromNote);
        final boolean preventWaypointsFromNote = getArguments().getBoolean(ARGUMENT_INITIAL_PREVENT);
        mPreventCheckbox.setChecked(preventWaypointsFromNote);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final TextView title = view.findViewById(R.id.dialog_title_title);
        title.setText(R.string.cache_personal_note);
        title.setVisibility(View.VISIBLE);

        final ImageButton cancel = view.findViewById(R.id.dialog_title_cancel);
        cancel.setOnClickListener(view1 -> dialog.dismiss());
        cancel.setVisibility(View.VISIBLE);

        final ImageButton done = view.findViewById(R.id.dialog_title_done);
        done.setOnClickListener(view12 -> {
            // trim note to avoid unnecessary uploads for whitespace only changes
            final String personalNote = StringUtils.trim(mEditText.getText().toString());
            ((EditNoteDialogListener) getActivity()).onFinishEditNoteDialog(personalNote, mPreventCheckbox.isChecked());
            dialog.dismiss();
        });
        done.setVisibility(View.VISIBLE);

        new Keyboard(activity).showDelayed(mEditText);
        return dialog;
    }
}
