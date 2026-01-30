package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.xml.XmlUtils;
import cgeo.org.kxml2.io.KXmlSerializer;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlSerializer;

public class IndividualRouteExportTask extends AsyncTaskWithProgress<RouteSegment, Uri> {
    private final String filename;
    private final boolean exportAsTrack;

    private static final String PREFIX_GPX = "";
    private static final String NS_GPX = "http://www.topografix.com/GPX/1/1";
    private static final String GPX_SCHEMA = NS_GPX + "/gpx.xsd";

    private static final String PREFIX_XSI = "xsi";
    private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String PREFIX_GROUNDSPEAK = "groundspeak";
    private static final String NS_GROUNDSPEAK = "http://www.groundspeak.com/cache/1/0/1";
    private static final String GROUNDSPEAK_SCHEMA = NS_GROUNDSPEAK + "/cache.xsd";

    private static final String PREFIX_GSAK = "gsak";
    private static final String NS_GSAK = "http://www.gsak.net/xmlv1/6";
    private static final String GSAK_SCHEMA = NS_GSAK + "/gsak.xsd";

    private static final SynchronizedDateFormat dateFormatZ = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

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
            gpx.setPrefix(PREFIX_GROUNDSPEAK, NS_GROUNDSPEAK);
            gpx.setPrefix(PREFIX_GSAK, NS_GSAK);

            gpx.startTag(NS_GPX, "gpx");
            gpx.attribute("", "version", "1.1");
            gpx.attribute("", "creator", "c:geo - http://www.cgeo.org/");
            gpx.attribute(NS_XSI, "schemaLocation", NS_GPX + " " + GPX_SCHEMA + " " + NS_GROUNDSPEAK + " " + GROUNDSPEAK_SCHEMA + " " + NS_GSAK + " " + GSAK_SCHEMA);

            final String timeInfo = CalendarUtils.formatDateTime("yyyy-MM-dd") + "T" + CalendarUtils.formatDateTime("HH:mm:ss") + "Z";

            gpx.startTag(NS_GPX, "metadata");
            XmlUtils.simpleText(gpx, NS_GPX, "name", "c:geo individual route");
            XmlUtils.simpleText(gpx, NS_GPX, "time", timeInfo);
            gpx.endTag(NS_GPX, "metadata");

            for (RouteSegment loc : trail) {
                // Export waypoints with cache/waypoint name for better readability and GPX compatibility
                exportWaypoint(gpx, loc.getItem());
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

    private void exportWaypoint(final XmlSerializer gpx, @NonNull final RouteItem item) throws IOException {
        final Geopoint point = item.getPoint();
        if (point == null) {
            return;
        }

        gpx.startTag(NS_GPX, "wpt");
        gpx.attribute("", "lat", String.valueOf(point.getLatitude()));
        gpx.attribute("", "lon", String.valueOf(point.getLongitude()));

        // Add name
        XmlUtils.simpleText(gpx, NS_GPX, "name", item.getIdentifier());

        // Add description with cache/waypoint name
        final String itemName = item.getName();
        if (StringUtils.isNotBlank(itemName)) {
            XmlUtils.simpleText(gpx, NS_GPX, "desc", itemName);
        }

        // Add type-specific information
        if (item.getType() == RouteItem.RouteItemType.GEOCACHE) {
            final Geocache cache = item.getGeocache();
            if (cache != null) {
                addGeocacheExtensions(gpx, cache);
            }
        } else if (item.getType() == RouteItem.RouteItemType.WAYPOINT) {
            final Waypoint waypoint = item.getWaypoint();
            if (waypoint != null) {
                addWaypointExtensions(gpx, waypoint);
            }
        }

        gpx.endTag(NS_GPX, "wpt");
    }

    private void addGeocacheExtensions(final XmlSerializer gpx, @NonNull final Geocache cache) throws IOException {
        // Add groundspeak cache extension
        gpx.startTag(NS_GROUNDSPEAK, "cache");
        gpx.attribute("", "id", cache.getCacheId());
        gpx.attribute("", "available", !cache.isDisabled() ? "True" : "False");
        gpx.attribute("", "archived", cache.isArchived() ? "True" : "False");

        XmlUtils.multipleTexts(gpx, NS_GROUNDSPEAK,
                "name", cache.getName(),
                "placed_by", cache.getOwnerDisplayName(),
                "type", cache.getType().pattern,
                "container", cache.getSize().id,
                "difficulty", integerIfPossible(cache.getDifficulty()),
                "terrain", integerIfPossible(cache.getTerrain()));

        gpx.endTag(NS_GROUNDSPEAK, "cache");

        // Add GSAK extension
        gpx.startTag(NS_GSAK, "wptExtension");
        XmlUtils.multipleTexts(gpx, NS_GSAK,
                "Watch", gpxBoolean(cache.isOnWatchlist()),
                "IsPremium", gpxBoolean(cache.isPremiumMembersOnly()),
                "FavPoints", Integer.toString(cache.getFavoritePoints()),
                "GcNote", StringUtils.trimToEmpty(cache.getPersonalNote()));

        if (cache.isFound()) {
            final long visited = cache.getVisitedDate();
            if (0 != visited) {
                gpx.startTag(NS_GSAK, "UserFound");
                gpx.text(dateFormatZ.format(new Date(visited)));
                gpx.endTag(NS_GSAK, "UserFound");
            }
        }

        gpx.endTag(NS_GSAK, "wptExtension");
    }

    private void addWaypointExtensions(final XmlSerializer gpx, @NonNull final Waypoint waypoint) throws IOException {
        // Add GSAK extension for waypoint parent reference
        gpx.startTag(NS_GSAK, "wptExtension");

        gpx.startTag(NS_GSAK, "Parent");
        gpx.text(waypoint.getGeocode());
        gpx.endTag(NS_GSAK, "Parent");

        if (waypoint.isUserDefined()) {
            gpx.startTag(NS_GSAK, "Child_ByGSAK");
            gpx.text("true");
            gpx.endTag(NS_GSAK, "Child_ByGSAK");
        }

        gpx.endTag(NS_GSAK, "wptExtension");
    }

    /**
     * @return XML schema compliant boolean representation of the boolean flag. This must be either true, false, 0 or 1,
     * but no other value (also not upper case True/False).
     */
    private static String gpxBoolean(final boolean boolFlag) {
        return boolFlag ? "true" : "false";
    }

    private static String integerIfPossible(final double value) {
        if (!Double.isFinite(value)) {
            return String.format(Locale.ENGLISH, "%s", value);
        }
        if (value == (long) value) {
            return String.format(Locale.ENGLISH, "%d", (long) value);
        }
        return String.format(Locale.ENGLISH, "%s", value);
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
