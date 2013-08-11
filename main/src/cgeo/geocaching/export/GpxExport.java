package cgeo.geocaching.export;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.CharEncoding;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.ContextThemeWrapper;
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

class GpxExport extends AbstractExport {

    protected GpxExport() {
        super(getString(R.string.export_gpx));
    }

    @Override
    public void export(final List<Geocache> caches, final Activity activity) {
        final String[] geocodes = getGeocodes(caches);
        if (null == activity) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new ExportTask(null).execute(geocodes);

        } else {
            // Show configuration dialog
            getExportDialog(geocodes, activity).show();
        }
    }

    private Dialog getExportDialog(final String[] geocodes, final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // AlertDialog has always dark style, so we have to apply it as well always
        final View layout = View.inflate(new ContextThemeWrapper(activity, R.style.dark), R.layout.gpx_export_dialog, null);
        builder.setView(layout);

        final TextView text = (TextView) layout.findViewById(R.id.info);
        text.setText(getString(R.string.export_gpx_info, Settings.getGpxExportDir()));

        final CheckBox shareOption = (CheckBox) layout.findViewById(R.id.share);

        shareOption.setChecked(Settings.getShareAfterExport());

        shareOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings.setShareAfterExport(shareOption.isChecked());
            }
        });

        builder.setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                new ExportTask(activity).execute(geocodes);
            }
        });

        return builder.create();
    }

    private static String[] getGeocodes(final List<Geocache> caches) {
        final ArrayList<String> allGeocodes = new ArrayList<String>(caches.size());
        for (final Geocache geocache : caches) {
            allGeocodes.add(geocache.getGeocode());
        }
        return allGeocodes.toArray(new String[allGeocodes.size()]);
    }

    protected class ExportTask extends AsyncTaskWithProgress<String, File> {
        private final Activity activity;

        /**
         * Instantiates and configures the task for exporting field notes.
         *
         * @param activity
         *            optional: Show a progress bar and toasts
         */
        public ExportTask(final Activity activity) {
            super(activity, getProgressTitle());
            this.activity = activity;
        }

        private File getExportFile() {
            final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            final Date now = new Date();
            return FileUtils.getUniqueNamedFile(Settings.getGpxExportDir() + File.separatorChar + "export_" + fileNameDateFormat.format(now) + ".gpx");
        }

        @Override
        protected File doInBackgroundInternal(String[] geocodes) {
            // quick check for being able to write the GPX file
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return null;
            }

            final List<String> allGeocodes = new ArrayList<String>(Arrays.asList(geocodes));

            setMessage(cgeoapplication.getInstance().getResources().getQuantityString(R.plurals.cache_counts, allGeocodes.size(), allGeocodes.size()));

            final File exportFile = getExportFile();
            BufferedWriter writer = null;
            try {
                final File exportLocation = new File(Settings.getGpxExportDir());
                exportLocation.mkdirs();

                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exportFile), CharEncoding.UTF_8));
                new GpxSerializer().writeGPX(allGeocodes, writer, new GpxSerializer.ProgressListener() {

                    @Override
                    public void publishProgress(final int countExported) {
                        ExportTask.this.publishProgress(countExported);
                    }
                });
            } catch (final Exception e) {
                Log.e("GpxExport.ExportTask export", e);

                if (writer != null) {
                    try {
                        writer.close();
                    } catch (final IOException e1) {
                        // Ignore double error
                    }
                }
                // delete partial gpx file on error
                if (exportFile.exists()) {
                    exportFile.delete();
                }

                return null;
            }

            return exportFile;
        }

        @Override
        protected void onPostExecuteInternal(final File exportFile) {
            if (null != activity) {
                if (exportFile != null) {
                    ActivityMixin.showToast(activity, getName() + ' ' + getString(R.string.export_exportedto) + ": " + exportFile.toString());
                    if (Settings.getShareAfterExport()) {
                        final Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportFile));
                        shareIntent.setType("application/xml");
                        activity.startActivity(Intent.createChooser(shareIntent, getString(R.string.export_gpx_to)));
                    }
                } else {
                    ActivityMixin.showToast(activity, getString(R.string.export_failed));
                }
            }
        }

    }
}
