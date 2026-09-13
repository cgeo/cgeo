package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.xml.XmlNode;

import java.util.List;

public interface IGPXExtension {

    default void enrichGeocache(final XmlNode wptNode, final Geocache cache) {
        //do nothing
    }

    default void enrichWaypoint(final XmlNode wptNode, final Waypoint waypoint) {
        //do nothing
    }

    default List<LogEntry> extractLogs(final XmlNode wptNode, final Geocache cache) {
        return null;
    }
}
