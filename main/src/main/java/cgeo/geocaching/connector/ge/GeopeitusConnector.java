package cgeo.geocaching.connector.ge;

import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class GeopeitusConnector extends AbstractConnector {

    @Override
    @NonNull
    public String getName() {
        return "geopeitus.ee";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return getName();
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + StringUtils.stripStart(cache.getGeocode().substring(2), "0");
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.geopeitus.ee";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return StringUtils.startsWith(geocode, "GE") && isNumericId(geocode.substring(2));
    }

    @NotNull
    @Override
    public String[] getGeocodeSqlLikeExpressions() {
        return new String[]{"GE%"};
    }


    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return getHostUrl() + "/aare/";
    }
}
