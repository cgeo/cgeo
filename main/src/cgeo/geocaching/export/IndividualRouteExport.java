package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.settings.Settings;
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

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlSerializer;

public class IndividualRouteExport {

    private String filename;

    public IndividualRouteExport(final Activity activity, final Route route) {
        // quick check for being able to write the GPX file
        if (!EnvironmentUtils.isExternalStorageAvailable()) {
            return;
        }
        filename = "route_" + CalendarUtils.formatDateTime("yyyy-MM-dd_HH-mm-ss") + ".gpx";

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.export_individualroute_title);

        final View layout = View.inflate(activity, R.layout.gpx_export_fragment, null);
        builder.setView(layout);

        final TextView text = layout.findViewById(R.id.info);
        text.setText(activity.getString(R.string.export_confirm_message, Settings.getGpxExportDir(), filename));

        final CheckBox shareOption = layout.findViewById(R.id.share);
        shareOption.setChecked(Settings.getShareAfterExport());

        builder.setPositiveButton(R.string.export, (dialog, which) -> {
            Settings.setShareAfterExport(shareOption.isChecked());
            dialog.dismiss();
            new Export(activity).execute(route.getSegments());
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private class Export extends AsyncTaskWithProgress<RouteSegment, File> {

        private static final String PREFIX_GPX = "";
        private static final String NS_GPX = "http://www.topografix.com/GPX/1/1";
        private static final String GPX_SCHEMA = NS_GPX + "/gpx.xsd";

        private static final String PREFIX_XSI = "xsi";
        private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

        Export(final Activity activity) {
            super(activity, activity.getString(R.string.export_individualroute_title));
        }

        @Override
        protected File doInBackgroundInternal(final RouteSegment[] trail) {
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

                final String timeInfo = CalendarUtils.formatDateTime("yyyy-MM-dd") + "T" + CalendarUtils.formatDateTime("hh:mm:ss") + "Z";

                gpx.startTag(NS_GPX, "metadata");
                XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo individual route");
                XmlUtils.simpleText(gpx, NS_GPX, "time", timeInfo);
                gpx.endTag(NS_GPX, "metadata");

                gpx.startTag(NS_GPX, "rte");
                XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo individual route " + timeInfo);
                for (RouteSegment loc : trail) {
                    gpx.startTag(null, "rtept");
                    final Geopoint point = loc.getPoint();
                    gpx.attribute(null, "lat", String.valueOf(point.getLatitude()));
                    gpx.attribute(null, "lon", String.valueOf(point.getLongitude()));
                    XmlUtils.simpleText(gpx, null, "name", loc.getItem().getGeocode());
                    gpx.endTag(null, "rtept");
                    countExported++;
                    publishProgress(countExported);
                }
                gpx.endTag(NS_GPX, "rte");
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
                    ActivityMixin.showToast(activity, String.format(activity.getString(R.string.export_individualroute_success), exportFile.toString()));
                    if (Settings.getShareAfterExport()) {
                        ShareUtils.share(activity, exportFile, "application/xml", R.string.export_gpx_to);
                    }
                } else {
                    ActivityMixin.showToast(activity, activity.getString(R.string.export_failed));
                }
            }
        }
    }

}
