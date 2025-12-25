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
import cgeo.geocaching.models.Trackable

import androidx.annotation.NonNull

import java.text.ParseException
import java.util.ArrayList
import java.util.Date
import java.util.EnumMap
import java.util.List
import java.util.Map

class GC2JVEH : MockedCache() {

    override     public String getName() {
        return "Auf den Spuren des Indianer Jones Teil 1"
    }

    public GC2JVEH() {
        super(Geopoint(52.37225, 9.735367))
    }

    override     public Float getDifficulty() {
        return 5.0f
    }

    override     public Float getTerrain() {
        return 3.0f
    }

    override     public String getGeocode() {
        return "GC2JVEH"
    }

    override     public String getCacheId() {
        return "1997597"
    }

    override     public String getGuid() {
        return "07270e8c-72ec-4821-8cb7-b01483f94cb5"
    }

    override     public String getOwnerDisplayName() {
        return "indianerjones, der merlyn,reflektordetektor"
    }

    override     public String getOwnerUserId() {
        return "indianerjones"
    }

    override     public CacheSize getSize() {
        return CacheSize.SMALL
    }

    override     public CacheType getType() {
        return CacheType.MYSTERY
    }

    override     public String getShortDescription() {
        return "Aufgabe zum Start: Finde die Schattenlinie. !!!Die Skizze mit den Zahlen solltest du mitnehmen!!! Du solltest den cache so beginnen, das du station 2 in der Zeit von mo- fr von 11-19 Uhr und sa von 11-16 Uhr erledigt hast. Achtung: Damit ihr die Zahlenpause in druckbarer Größe sehen könnt müsst ihr über die Bildergalerie gehen nicht über den unten zu sehenden link....."
    }

    override     public String getDescription() {
        return "<img src=\"http://img.geocaching.com/cache/large/1711f8a1-796a-405b-82ba-8685f2e9f024.jpg\">"
    }

    override     public String getLocation() {
        return "Niedersachsen, Germany"
    }

    override     public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2010-11-28", getDateFormat())
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null
    }

    override     public List<String> getAttributes() {
        final String[] attributes = {
                "winter_yes",
                "flashlight_yes",
                "stealth_yes",
                "parking_yes",
                "abandonedbuilding_yes",
                "hike_med_yes",
                "rappelling_yes"
        }
        return MockedLazyInitializedList<>(attributes)
    }

    override     public Map<LogType, Integer> getLogCounts() {
        val logCounts: Map<LogType, Integer> = EnumMap<>(LogType.class)
        logCounts.put(LogType.FOUND_IT, 101)
        logCounts.put(LogType.NOTE, 7)
        logCounts.put(LogType.TEMP_DISABLE_LISTING, 1)
        logCounts.put(LogType.ENABLE_LISTING, 1)
        logCounts.put(LogType.PUBLISH_LISTING, 1)
        return logCounts
    }

    override     public Int getFavoritePoints() {
        return 23
    }

    override     public Boolean isPremiumMembersOnly() {
        return true
    }

    override     public List<Trackable> getInventory() {
        val inventory: ArrayList<Trackable> = ArrayList<>()
        inventory.add(Trackable())
        return inventory
    }

    override     public List<Image> getSpoilers() {
        val spoilers: ArrayList<Image> = ArrayList<>()
        val mockedImage: Image = Image.NONE
        spoilers.add(mockedImage)
        spoilers.add(mockedImage)
        spoilers.add(mockedImage)
        return spoilers
    }

    override     public String getPersonalNote() {
        if ("blafoo" == (this.getMockedDataUser())) {
            return "Selig"
        }
        return super.getPersonalNote()
    }

    override     public Boolean isFound() {
        if ("blafoo" == (this.getMockedDataUser()) || "JoSaMaJa" == (this.getMockedDataUser())) {
            return true
        }
        return super.isFound()
    }

    override     public Boolean isFavorite() {
        if ("blafoo" == (this.getMockedDataUser())) {
            return true
        }
        return super.isFavorite()
    }

    override     public Boolean isArchived() {
        return true
    }
}
