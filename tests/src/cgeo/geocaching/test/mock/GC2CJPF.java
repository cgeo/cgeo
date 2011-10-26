package cgeo.geocaching.test.mock;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GC2CJPF extends MockedCache {

    public GC2CJPF() {
        super(new Geopoint(52425067, 9664200));
    }

    @Override
    public String getName() {
        return "Kinderwald KiC";
    }

    @Override
    public Float getDifficulty() {
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
    public Float getTerrain() {
        return 2.0f;
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.MULTI;
    }

    @Override
    public String getHint() {
        return "Das Final (unter Steinen) ist mit GC gekennzeichnet.";
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
    public Map<Integer, Integer> getLogCounts() {
        Map<Integer, Integer> logCounts = new HashMap<Integer, Integer>();
        logCounts.put(cgBase.LOG_PUBLISH_LISTING, 1);
        logCounts.put(cgBase.LOG_FOUND_IT, 55);
        logCounts.put(cgBase.LOG_DIDNT_FIND_IT, 1);
        logCounts.put(cgBase.LOG_NOTE, 5);
        logCounts.put(cgBase.LOG_ENABLE_LISTING, 2);
        logCounts.put(cgBase.LOG_TEMP_DISABLE_LISTING, 2);
        logCounts.put(cgBase.LOG_OWNER_MAINTENANCE, 2);
        logCounts.put(cgBase.LOG_NEEDS_MAINTENANCE, 2);
        return logCounts;
    }

    @Override
    public Integer getFavoritePoints() {
        return new Integer(6);
    }

}
