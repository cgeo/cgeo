package cgeo.geocaching.connector;

import cgeo.contacts.ContactsAddon;
import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.capability.IFavoriteCapability;
import cgeo.geocaching.connector.capability.ISearchByFilter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.ClipboardUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractConnector implements IConnector {

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return false;
    }

    @Override
    public Set<String> handledGeocodes(@NonNull final Set<String> geocodes) {
        final Set<String> strippedList = new HashSet<>();
        for (final String geocode : geocodes) {
            if (canHandle(geocode)) {
                strippedList.add(geocode);
            }
        }
        return strippedList;
    }

    @NotNull
    @Override
    public String[] getGeocodeSqlLikeExpressions() {
        return new String[]{"%"}; //will match everything
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
    public boolean supportsNamechange() {
        return false;
    }

    @Override
    public boolean supportsDescriptionchange() {
        return false;
    }

    @Override
    public String getExtraDescription() {
        return "";
    }

    @Override
    public boolean supportsSettingFoundState() {
        return false;
    }

    @Override
    @NonNull
    public String getLicenseText(@NonNull final Geocache cache) {
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
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final String urlPrefix = getCacheUrlPrefix();
        if (StringUtils.isEmpty(urlPrefix) || StringUtils.startsWith(url, urlPrefix)) {
            final String geocode = url.substring(urlPrefix.length());
            if (canHandle(geocode)) {
                return geocode;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public String getGeocodeFromText(@NonNull final String text) {
        return null;
    }

    @NonNull
    protected abstract String getCacheUrlPrefix();

    @Override
    public boolean isHttps() {
        return true;
    }

    @Override
    @NonNull
    public String getHostUrl() {
        if (StringUtils.isBlank(getHost())) {
            return "";
        }
        return (isHttps() ? "https://" : "http://") + getHost();
    }

    @Override
    @NonNull
    public String getTestUrl() {
        return getHostUrl();
    }

    @Override
    @Nullable
    public String getLongCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrl(cache);
    }

    @Override
    @Nullable
    public String getCacheLogUrl(@NonNull final Geocache cache, @NonNull final LogEntry logEntry) {
        return null; //by default, Connector does not support log urls
    }

    @Override
    @Nullable
    public String getCacheCreateNewLogUrl(@NonNull final Geocache cache) {
        return null; //by default, Connector does not support creating new logs online
    }

    @Override
    @Nullable
    public String getServiceSpecificLogId(@Nullable final String serviceLogId) {
        return serviceLogId; //by default, log id is directly usable
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public int getCacheMapMarkerId() {
        return R.drawable.marker_other;
    }

    @Override
    public int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_other;
    }

    @Override
    public int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker_other;
    }

    @Override
    public int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_other;
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
        } else if (geocache.getType() == CacheType.WEBCAM) {
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
            } else {
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
    @NonNull
    public String getWaypointGpxId(@NonNull final String prefix, @NonNull final String geocode) {
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
        addCapability(list, GeocacheFilterType.NAME, R.string.feature_search_keyword);
        addCapability(list, GeocacheFilterType.DISTANCE, R.string.feature_search_center);
        addCapability(list, ISearchByGeocode.class, R.string.feature_search_geocode);
        addCapability(list, GeocacheFilterType.OWNER, R.string.feature_search_owner);
        addCapability(list, GeocacheFilterType.LOG_ENTRY, R.string.feature_search_finder);
        if (supportsLogging()) {
            list.add(feature(R.string.feature_online_logging));
        }
        if (supportsLogImages()) {
            list.add(feature(R.string.feature_log_images));
        }
        addCapability(list, PersonalNoteCapability.class, R.string.feature_personal_notes);
        if (supportsOwnCoordinates()) {
            list.add(feature(R.string.feature_own_coordinates));
        }
        addCapability(list, WatchListCapability.class, R.string.feature_watch_list);
        addCapability(list, IFavoriteCapability.class, R.string.feature_favorite);
        addCapability(list, IVotingCapability.class, R.string.feature_voting);
        return list;
    }

    private void addCapability(final List<String> capabilities, final Class<? extends IConnector> clazz, @StringRes final int featureResourceId) {
        if (clazz.isInstance(this)) {
            capabilities.add(feature(featureResourceId));
        }
    }

    private void addCapability(final List<String> capabilities, final GeocacheFilterType filterType, @StringRes final int featureResourceId) {
        if (this instanceof ISearchByFilter && ((ISearchByFilter) this).getFilterCapabilities().contains(filterType)) {
            capabilities.add(feature(featureResourceId));
        }
    }

    private static String feature(@StringRes final int featureResourceId) {
        return CgeoApplication.getInstance().getString(featureResourceId);
    }

    @Override
    @NonNull
    public List<UserAction> getUserActions(final UserAction.UAContext user) {
        final List<UserAction> actions = getDefaultUserActions();

        if (this instanceof ISearchByFilter) {
            final ISearchByFilter sbf = (ISearchByFilter) this;
            if (sbf.getFilterCapabilities().contains(GeocacheFilterType.OWNER)) {
                actions.add(new UserAction(R.string.user_menu_view_hidden, R.drawable.ic_menu_owned, context -> CacheListActivity.startActivityOwner(context.getContext(), context.userName)));
            }
            if (sbf.getFilterCapabilities().contains(GeocacheFilterType.LOG_ENTRY)) {
                actions.add(new UserAction(R.string.user_menu_view_found, R.drawable.ic_menu_emoticons, context -> CacheListActivity.startActivityFinder(context.getContext(), context.userName)));
            }
        }

        actions.add(new UserAction(R.string.copy_to_clipboard, R.drawable.ic_menu_copy, context -> {
            ClipboardUtils.copyToClipboard(context.userName);
            ActivityMixin.showToast(context.getContext(), R.string.clipboard_copy_ok);
        }));
        return actions;
    }

    /**
     * @return user actions which are always available (independent of cache or trackable)
     */
    @NonNull
    public static List<UserAction> getDefaultUserActions() {
        final List<UserAction> actions = new ArrayList<>();
        if (ContactsAddon.isAvailable()) {
            actions.add(new UserAction(R.string.user_menu_open_contact, R.drawable.ic_menu_contactcard, context -> ContactsAddon.openContactCard(context.getContext(), context.userName)));
        }

        return actions;
    }

    public void logout() {
    }

    public String getShortHost() {
        return StringUtils.remove(getHost(), "www.");
    }

    @Override
    @Nullable
    public String getCreateAccountUrl() {
        return null;
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (!(obj instanceof AbstractConnector)) {
            return false;
        }

        return ((AbstractConnector) obj).getName().equals(this.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }


}
