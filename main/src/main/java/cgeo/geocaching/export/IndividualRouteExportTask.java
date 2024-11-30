package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.xml.XmlUtils;
import cgeo.org.kxml2.io.KXmlSerializer;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlSerializer;

public class IndividualRouteExportTask extends AsyncTaskWithProgress<RouteSegment, Uri> {
    private final String filename;
    private final boolean exportAsTrack;

    private static final String PREFIX_GPX = "";
    private static final String NS_GPX = "http://www.topografix.com/GPX/1/1";
    private static final String GPX_SCHEMA = NS_GPX + "/gpx.xsd";

    private static final String PREFIX_XSI = "xsi";
    private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    IndividualRouteExportTask(final Activity activity, final String filename, final boolean exportAsTrack) {
        super(activity, activity.getString(R.string.export_individualroute_title));
        this.filename = filename;
        this.exportAsTrack = exportAsTrack;
    }

    @Override
    protected Uri doInBackgroundInternal(final RouteSegment[] trail) {
        final Uri uri = ContentStorage.get().create(PersistableFolder.GPX, filename);
        if (uri == null) {
            return null;
        }
        final XmlSerializer gpx = new KXmlSerializer();
        Writer writer = null;
        try {
            final OutputStream os = ContentStorage.get().openForWrite(uri);
            if (os == null) {
                return null;
            }
            writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

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

            final String timeInfo = CalendarUtils.formatDateTime("yyyy-MM-dd") + "T" + CalendarUtils.formatDateTime("hh:mm:ss") + "Z";

            gpx.startTag(NS_GPX, "metadata");
            XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo individual route");
            XmlUtils.simpleText(gpx, NS_GPX, "time", timeInfo);
            gpx.endTag(NS_GPX, "metadata");

            for (RouteSegment loc : trail) {
                exportPoint(gpx, "wpt", loc.getItem().getPoint(), loc.getItem().getIdentifier());
            }

            gpx.startTag(NS_GPX, exportAsTrack ? "trk" : "rte");
            XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo individual route " + timeInfo);
            if (exportAsTrack) {
                gpx.startTag(null, "trkseg");
            }
            for (RouteSegment loc : trail) {
                final String segmentName = loc.getItem().getIdentifier();
                if (exportAsTrack) {
                    final ArrayList<Geopoint> points = loc.getPoints();
                    // trkseg does not have a name entity, so we put the name into the last trkpt
                    final int size = points.size();
                    int current = 1;
                    for (Geopoint point : points) {
                        exportPoint(gpx, "trkpt", point, current < size ? null : segmentName);
                        current++;
                    }
                } else {
                    exportPoint(gpx, "rtept", loc.getPoint(), segmentName);
                }
                countExported++;
                publishProgress(countExported);
            }
            if (exportAsTrack) {
                gpx.endTag(null, "trkseg");
            }
            gpx.endTag(NS_GPX, exportAsTrack ? "trk" : "rte");
            gpx.endTag(NS_GPX, "gpx");
        } catch (final IOException e) {
            Log.w("Could not write route to uri '" + uri + "'", e);
            // delete partial GPX file on error
            ContentStorage.get().delete(uri);
            return null;
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return uri;
    }

    private void exportPoint(final XmlSerializer gpx, final String tag, final Geopoint point, @Nullable final String name) throws IOException {
        gpx.startTag(null, tag);
        gpx.attribute(null, "lat", String.valueOf(point.getLatitude()));
        gpx.attribute(null, "lon", String.valueOf(point.getLongitude()));
        if (name != null) {
            XmlUtils.simpleText(gpx, null, "name", name);
        }
        gpx.endTag(null, tag);
    }

    @Override
    protected void onPostExecuteInternal(final Uri exportUri) {
        if (null != activity) {
            if (null != exportUri) {
                ShareUtils.shareOrDismissDialog(activity, exportUri, ShareUtils.TYPE_XML, R.string.export, String.format(activity.getString(R.string.export_individualroute_success), UriUtils.toUserDisplayableString(exportUri)));
            } else {
                ActivityMixin.showToast(activity, activity.getString(R.string.export_failed));
            }
        }
    }
}
