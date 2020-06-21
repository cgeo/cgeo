package cgeo.geocaching.test.mock;

import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GC2JVEH extends MockedCache {

    @Override
    public String getName() {
        return "Auf den Spuren des Indianer Jones Teil 1";
    }

    public GC2JVEH() {
        super(new Geopoint(52.37225, 9.735367));
    }

    @Override
    public float getDifficulty() {
        return 5.0f;
    }

    @Override
    public float getTerrain() {
        return 3.0f;
    }

    @NonNull
    @Override
    public String getGeocode() {
        return "GC2JVEH";
    }

    @Override
    public String getCacheId() {
        return "1997597";
    }

    @Override
    public String getGuid() {
        return "07270e8c-72ec-4821-8cb7-b01483f94cb5";
    }

    @Override
    public String getOwnerDisplayName() {
        return "indianerjones, der merlyn,reflektordetektor";
    }

    @NonNull
    @Override
    public String getOwnerUserId() {
        return "indianerjones";
    }

    @NonNull
    @Override
    public CacheSize getSize() {
        return CacheSize.SMALL;
    }

    @Override
    public CacheType getType() {
        return CacheType.MYSTERY;
    }

    @Override
    public String getShortDescription() {
        return "Aufgabe zum Start: Finde die Schattenlinie. !!!Die Skizze mit den Zahlen solltest du mitnehmen!!! Du solltest den cache so beginnen, das du station 2 in der Zeit von mo- fr von 11-19 Uhr und sa von 11-16 Uhr erledigt hast. Achtung: Damit ihr die Zahlenpause in druckbarer Größe sehen könnt müsst ihr über die Bildergalerie gehen nicht über den unten zu sehenden link.....";
    }

    @Override
    public String getDescription() {
        return "<img src=\"http://img.geocaching.com/cache/large/1711f8a1-796a-405b-82ba-8685f2e9f024.jpg\" />";
    }

    @Override
    public String getLocation() {
        return "Niedersachsen, Germany";
    }

    @Override
    public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2010-11-28", getDateFormat());
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @NonNull
    @Override
    public List<String> getAttributes() {
        final String[] attributes = {
                "winter_yes",
                "flashlight_yes",
                "stealth_yes",
                "parking_yes",
                "abandonedbuilding_yes",
                "hike_med_yes",
                "rappelling_yes"
        };
        return new MockedLazyInitializedList<>(attributes);
    }

    @Override
    public Map<LogType, Integer> getLogCounts() {
        final Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);
        logCounts.put(LogType.FOUND_IT, 101);
        logCounts.put(LogType.NOTE, 7);
        logCounts.put(LogType.TEMP_DISABLE_LISTING, 1);
        logCounts.put(LogType.ENABLE_LISTING, 1);
        logCounts.put(LogType.PUBLISH_LISTING, 1);
        return logCounts;
    }

    @Override
    public int getFavoritePoints() {
        return 23;
    }

    @Override
    public boolean isPremiumMembersOnly() {
        return true;
    }

    @Override
    @NonNull
    public List<Trackable> getInventory() {
        final ArrayList<Trackable> inventory = new ArrayList<>();
        inventory.add(new Trackable());
        return inventory;
    }

    @Override
    @NonNull
    public List<Image> getSpoilers() {
        final ArrayList<Image> spoilers = new ArrayList<>();
        final Image mockedImage = Image.NONE;
        spoilers.add(mockedImage);
        spoilers.add(mockedImage);
        spoilers.add(mockedImage);
        return spoilers;
    }

    @Override
    public String getPersonalNote() {
        if ("blafoo".equals(this.getMockedDataUser())) {
            return "Selig";
        }
        return super.getPersonalNote();
    }

    @Override
    public boolean isFound() {
        if ("blafoo".equals(this.getMockedDataUser()) || "JoSaMaJa".equals(this.getMockedDataUser())) {
            return true;
        }
        return super.isFound();
    }

    @Override
    public boolean isFavorite() {
        if ("blafoo".equals(this.getMockedDataUser())) {
            return true;
        }
        return super.isFavorite();
    }

    @Override
    public boolean isArchived() {
        return true;
    }
}
