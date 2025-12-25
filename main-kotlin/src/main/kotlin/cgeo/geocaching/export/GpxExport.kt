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
import cgeo.geocaching.utils.FileNameCreator

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.CheckBox
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog

import java.util.List

import org.apache.commons.lang3.StringUtils

class GpxExport : AbstractExport() {

    private var fileName: String = "geocache.gpx"; // used in tests
    private var title: String = null

    public GpxExport() {
        super(R.string.gpx)
    }

    public Unit export(final List<Geocache> caches, final Activity activity, final String title) {
        if (StringUtils.isNotBlank(title)) {
            this.title = title.replace("/", "_")
        }
        export(caches, activity)
    }

    override     public Unit export(final List<Geocache> caches, final Activity activity) {
        final String[] geocodes = getGeocodes(caches)
        calculateFileName(geocodes)
        if (activity == null) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            GpxExportTask(null, getProgressTitle(), fileName, getName()).execute(geocodes)

        } else {
            // Show configuration dialog
            getExportDialog(geocodes, activity).show()
        }
    }

    private Unit calculateFileName(final String[] geocodes) {
        if (geocodes.length == 1) {
            // geocode as file name
            fileName = geocodes[0] + (StringUtils.isNotBlank(title) ? " " + title : "") + ".gpx"
        } else {
            fileName = FileNameCreator.GPX_EXPORT.createName()
            if (StringUtils.isNotBlank(title)) {
                val pos: Int = fileName.lastIndexOf(".")
                if (pos >= 0) {
                    val first: String = fileName.substring(0, pos)
                    val last: String = fileName.substring(pos)
                    fileName = first + " " + title + last
                } else {
                    fileName += " " + title
                }
            }
        }
    }

    private Dialog getExportDialog(final String[] geocodes, final Activity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
        builder.setTitle(activity.getString(R.string.export_confirm_title, activity.getString(R.string.gpx)))

        val layout: View = View.inflate(activity, R.layout.gpx_export_dialog, null)
        builder.setView(layout)

        val text: TextView = layout.findViewById(R.id.info)
        text.setText(activity.getString(R.string.export_confirm_message, PersistableFolder.GPX.toUserDisplayableValue(), fileName))

        val includeFoundStatus: CheckBox = layout.findViewById(R.id.include_found_status)
        includeFoundStatus.setChecked(Settings.getIncludeFoundStatus())

        val includeLogs: CheckBox = layout.findViewById(R.id.include_logs)
        includeLogs.setChecked(Settings.getIncludeLogs())

        val includeTravelBugs: CheckBox = layout.findViewById(R.id.include_travelbugs)
        includeTravelBugs.setChecked(Settings.getIncludeTravelBugs())

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            Settings.setIncludeFoundStatus(includeFoundStatus.isChecked())
            Settings.setIncludeLogs(includeLogs.isChecked())
            Settings.setIncludeTravelBugs(includeTravelBugs.isChecked())
            dialog.dismiss()
            GpxExportTask(activity, getProgressTitle(), fileName, getName()).execute(geocodes)
        })

        return builder.create()
    }

    private static String[] getGeocodes(final List<Geocache> caches) {
        return Geocache.getGeocodes(caches).toArray(String[caches.size()])
    }

}
