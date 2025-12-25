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
import cgeo.geocaching.settings.Settings

import androidx.annotation.NonNull

import java.text.ParseException
import java.util.Collections
import java.util.Date
import java.util.EnumMap
import java.util.List
import java.util.Map

class GC3XX5J : MockedCache() {

    public GC3XX5J() {
        super(Geopoint(46.080467, 14.5))
    }

    override     public String getName() {
        return "Zaraščen Tir"
    }

    override     public Float getDifficulty() {
        return 1.5f
    }

    override     public String getGeocode() {
        return "GC3XX5J"
    }

    override     public String getOwnerDisplayName() {
        return "David & Ajda"
    }

    override     public CacheSize getSize() {
        return CacheSize.SMALL
    }

    override     public Float getTerrain() {
        return 2.0f
    }

    override     public CacheType getType() {
        return CacheType.TRADITIONAL
    }

    override     public Boolean isArchived() {
        // The cache has been archived since 2015-01-13.
        return true
    }

    override     public String getOwnerUserId() {
        return "Murncki"
    }

    override     public String getDescription() {
        return "SLO:<br>"
    }

    override     public String getCacheId() {
        return "3220672"
    }

    override     public String getGuid() {
        return "51e40dec-6272-4dad-934b-e175daaac265"
    }

    override     public String getLocation() {
        return "Slovenia"
    }

    override     public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2012-10-01", "yyyy-MM-dd")
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null
    }

    override     public List<String> getAttributes() {
        final String[] attributes = {
                "stroller_no",
                "kids_no",
                "bicycles_yes",
                "night_yes",
                "available_yes",
                "stealth_yes",
                "parking_yes",
                "hike_short_yes",
                "parkngrab_yes",
                "dogs_yes"
        }
        return MockedLazyInitializedList<>(attributes)
    }

    override     public Map<LogType, Integer> getLogCounts() {
        val logCounts: Map<LogType, Integer> = EnumMap<>(LogType.class)
        logCounts.put(LogType.PUBLISH_LISTING, 2)
        logCounts.put(LogType.FOUND_IT, 65)
        logCounts.put(LogType.RETRACT, 1)
        return logCounts
    }

    override     public Int getFavoritePoints() {
        return 1
    }

    override     public String getHint() {
        return "Odmakni kamen ob tiru / Remove the stone wich lies beside the rail"
    }

    override     public String getShortDescription() {
        return "Kadar zbolimo nam pomaga...<br> <br> When we get sick, they are helpful..."
    }

    override     public Boolean isFound() {
        return Settings.getUserName() == ("mucek4")
    }

    override     public List<Image> getSpoilers() {
        return Collections.singletonList(Image.Builder().setUrl("https://lh6.googleusercontent.com/-PoDn9PmtYmg/UGnOZLEQboI/AAAAAAAAAHM/hBXxerWnSdA/s254/lek-verovskova.jpg").setTitle("Cache listing background image").build())
    }

}
