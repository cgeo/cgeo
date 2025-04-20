package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.xml.XmlUtils;
import cgeo.org.kxml2.io.KXmlSerializer;

import android.app.Activity;
import android.net.Uri;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlSerializer;

public class TrailHistoryExportTask extends AsyncTaskWithProgress<TrailHistoryElement, Uri> {

    private static final String PREFIX_GPX = "";
    private static final String NS_GPX = "http://www.topografix.com/GPX/1/1";
    private static final String GPX_SCHEMA = NS_GPX + "/gpx.xsd";

    private static final String PREFIX_XSI = "xsi";
    private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private final String filename;
    private final Runnable clearTrailHistory;

    TrailHistoryExportTask(final Activity activity, final Runnable clearTrailHistory, final String filename) {
        super(activity, activity.getString(R.string.export_trailhistory_title));
        this.clearTrailHistory = clearTrailHistory;
        this.filename = filename;
    }

    @Override
    protected Uri doInBackgroundInternal(final TrailHistoryElement[] trail) {

        final Uri uri = ContentStorage.get().create(PersistableFolder.GPX, filename);
        if (uri == null) {
            return null;
        }

        BufferedWriter writer = null;
        OutputStream os = null;
        try {
            os = ContentStorage.get().openForWrite(uri);
            if (os == null) {
                return null;
            }
            try {
                writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                final XmlSerializer gpx = new KXmlSerializer();

                int countExported = 0;
                gpx.setOutput(writer);
                gpx.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

                gpx.startDocument(StandardCharsets.UTF_8.name(), true);
                gpx.setPrefix(PREFIX_GPX, NS_GPX);
                gpx.setPrefix(PREFIX_XSI, NS_XSI);

                gpx.startTag(NS_GPX, "gpx");
                gpx.attribute("", "version", "1.1");
                gpx.attribute("", "creator", "c:geo - http://www.cgeo.org/");
                gpx.attribute(NS_XSI, "schemaLocation", NS_GPX + " " + GPX_SCHEMA);

                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
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
                    // all decimal points have to be ".", thus use non-localizing methods
                    gpx.attribute(null, "lat", String.valueOf(trailHistoryElement.getLatitude()));
                    gpx.attribute(null, "lon", String.valueOf(trailHistoryElement.getLongitude()));
                    XmlUtils.simpleText(gpx, null, "ele", String.format(Locale.US, "%.2f", trailHistoryElement.getAltitude()));
                    XmlUtils.simpleText(gpx, null, "time", formatter.format(trailHistoryElement.getTimestamp()));
                    gpx.endTag(null, "trkpt");
                    countExported++;
                    publishProgress(countExported);
                }
                gpx.endTag(NS_GPX, "trkseg");
                gpx.endTag(NS_GPX, "trk");
                gpx.endTag(NS_GPX, "gpx");
            } catch (final IOException e) {
                // delete partial GPX file on error
                Log.e("IOException on trail export: " + e.getMessage());
                ContentStorage.get().delete(uri);
                return null;
            } finally {
                IOUtils.closeQuietly(writer);
                IOUtils.closeQuietly(os);
            }
        } catch (final Exception e) {
            Log.e("Exception on trail export: " + e.getMessage());
            return null;
        }

        return uri;
    }

    @Override
    protected void onPostExecuteInternal(final Uri uri) {
        if (null != activity) {
            if (null != uri) {
                ShareUtils.shareOrDismissDialog(activity, uri, ShareUtils.TYPE_XML, R.string.export, String.format(activity.getString(R.string.export_trailhistory_success), UriUtils.toUserDisplayableString(uri)));
                if (Settings.getClearTrailAfterExportStatus()) {
                    clearTrailHistory.run();
                }
            } else {
                ActivityMixin.showToast(activity, activity.getString(R.string.export_failed));
            }
        }
    }
}
