package cgeo.geocaching.connector.wm;

import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class WaymarkingConnector extends AbstractConnector {

    @Override
    @NonNull
    public String getName() {
        return "Waymarking";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return "WM";
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.waymarking.com";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        // this connector has no user management
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return "https://" + getHost() + "/waymarks/";
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return StringUtils.startsWith(geocode, "WM");
    }

    @NotNull
    @Override
    public String[] getGeocodeSqlLikeExpressions() {
        return new String[]{"WM%"};
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        // coord.info URLs
        final String topLevel = StringUtils.substringAfterLast(url, "coord.info/");
        if (canHandle(topLevel)) {
            return topLevel;
        }
        // waymarking URLs https://www.waymarking.com/waymarks/WMNCDT_American_Legion_Flagpole_1983_University_of_Oregon
        final String waymark = StringUtils.substringBetween(url, "waymarks/", "_");
        return waymark != null && canHandle(waymark) ? waymark : null;
    }
}
