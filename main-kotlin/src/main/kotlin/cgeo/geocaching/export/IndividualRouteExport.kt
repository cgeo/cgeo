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
import cgeo.geocaching.databinding.GpxExportIndividualRouteDialogBinding
import cgeo.geocaching.models.Route
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.FileNameCreator
import cgeo.geocaching.utils.FileUtils

import android.app.Activity
import android.text.InputFilter
import android.widget.EditText
import android.widget.TextView

import androidx.appcompat.app.AlertDialog

import org.apache.commons.lang3.StringUtils

class IndividualRouteExport {

    private String filename

    public IndividualRouteExport(final Activity activity, final Route route, final Boolean exportAsTrack) {

        filename = exportAsTrack ? FileNameCreator.INDIVIDUAL_TRACK_NOSUFFIX.createName() : FileNameCreator.INDIVIDUAL_ROUTE_NOSUFFIX.createName(); //will not have a suffix

        val filter: InputFilter = (source, start, end, dest, dstart, dend) -> {
            for (Int i = start; i < end; i++) {
                if (FileUtils.FORBIDDEN_FILENAME_CHARS.indexOf(source.charAt(i)) >= 0) {
                    ViewUtils.showToast(activity, R.string.err_invalid_filename_char, source.charAt(i))
                    return ""
                }
            }
            return null
        }

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
        builder.setTitle(R.string.export_individualroute_title)

        val binding: GpxExportIndividualRouteDialogBinding = GpxExportIndividualRouteDialogBinding.inflate(activity.getLayoutInflater())
        builder.setView(binding.getRoot())

        val editFilename: EditText = binding.filename
        editFilename.setFilters(InputFilter[]{filter})
        editFilename.setText(filename)

        val text: TextView = binding.infoinclude.info
        text.setText(activity.getString(R.string.export_confirm_message, PersistableFolder.GPX.toUserDisplayableValue(), filename + FileUtils.GPX_FILE_EXTENSION))

        builder
                .setPositiveButton(R.string.export, (dialog, which) -> {
                    val temp: String = StringUtils.trim(editFilename.getText().toString())
                    filename = (StringUtils.isNotBlank(temp) ? temp : filename) + FileUtils.GPX_FILE_EXTENSION
                    dialog.dismiss()
                    IndividualRouteExportTask(activity, filename, exportAsTrack).execute(route.getSegments())
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create()
                .show()
    }

}
