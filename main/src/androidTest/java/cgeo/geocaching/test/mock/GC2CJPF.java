package cgeo.geocaching.test.mock;

import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GC2CJPF extends MockedCache {

    public GC2CJPF() {
        super(new Geopoint(52.425067, 9.664200));
    }

    @Override
    public String getName() {
        return "Kinderwald KiC";
    }

    @Override
    public float getDifficulty() {
        return 2.5f;
    }

    @NonNull
    @Override
    public String getGeocode() {
        return "GC2CJPF";
    }

    @Override
    public String getOwnerDisplayName() {
        return "Tom03";
    }

    @Override
    public boolean isArchived() {
        return true;
    }

    @NonNull
    @Override
    public String getOwnerUserId() {
        return getOwnerDisplayName();
    }

    @NonNull
    @Override
    public CacheSize getSize() {
        return CacheSize.SMALL;
    }

    @Override
    public float getTerrain() {
        return 2.0f;
    }

    @Override
    public CacheType getType() {
        return CacheType.MULTI;
    }

    @Override
    public String getHint() {
        return "Das Final ist unter Steinen";
    }

    @Override
    public String getDescription() {
        return "Kleiner Multi über 7 Stationen";
    }

    @Override
    public String getShortDescription() {
        return "Von Nachwuchs-Cachern für Nachwuchs-Cacher.";
    }

    @Override
    public String getCacheId() {
        return "1811409";
    }

    @Override
    public String getGuid() {
        return "73246a5a-ebb9-4d4f-8db9-a951036f5376";
    }

    @Override
    public String getLocation() {
        return "Niedersachsen, Germany";
    }

    @Override
    public boolean isFound() {
        if ("blafoo".equals(this.getMockedDataUser())) {
            return true;
        }
        return super.isFound();
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.test.mock.MockedCache#isOwn()
     */
    @Override
    public boolean isOwner() {
        if ("Tom03".equals(Settings.getUserName())) {
            return true;
        }
        return super.isOwner();
    }

    @Override
    public boolean isFavorite() {
        if ("blafoo".equals(this.getMockedDataUser())) {
            return true;
        }
        return super.isFavorite();
    }

    @Override
    public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2010-07-31", getDateFormat());
        } catch (final ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @NonNull
    @Override
    public List<String> getAttributes() {
        final String[] attributes = {
                "motorcycles_no",
                "wheelchair_no",
                "winter_yes",
                "available_yes",
                "wading_yes",
                "scenic_yes",
                "onehour_yes",
                "kids_yes",
                "bicycles_yes",
                "dogs_yes"
        };
        return new MockedLazyInitializedList<>(attributes);
    }

    @Override
    public Map<LogType, Integer> getLogCounts() {
        final Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);
        logCounts.put(LogType.PUBLISH_LISTING, 1);
        logCounts.put(LogType.FOUND_IT, 119);
        logCounts.put(LogType.DIDNT_FIND_IT, 3);
        logCounts.put(LogType.NOTE, 7);
        logCounts.put(LogType.ENABLE_LISTING, 2);
        logCounts.put(LogType.TEMP_DISABLE_LISTING, 2);
        logCounts.put(LogType.OWNER_MAINTENANCE, 3);
        logCounts.put(LogType.NEEDS_MAINTENANCE, 2);
        return logCounts;
    }

    @Override
    public int getFavoritePoints() {
        return 7;
    }

    @NonNull
    @Override
    public List<Image> getSpoilers() {
        return Collections.singletonList(new Image.Builder().setUrl("http://www.blafoo.de/images/Kinderwald.jpg").setTitle("Cache listing background image").build());
    }

    @NonNull
    @Override
    public List<Waypoint> getWaypoints() {
        final List<Waypoint> waypoints = new ArrayList<>();
        waypoints.add(new Waypoint("FINAL", null, "GC2CJPF Final", "FN", "", WaypointType.FINAL));
        waypoints.add(new Waypoint("PARKNG", new Geopoint("N 52° 25.384 E 009° 39.023"), "GC2CJPF Parking", "PK", "Kein \"offizieller\" Parkplatz, Parken trotzdem möglich.", WaypointType.PARKING));
        waypoints.add(new Waypoint("START", new Geopoint("N 52° 25.504 E 009° 39.852"), "GC2CJPF Start", "ST", "", WaypointType.PUZZLE));
        waypoints.add(new Waypoint("SCENIC", new Geopoint("N 52° 25.488 E 009° 39.432"), "Aussichtspunkt", "WO", "Ehemalige Finallocation wo es gebrannt hat. Gleichzeitig netter Aussichtspunkt.", WaypointType.WAYPOINT));
        return waypoints;
    }

}
