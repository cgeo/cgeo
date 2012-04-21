package cgeo.geocaching.export;

import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports offline-logs in the Groundspeak Field Note format.<br>
 * <br>
 *
 * Field Notes are simple plain text files, but poorly documented. Syntax:<br>
 * <code>GCxxxxx,yyyy-mm-ddThh:mm:ssZ,Found it,"logtext"</code>
 */
class FieldnoteExport extends AbstractExport {
    private static final File exportLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/field-notes");
    private static final SimpleDateFormat fieldNoteDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    protected FieldnoteExport() {
        super(getString(R.string.export_fieldnotes));
    }

    /**
     * A dialog to allow the user to set options for the export.
     *
     * Currently available options are: upload field notes, only new logs since last export/upload
     */
    private class ExportOptionsDialog extends AlertDialog {
        public ExportOptionsDialog(final List<cgCache> caches, final Activity activity) {
            super(activity);

            View layout = activity.getLayoutInflater().inflate(R.layout.fieldnote_export_dialog, null);
            setView(layout);

            ((Button) layout.findViewById(R.id.export)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    new ExportTask(
                            caches,
                            activity,
                            ((CheckBox) findViewById(R.id.upload)).isChecked(),
                            ((CheckBox) findViewById(R.id.onlynew)).isChecked())
                            .execute((Void) null);
                }
            });
        }
    }

    @Override
    public void export(final List<cgCache> caches, final Activity activity) {
        if (null == activity) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new ExportTask(caches, null, false, false).execute((Void) null);
        } else {
            // Show configuration dialog
            new ExportOptionsDialog(caches, activity).show();
        }
    }

    private class ExportTask extends AsyncTask<Void, Integer, Boolean> {
        private final List<cgCache> caches;
        private final Activity activity;
        private final boolean onlyNew;
        private final Progress progress = new Progress();
        private File exportFile;

        private static final int STATUS_UPLOAD = -1;

        /**
         * Instantiates and configurates the task for exporting field notes.
         *
         * @param caches
         *            The {@link List} of {@link cgCache} to be exported
         * @param activity
         *            optional: Show a progress bar and toasts
         * @param upload
         *            Upload the Field Note to geocaching.com
         * @param onlyNew
         *            Upload/export only new logs since last export
         */
        public ExportTask(final List<cgCache> caches, final Activity activity, final boolean upload, final boolean onlyNew) {
            this.caches = caches;
            this.activity = activity;
            this.onlyNew = onlyNew;
        }

        @Override
        protected void onPreExecute() {
            if (null != activity) {
                progress.show(activity, null, getString(R.string.export) + ": " + getName(), ProgressDialog.STYLE_HORIZONTAL, null);
                progress.setMaxProgressAndReset(caches.size());
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final StringBuilder fieldNoteBuffer = new StringBuilder();

            // We need our own HashMap because LogType will give us localized and maybe
            // different strings than gc.com expects in the field note
            // We only need such logtypes that are possible to log via c:geo
            Map<LogType, String> logTypes = new HashMap<LogType, String>();
            logTypes.put(LogType.LOG_FOUND_IT, "Found it");
            logTypes.put(LogType.LOG_DIDNT_FIND_IT, "Didn't find it");
            logTypes.put(LogType.LOG_NOTE, "Write Note");
            logTypes.put(LogType.LOG_NEEDS_ARCHIVE, "Needs archived");
            logTypes.put(LogType.LOG_NEEDS_MAINTENANCE, "Needs Maintenance");
            logTypes.put(LogType.LOG_WILL_ATTEND, "Will Attend");
            logTypes.put(LogType.LOG_ATTENDED, "Attended");
            logTypes.put(LogType.LOG_WEBCAM_PHOTO_TAKEN, "Webcam Photo Taken");

            for (int i = 0; i < caches.size(); i++) {
                try {
                    final cgCache cache = caches.get(i);
                    if (cache.isLogOffline()) {
                        LogEntry log = cgeoapplication.getInstance().loadLogOffline(cache.getGeocode());
                        if (null != logTypes.get(log.type)) {
                            fieldNoteBuffer.append(cache.getGeocode())
                                    .append(',')
                                    .append(fieldNoteDateFormat.format(new Date(log.date)))
                                    .append(',')
                                    .append(logTypes.get(log.type))
                                    .append(",\"")
                                    .append(StringUtils.replaceChars(log.log, '"', '\''))
                                    .append("\"\n");
                        }
                    }
                    publishProgress(i + 1);
                } catch (Exception e) {
                    Log.e("FieldnoteExport.ExportTask generation", e);
                    return false;
                }
            }

            fieldNoteBuffer.append('\n');

            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                exportLocation.mkdirs();

                SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                exportFile = new File(exportLocation.toString() + '/' + fileNameDateFormat.format(new Date()) + ".txt");

                OutputStream os = null;
                Writer fw = null;
                try {
                    os = new FileOutputStream(exportFile);
                    fw = new OutputStreamWriter(os, "ISO-8859-1"); // TODO: gc.com doesn't support UTF-8
                    fw.write(fieldNoteBuffer.toString());
                } catch (IOException e) {
                    Log.e("FieldnoteExport.ExportTask export", e);
                    return false;
                } finally {
                    if (fw != null) {
                        try {
                            fw.close();
                        } catch (IOException e) {
                            Log.e("FieldnoteExport.ExportTask export", e);
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }

            /*
             * if (upload) {
             * TODO Use multipart POST request for uploading
             * publishProgress(STATUS_UPLOAD);
             *
             * final Parameters uploadParams = new Parameters(
             * "__EVENTTARGET", "",
             * "__EVENTARGUMENT", "",
             * "__VIEWSTATE", "",
             * //TODO "ctl00$ContentBody$chkSuppressDate", "on",
             * "ctl00$ContentBody$FieldNoteLoader", fieldNoteBuffer.toString(),
             * "ctl00$ContentBody$btnUpload", "Upload Field Note");
             * final String uri = "http://www.geocaching.com/my/uploadfieldnotes.aspx";
             *
             * String page = Network.getResponseData(Network.postRequest(uri, uploadParams));
             * if (!Login.getLoginStatus(page)) {
             * final StatusCode loginState = Login.login();
             * if (loginState == StatusCode.NO_ERROR) {
             * page = Network.getResponseData(Network.postRequest(uri, uploadParams));
             * } else {
             * Log.e(Settings.tag, "FieldnoteExport.ExportTask upload: No login (error: " + loginState + ")");
             * return false;
             * }
             * }
             *
             * if (StringUtils.isBlank(page)) {
             * Log.e(Settings.tag, "FieldnoteExport.ExportTask upload: No data from server");
             * return false;
             * }
             * }
             */

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (null != activity) {
                progress.dismiss();

                if (result) {
                    if (onlyNew) {
                        // update last export time in settings
                    }
                    ActivityMixin.showToast(activity, getName() + ' ' + getString(R.string.export_exportedto) + ": " + exportFile.toString());
                } else {
                    ActivityMixin.showToast(activity, getString(R.string.export_failed));
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... status) {
            if (null != activity) {
                if (STATUS_UPLOAD == status[0]) {
                    progress.setMessage(getString(R.string.export_fieldnotes_uploading));
                } else {
                    progress.setProgress(status[0]);
                }
            }
        }
    }
}
