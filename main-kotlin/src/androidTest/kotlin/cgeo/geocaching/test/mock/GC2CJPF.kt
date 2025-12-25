// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.test.mock

import cgeo.geocaching.connector.gc.GCLogin
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings

import androidx.annotation.NonNull

import java.text.ParseException
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.EnumMap
import java.util.List
import java.util.Map

class GC2CJPF : MockedCache() {

    public GC2CJPF() {
        super(Geopoint(52.425067, 9.664200))
    }

    override     public String getName() {
        return "Kinderwald KiC"
    }

    override     public Float getDifficulty() {
        return 2.5f
    }

    override     public String getGeocode() {
        return "GC2CJPF"
    }

    override     public String getOwnerDisplayName() {
        return "Tom03"
    }

    override     public Boolean isArchived() {
        return true
    }

    override     public String getOwnerUserId() {
        return getOwnerDisplayName()
    }

    override     public CacheSize getSize() {
        return CacheSize.SMALL
    }

    override     public Float getTerrain() {
        return 2.0f
    }

    override     public CacheType getType() {
        return CacheType.MULTI
    }

    override     public String getHint() {
        return "Das Final ist unter Steinen"
    }

    override     public String getDescription() {
        return "Kleiner Multi über 7 Stationen"
    }

    override     public String getShortDescription() {
        return "Von Nachwuchs-Cachern für Nachwuchs-Cacher."
    }

    override     public String getCacheId() {
        return "1811409"
    }

    override     public String getGuid() {
        return "73246a5a-ebb9-4d4f-8db9-a951036f5376"
    }

    override     public String getLocation() {
        return "Niedersachsen, Germany"
    }

    override     public Boolean isFound() {
        if ("blafoo" == (this.getMockedDataUser())) {
            return true
        }
        return super.isFound()
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.test.mock.MockedCache#isOwn()
     */
    override     public Boolean isOwner() {
        if ("Tom03" == (Settings.getUserName())) {
            return true
        }
        return super.isOwner()
    }

    override     public Boolean isFavorite() {
        if ("blafoo" == (this.getMockedDataUser())) {
            return true
        }
        return super.isFavorite()
    }

    override     public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2010-07-31", getDateFormat())
        } catch (final ParseException e) {
            // intentionally left blank
        }
        return null
    }

    override     public List<String> getAttributes() {
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
        }
        return MockedLazyInitializedList<>(attributes)
    }

    override     public Map<LogType, Integer> getLogCounts() {
        val logCounts: Map<LogType, Integer> = EnumMap<>(LogType.class)
        logCounts.put(LogType.PUBLISH_LISTING, 1)
        logCounts.put(LogType.FOUND_IT, 119)
        logCounts.put(LogType.DIDNT_FIND_IT, 3)
        logCounts.put(LogType.NOTE, 7)
        logCounts.put(LogType.ENABLE_LISTING, 2)
        logCounts.put(LogType.TEMP_DISABLE_LISTING, 2)
        logCounts.put(LogType.OWNER_MAINTENANCE, 3)
        logCounts.put(LogType.NEEDS_MAINTENANCE, 2)
        return logCounts
    }

    override     public Int getFavoritePoints() {
        return 7
    }

    override     public List<Image> getSpoilers() {
        return Collections.singletonList(Image.Builder().setUrl("http://www.blafoo.de/images/Kinderwald.jpg").setTitle("Cache listing background image").build())
    }

    override     public List<Waypoint> getWaypoints() {
        val waypoints: List<Waypoint> = ArrayList<>()
        waypoints.add(Waypoint("FINAL", null, "GC2CJPF Final", "FN", "", WaypointType.FINAL))
        waypoints.add(Waypoint("PARKNG", Geopoint("N 52° 25.384 E 009° 39.023"), "GC2CJPF Parking", "PK", "Kein \"offizieller\" Parkplatz, Parken trotzdem möglich.", WaypointType.PARKING))
        waypoints.add(Waypoint("START", Geopoint("N 52° 25.504 E 009° 39.852"), "GC2CJPF Start", "ST", "", WaypointType.PUZZLE))
        waypoints.add(Waypoint("SCENIC", Geopoint("N 52° 25.488 E 009° 39.432"), "Aussichtspunkt", "WO", "Ehemalige Finallocation wo es gebrannt hat. Gleichzeitig netter Aussichtspunkt.", WaypointType.WAYPOINT))
        return waypoints
    }

}
