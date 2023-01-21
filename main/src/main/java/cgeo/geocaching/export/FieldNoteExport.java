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
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Exports offline logs in the Groundspeak Field Note format.
 */
public class FieldNoteExport extends AbstractExport {
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

    @SuppressLint("SetTextI18n")
    private Dialog getExportOptionsDialog(final Geocache[] caches, final Activity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(activity.getString(R.string.export_confirm_title, activity.getString(R.string.export_fieldnotes)));

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
            new ExportTask(activity, upload, onlyNew).execute(caches);
        });

        return builder.create();
    }

    private class ExportTask extends AsyncTaskWithProgress<Geocache, Boolean> {
        private final boolean upload;
        private final boolean onlyNew;
        private Uri exportUri;

        private static final int STATUS_UPLOAD = -1;

        /**
         * Instantiates and configures the task for exporting field notes.
         *
         * @param activity optional: Show a progress bar and toasts
         * @param upload   Upload the Field Note to geocaching.com
         * @param onlyNew  Upload/export only new logs since last export
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

            // write to uri
            exportUri = fieldNotes.writeToFolder(PersistableFolder.FIELD_NOTES.getFolder(), fileName);
            if (exportUri == null) {
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
                final File tempFile = ContentStorage.get().writeUriToTempFile(exportUri, fileName);
                if (tempFile != null) {
                    for (final IConnector connector : ConnectorFactory.getConnectors()) {
                        if (connector instanceof FieldNotesCapability) {
                            uploadResult &= ((FieldNotesCapability) connector).uploadFieldNotes(tempFile);
                        }
                    }
                    if (!tempFile.delete()) {
                        Log.i("Temp file could not be deleted: " + tempFile);
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
                    if (cache.hasLogOffline()) {
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
                if (result && exportUri != null) {
                    Settings.setFieldnoteExportDate(System.currentTimeMillis());

                    ShareUtils.shareOrDismissDialog(activity, exportUri, "text/plain", R.string.export, getName() + " " + nonNullActivity.getString(R.string.export_exportedto) + ": " + UriUtils.toUserDisplayableString(exportUri));

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
