package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.UriUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Exports offline logs in the Groundspeak Field Note format.
 */
public class FieldNoteExport extends AbstractExport {
    private final String fileName;

    public FieldNoteExport() {
        super(R.string.fieldnotes);
        final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        fileName = fileNameDateFormat.format(new Date()) + ".txt";
    }

    @Override
    public void export(@NonNull final List<Geocache> cachesList, @Nullable final Activity activity) {
        final Geocache[] caches = cachesList.toArray(new Geocache[0]);
        if (activity == null) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new FieldNoteExportTask(null, false, false, getProgressTitle(), fileName, getName()).execute(caches);
        } else {
            // Show configuration dialog
            getExportOptionsDialog(caches, activity).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private Dialog getExportOptionsDialog(final Geocache[] caches, final Activity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(activity.getString(R.string.export_confirm_title, activity.getString(R.string.fieldnotes)));

        final View layout = View.inflate(activity, R.layout.fieldnote_export_dialog, null);
        builder.setView(layout);

        final TextView text = layout.findViewById(R.id.info);
        text.setText(activity.getString(R.string.export_confirm_message, UriUtils.toUserDisplayableString(PersistableFolder.FIELD_NOTES.getUri()), fileName));

        final CheckBox uploadOption = layout.findViewById(R.id.upload);
        uploadOption.setChecked(Settings.getFieldNoteExportUpload());
        final CheckBox onlyNewOption = layout.findViewById(R.id.onlynew);
        onlyNewOption.setChecked(Settings.getFieldNoteExportOnlyNew());

        if (Settings.getFieldnoteExportDate() > 0) {
            onlyNewOption.setText(activity.getString(R.string.export_fieldnotes_onlynew) + " (" + Formatter.formatDateTime(Settings.getFieldnoteExportDate()) + ')');
        }

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            final boolean upload = uploadOption.isChecked();
            final boolean onlyNew = onlyNewOption.isChecked();
            Settings.setFieldNoteExportUpload(upload);
            Settings.setFieldNoteExportOnlyNew(onlyNew);
            dialog.dismiss();
            new FieldNoteExportTask(activity, upload, onlyNew, getProgressTitle(), fileName, getName()).execute(caches);
        });

        return builder.create();
    }

}
