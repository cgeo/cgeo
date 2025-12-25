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
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings

import androidx.annotation.NonNull

import java.text.ParseException
import java.util.ArrayList
import java.util.Date
import java.util.EnumMap
import java.util.List
import java.util.Map

class GC40 : MockedCache() {

    public GC40() {
        super(Geopoint(50.0, 5.0))
    }

    override     public String getName() {
        return "Geocache"
    }

    override     public Float getDifficulty() {
        return 1.0f
    }

    override     public Float getTerrain() {
        return 1.5f
    }

    override     public String getGeocode() {
        return "GC40"
    }

    override     public String getOwnerDisplayName() {
        return "Pierre Cao, Speerpunt, adopted by Grokky"
    }

    override     public String getOwnerUserId() {
        return "Grokky Grokson"
    }

    override     public CacheSize getSize() {
        return CacheSize.REGULAR
    }

    override     public CacheType getType() {
        return CacheType.TRADITIONAL
    }

    override     public String getHint() {
        return ""
    }

    override     public String getDescription() {
        return "This is the oldest cache that you can log on continental Europe.<br>"
    }

    override     public String getShortDescription() {
        return ""
    }

    override     public String getCacheId() {
        return "64"
    }

    override     public String getGuid() {
        return "e10600a4-fd99-474d-aa0d-34a96672906a"
    }

    override     public String getLocation() {
        return "Namur, Belgium"
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.test.mock.MockedCache#isOwn()
     */
    override     public Boolean isOwner() {
        if ("Grokky Grokson" == (Settings.getUserName())) {
            return true
        }
        return super.isOwner()
    }

    override     public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2000-07-07", getDateFormat())
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null
    }

    override     public List<String> getAttributes() {
        final String[] attributes = {
                "available_yes",
                "onehour_yes"
        }
        return MockedLazyInitializedList<>(attributes)
    }

    override     public Map<LogType, Integer> getLogCounts() {
        val logCounts: Map<LogType, Integer> = EnumMap<>(LogType.class)
        logCounts.put(LogType.FOUND_IT, 12730)
        logCounts.put(LogType.DIDNT_FIND_IT, 14)
        logCounts.put(LogType.NOTE, 707)
        logCounts.put(LogType.ENABLE_LISTING, 1)
        logCounts.put(LogType.TEMP_DISABLE_LISTING, 1)
        logCounts.put(LogType.OWNER_MAINTENANCE, 7)
        logCounts.put(LogType.NEEDS_MAINTENANCE, 4)
        return logCounts
    }

    override     public Int getFavoritePoints() {
        return 5829
    }

    override     public List<Image> getSpoilers() {
        val spoilers: ArrayList<Image> = ArrayList<>()
        val mockedImage: Image = Image.NONE
        spoilers.add(mockedImage)
        spoilers.add(mockedImage)
        return spoilers
    }

    override     public List<Waypoint> getWaypoints() {
        return ArrayList<>()
    }

}
