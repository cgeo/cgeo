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
        final String visited = GPXUtils.gpxNodeChildText(wptNode, "visited", CGEO_NS);
        if (visited != null) {
            waypoint.setVisited(Boolean.parseBoolean(StringUtils.trim(visited)));
        }
        final String originalCoordsEmpty = GPXUtils.gpxNodeChildText(wptNode, "originalCoordsEmpty", CGEO_NS);
        if (originalCoordsEmpty != null) {
            waypoint.setOriginalCoordsEmpty(Boolean.parseBoolean(StringUtils.trim(originalCoordsEmpty)));
        }
        final String userdefined = GPXUtils.gpxNodeChildText(wptNode, "userdefined", CGEO_NS);
        if (Boolean.parseBoolean(StringUtils.trim(userdefined))) {
            waypoint.setUserDefined();
        }
    }

}
