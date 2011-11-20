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


public class GC1ZXX2 extends MockedCache {

    public GC1ZXX2() {
        super(new Geopoint(52373217, 9710800));
    }

    @Override
    public String getName() {
        return "Hannopoly: Eislisenstrasse";
    }

    @Override
    public Float getDifficulty() {
        return 3.0f;
    }

    @Override
    public String getGeocode() {
        return "GC1ZXX2";
    }

    @Override
    public String getOwner() {
        return "Rich Uncle Pennybags";
    }

    @Override
    public CacheSize getSize() {
        return CacheSize.OTHER;
    }

    @Override
    public Float getTerrain() {
        return 1.5f;
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.TRADITIONAL;
    }

    @Override
    public boolean isArchived() {
        return true;
    }

    @Override
    public String getOwnerReal() {
        return "daniel354";
    }

    @Override
    public String getDescription() {
        return "<center><img width=\"500\"";
    }

    @Override
    public String getCacheId() {
        return "1433909";
    }

    @Override
    public String getGuid() {
        return "36d45871-b99d-46d6-95fc-ff86ab564c98";
    }

    @Override
    public String getLocation() {
        return "Niedersachsen, Germany";
    }

    @Override
    public boolean isWatchlist() {
        if ("blafoo".equals(this.getUserLoggedIn())) {
            return true;
        }
        return false;
    }

    @Override
    public Date getHiddenDate() {
        try {
            return cgBase.parseGcCustomDate("16/10/2009");
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @Override
    public List<String> getAttributes() {
        String[] attributes = new String[] {
                "bicycles_yes",
                "available_yes",
                "stroller_yes",
                "parking_yes",
                "onehour_yes",
                "kids_yes",
                "dogs_yes"
        };
        return Arrays.asList(attributes);
    }


    @Override
    public Map<Integer, Integer> getLogCounts() {
        Map<Integer, Integer> logCounts = new HashMap<Integer, Integer>();
        logCounts.put(cgBase.LOG_PUBLISH_LISTING, 1);
        logCounts.put(cgBase.LOG_FOUND_IT, 369);
        logCounts.put(cgBase.LOG_POST_REVIEWER_NOTE, 1);
        logCounts.put(cgBase.LOG_DIDNT_FIND_IT, 7);
        logCounts.put(cgBase.LOG_NOTE, 10);
        logCounts.put(cgBase.LOG_ARCHIVE, 1);
        logCounts.put(cgBase.LOG_ENABLE_LISTING, 2);
        logCounts.put(cgBase.LOG_TEMP_DISABLE_LISTING, 3);
        logCounts.put(cgBase.LOG_OWNER_MAINTENANCE, 7);
        return logCounts;
    }

    @Override
    public Integer getFavoritePoints() {
        return 47;
    }

    @Override
    public String getPersonalNote() {
        if ("blafoo".equals(this.getUserLoggedIn())) {
            return "Test für c:geo";
        }
        return null;
    }

}
