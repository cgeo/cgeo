package cgeo.geocaching.connector.unknown;

import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class UnknownConnector extends AbstractConnector {

    @Override
    @NonNull
    public String getName() {
        return "Unknown caches";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        throw new IllegalStateException("no valid name for unknown connector");
    }

    @Override
    @Nullable
    public String getCacheUrl(@NonNull final Geocache cache) {
        return null;
    }

    @Override
    @NonNull
    public String getHost() {
        return StringUtils.EMPTY; // we have no host for these caches
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return StringUtils.isNotBlank(geocode);
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        throw new IllegalStateException("getCacheUrl cannot be called on unknown caches");
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        return null;
    }

    @Override
    public boolean supportsSettingFoundState() {
        return true;
    }
}
