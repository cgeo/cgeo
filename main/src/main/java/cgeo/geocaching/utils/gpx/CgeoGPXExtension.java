package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.EmojiUtilsLegacyMigration;
import cgeo.geocaching.utils.xml.XmlNode;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class CgeoGPXExtension implements IGPXExtension {

    //Namespaces of extensions
    private static final Set<String> CGEO_NS = Set.of(
            "http://www.cgeo.org/wptext/1/0"
    );

    @Override
    public void enrichGeocache(final XmlNode wptNode, final Geocache cache) {
        final String assignedEmojiText = GPXUtils.gpxNodeChildText(GPXUtils.gpxNodeChild(wptNode, "cacheExtension", CGEO_NS), "assignedEmoji", CGEO_NS);
        if (StringUtils.isNotBlank(assignedEmojiText)) {
            cache.setAssignedEmoji(EmojiUtilsLegacyMigration.parseGpxAssignedEmoji(assignedEmojiText));
        }
    }

    @Override
    public void enrichWaypoint(final XmlNode wptNode, final Waypoint waypoint) {
        final Boolean visited = GPXUtils.gpxNodeChildBoolean(wptNode, "visited", CGEO_NS, null);
        if (visited != null) {
            waypoint.setVisited(visited);
        }
        final Boolean originalCoordsEmpty = GPXUtils.gpxNodeChildBoolean(wptNode, "originalCoordsEmpty", CGEO_NS, null);
        if (originalCoordsEmpty != null) {
            waypoint.setOriginalCoordsEmpty(originalCoordsEmpty);
        }
        final Boolean userdefined = GPXUtils.gpxNodeChildBoolean(wptNode, "userdefined", CGEO_NS, Boolean.FALSE);
        if (userdefined != null && userdefined) {
            waypoint.setUserDefined();
        }
    }

}
