package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.EnvironmentUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.XmlUtils;
import cgeo.org.kxml2.io.KXmlSerializer;

import android.app.Activity;
import android.app.AlertDialog;
import android.location.Location;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.xmlpull.v1.XmlSerializer;

public class TrailHistoryExport {

    private String filename;

    public TrailHistoryExport(final Activity activity, final Runnable clearTrailHistory) {
        // quick check for being able to write the GPX file
        if (!EnvironmentUtils.isExternalStorageAvailable()) {
            return;
        }
        filename = "trail_" + CalendarUtils.formatDateTime("yyyy-MM-dd_hh-mm-ss") + ".gpx";

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.export_trailhistory_title);

        final View layout = View.inflate(activity, R.layout.gpx_export_trail_dialog, null);
        builder.setView(layout);

        final TextView text = layout.findViewById(R.id.info);
        text.setText(activity.getString(R.string.export_confirm_message, Settings.getGpxExportDir(), filename));

        final CheckBox shareOption = layout.findViewById(R.id.share);
        shareOption.setChecked(Settings.getShareAfterExport());

        final CheckBox clearAfterExport = layout.findViewById(R.id.clear_trailhistory_after_export);
        clearAfterExport.setChecked(Settings.getClearTrailAfterExportStatus());

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            Settings.setShareAfterExport(shareOption.isChecked());
            Settings.setClearTrailAfterExportStatus(clearAfterExport.isChecked());
            dialog.dismiss();
            new Export(activity, clearTrailHistory).execute(DataStore.loadTrailHistoryAsArray());
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private class Export extends AsyncTaskWithProgress<Location, File> {

        private static final String PREFIX_GPX = "";
        private static final String NS_GPX = "http://www.topografix.com/GPX/1/0";
        private static final String GPX_SCHEMA = NS_GPX + "/gpx.xsd";

        private static final String PREFIX_XSI = "xsi";
        private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

        private Runnable clearTrailHistory;

        Export(final Activity activity, final Runnable clearTrailHistory) {
            super(activity, activity.getString(R.string.export_trailhistory_title));
            this.clearTrailHistory = clearTrailHistory;
        }

        @Override
        protected File doInBackgroundInternal(final Location[] trail) {
            BufferedWriter writer = null;
            final File exportFile = new File(LocalStorage.getGpxExportDirectory(), filename);

            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exportFile), CharEncoding.UTF_8));
            } catch (IOException e) {
                return null;
            }
            final XmlSerializer gpx = new KXmlSerializer();
            try {
                int countExported = 0;
                gpx.setOutput(writer);

                gpx.startDocument(CharEncoding.UTF_8, true);
                gpx.setPrefix(PREFIX_GPX, NS_GPX);
                gpx.setPrefix(PREFIX_XSI, NS_XSI);

                gpx.startTag(NS_GPX, "gpx");
                gpx.attribute("", "version", "1.0");
                gpx.attribute("", "creator", "c:geo - http://www.cgeo.org/");
                gpx.attribute(NS_XSI, "schemaLocation", NS_GPX + " " + GPX_SCHEMA);

                    gpx.startTag(NS_GPX, "metadata");
                    XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo history trail");
                    XmlUtils.simpleText(gpx, NS_GPX, "time", CalendarUtils.formatDateTime("yyyy-MM-dd hh-mm-ss"));
                    gpx.endTag(NS_GPX, "metadata");

                    gpx.startTag(NS_GPX, "trk");
                        XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo history trail " + CalendarUtils.formatDateTime("yyyy-MM-dd hh-mm-ss"));
                        gpx.startTag(NS_GPX, "trkseg");
                            for (Location loc : trail) {
                                gpx.startTag(NS_GPX, "trkpt");
                                gpx.attribute(NS_GPX, "lat", String.valueOf(loc.getLatitude()));
                                gpx.attribute(NS_GPX, "lat", String.valueOf(loc.getLongitude()));
                                gpx.endTag(NS_GPX, "trkpt");
                                countExported++;
                                publishProgress(countExported);
                            }
                        gpx.endTag(NS_GPX, "trkseg");
                    gpx.endTag(NS_GPX, "trk");
                gpx.endTag(NS_GPX, "gpx");
            } catch (final IOException e) {
                // delete partial GPX file on error
                if (exportFile.exists()) {
                    FileUtils.deleteIgnoringFailure(exportFile);
                }
                return null;
            } finally {
                IOUtils.closeQuietly(writer);
            }
            return exportFile;
        }

        @Override
        protected void onPostExecuteInternal(final File exportFile) {
            if (null != activity) {
                if (null != exportFile) {
                    ActivityMixin.showToast(activity, String.format(activity.getString(R.string.export_trailhistory_success), exportFile.toString()));
                    if (Settings.getShareAfterExport()) {
                        ShareUtils.share(activity, exportFile, "application/xml", R.string.export_gpx_to);
                    }
                    if (Settings.getClearTrailAfterExportStatus()) {
                        clearTrailHistory.run();
                    }
                } else {
                    ActivityMixin.showToast(activity, activity.getString(R.string.export_failed));
                }
            }
        }
    }
}
