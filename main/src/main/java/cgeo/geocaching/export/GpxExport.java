package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.FileNameCreator;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class GpxExport extends AbstractExport {

    private String fileName = "geocache.gpx"; // used in tests
    private String title = null;

    public GpxExport() {
        super(R.string.export_gpx);
    }

    public void export(@NonNull final List<Geocache> caches, @Nullable final Activity activity, @Nullable final String title) {
        if (StringUtils.isNotBlank(title)) {
            this.title = title.replace("/", "_");
        }
        export(caches, activity);
    }

    @Override
    public void export(@NonNull final List<Geocache> caches, @Nullable final Activity activity) {
        final String[] geocodes = getGeocodes(caches);
        calculateFileName(geocodes);
        if (activity == null) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new GpxExportTask(null, getProgressTitle(), fileName, getName()).execute(geocodes);

        } else {
            // Show configuration dialog
            getExportDialog(geocodes, activity).show();
        }
    }

    private void calculateFileName(final String[] geocodes) {
        if (geocodes.length == 1) {
            // geocode as file name
            fileName = geocodes[0] + (StringUtils.isNotBlank(title) ? " " + title : "") + ".gpx";
        } else {
            fileName = FileNameCreator.GPX_EXPORT.createName();
            if (StringUtils.isNotBlank(title)) {
                final int pos = fileName.lastIndexOf(".");
                if (pos >= 0) {
                    final String first = fileName.substring(0, pos);
                    final String last = fileName.substring(pos);
                    fileName = first + " " + title + last;
                } else {
                    fileName += " " + title;
                }
            }
        }
    }

    private Dialog getExportDialog(final String[] geocodes, final Activity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(activity.getString(R.string.export_confirm_title, activity.getString(R.string.export_gpx)));

        final View layout = View.inflate(activity, R.layout.gpx_export_dialog, null);
        builder.setView(layout);

        final TextView text = layout.findViewById(R.id.info);
        text.setText(activity.getString(R.string.export_confirm_message, PersistableFolder.GPX.toUserDisplayableValue(), fileName));

        final CheckBox includeFoundStatus = layout.findViewById(R.id.include_found_status);
        includeFoundStatus.setChecked(Settings.getIncludeFoundStatus());

        final CheckBox includeLogs = layout.findViewById(R.id.include_logs);
        includeLogs.setChecked(Settings.getIncludeLogs());

        final CheckBox includeTravelBugs = layout.findViewById(R.id.include_travelbugs);
        includeTravelBugs.setChecked(Settings.getIncludeTravelBugs());

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            Settings.setIncludeFoundStatus(includeFoundStatus.isChecked());
            Settings.setIncludeLogs(includeLogs.isChecked());
            Settings.setIncludeTravelBugs(includeTravelBugs.isChecked());
            dialog.dismiss();
            new GpxExportTask(activity, getProgressTitle(), fileName, getName()).execute(geocodes);
        });

        return builder.create();
    }

    private static String[] getGeocodes(final List<Geocache> caches) {
        return Geocache.getGeocodes(caches).toArray(new String[caches.size()]);
    }

}
