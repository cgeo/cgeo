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

package cgeo.geocaching.export

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.UriUtils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.CheckBox
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog

import java.text.SimpleDateFormat
import java.util.Date
import java.util.List
import java.util.Locale

/**
 * Exports offline logs in the Groundspeak Field Note format.
 */
class FieldNoteExport : AbstractExport() {
    private final String fileName

    public FieldNoteExport() {
        super(R.string.fieldnotes)
        val fileNameDateFormat: SimpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        fileName = fileNameDateFormat.format(Date()) + ".txt"
    }

    override     public Unit export(final List<Geocache> cachesList, final Activity activity) {
        final Geocache[] caches = cachesList.toArray(Geocache[0])
        if (activity == null) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            FieldNoteExportTask(null, false, false, getProgressTitle(), fileName, getName()).execute(caches)
        } else {
            // Show configuration dialog
            getExportOptionsDialog(caches, activity).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private Dialog getExportOptionsDialog(final Geocache[] caches, final Activity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
        builder.setTitle(activity.getString(R.string.export_confirm_title, activity.getString(R.string.fieldnotes)))

        val layout: View = View.inflate(activity, R.layout.fieldnote_export_dialog, null)
        builder.setView(layout)

        val text: TextView = layout.findViewById(R.id.info)
        text.setText(activity.getString(R.string.export_confirm_message, UriUtils.toUserDisplayableString(PersistableFolder.FIELD_NOTES.getUri()), fileName))

        val uploadOption: CheckBox = layout.findViewById(R.id.upload)
        uploadOption.setChecked(Settings.getFieldNoteExportUpload())
        val onlyNewOption: CheckBox = layout.findViewById(R.id.onlynew)
        onlyNewOption.setChecked(Settings.getFieldNoteExportOnlyNew())

        if (Settings.getFieldnoteExportDate() > 0) {
            onlyNewOption.setText(activity.getString(R.string.export_fieldnotes_onlynew) + " (" + Formatter.formatDateTime(Settings.getFieldnoteExportDate()) + ')')
        }

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            val upload: Boolean = uploadOption.isChecked()
            val onlyNew: Boolean = onlyNewOption.isChecked()
            Settings.setFieldNoteExportUpload(upload)
            Settings.setFieldNoteExportOnlyNew(onlyNew)
            dialog.dismiss()
            FieldNoteExportTask(activity, upload, onlyNew, getProgressTitle(), fileName, getName()).execute(caches)
        })

        return builder.create()
    }

}
