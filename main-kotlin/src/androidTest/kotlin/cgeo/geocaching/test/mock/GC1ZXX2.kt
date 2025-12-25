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

import androidx.annotation.NonNull

import java.text.ParseException
import java.util.Date
import java.util.EnumMap
import java.util.List
import java.util.Map

class GC1ZXX2 : MockedCache() {

    public GC1ZXX2() {
        super(Geopoint(52.373217, 9.710800))
    }

    override     public String getName() {
        return "Hannopoly: Eislisenstrasse"
    }

    override     public Float getDifficulty() {
        return 3.0f
    }

    override     public String getGeocode() {
        return "GC1ZXX2"
    }

    override     public String getOwnerDisplayName() {
        return "Rich Uncle Pennybags"
    }

    override     public CacheSize getSize() {
        return CacheSize.OTHER
    }

    override     public Float getTerrain() {
        return 1.5f
    }

    override     public CacheType getType() {
        return CacheType.TRADITIONAL
    }

    override     public Boolean isArchived() {
        return true
    }

    override     public String getOwnerUserId() {
        return "daniel5005"
    }

    override     public String getDescription() {
        return "<center><img width=\"500\""
    }

    override     public String getCacheId() {
        return "1433909"
    }

    override     public String getGuid() {
        return "36d45871-b99d-46d6-95fc-ff86ab564c98"
    }

    override     public String getLocation() {
        return "Niedersachsen, Germany"
    }

    override     public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2009-10-16", getDateFormat())
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null
    }

    override     public List<String> getAttributes() {
        final String[] attributes = {
                "bicycles_yes",
                "available_yes",
                "stroller_yes",
                "parking_yes",
                "onehour_yes",
                "kids_yes",
                "dogs_yes"
        }
        return MockedLazyInitializedList<>(attributes)
    }

    override     public Map<LogType, Integer> getLogCounts() {
        val logCounts: Map<LogType, Integer> = EnumMap<>(LogType.class)
        logCounts.put(LogType.PUBLISH_LISTING, 1)
        logCounts.put(LogType.FOUND_IT, 368)
        logCounts.put(LogType.POST_REVIEWER_NOTE, 1)
        logCounts.put(LogType.DIDNT_FIND_IT, 7)
        logCounts.put(LogType.NOTE, 10)
        logCounts.put(LogType.ARCHIVE, 1)
        logCounts.put(LogType.ENABLE_LISTING, 2)
        logCounts.put(LogType.TEMP_DISABLE_LISTING, 3)
        logCounts.put(LogType.OWNER_MAINTENANCE, 7)
        return logCounts
    }

    override     public Int getFavoritePoints() {
        return 30
    }

    override     public String getPersonalNote() {
        if ("blafoo" == (this.getMockedDataUser()) || "JoSaMaJa" == (this.getMockedDataUser())) {
            return "Test für c:geo"
        }
        return super.getPersonalNote()
    }

}
