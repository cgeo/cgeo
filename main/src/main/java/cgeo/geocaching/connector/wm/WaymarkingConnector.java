package cgeo.geocaching.connector.wm;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WaymarkingConnector extends AbstractConnector /*TODO implements ISearchByGeocode, ISearchByFilter, ISearchByViewPort*/ {

    private final String name;

    private WaymarkingConnector() {
        // singleton
        name = LocalizationUtils.getString(R.string.init_wm);
        prefKey = R.string.preference_screen_wm;
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull private static final WaymarkingConnector INSTANCE = new WaymarkingConnector();
    }

    @NonNull
    public static WaymarkingConnector getInstance() {
        return WaymarkingConnector.Holder.INSTANCE;
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
    @NonNull
    public String getHost() {
        return "www.waymarking.com";
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return "https://" + getHost() + "/waymarks/";
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return "WM";
    }

    @Override
    @NonNull
    public String getExtraDescription() {
        return ""; //TODO maybe visit log requirements?
    }

    @Override
    public boolean supportsDifficultyTerrain() {
        return false;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    public boolean canEditLog(final Geocache cache, final LogEntry logEntry) {
        return false; // TODO
    }

    @Override
    public boolean canDeleteLog(final Geocache cache, final LogEntry logEntry) {
        return false; // TODO
    }

    @Override
    public boolean supportsLogImages() {
        return true;
    }

    @Override
    public boolean canLog(@NonNull final Geocache cache) {
        return false; // TODO
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes(@NonNull final Geocache geocache) {
        return List.of(LogType.FOUND_IT, LogType.NOTE);
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false; // TODO
    }

    /*TODO @Override
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final Geocache cache) {
        return new NoLoggingManager(this, cache);
    }*/

    @Override
    public boolean isActive() {
        return Settings.isWMConnectorActive() && Settings.isGCConnectorActive();
    }

    @Override
    public int getCacheMapMarkerId() {
        return R.drawable.marker;
    }

    @Override
    public int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_gc;
    }

    @Override
    public int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker;
    }

    @Override
    public int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_gc;
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
