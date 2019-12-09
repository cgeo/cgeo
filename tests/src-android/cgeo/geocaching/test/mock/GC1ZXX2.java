package cgeo.geocaching.test.mock;

import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GC1ZXX2 extends MockedCache {

    public GC1ZXX2() {
        super(new Geopoint(52.373217, 9.710800));
    }

    @Override
    public String getName() {
        return "Hannopoly: Eislisenstrasse";
    }

    @Override
    public float getDifficulty() {
        return 3.0f;
    }

    @NonNull
    @Override
    public String getGeocode() {
        return "GC1ZXX2";
    }

    @Override
    public String getOwnerDisplayName() {
        return "Rich Uncle Pennybags";
    }

    @NonNull
    @Override
    public CacheSize getSize() {
        return CacheSize.OTHER;
    }

    @Override
    public float getTerrain() {
        return 1.5f;
    }

    @Override
    public CacheType getType() {
        return CacheType.TRADITIONAL;
    }

    @Override
    public boolean isArchived() {
        return true;
    }

    @NonNull
    @Override
    public String getOwnerUserId() {
        return "daniel5005";
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
    public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2009-10-16", getDateFormat());
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @NonNull
    @Override
    public List<String> getAttributes() {
        final String[] attributes = {
                "bicycles_yes",
                "available_yes",
                "stroller_yes",
                "parking_yes",
                "onehour_yes",
                "kids_yes",
                "dogs_yes"
        };
        return new MockedLazyInitializedList<>(attributes);
    }

    @Override
    public Map<LogType, Integer> getLogCounts() {
        final Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);
        logCounts.put(LogType.PUBLISH_LISTING, 1);
        logCounts.put(LogType.FOUND_IT, 368);
        logCounts.put(LogType.POST_REVIEWER_NOTE, 1);
        logCounts.put(LogType.DIDNT_FIND_IT, 7);
        logCounts.put(LogType.NOTE, 10);
        logCounts.put(LogType.ARCHIVE, 1);
        logCounts.put(LogType.ENABLE_LISTING, 2);
        logCounts.put(LogType.TEMP_DISABLE_LISTING, 3);
        logCounts.put(LogType.OWNER_MAINTENANCE, 7);
        return logCounts;
    }

    @Override
    public int getFavoritePoints() {
        return 30;
    }

    @Override
    public String getPersonalNote() {
        if ("blafoo".equals(this.getMockedDataUser()) || "JoSaMaJa".equals(this.getMockedDataUser())) {
            return "Test für c:geo";
        }
        return super.getPersonalNote();
    }

}
