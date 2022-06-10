package cgeo.geocaching.test.mock;

import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GC40 extends MockedCache {

    public GC40() {
        super(new Geopoint(50.0, 5.0));
    }

    @Override
    public String getName() {
        return "Geocache";
    }

    @Override
    public float getDifficulty() {
        return 1.0f;
    }

    @Override
    public float getTerrain() {
        return 1.5f;
    }

    @NonNull
    @Override
    public String getGeocode() {
        return "GC40";
    }

    @Override
    public String getOwnerDisplayName() {
        return "Pierre Cao, Speerpunt, adopted by Grokky";
    }

    @NonNull
    @Override
    public String getOwnerUserId() {
        return "Grokky Grokson";
    }

    @NonNull
    @Override
    public CacheSize getSize() {
        return CacheSize.REGULAR;
    }

    @Override
    public CacheType getType() {
        return CacheType.TRADITIONAL;
    }

    @Override
    public String getHint() {
        return "";
    }

    @Override
    public String getDescription() {
        return "<br> <br> This is the oldest cache that you can log on continental Europe.<br>";
    }

    @Override
    public String getShortDescription() {
        return "";
    }

    @Override
    public String getCacheId() {
        return "64";
    }

    @Override
    public String getGuid() {
        return "e10600a4-fd99-474d-aa0d-34a96672906a";
    }

    @Override
    public String getLocation() {
        return "Namur, Belgium";
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.test.mock.MockedCache#isOwn()
     */
    @Override
    public boolean isOwner() {
        if ("Grokky Grokson".equals(Settings.getUserName())) {
            return true;
        }
        return super.isOwner();
    }

    @Override
    public Date getHiddenDate() {
        try {
            return GCLogin.parseGcCustomDate("2000-07-07", getDateFormat());
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @NonNull
    @Override
    public List<String> getAttributes() {
        final String[] attributes = {
                "available_yes",
                "onehour_yes"
        };
        return new MockedLazyInitializedList<>(attributes);
    }

    @Override
    public Map<LogType, Integer> getLogCounts() {
        final Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);
        logCounts.put(LogType.FOUND_IT, 12730);
        logCounts.put(LogType.DIDNT_FIND_IT, 14);
        logCounts.put(LogType.NOTE, 707);
        logCounts.put(LogType.ENABLE_LISTING, 1);
        logCounts.put(LogType.TEMP_DISABLE_LISTING, 1);
        logCounts.put(LogType.OWNER_MAINTENANCE, 7);
        logCounts.put(LogType.NEEDS_MAINTENANCE, 4);
        return logCounts;
    }

    @Override
    public int getFavoritePoints() {
        return 5829;
    }

    @Override
    @NonNull
    public List<Image> getSpoilers() {
        final ArrayList<Image> spoilers = new ArrayList<>();
        final Image mockedImage = Image.NONE;
        spoilers.add(mockedImage);
        spoilers.add(mockedImage);
        return spoilers;
    }

    @NonNull
    @Override
    public List<Waypoint> getWaypoints() {
        return new ArrayList<>();
    }

}
