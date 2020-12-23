package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.FieldNotesCapability;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Exports offline logs in the Groundspeak Field Note format.
 *
 */
public class FieldNoteExport extends AbstractExport {
    private static final File exportLocation = LocalStorage.getFieldNotesDirectory();
    private static int fieldNotesCount = 0;
    private final String fileName;

    public FieldNoteExport() {
        super(R.string.export_fieldnotes);
        final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        fileName = fileNameDateFormat.format(new Date()) + ".txt";
    }

    @Override
    public void export(@NonNull final List<Geocache> cachesList, @Nullable final Activity activity) {
        final Geocache[] caches = cachesList.toArray(new Geocache[cachesList.size()]);
        if (activity == null) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new ExportTask(null, false, false).execute(caches);
        } else {
            // Show configuration dialog
            getExportOptionsDialog(caches, activity).show();
        }
    }

    private Dialog getExportOptionsDialog(final Geocache[] caches, final Activity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(activity.getString(R.string.export_confirm_title, activity.getString(R.string.export_fieldnotes)));

        final View layout = View.inflate(activity, R.layout.fieldnote_export_dialog, null);
        builder.setView(layout);

        final TextView text = layout.findViewById(R.id.info);
        text.setText(activity.getString(R.string.export_confirm_message, exportLocation.getAbsolutePath(), fileName));

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
            new ExportTask(activity, upload, onlyNew).execute(caches);
        });

        return builder.create();
    }

    private class ExportTask extends AsyncTaskWithProgress<Geocache, Boolean> {
        private final boolean upload;
        private final boolean onlyNew;
        private File exportFile;

        private static final int STATUS_UPLOAD = -1;

        /**
         * Instantiates and configures the task for exporting field notes.
         *
         * @param activity
         *            optional: Show a progress bar and toasts
         * @param upload
         *            Upload the Field Note to geocaching.com
         * @param onlyNew
         *            Upload/export only new logs since last export
         */
        ExportTask(@Nullable final Activity activity, final boolean upload, final boolean onlyNew) {
            super(activity, getProgressTitle(), CgeoApplication.getInstance().getString(R.string.export_fieldnotes_creating), true);
            this.upload = upload;
            this.onlyNew = onlyNew;
        }

        @Override
        protected Boolean doInBackgroundInternal(final Geocache[] caches) {
            // export all field notes, without any filtering by connector
            final FieldNotes fieldNotes = createFieldNotes(caches);
            if (fieldNotes == null) {
                return false;
            }
            // write to file
            exportFile = fieldNotes.writeToDirectory(exportLocation, fileName);
            if (exportFile == null) {
                return false;
            }
            fieldNotesCount = fieldNotes.size();
            // upload same file to multiple connectors, if they support the upload
            return uploadFieldNotes();
        }

        private Boolean uploadFieldNotes() {
            boolean uploadResult = true;
            if (upload) {
                publishProgress(STATUS_UPLOAD);
                for (final IConnector connector : ConnectorFactory.getConnectors()) {
                    if (connector instanceof FieldNotesCapability) {
                        uploadResult &= ((FieldNotesCapability) connector).uploadFieldNotes(exportFile);
                    }
                }
            }
            return uploadResult;
        }

        @Nullable
        private FieldNotes createFieldNotes(final Geocache[] caches) {
            final FieldNotes fieldNotes = new FieldNotes();
            try {
                for (final Geocache cache : caches) {
                    if (cache.isLogOffline()) {
                        final LogEntry log = DataStore.loadLogOffline(cache.getGeocode());
                        if (log != null && (!onlyNew || log.date > Settings.getFieldnoteExportDate())) {
                            fieldNotes.add(cache, log);
                        }
                    }
                    publishProgress(fieldNotes.size());
                }
            } catch (final Exception e) {
                Log.e("FieldNoteExport.ExportTask generation", e);
                return null;
            }
            return fieldNotes;
        }

        @Override
        protected void onPostExecuteInternal(final Boolean result) {
            if (activity != null) {
                final Context nonNullActivity = activity;
                if (result && exportFile != null) {
                    Settings.setFieldnoteExportDate(System.currentTimeMillis());

                    ShareUtils.shareFileOrDismissDialog(activity, exportFile, "text/plain", R.string.export, getName() + " " + nonNullActivity.getString(R.string.export_exportedto) + ": " + exportFile.toString());

                    if (upload) {
                        ActivityMixin.showToast(activity, nonNullActivity.getString(R.string.export_fieldnotes_upload_success));
                    }
                } else {
                    ActivityMixin.showToast(activity, nonNullActivity.getString(R.string.export_failed));
                }
            }
        }

        @Override
        protected void onProgressUpdateInternal(final Integer status) {
            if (activity != null) {
                setMessage(activity.getString(status == STATUS_UPLOAD ? R.string.export_fieldnotes_uploading : R.string.export_fieldnotes_creating) + " (" + fieldNotesCount + ')');
            }
        }
    }

}
