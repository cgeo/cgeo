package cgeo.geocaching.export;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.cgData;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.IOUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CheckBox;

import java.io.BufferedOutputStream;
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
import java.util.TimeZone;

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
    static {
        fieldNoteDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected FieldnoteExport() {
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

        // AlertDialog has always dark style, so we have to apply it as well always
        final View layout = View.inflate(new ContextThemeWrapper(activity, R.style.dark), R.layout.fieldnote_export_dialog, null);
        builder.setView(layout);

        final CheckBox uploadOption = (CheckBox) layout.findViewById(R.id.upload);
        final CheckBox onlyNewOption = (CheckBox) layout.findViewById(R.id.onlynew);

        uploadOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onlyNewOption.setEnabled(uploadOption.isChecked());
            }
        });

        builder.setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                new ExportTask(
                        activity,
                        uploadOption.isChecked(),
                        onlyNewOption.isChecked())
                        .execute(caches);
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
            super(activity, getProgressTitle(), getString(R.string.export_fieldnotes_creating));
            this.activity = activity;
            this.upload = upload;
            this.onlyNew = onlyNew;
        }

        @Override
        protected Boolean doInBackgroundInternal(Geocache[] caches) {
            final StringBuilder fieldNoteBuffer = new StringBuilder();
            try {
                int i = 0;
                for (final Geocache cache : caches) {
                    if (cache.isLogOffline()) {
                        appendFieldNote(fieldNoteBuffer, cache, cgData.loadLogOffline(cache.getGeocode()));
                        publishProgress(++i);
                    }
                }
            } catch (final Exception e) {
                Log.e("FieldnoteExport.ExportTask generation", e);
                return false;
            }

            fieldNoteBuffer.append('\n');

            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return false;
            }

            exportLocation.mkdirs();

            final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            exportFile = new File(exportLocation.toString() + '/' + fileNameDateFormat.format(new Date()) + ".txt");

            Writer fileWriter = null;
            BufferedOutputStream buffer = null;
            try {
                final OutputStream os = new FileOutputStream(exportFile);
                buffer = new BufferedOutputStream(os);
                fileWriter = new OutputStreamWriter(buffer, CharEncoding.UTF_16);
                fileWriter.write(fieldNoteBuffer.toString());
            } catch (final IOException e) {
                Log.e("FieldnoteExport.ExportTask export", e);
                return false;
            } finally {
                IOUtils.closeQuietly(fileWriter);
                IOUtils.closeQuietly(buffer);
            }

            if (upload) {
                publishProgress(STATUS_UPLOAD);

                if (!Login.isActualLoginStatus()) {
                    // no need to upload (possibly large file) if we're not logged in
                    final StatusCode loginState = Login.login();
                    if (loginState != StatusCode.NO_ERROR) {
                        Log.e("FieldnoteExport.ExportTask upload: Login failed");
                    }
                }

                final String uri = "http://www.geocaching.com/my/uploadfieldnotes.aspx";
                final String page = Login.getRequestLogged(uri, null);

                if (StringUtils.isBlank(page)) {
                    Log.e("FieldnoteExport.ExportTask get page: No data from server");
                    return false;
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
        protected void onPostExecuteInternal(Boolean result) {
            if (null != activity) {
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
        protected void onProgressUpdateInternal(int status) {
            if (null != activity) {
                if (STATUS_UPLOAD == status) {
                    setMessage(getString(R.string.export_fieldnotes_uploading));
                } else {
                    setMessage(getString(R.string.export_fieldnotes_creating) + " (" + status + ')');
                }
            }
        }
    }

    static void appendFieldNote(final StringBuilder fieldNoteBuffer, final Geocache cache, final LogEntry log) {
        fieldNoteBuffer.append(cache.getGeocode())
                .append(',')
                .append(fieldNoteDateFormat.format(new Date(log.date)))
                .append(',')
                .append(StringUtils.capitalize(log.type.type))
                .append(",\"")
                .append(StringUtils.replaceChars(log.log, '"', '\''))
                .append("\"\n");
    }
}
