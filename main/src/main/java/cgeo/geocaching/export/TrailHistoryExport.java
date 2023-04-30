package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.EnvironmentUtils;
import cgeo.geocaching.utils.FileNameCreator;

import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

public class TrailHistoryExport {

    private String filename;

    public TrailHistoryExport(final Activity activity, final Runnable clearTrailHistory) {
        // quick check for being able to write the GPX file
        if (!EnvironmentUtils.isExternalStorageAvailable()) {
            return;
        }
        filename = FileNameCreator.TRAIL_HISTORY.createName();

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(R.string.export_trailhistory_title);

        final View layout = View.inflate(activity, R.layout.gpx_export_trail_dialog, null);
        builder.setView(layout);

        final TextView text = layout.findViewById(R.id.info);
        text.setText(activity.getString(R.string.export_confirm_message, PersistableFolder.GPX.toUserDisplayableValue(), filename));

        final CheckBox clearAfterExport = layout.findViewById(R.id.clear_trailhistory_after_export);
        clearAfterExport.setChecked(Settings.getClearTrailAfterExportStatus());

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            Settings.setClearTrailAfterExportStatus(clearAfterExport.isChecked());
            dialog.dismiss();
            new TrailHistoryExportTask(activity, clearTrailHistory, filename).execute(DataStore.loadTrailHistoryAsArray());
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

}
