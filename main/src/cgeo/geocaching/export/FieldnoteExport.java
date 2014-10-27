package cgeo.geocaching.export;

import butterknife.ButterKnife;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.FieldNotesCapability;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CheckBox;

import java.io.File;
import java.util.List;

/**
 * Exports offline logs in the Groundspeak Field Note format.
 *
 */
public class FieldnoteExport extends AbstractExport {
    private static final File exportLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/field-notes");
    private static int fieldNotesCount = 0;

    public FieldnoteExport() {
        super(getString(R.string.export_fieldnotes));
    }

    @Override
    public void export(final List<Geocache> cachesList, final Activity activity) {
        final Geocache[] caches = cachesList.toArray(new Geocache[cachesList.size()]);
        if (null == activity) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new ExportTask(null, false, false).execute(caches);
        } else {
            // Show configuration dialog
            getExportOptionsDialog(caches, activity).show();
        }
    }

    private Dialog getExportOptionsDialog(final Geocache[] caches, final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final Context themedContext;
        if (Settings.isLightSkin() && VERSION.SDK_INT < VERSION_CODES.HONEYCOMB)
            themedContext = new ContextThemeWrapper(activity, R.style.dark);
        else
            themedContext = activity;
        final View layout = View.inflate(themedContext, R.layout.fieldnote_export_dialog, null);

        builder.setView(layout);

        final CheckBox uploadOption = ButterKnife.findById(layout, R.id.upload);
        uploadOption.setChecked(Settings.getFieldNoteExportUpload());
        final CheckBox onlyNewOption = ButterKnife.findById(layout, R.id.onlynew);
        onlyNewOption.setChecked(Settings.getFieldNoteExportOnlyNew());

        if (Settings.getFieldnoteExportDate() > 0) {
            onlyNewOption.setText(getString(R.string.export_fieldnotes_onlynew) + " (" + Formatter.formatDateTime(Settings.getFieldnoteExportDate()) + ')');
        }

        builder.setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                final boolean upload = uploadOption.isChecked();
                final boolean onlyNew = onlyNewOption.isChecked();
                Settings.setFieldNoteExportUpload(upload);
                Settings.setFieldNoteExportOnlyNew(onlyNew);
                dialog.dismiss();
                new ExportTask(activity, upload, onlyNew).execute(caches);
            }
        });

        return builder.create();
    }

    private class ExportTask extends AsyncTaskWithProgress<Geocache, Boolean> {
        private final Activity activity;
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
        public ExportTask(final Activity activity, final boolean upload, final boolean onlyNew) {
            super(activity, getProgressTitle(), getString(R.string.export_fieldnotes_creating), true);
            this.activity = activity;
            this.upload = upload;
            this.onlyNew = onlyNew;
        }

        @Override
        protected Boolean doInBackgroundInternal(final Geocache[] caches) {
            // export field notes separately for each connector, so the file can be uploaded to the respective site afterwards
            for (final IConnector connector : ConnectorFactory.getConnectors()) {
                if (connector instanceof FieldNotesCapability) {
                    exportFieldNotes((FieldNotesCapability) connector, caches);
                }
            }
            return true;
        }

        private boolean exportFieldNotes(final FieldNotesCapability connector, final Geocache[] caches) {
            final FieldNotes fieldNotes = new FieldNotes();
            try {
                int i = 0;
                for (final Geocache cache : caches) {
                    if (ConnectorFactory.getConnector(cache).equals(connector) && cache.isLogOffline()) {
                        final LogEntry log = DataStore.loadLogOffline(cache.getGeocode());
                        if (!onlyNew || log.date > Settings.getFieldnoteExportDate()) {
                            fieldNotes.add(cache, log);
                        }
                    }
                    publishProgress(++i);
                }
            } catch (final Exception e) {
                Log.e("FieldnoteExport.ExportTask generation", e);
                return false;
            }
            fieldNotesCount += fieldNotes.size();

            exportFile = fieldNotes.writeToDirectory(exportLocation);
            if (exportFile == null) {
                return false;
            }

            if (upload) {
                publishProgress(STATUS_UPLOAD);
                if (!connector.uploadFieldNotes(exportFile)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        protected void onPostExecuteInternal(final Boolean result) {
            if (null != activity) {
                if (result) {
                    Settings.setFieldnoteExportDate(System.currentTimeMillis());

                    ActivityMixin.showToast(activity, getName() + ' ' + getString(R.string.export_exportedto) + ": " + exportFile.toString());

                    if (upload) {
                        ActivityMixin.showToast(activity, getString(R.string.export_fieldnotes_upload_success));
                    }
                } else {
                    ActivityMixin.showToast(activity, getString(R.string.export_failed));
                }
            }
        }

        @Override
        protected void onProgressUpdateInternal(final int status) {
            if (null != activity) {
                setMessage(getString(STATUS_UPLOAD == status ? R.string.export_fieldnotes_uploading : R.string.export_fieldnotes_creating) + " (" + fieldNotesCount + ')');
            }
        }
    }

}
