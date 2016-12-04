package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.EnvironmentUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;

public class GpxExport extends AbstractExport {

    private String fileName = "geocache.gpx"; // used in tests

    public GpxExport() {
        super(R.string.export_gpx);
    }

    @Override
    public void export(@NonNull final List<Geocache> caches, @Nullable final Activity activity) {
        final String[] geocodes = getGeocodes(caches);
        calculateFileName(geocodes);
        if (activity == null) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new ExportTask(null).execute(geocodes);

        } else {
            // Show configuration dialog
            getExportDialog(geocodes, activity).show();
        }
    }

    private void calculateFileName(final String[] geocodes) {
        if (geocodes.length == 1) {
            // geocode as file name
            fileName = geocodes[0] + ".gpx";
        } else {
            // date and time as file name
            final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            fileName = "export_" + fileNameDateFormat.format(new Date()) + ".gpx";
        }
        fileName = FileUtils.getUniqueNamedFile(new File(Settings.getGpxExportDir(), fileName)).getName();
    }

    private Dialog getExportDialog(final String[] geocodes, final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.export_confirm_title, activity.getString(R.string.export_gpx)));

        final View layout = View.inflate(activity, R.layout.gpx_export_dialog, null);
        builder.setView(layout);

        final TextView text = ButterKnife.findById(layout, R.id.info);
        text.setText(activity.getString(R.string.export_confirm_message, Settings.getGpxExportDir(), fileName));

        final CheckBox shareOption = ButterKnife.findById(layout, R.id.share);
        shareOption.setChecked(Settings.getShareAfterExport());

        final CheckBox includeFoundStatus = ButterKnife.findById(layout, R.id.include_found_status);
        includeFoundStatus.setChecked(Settings.getIncludeFoundStatus());

        builder.setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                Settings.setShareAfterExport(shareOption.isChecked());
                Settings.setIncludeFoundStatus(includeFoundStatus.isChecked());
                dialog.dismiss();
                new ExportTask(activity).execute(geocodes);
            }
        });

        return builder.create();
    }

    private static String[] getGeocodes(final List<Geocache> caches) {
        return Geocache.getGeocodes(caches).toArray(new String[caches.size()]);
    }

    protected class ExportTask extends AsyncTaskWithProgress<String, File> {

        /**
         * Instantiates and configures the task for exporting field notes.
         *
         * @param activity
         *            optional: Show a progress bar and toasts
         */
        public ExportTask(final Activity activity) {
            super(activity, getProgressTitle());
        }

        private File getExportFile() {
            return FileUtils.getUniqueNamedFile(new File(Settings.getGpxExportDir(), fileName));
        }

        @Override
        protected File doInBackgroundInternal(final String[] geocodes) {
            // quick check for being able to write the GPX file
            if (!EnvironmentUtils.isExternalStorageAvailable()) {
                return null;
            }

            final List<String> allGeocodes = new ArrayList<>(Arrays.asList(geocodes));

            setMessage(CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.cache_counts, allGeocodes.size(), allGeocodes.size()));

            final File exportFile = getExportFile();
            BufferedWriter writer = null;
            try {
                final File exportLocation = new File(Settings.getGpxExportDir());
                FileUtils.mkdirs(exportLocation);

                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exportFile), CharEncoding.UTF_8));
                new GpxSerializer().writeGPX(allGeocodes, writer, new GpxSerializer.ProgressListener() {

                    @Override
                    public void publishProgress(final int countExported) {
                        ExportTask.this.publishProgress(countExported);
                    }
                });
            } catch (final IOException e) {
                Log.e("GpxExport.ExportTask export", e);
                IOUtils.closeQuietly(writer);
                // delete partial GPX file on error
                if (exportFile.exists()) {
                    FileUtils.deleteIgnoringFailure(exportFile);
                }

                return null;
            }

            return exportFile;
        }

        @Override
        protected void onPostExecuteInternal(final File exportFile) {
            final Activity activityLocal = activity;
            if (activityLocal != null) {
                if (exportFile != null) {
                    ActivityMixin.showToast(activityLocal, getName() + ' ' + activityLocal.getString(R.string.export_exportedto) + ": " + exportFile.toString());
                    if (Settings.getShareAfterExport()) {
                        ShareUtils.share(activityLocal, exportFile, "application/xml", R.string.export_gpx_to);
                    }
                } else {
                    ActivityMixin.showToast(activityLocal, activityLocal.getString(R.string.export_failed));
                }
            }
        }

    }
}
