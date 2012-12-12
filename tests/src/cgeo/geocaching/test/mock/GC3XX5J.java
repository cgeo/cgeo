package cgeo.geocaching.test.mock;

import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.LazyInitializedList;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
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

    @Override
    public String getGeocode() {
        return "GC3XX5J";
    }

    @Override
    public String getOwnerDisplayName() {
        return "David & Ajda";
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
        return CacheType.TRADITIONAL;
    }

    @Override
    public boolean isArchived() {
        return false;
    }

    @Override
    public String getOwnerUserId() {
        return "David & Ajda";
    }

    @Override
    public String getDescription() {
        return "SLO:<br />";
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
            return Login.parseGcCustomDate("2012-10-01", getDateFormat());
        } catch (ParseException e) {
            // intentionally left blank
        }
        return null;
    }

    @Override
    public LazyInitializedList<String> getAttributes() {
        String[] attributes = new String[] {
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
        return new MockedLazyInitializedList<String>(attributes);
    }


    @Override
    public Map<LogType, Integer> getLogCounts() {
        Map<LogType, Integer> logCounts = new HashMap<LogType, Integer>();
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
        return "Bqznxav xnzra bo gveh / Erzbir gur fgbar jvpu yvrf orfvqr gur envy";
    }

    @Override
    public String getShortDescription() {
        return "Kadar zbolimo nam pomaga...<br /> <br /> When we get sick, they are helpful...";
    }

    @Override
    public String getPersonalNote() {
        return super.getPersonalNote();
    }

}
