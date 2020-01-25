package cgeo.geocaching.test.mock;

import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GC3XX5J extends MockedCache {

    public GC3XX5J() {
        super(new Geopoint(46.080467, 14.5));
    }

    @Override
    public String getName() {
        return "Zaraščen Tir";
    }

    @Override
    public float getDifficulty() {
        return 1.5f;
    }

    @NonNull
    @Override
    public String getGeocode() {
        return "GC3XX5J";
    }

    @Override
    public String getOwnerDisplayName() {
        return "David & Ajda";
    }

    @NonNull
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
        return CacheType.TRADITIONAL;
    }

    @Override
    public boolean isArchived() {
        // The cache has been archived since 2015-01-13.
        return true;
    }

    @NonNull
    @Override
    public String getOwnerUserId() {
        return "Murncki";
    }

    @Override
    public String getDescription() {
        return "SLO:<br>";
    }

    @Override
    public String getCacheId() {
        return "3220672";
    }

    @Override
    public String getGuid() {
        return "51e40dec-6272-4dad-934b-e175daaac265";
    }

    @Override
    public String getLocation() {
        return "Slovenia";
    }

    @Override
    public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2012-10-01", "yyyy-MM-dd");
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @NonNull
    @Override
    public List<String> getAttributes() {
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
        };
        return new MockedLazyInitializedList<>(attributes);
    }

    @Override
    public Map<LogType, Integer> getLogCounts() {
        final Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);
        logCounts.put(LogType.PUBLISH_LISTING, 2);
        logCounts.put(LogType.FOUND_IT, 65);
        logCounts.put(LogType.RETRACT, 1);
        return logCounts;
    }

    @Override
    public int getFavoritePoints() {
        return 1;
    }

    @Override
    public String getHint() {
        return "Odmakni kamen ob tiru / Remove the stone wich lies beside the rail";
    }

    @Override
    public String getShortDescription() {
        return "Kadar zbolimo nam pomaga...<br> <br> When we get sick, they are helpful...";
    }

    @Override
    public boolean isFound() {
        return Settings.getUserName().equals("mucek4");
    }

    @NonNull
    @Override
    public List<Image> getSpoilers() {
        return Collections.singletonList(new Image.Builder().setUrl("https://lh6.googleusercontent.com/-PoDn9PmtYmg/UGnOZLEQboI/AAAAAAAAAHM/hBXxerWnSdA/s254/lek-verovskova.jpg").setTitle("Cache listing background image").build());
    }

}
