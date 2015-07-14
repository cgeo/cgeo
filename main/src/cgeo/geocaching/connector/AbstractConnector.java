package cgeo.geocaching.connector;

import cgeo.contacts.ContactsAddon;
import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.UserAction.Context;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByFinder;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByOwner;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.location.Geopoint;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.functions.Action1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractConnector implements IConnector {

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return false;
    }

    @Override
    public Set<String> handledGeocodes(@NonNull final Set<String> geocodes) {
        final Set<String> strippedList = new HashSet<>();
        for (final String geocode: geocodes) {
            if (canHandle(geocode)) {
                strippedList.add(geocode);
            }
        }
        return strippedList;
    }

    @Override
    public boolean supportsWatchList() {
        return false;
    }

    @Override
    public boolean addToWatchlist(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean removeFromWatchlist(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean supportsPersonalNote() {
        return false;
    }

    @Override
    public boolean uploadPersonalNote(@NonNull final Geocache cache) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsOwnCoordinates() {
        return false;
    }

    @Override
    public boolean uploadModifiedCoordinates(@NonNull final Geocache cache, @NonNull final Geopoint wpt) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@link IConnector}
     */
    @Override
    public boolean deleteModifiedCoordinates(@NonNull final Geocache cache) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsFavoritePoints(@NonNull final Geocache cache) {
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
    public boolean canLog(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final Geocache cache) {
        return new NoLoggingManager();
    }

    @Override
    @NonNull
    public String getLicenseText(final @NonNull Geocache cache) {
        return StringUtils.EMPTY;
    }

    protected static boolean isNumericId(final String str) {
        try {
            return Integer.parseInt(str) > 0;
        } catch (final NumberFormatException ignored) {
        }
        return false;
    }

    @Override
    public boolean isZippedGPXFile(@NonNull final String fileName) {
        // don't accept any file by default
        return false;
    }

    @Override
    public boolean isReliableLatLon(final boolean cacheHasReliableLatLon) {
        // let every cache have reliable coordinates by default
        return true;
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final String urlPrefix = getCacheUrlPrefix();
        if (StringUtils.isEmpty(urlPrefix) || StringUtils.startsWith(url, urlPrefix)) {
            @NonNull final String geocode = url.substring(urlPrefix.length());
            if (canHandle(geocode)) {
                return geocode;
            }
        }
        return null;
    }

    @NonNull
    abstract protected String getCacheUrlPrefix();

    @Override
    @Nullable
    public String getLongCacheUrl(final @NonNull Geocache cache) {
        return getCacheUrl(cache);
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public int getCacheMapMarkerId(final boolean disabled) {
        if (disabled) {
            return R.drawable.marker_disabled_other;
        }
        return R.drawable.marker_other;
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes(@NonNull final Geocache geocache) {
        final List<LogType> logTypes = new ArrayList<>();
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
    public String getWaypointGpxId(final String prefix, @NonNull final String geocode) {
        // Default: just return the prefix
        return prefix;
    }

    @Override
    @NonNull
    public String getWaypointPrefix(final String name) {
        // Default: just return the name
        return name;
    }

    @Override
    public int getMaxTerrain() {
        return 5;
    }

    @Override
    @NonNull
    public final Collection<String> getCapabilities() {
        final List<String> list = new ArrayList<>();
        addCapability(list, ISearchByViewPort.class, R.string.feature_search_live_map);
        addCapability(list, ISearchByKeyword.class, R.string.feature_search_keyword);
        addCapability(list, ISearchByCenter.class, R.string.feature_search_center);
        addCapability(list, ISearchByGeocode.class, R.string.feature_search_geocode);
        addCapability(list, ISearchByOwner.class, R.string.feature_search_owner);
        addCapability(list, ISearchByFinder.class, R.string.feature_search_finder);
        if (supportsLogging()) {
            list.add(feature(R.string.feature_online_logging));
        }
        if (supportsLogImages()) {
            list.add(feature(R.string.feature_log_images));
        }
        if (supportsPersonalNote()) {
            list.add(feature(R.string.feature_personal_notes));
        }
        if (supportsOwnCoordinates()) {
            list.add(feature(R.string.feature_own_coordinates));
        }
        if (supportsWatchList()) {
            list.add(feature(R.string.feature_watch_list));
        }
        return list;
    }

    private void addCapability(final List<String> capabilities, final Class<? extends IConnector> clazz, final int featureResourceId) {
        if (clazz.isInstance(this)) {
            capabilities.add(feature(featureResourceId));
        }
    }

    private static String feature(final int featureResourceId) {
        return CgeoApplication.getInstance().getString(featureResourceId);
    }

    @Override
    public @NonNull
    List<UserAction> getUserActions() {
        final List<UserAction> actions = getDefaultUserActions();

        if (this instanceof ISearchByOwner) {
            actions.add(new UserAction(R.string.user_menu_view_hidden, new Action1<Context>() {

                @Override
                public void call(final Context context) {
                    CacheListActivity.startActivityOwner(context.activity, context.userName);
                }
            }));
        }

        if (this instanceof ISearchByFinder) {
            actions.add(new UserAction(R.string.user_menu_view_found, new Action1<UserAction.Context>() {

                @Override
                public void call(final Context context) {
                    CacheListActivity.startActivityFinder(context.activity, context.userName);
                }
            }));
        }
        return actions;
    }

    /**
     * @return user actions which are always available (independent of cache or trackable)
     */
    static @NonNull
    public List<UserAction> getDefaultUserActions() {
        final List<UserAction> actions = new ArrayList<>();
        if (ContactsAddon.isAvailable()) {
            actions.add(new UserAction(R.string.user_menu_open_contact, new Action1<UserAction.Context>() {

                @Override
                public void call(final Context context) {
                    ContactsAddon.openContactCard(context.activity, context.userName);
                }
            }));
        }

        return actions;
    }

    public void logout() {
    }
}
