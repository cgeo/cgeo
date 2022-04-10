package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.widget.Toolbar;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class EditNoteDialog extends AbstractFullscreenDialog {

    public static final String ARGUMENT_INITIAL_NOTE = "initialNote";
    public static final String ARGUMENT_INITIAL_PREVENT = "initialPrevent";

    private Toolbar toolbar;
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
    public View onCreateView(@NotNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_edit_note, container, false);

        toolbar = view.findViewById(R.id.toolbar);

        return view;
    }

    @Override
    public void onViewCreated(@NotNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


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

        toolbar.setNavigationOnClickListener(v -> dismiss());
        toolbar.setTitle(R.string.cache_personal_note);
        toolbar.inflateMenu(R.menu.menu_ok_cancel);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_item_save) {
                ((EditNoteDialogListener) requireActivity()).onFinishEditNoteDialog(mEditText.getText().toString(), mPreventCheckbox.isChecked());
            }
            dismiss();
            return true;
        });

        Keyboard.show(requireActivity(), mEditText);
    }
}
