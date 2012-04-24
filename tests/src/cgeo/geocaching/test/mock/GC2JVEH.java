package cgeo.geocaching.test.mock;

import cgeo.geocaching.cgImage;
import cgeo.geocaching.cgTrackable;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
    public String getOwner() {
        return "indianerjones, der merlyn,reflektordetektor";
    }

    @Override
    public String getOwnerReal() {
        return "indianerjones";
    }

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
    public boolean isWatchlist() {
        if ("blafoo".equals(this.getMockedDataUser())) {
            return true;
        }
        return super.isWatchlist();
    }

    @Override
    public Date getHiddenDate() {
        try {
            return Login.parseGcCustomDate("28/11/2010", getDateFormat());
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @Override
    public List<String> getAttributes() {
        final String[] attributes = new String[] {
                "winter_yes",
                "flashlight_yes",
                "stealth_yes",
                "parking_yes",
                "abandonedbuilding_yes",
                "hike_med_yes",
                "rappelling_yes"
        };
        return Arrays.asList(attributes);
    }

    @Override
    public Map<LogType, Integer> getLogCounts() {
        final Map<LogType, Integer> logCounts = new HashMap<LogType, Integer>();
        logCounts.put(LogType.LOG_FOUND_IT, 66);
        logCounts.put(LogType.LOG_NOTE, 7);
        logCounts.put(LogType.LOG_TEMP_DISABLE_LISTING, 1);
        logCounts.put(LogType.LOG_ENABLE_LISTING, 1);
        logCounts.put(LogType.LOG_PUBLISH_LISTING, 1);
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
    public List<cgTrackable> getInventory() {
        final ArrayList<cgTrackable> inventory = new ArrayList<cgTrackable>();
        inventory.add(new cgTrackable());
        return inventory;
    }

    @Override
    public List<cgImage> getSpoilers() {
        final ArrayList<cgImage> spoilers = new ArrayList<cgImage>();
        final cgImage mockedImage = new cgImage(null, null, null);
        spoilers.add(mockedImage);
        spoilers.add(mockedImage);
        spoilers.add(mockedImage);
        return spoilers;
    }

}
