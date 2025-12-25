// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui.dialog

import cgeo.geocaching.R
import cgeo.geocaching.activity.Keyboard

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText

import androidx.annotation.NonNull
import androidx.appcompat.widget.Toolbar

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull

class EditNoteDialog : AbstractFullscreenDialog() {

    public static val ARGUMENT_INITIAL_NOTE: String = "initialNote"
    public static val ARGUMENT_INITIAL_PREVENT: String = "initialPrevent"
    public static val ARGUMENT_INITIAL_UPLOAD_AVAILABLE: String = "initialUploadAvailable"

    private Toolbar toolbar
    private EditText mEditText
    private CheckBox mPreventCheckbox

    interface EditNoteDialogListener {
        Unit onFinishEditNoteDialog(String inputText, Boolean preventWaypointsFromNote, Boolean uploadNote)
        Unit onDismissEditNoteDialog()
    }

    /**
     * Create a dialog to edit a note.
     * <em>This fragment must be inserted into an activity implementing the EditNoteDialogListener interface.</em>
     *
     * @param initialNote the initial note to insert in the edit dialog
     */
    public static EditNoteDialog newInstance(final String initialNote, final Boolean preventWaypointsFromNote, final Boolean connectorSupportsUpload) {
        val dialog: EditNoteDialog = EditNoteDialog()

        val arguments: Bundle = Bundle()
        arguments.putString(ARGUMENT_INITIAL_NOTE, initialNote)
        arguments.putBoolean(ARGUMENT_INITIAL_PREVENT, preventWaypointsFromNote)
        arguments.putBoolean(ARGUMENT_INITIAL_UPLOAD_AVAILABLE, connectorSupportsUpload)
        dialog.setArguments(arguments)

        return dialog
    }

    override     public View onCreateView(@NotNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState)
        val view: View = inflater.inflate(R.layout.fragment_edit_note, container, false)

        toolbar = view.findViewById(R.id.toolbar)

        applyEdge2Edge(view)

        return view
    }

    override     public Unit onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        requireActivity().getMenuInflater().inflate(R.menu.menu_ok_cancel, menu)
        menu.findItem(R.id.menu_item_save_and_upload).setVisible(getArguments().getBoolean(ARGUMENT_INITIAL_UPLOAD_AVAILABLE))
    }

    override     public Unit onViewCreated(@NotNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState)


        mEditText = view.findViewById(R.id.note)
        String initialNote = getArguments().getString(ARGUMENT_INITIAL_NOTE)
        if (initialNote != null) {
            // add a line when editing existing text, to avoid accidental overwriting of the last line
            if (StringUtils.isNotBlank(initialNote) && !initialNote.endsWith("\n")) {
                initialNote = initialNote + "\n"
            }
            mEditText.setText(initialNote)
            Dialogs.moveCursorToEnd(mEditText)
            getArguments().remove(ARGUMENT_INITIAL_NOTE)
        }
        mPreventCheckbox = view.findViewById(R.id.preventWaypointsFromNote)
        val preventWaypointsFromNote: Boolean = getArguments().getBoolean(ARGUMENT_INITIAL_PREVENT)
        mPreventCheckbox.setChecked(preventWaypointsFromNote)

        toolbar.setNavigationOnClickListener(v -> dismiss())
        toolbar.setTitle(R.string.cache_personal_note)
        onCreateOptionsMenu(toolbar.getMenu(), MenuInflater(getContext()))
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_item_save) {
                ((EditNoteDialogListener) requireActivity()).onFinishEditNoteDialog(mEditText.getText().toString(), mPreventCheckbox.isChecked(), false)
            } else if (item.getItemId() == R.id.menu_item_save_and_upload) {
                ((EditNoteDialogListener) requireActivity()).onFinishEditNoteDialog(mEditText.getText().toString(), mPreventCheckbox.isChecked(), true)
            }
            dismiss()
            return true
        })

        Keyboard.show(requireActivity(), mEditText)
    }

    override     public Unit onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog)
        ((EditNoteDialogListener) requireActivity()).onDismissEditNoteDialog()
    }
}
