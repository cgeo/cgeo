package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.TrailHistoryElement;
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
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlSerializer;

public class TrailHistoryExport {

    private String filename;

    public TrailHistoryExport(final Activity activity, final Runnable clearTrailHistory) {
        // quick check for being able to write the GPX file
        if (!EnvironmentUtils.isExternalStorageAvailable()) {
            return;
        }
        filename = "trail_" + CalendarUtils.formatDateTime("yyyy-MM-dd_HH-mm-ss") + ".gpx";

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

    private class Export extends AsyncTaskWithProgress<TrailHistoryElement, File> {

        private static final String PREFIX_GPX = "";
        private static final String NS_GPX = "http://www.topografix.com/GPX/1/1";
        private static final String GPX_SCHEMA = NS_GPX + "/gpx.xsd";

        private static final String PREFIX_XSI = "xsi";
        private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

        private final Runnable clearTrailHistory;

        Export(final Activity activity, final Runnable clearTrailHistory) {
            super(activity, activity.getString(R.string.export_trailhistory_title));
            this.clearTrailHistory = clearTrailHistory;
        }

        @Override
        protected File doInBackgroundInternal(final TrailHistoryElement[] trail) {
            BufferedWriter writer = null;
            final File exportFile = new File(LocalStorage.getGpxExportDirectory(), filename);

            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exportFile), StandardCharsets.UTF_8));
            } catch (IOException e) {
                return null;
            }
            final XmlSerializer gpx = new KXmlSerializer();
            try {
                int countExported = 0;
                gpx.setOutput(writer);

                gpx.startDocument(StandardCharsets.UTF_8.name(), true);
                gpx.setPrefix(PREFIX_GPX, NS_GPX);
                gpx.setPrefix(PREFIX_XSI, NS_XSI);

                gpx.startTag(NS_GPX, "gpx");
                gpx.attribute("", "version", "1.1");
                gpx.attribute("", "creator", "c:geo - http://www.cgeo.org/");
                gpx.attribute(NS_XSI, "schemaLocation", NS_GPX + " " + GPX_SCHEMA);

                    final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

                    gpx.startTag(NS_GPX, "metadata");
                    XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo history trail");
                    XmlUtils.simpleText(gpx, NS_GPX, "time", formatter.format(new Date()));
                    gpx.endTag(NS_GPX, "metadata");

                    gpx.startTag(NS_GPX, "trk");
                        XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo history trail " + formatter.format(new Date()));
                        gpx.startTag(NS_GPX, "trkseg");
                            for (TrailHistoryElement trailHistoryElement : trail) {
                                gpx.startTag(null, "trkpt");
                                    gpx.attribute(null, "lat", String.valueOf(trailHistoryElement.getLatitude()));
                                    gpx.attribute(null, "lon", String.valueOf(trailHistoryElement.getLongitude()));
                                    XmlUtils.simpleText(gpx, null, "time", formatter.format(trailHistoryElement.getTimestamp()));
                                    // write additional dummy elevation info to make track file importable by osm.org
                                    XmlUtils.simpleText(gpx, null, "ele", "0.0");
                                gpx.endTag(null, "trkpt");
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
