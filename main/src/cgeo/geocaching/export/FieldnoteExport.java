package cgeo.geocaching.export;

import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Exports offline-logs in the Groundspeak Field Note format.<br>
 * <br>
 *
 * Field Notes are simple plain text files, but poorly documented. Syntax:<br>
 * <code>GCxxxxx,yyyy-mm-ddThh:mm:ssZ,Found it,"logtext"</code>
 */
class FieldnoteExport extends AbstractExport {
    private static final File exportLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/field-notes");
    private static final SimpleDateFormat fieldNoteDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

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

            final CheckBox uploadOption = (CheckBox) layout.findViewById(R.id.upload);
            final CheckBox onlyNewOption = (CheckBox) layout.findViewById(R.id.onlynew);

            uploadOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onlyNewOption.setEnabled(uploadOption.isChecked());
                }
            });

            layout.findViewById(R.id.export).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    new ExportTask(
                            caches,
                            activity,
                            uploadOption.isChecked(),
                            onlyNewOption.isChecked())
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
        private final boolean upload;
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
            this.upload = upload;
            this.onlyNew = onlyNew;
        }

        @Override
        protected void onPreExecute() {
            if (null != activity) {
                progress.show(activity, getString(R.string.export) + ": " + getName(), getString(R.string.export_fieldnotes_creating), true, null);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final StringBuilder fieldNoteBuffer = new StringBuilder();

            try {
                int i = 0;
                for (cgCache cache : caches) {
                    if (cache.isLogOffline()) {
                        final LogEntry log = cgeoapplication.getInstance().loadLogOffline(cache.getGeocode());
                        fieldNoteBuffer.append(cache.getGeocode())
                                .append(',')
                                .append(fieldNoteDateFormat.format(new Date(log.date)))
                                .append(',')
                                .append(log.type.type)
                                .append(",\"")
                                .append(StringUtils.replaceChars(log.log, '"', '\''))
                                .append("\"\n");
                        publishProgress(++i);
                    }
                }
            } catch (Exception e) {
                Log.e("FieldnoteExport.ExportTask generation", e);
                return false;
            }

            fieldNoteBuffer.append('\n');

            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                exportLocation.mkdirs();

                SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                exportFile = new File(exportLocation.toString() + '/' + fileNameDateFormat.format(new Date()) + ".txt");

                OutputStream os;
                Writer fw = null;
                try {
                    os = new FileOutputStream(exportFile);
                    fw = new OutputStreamWriter(os, "UTF-16");
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

            if (upload) {
                publishProgress(STATUS_UPLOAD);

                final String uri = "http://www.geocaching.com/my/uploadfieldnotes.aspx";

                if (!Login.isActualLoginStatus()) {
                    // no need to upload (possibly large file) if we're not logged in
                    final StatusCode loginState = Login.login();
                    if (loginState != StatusCode.NO_ERROR) {
                        Log.e("FieldnoteExport.ExportTask upload: Login failed");
                    }
                }

                String page = Network.getResponseData(Network.getRequest(uri));

                if (!Login.getLoginStatus(page)) {
                    // Login.isActualLoginStatus() was wrong, we are not logged in
                    final StatusCode loginState = Login.login();
                    if (loginState == StatusCode.NO_ERROR) {
                        page = Network.getResponseData(Network.getRequest(uri));
                    } else {
                        Log.e("FieldnoteExport.ExportTask upload: No login (error: " + loginState + ')');
                        return false;
                    }
                }

                final String[] viewstates = Login.getViewstates(page);

                final Parameters uploadParams = new Parameters(
                        "__EVENTTARGET", "",
                        "__EVENTARGUMENT", "",
                        "ctl00$ContentBody$btnUpload", "Upload Field Note");

                if (onlyNew) {
                    uploadParams.put("ctl00$ContentBody$chkSuppressDate", "on");
                }

                Login.putViewstates(uploadParams, viewstates);

                Network.getResponseData(Network.postRequest(uri, uploadParams, "ctl00$ContentBody$FieldNoteLoader", "text/plain", exportFile));

                if (StringUtils.isBlank(page)) {
                    Log.e("FieldnoteExport.ExportTask upload: No data from server");
                    return false;
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (null != activity) {
                progress.dismiss();

                if (result) {
                    //                    if (onlyNew) {
                    //                        // update last export time in settings when doing it ourself (currently we use the date check from gc.com)
                    //                    }

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
        protected void onProgressUpdate(Integer... status) {
            if (null != activity) {
                if (STATUS_UPLOAD == status[0]) {
                    progress.setMessage(getString(R.string.export_fieldnotes_uploading));
                } else {
                    progress.setMessage(getString(R.string.export_fieldnotes_creating) + " (" + status[0] + ')');
                }
            }
        }
    }
}
