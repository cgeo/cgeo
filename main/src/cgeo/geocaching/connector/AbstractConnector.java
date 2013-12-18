package cgeo.geocaching.connector;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractConnector implements IConnector {

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return false;
    }

    @Override
    public boolean supportsWatchList() {
        return false;
    }

    @Override
    public boolean addToWatchlist(Geocache cache) {
        return false;
    }

    @Override
    public boolean removeFromWatchlist(Geocache cache) {
        return false;
    }

    @Override
    public boolean supportsPersonalNote() {
        return false;
    }

    @Override
    public boolean uploadPersonalNote(Geocache cache) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsOwnCoordinates() {
        return false;
    }

    @Override
    public boolean uploadModifiedCoordinates(Geocache cache, Geopoint wpt) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@link IConnector}
     */
    @Override
    public boolean deleteModifiedCoordinates(Geocache cache) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsFavoritePoints() {
        return false;
    }

    @Override
    public boolean supportsLogging() {
        return false;
    }

    @Override
    public boolean supportsLogImages() {
        return false;
    }

    @Override
    public boolean canLog(Geocache cache) {
        return false;
    }

    @Override
    public ILoggingManager getLoggingManager(final LogCacheActivity activity, final Geocache cache) {
        return new NoLoggingManager();
    }

    @Override
    public String getLicenseText(final @NonNull Geocache cache) {
        return null;
    }

    @Override
    public boolean supportsUserActions() {
        return false;
    }

    protected static boolean isNumericId(final String string) {
        try {
            return Integer.parseInt(string) > 0;
        } catch (NumberFormatException e) {
        }
        return false;
    }

    @Override
    public boolean isZippedGPXFile(String fileName) {
        // don't accept any file by default
        return false;
    }

    @Override
    public boolean isReliableLatLon(boolean cacheHasReliableLatLon) {
        // let every cache have reliable coordinates by default
        return true;
    }

    @Override
    public String getGeocodeFromUrl(final String url) {
        final String urlPrefix = getCacheUrlPrefix();
        if (StringUtils.startsWith(url, urlPrefix)) {
            String geocode = url.substring(urlPrefix.length());
            if (canHandle(geocode)) {
                return geocode;
            }
        }
        return null;
    }

    abstract protected String getCacheUrlPrefix();

    @Override
    public String getLongCacheUrl(final @NonNull Geocache cache) {
        return getCacheUrl(cache);
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public int getCacheMapMarkerId(boolean disabled) {
        if (disabled) {
            return R.drawable.marker_disabled_other;
        }
        return R.drawable.marker_other;
    }

    @Override
    public List<LogType> getPossibleLogTypes(Geocache geocache) {
        final List<LogType> logTypes = new ArrayList<LogType>();
        if (geocache.isEventCache()) {
            logTypes.add(LogType.WILL_ATTEND);
            logTypes.add(LogType.ATTENDED);
            if (geocache.isOwner()) {
                logTypes.add(LogType.ANNOUNCEMENT);
            }
        } else if (CacheType.WEBCAM == geocache.getType()) {
            logTypes.add(LogType.WEBCAM_PHOTO_TAKEN);
        } else {
            logTypes.add(LogType.FOUND_IT);
        }
        if (!geocache.isEventCache()) {
            logTypes.add(LogType.DIDNT_FIND_IT);
        }
        logTypes.add(LogType.NOTE);
        if (!geocache.isEventCache()) {
            logTypes.add(LogType.NEEDS_MAINTENANCE);
        }
        if (geocache.isOwner()) {
            logTypes.add(LogType.OWNER_MAINTENANCE);
            if (geocache.isDisabled()) {
                logTypes.add(LogType.ENABLE_LISTING);
            }
            else {
                logTypes.add(LogType.TEMP_DISABLE_LISTING);
            }
            logTypes.add(LogType.ARCHIVE);
        }
        if (!geocache.isArchived() && !geocache.isOwner()) {
            logTypes.add(LogType.NEEDS_ARCHIVE);
        }
        return logTypes;
    }

    @Override
    public String getWaypointGpxId(String prefix, String geocode) {
        // Default: just return the prefix
        return prefix;
    }

    @Override
    public String getWaypointPrefix(String name) {
        // Default: just return the name
        return name;
    }

    @Override
    public int getMaxTerrain() {
        return 5;
    }

    @Override
    public final Collection<String> getCapabilities() {
        ArrayList<String> builder = new ArrayList<String>();
        builder.add(capability(ISearchByViewPort.class, R.string.feature_search_live_map));
        builder.add(capability(ISearchByKeyword.class, R.string.feature_search_keyword));
        builder.add(capability(ISearchByCenter.class, R.string.feature_search_center));
        builder.add(capability(ISearchByGeocode.class, R.string.feature_search_geocode));
        if (supportsUserActions()) {
            builder.add(feature(R.string.feature_search_user));
        }
        if (supportsLogging()) {
            builder.add(feature(R.string.feature_online_logging));
        }
        if (supportsLogImages()) {
            builder.add(feature(R.string.feature_log_images));
        }
        if (supportsPersonalNote()) {
            builder.add(feature(R.string.feature_personal_notes));
        }
        if (supportsOwnCoordinates()) {
            builder.add(feature(R.string.feature_own_coordinates));
        }
        if (supportsWatchList()) {
            builder.add(feature(R.string.feature_watch_list));
        }
        return builder;
    }

    private String capability(Class<? extends IConnector> clazz, final int featureResourceId) {
        if (clazz.isInstance(this)) {
            return feature(featureResourceId);
        }
        return StringUtils.EMPTY;
    }

    private static String feature(int featureResourceId) {
        return CgeoApplication.getInstance().getString(featureResourceId);
    }
}
