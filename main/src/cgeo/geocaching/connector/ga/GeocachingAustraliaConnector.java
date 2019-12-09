package cgeo.geocaching.connector.ga;

import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

public class GeocachingAustraliaConnector extends AbstractConnector {

    @Override
    @NonNull
    public String getName() {
        return "Geocaching Australia";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return "GCAU";
    }

    @Override
    public boolean getHttps() {
        return false;
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    @NonNull
    public String getHost() {
        return "geocaching.com.au";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return (StringUtils.startsWithIgnoreCase(geocode, "GA") || StringUtils.startsWithIgnoreCase(geocode, "TP")) && isNumericId(geocode.substring(2));
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return getHostUrl() + "/cache/";
    }
}
