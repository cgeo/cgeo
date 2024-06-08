package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.GpxExportIndividualRouteDialogBinding;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;

import android.app.Activity;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.apache.commons.lang3.StringUtils;

public class IndividualRouteExport {

    private String filename;

    public IndividualRouteExport(final Activity activity, final Route route, final boolean exportAsTrack) {

        filename = exportAsTrack ? FileNameCreator.INDIVIDUAL_TRACK_NOSUFFIX.createName() : FileNameCreator.INDIVIDUAL_ROUTE_NOSUFFIX.createName(); //will not have a suffix

        final InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (FileUtils.FORBIDDEN_FILENAME_CHARS.indexOf(source.charAt(i)) >= 0) {
                    ViewUtils.showToast(activity, R.string.err_invalid_filename_char, source.charAt(i));
                    return "";
                }
            }
            return null;
        };

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(R.string.export_individualroute_title);

        final GpxExportIndividualRouteDialogBinding binding = GpxExportIndividualRouteDialogBinding.inflate(activity.getLayoutInflater());
        builder.setView(binding.getRoot());

        final EditText editFilename = binding.filename;
        editFilename.setFilters(new InputFilter[]{filter});
        editFilename.setText(filename);

        final TextView text = binding.infoinclude.info;
        text.setText(activity.getString(R.string.export_confirm_message, PersistableFolder.GPX.toUserDisplayableValue(), filename + FileUtils.GPX_FILE_EXTENSION));

        builder
                .setPositiveButton(R.string.export, (dialog, which) -> {
                    final String temp = StringUtils.trim(editFilename.getText().toString());
                    filename = (StringUtils.isNotBlank(temp) ? temp : filename) + FileUtils.GPX_FILE_EXTENSION;
                    dialog.dismiss();
                    new IndividualRouteExportTask(activity, filename, exportAsTrack).execute(route.getSegments());
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

}
