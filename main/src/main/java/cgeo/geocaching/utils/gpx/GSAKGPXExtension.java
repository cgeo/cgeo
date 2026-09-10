package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.xml.XmlNode;
import cgeo.geocaching.utils.xml.XmlUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class GSAKGPXExtension implements IGPXExtension {

    //Namespaces of extensions
    public static final Set<String> GSAK_NS = Set.of(
            "http://www.gsak.net/xmlv1/1",
            "http://www.gsak.net/xmlv1/2",
            "http://www.gsak.net/xmlv1/3",
            "http://www.gsak.net/xmlv1/4",
            "http://www.gsak.net/xmlv1/5",
            "http://www.gsak.net/xmlv1/6"
    );

    @Override
    public void enrichGeocache(final XmlNode wptType, final Geocache cache) {
        final XmlNode gsak = GPXUtils.gpxNodeChild(wptType, "wptExtension", GSAK_NS);
        if (gsak == null) {
            return;
        }
        final String watch = GPXUtils.gpxNodeChildText(gsak, "Watch", GSAK_NS);
        if (watch != null) {
            cache.setOnWatchlist(Boolean.parseBoolean(watch.trim()));
        }
        final String favPoints = GPXUtils.gpxNodeChildText(gsak, "FavPoints", GSAK_NS);
        if (favPoints != null) {
            try {
                cache.setFavoritePoints(Integer.parseInt(favPoints.trim()));
            } catch (final NumberFormatException ignored) {
                // ignore malformed favorite points
            }
        }
        final String gcNote = GPXUtils.gpxNodeChildText(gsak, "GcNote", GSAK_NS);
        if (StringUtils.isNotBlank(gcNote)) {
            cache.setPersonalNote(StringUtils.trim(gcNote), true);
        }
        final String isPremium = GPXUtils.gpxNodeChildText(gsak, "IsPremium", GSAK_NS);
        if (isPremium != null) {
            cache.setPremiumMembersOnly(Boolean.parseBoolean(isPremium.trim()));
        }
        final String code = GPXUtils.gpxNodeChildText(gsak, "Code", GSAK_NS);
        if (StringUtils.isNotBlank(code)) {
            cache.setGeocode(StringUtils.trim(code));
        }
        final String dnf = GPXUtils.gpxNodeChildText(gsak, "DNF", GSAK_NS);
        if (dnf != null && !cache.isFound()) {
            cache.setDNF(Boolean.parseBoolean(dnf.trim()));
        }
        final String dnfDate = GPXUtils.gpxNodeChildText(gsak, "DNFDate", GSAK_NS);
        if (dnfDate != null && cache.getVisitedDate() == 0) {
            final Date parsed = XmlUtils.parseDate(dnfDate);
            if (parsed != null) {
                cache.setVisitedDate(parsed.getTime());
            }
        }
        final String userFound = GPXUtils.gpxNodeChildText(gsak, "UserFound", GSAK_NS);
        if (userFound != null && cache.getVisitedDate() == 0) {
            final Date parsed = XmlUtils.parseDate(userFound);
            if (parsed != null) {
                cache.setVisitedDate(parsed.getTime());
            }
        }

        final StringBuilder userDataNote = new StringBuilder();
        appendUserData(userDataNote, GPXUtils.gpxNodeChildText(gsak, "UserData", GSAK_NS));
        for (int i = 2; i <= 4; i++) {
            appendUserData(userDataNote, GPXUtils.gpxNodeChildText(gsak, "User" + i, GSAK_NS));
        }
        if (StringUtils.isBlank(cache.getPersonalNote()) && userDataNote.length() > 0) {
            cache.setPersonalNote(userDataNote.toString().trim(), true);
        }
        final Geopoint originalCoords = XmlUtils.parseGeopoint(GPXUtils.gpxNodeChildText(gsak, "LatBeforeCorrect", GSAK_NS), GPXUtils.gpxNodeChildText(gsak, "LonBeforeCorrect", GSAK_NS), false);
        if (originalCoords != null) {
            final Waypoint original = new Waypoint(WaypointType.ORIGINAL.gpx, WaypointType.ORIGINAL, false);
            original.setGeocode(cache.getGeocode());
            original.setCoords(originalCoords);
            cache.setWaypoints(Collections.singletonList(original));
            cache.setUserModifiedCoords(true);
        }
    }

    private static void appendUserData(final StringBuilder buffer, final String userData) {
        if (StringUtils.isNotBlank(userData)) {
            buffer.append(' ').append(userData);
        }
    }

}
