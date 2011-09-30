package cgeo.geocaching.test.mock;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.geopoint.Geopoint;


public class GC1ZXX2 extends MockedCache {

    public GC1ZXX2() {
        super(new Geopoint(52373217, 9710800));
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
        return "Rich Uncle Pennybags ";
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
    public String getType() {
        return "traditional";
    }

    @Override
    public boolean isArchived() {
        return false;
    }

    @Override
    public boolean isDisabled() {
        return true;
    }

    @Override
    public boolean isMembersOnly() {
        return false;
    }

    @Override
    public boolean isOwn() {
        return false;
    }

    @Override
    public String getOwnerReal() {
        return "daniel354";
    }

    @Override
    public String getHint() {
        return "";
    }

    @Override
    public String getDescription() {
        return "<center><img width=\"500\"";
    }

    @Override
    public String getShortDescription() {
        return "";
    }

    @Override
    public String getName() {
        return "Hannopoly: Eislisenstrasse ";
    }

}
