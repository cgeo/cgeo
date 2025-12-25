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
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.EnvironmentUtils
import cgeo.geocaching.utils.FileNameCreator

import android.app.Activity
import android.view.View
import android.widget.CheckBox
import android.widget.TextView

import androidx.appcompat.app.AlertDialog

class TrailHistoryExport {

    private String filename

    public TrailHistoryExport(final Activity activity, final Runnable clearTrailHistory) {
        // quick check for being able to write the GPX file
        if (!EnvironmentUtils.isExternalStorageAvailable()) {
            return
        }
        filename = FileNameCreator.TRAIL_HISTORY.createName()

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
        builder.setTitle(R.string.export_trailhistory_title)

        val layout: View = View.inflate(activity, R.layout.gpx_export_trail_dialog, null)
        builder.setView(layout)

        val text: TextView = layout.findViewById(R.id.info)
        text.setText(activity.getString(R.string.export_confirm_message, PersistableFolder.GPX.toUserDisplayableValue(), filename))

        val clearAfterExport: CheckBox = layout.findViewById(R.id.clear_trailhistory_after_export)
        clearAfterExport.setChecked(Settings.getClearTrailAfterExportStatus())

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            Settings.setClearTrailAfterExportStatus(clearAfterExport.isChecked())
            dialog.dismiss()
            TrailHistoryExportTask(activity, clearTrailHistory, filename).execute(DataStore.loadTrailHistoryAsArray())
        })

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())

        builder.create().show()
    }

}
