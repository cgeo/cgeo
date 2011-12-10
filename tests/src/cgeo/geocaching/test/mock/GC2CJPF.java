package cgeo.geocaching.test.mock;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GC2CJPF extends MockedCache {

    final protected Geopoint userCoords;

    public GC2CJPF() {
        super(new Geopoint(52425067, 9664200));
        this.userCoords = new Geopoint("N 52° 25.111 E 009° 39.111");
    }

    @Override
    public String getName() {
        return "Kinderwald KiC";
    }

    @Override
    public float getDifficulty() {
        return 2.5f;
    }

    @Override
    public String getGeocode() {
        return "GC2CJPF";
    }

    @Override
    public String getOwner() {
        return "Tom03";
    }
    @Override
    public String getOwnerReal() {
        return getOwner();
    }

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
        if ("blafoo".equals(this.getUserLoggedIn())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isFavorite() {
        if ("blafoo".equals(this.getUserLoggedIn())) {
            return true;
        }
        return false;
    }

    @Override
    public Date getHiddenDate() {
        try {
            return cgBase.parseGcCustomDate("31/07/2010");
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @Override
    public List<String> getAttributes() {
        String[] attributes = new String[] {
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
        return Arrays.asList(attributes);
    }

    @Override
    public Map<LogType, Integer> getLogCounts() {
        Map<LogType, Integer> logCounts = new HashMap<LogType, Integer>();
        logCounts.put(LogType.LOG_PUBLISH_LISTING, 1);
        logCounts.put(LogType.LOG_FOUND_IT, 62);
        logCounts.put(LogType.LOG_DIDNT_FIND_IT, 3);
        logCounts.put(LogType.LOG_NOTE, 6);
        logCounts.put(LogType.LOG_ENABLE_LISTING, 2);
        logCounts.put(LogType.LOG_TEMP_DISABLE_LISTING, 2);
        logCounts.put(LogType.LOG_OWNER_MAINTENANCE, 3);
        logCounts.put(LogType.LOG_NEEDS_MAINTENANCE, 2);
        return logCounts;
    }

    @Override
    public int getFavoritePoints() {
        return 7;
    }

    @Override
    public String getLatitude() {
        if ("blafoo".equals(this.getUserLoggedIn())) {
            return userCoords.format(GeopointFormatter.Format.LAT_DECMINUTE);
        }
        return coords.format(GeopointFormatter.Format.LAT_DECMINUTE);
    }

    @Override
    public String getLongitude() {
        if ("blafoo".equals(this.getUserLoggedIn())) {
            return userCoords.format(GeopointFormatter.Format.LON_DECMINUTE);
        }
        return coords.format(GeopointFormatter.Format.LON_DECMINUTE);

    }

}
