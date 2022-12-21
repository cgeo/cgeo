package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.capability.FieldNotesCapability;
import cgeo.geocaching.connector.capability.IAvatar;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.capability.IFavoriteCapability;
import cgeo.geocaching.connector.capability.IIgnoreCapability;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByFilter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByNextPage;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
import cgeo.geocaching.connector.capability.PgcChallengeCheckerCapability;
import cgeo.geocaching.connector.capability.Smiley;
import cgeo.geocaching.connector.capability.SmileyCapability;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.FoundNumCounter;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class GCConnector extends AbstractConnector implements ISearchByGeocode, ISearchByNextPage, ISearchByFilter, ISearchByViewPort, ILogin, ICredentials, FieldNotesCapability, IIgnoreCapability, WatchListCapability, PersonalNoteCapability, SmileyCapability, PgcChallengeCheckerCapability, IFavoriteCapability, IVotingCapability, IAvatar {

    private static final float MIN_RATING = 1;
    private static final float MAX_RATING = 5;

    public static final String SEARCH_CONTEXT_LEGACY_PAGING = "sc_gc_legacy_paging";
    public static final String SEARCH_CONTEXT_FILTER = "sc_gc_filter";
    public static final String SEARCH_CONTEXT_TOOK_TOTAL = "sc_gc_took_total";

    @NonNull
    private static final String GC_BASE_URL = "https://www.geocaching.com/";

    @NonNull
    private static final String CACHE_URL_SHORT = "https://coord.info/";

    @NonNull
    private static final String CACHE_URL_LONG = GC_BASE_URL + "geocache/";
    /**
     * Pocket queries downloaded from the website use a numeric prefix. The pocket query creator Android app adds a
     * verbatim "pocketquery" prefix.
     */
    @NonNull
    private static final Pattern GPX_ZIP_FILE_PATTERN = Pattern.compile("((\\d{7,})|(pocketquery))" + "(_.+)?" + "\\.zip", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for GC codes
     */
    @NonNull
    private static final Pattern PATTERN_GC_CODE = Pattern.compile(GCConstants.GEOCODE_PATTERN, Pattern.CASE_INSENSITIVE);

    private GCConnector() {
        // singleton
    }

    @NonNull
    public static GCConnector getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Indicates whether voting is possible for this cache for this type of log entry
     *
     * @param cache   cache to check
     * @param logType log type to check
     */
    @Override
    public boolean canVote(@NonNull final Geocache cache, @NonNull final LogType logType) {
        return Settings.isGCVoteLoginValid() && StringUtils.isNotBlank(cache.getGuid());
    }

    /**
     * Indicates whether voting is possible for this cache in general
     *
     * @param cache cache to check
     */
    @Override
    public boolean supportsVoting(@NonNull final Geocache cache) {
        return true;
    }

    @Override
    public String getDescription(final float rating) {
        return IVotingCapability.getDefaultFiveStarsDescription(rating);
    }

    /**
     * Posts single request to update the vote only.
     *
     * @param cache  cache to vote for
     * @param rating vote to set
     * @return status of the request
     */
    @Override
    public boolean postVote(@NonNull final Geocache cache, final float rating) {
        return GCVote.setRating(cache, rating);
    }

    /**
     * Indicates whether the given rating is acceptable (i.e. one might accept only integer values or restrict some other values)
     *
     * @param rating rating given
     * @return true if rating is acceptable, false if not
     */
    @Override
    public boolean isValidRating(final float rating) {
        return rating >= MIN_RATING && rating <= MAX_RATING;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return PATTERN_GC_CODE.matcher(geocode).matches();
    }

    @NotNull
    @Override
    public String[] getGeocodeSqlLikeExpressions() {
        return new String[]{"GC%"};
    }


    @Override
    @NonNull
    public String getLongCacheUrl(@NonNull final Geocache cache) {
        return CACHE_URL_LONG + cache.getGeocode();
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return CACHE_URL_SHORT + cache.getGeocode();
    }

    @Override
    public String getCacheLogUrl(@NonNull final Geocache cache, @NonNull final LogEntry logEntry) {
        if (StringUtils.isNotBlank(logEntry.serviceLogId)) {
            return CACHE_URL_SHORT + logEntry.serviceLogId;
        }
        return null;
    }

    @NonNull
    @Override
    public String getCacheCreateNewLogUrl(@NonNull final Geocache cache) {
        return GC_BASE_URL + "play/geocache/" + cache.getGeocode() + "/log";
    }

    @Override
    public boolean canAddPersonalNote(@NonNull final Geocache cache) {
        return Settings.isGCPremiumMember();
    }

    @Override
    public boolean supportsOwnCoordinates() {
        return true;
    }

    @Override
    public boolean canAddToWatchList(@NonNull final Geocache cache) {
        return true;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    public boolean supportsLogImages() {
        return true;
    }

    @Override
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final Geocache cache) {
        return new GCLoggingManager(activity, cache);
    }

    @Override
    public boolean canLog(@NonNull final Geocache cache) {
        return StringUtils.isNotBlank(cache.getCacheId());
    }

    @Override
    @NonNull
    public String getName() {
        return "geocaching.com";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return "GC";
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.geocaching.com";
    }

    @Override
    @NonNull
    public String getTestUrl() {
        return "https://" + getHost() + "/play";
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final DisposableHandler handler) {

        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final String page = GCParser.requestHtmlPage(geocode, guid, "y");

        if (StringUtils.isEmpty(page)) {
            Log.e("GCConnector.searchByGeocode: No data from server");
            final SearchResult search = new SearchResult();
            search.setError(this, StatusCode.CACHE_NOT_FOUND);
            return search;
        }

        final SearchResult searchResult = GCParser.parseCache(this, page, handler);

        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCConnector.searchByGeocode: No cache parsed");
            return searchResult;
        }

        // do not filter when searching for one specific cache
        return searchResult;
    }

    @Override
    public SearchResult searchByNextPage(final Bundle context) {
        final String filterConfig = context.getString(SEARCH_CONTEXT_FILTER);
        GeocacheFilter filter = null;
        if (filterConfig != null) {
            filter = GeocacheFilter.createFromConfig(filterConfig);
            final OriginGeocacheFilter origin = GeocacheFilter.findInChain(filter.getAndChainIfPossible(), OriginGeocacheFilter.class);
            if (origin != null && !origin.allowsCachesOf(this)) {
                return new SearchResult();
            }
        }

        if (context.getBoolean(SEARCH_CONTEXT_LEGACY_PAGING)) {
            return GCParser.searchByNextPage(this, context);
        }

        if (filter == null) {
            //non-legacy-nextpage needs a filter to proceed. If none is there then return empty result
            return new SearchResult();
        }

        return GCMap.searchByNextPage(this, context, filter);
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport) {
        return GCMap.searchByViewport(this, viewport);
    }

    @NonNull
    @Override
    public EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.of(GeocacheFilterType.DISTANCE, GeocacheFilterType.ORIGIN,
                GeocacheFilterType.NAME, GeocacheFilterType.OWNER,
                GeocacheFilterType.TYPE, GeocacheFilterType.SIZE,
                GeocacheFilterType.DIFFICULTY, GeocacheFilterType.TERRAIN, GeocacheFilterType.DIFFICULTY_TERRAIN,
                GeocacheFilterType.FAVORITES, GeocacheFilterType.STATUS, GeocacheFilterType.HIDDEN, GeocacheFilterType.LOG_ENTRY);
    }


    @Override
    @NonNull
    public SearchResult searchByFilter(@NonNull final GeocacheFilter filter) {
        return GCMap.searchByFilter(this, filter);
    }

    @Override
    public boolean isZippedGPXFile(@NonNull final String fileName) {
        return GPX_ZIP_FILE_PATTERN.matcher(fileName).matches();
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        final String user = Settings.getUserName();
        return StringUtils.isNotEmpty(user) && StringUtils.equalsIgnoreCase(cache.getOwnerUserId(), user);
    }

    @Override
    public boolean addToWatchlist(@NonNull final Geocache cache) {
        final boolean added = GCParser.addToWatchlist(cache);
        if (added) {
            DataStore.saveChangedCache(cache);
        }
        return added;
    }

    @Override
    public boolean removeFromWatchlist(@NonNull final Geocache cache) {
        final boolean removed = GCParser.removeFromWatchlist(cache);
        if (removed) {
            DataStore.saveChangedCache(cache);
        }
        return removed;
    }

    /**
     * Add a cache to the favorites list.
     *
     * This must not be called from the UI thread.
     *
     * @param cache the cache to add
     * @return {@code true} if the cache was successfully added, {@code false} otherwise
     */
    @Override
    public boolean addToFavorites(@NonNull final Geocache cache) {
        final boolean added = GCParser.addToFavorites(cache);
        if (added) {
            DataStore.saveChangedCache(cache);
        }
        return added;
    }

    /**
     * Remove a cache from the favorites list.
     *
     * This must not be called from the UI thread.
     *
     * @param cache the cache to add
     * @return {@code true} if the cache was successfully added, {@code false} otherwise
     */
    @Override
    public boolean removeFromFavorites(@NonNull final Geocache cache) {
        final boolean removed = GCParser.removeFromFavorites(cache);
        if (removed) {
            DataStore.saveChangedCache(cache);
        }
        return removed;
    }

    @Override
    public boolean uploadModifiedCoordinates(@NonNull final Geocache cache, @NonNull final Geopoint wpt) {
        final boolean uploaded = GCParser.uploadModifiedCoordinates(cache, wpt);
        if (uploaded) {
            DataStore.saveChangedCache(cache);
        }
        return uploaded;
    }

    @Override
    public boolean deleteModifiedCoordinates(@NonNull final Geocache cache) {
        final boolean deleted = GCParser.deleteModifiedCoordinates(cache);
        if (deleted) {
            DataStore.saveChangedCache(cache);
        }
        return deleted;
    }

    @Override
    public boolean uploadPersonalNote(@NonNull final Geocache cache) {
        final boolean uploaded = GCParser.uploadPersonalNote(cache);
        if (uploaded) {
            DataStore.saveChangedCache(cache);
        }
        return uploaded;
    }

    @Override
    public int getPersonalNoteMaxChars() {
        return 2500;
    }


    @Override
    public boolean supportsFavoritePoints(@NonNull final Geocache cache) {
        return Settings.isGCPremiumMember() && !cache.getType().isEvent() && !cache.isOwner();
    }

    @Override
    public boolean supportsAddToFavorite(@NonNull final Geocache cache, final LogType type) {
        return cache.supportsFavoritePoints() && type.isFoundLog();
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return StringUtils.EMPTY; // UNUSED
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        // coord.info URLs
        final String coordinfoUrl = TextUtils.getMatch(url, Pattern.compile("coord.info/" + GCConstants.GEOCODE_PATTERN, Pattern.CASE_INSENSITIVE), false, "");
        if (canHandle(coordinfoUrl)) {
            return coordinfoUrl;
        }
        // expanded geocaching.com URLs
        final String geocachingcomUrl = TextUtils.getMatch(url, Pattern.compile("geocaching.com/geocache/" + GCConstants.GEOCODE_PATTERN, Pattern.CASE_INSENSITIVE), false, "");
        if (canHandle(geocachingcomUrl)) {
            return geocachingcomUrl;
        }
        return null;
    }

    @Override
    @Nullable
    public String getGeocodeFromText(@NonNull final String text) {
        // Text containing a Geocode
        final String geocodeInText = TextUtils.getMatch(text, Pattern.compile(GCConstants.GEOCODE_PATTERN, Pattern.CASE_INSENSITIVE), false, "");
        if (canHandle(geocodeInText)) {
            return geocodeInText;
        }
        return null;
    }

    @Override
    public boolean isActive() {
        return Settings.isGCConnectorActive();
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
    public boolean login() {
        // login
        final StatusCode status = GCLogin.getInstance().login();
        // update cache counter
        FoundNumCounter.getAndUpdateFoundNum(this);

        return status == StatusCode.NO_ERROR;
    }

    @Override
    public void logout() {
        GCLogin.getInstance().logout();
    }

    @Override
    public String getUserName() {
        return GCLogin.getInstance().getActualUserName();
    }

    @Override
    public Credentials getCredentials() {
        return Settings.getCredentials(R.string.pref_username, R.string.pref_password);
    }

    @Override
    public int getCachesFound() {
        return GCLogin.getInstance().getActualCachesFound();
    }

    @Override
    public String getLoginStatusString() {
        return GCLogin.getInstance().getActualStatus();
    }

    @Override
    public boolean isLoggedIn() {
        return GCLogin.getInstance().isActualLoginStatus();
    }

    @Override
    @NonNull
    public String getWaypointGpxId(@NonNull final String prefix, @NonNull final String geocode) {
        String gpxId = prefix;
        if (StringUtils.isNotBlank(geocode) && geocode.length() > 2) {
            gpxId += geocode.substring(2);
        }
        return gpxId;
    }

    @Override
    @NonNull
    public String getWaypointPrefix(final String name) {
        String prefix = name;
        if (StringUtils.isNotBlank(prefix) && prefix.length() >= 2) {
            prefix = name.substring(0, 2);
        }
        return prefix;
    }

    @Override
    public int getUsernamePreferenceKey() {
        return R.string.pref_username;
    }

    @Override
    public int getPasswordPreferenceKey() {
        return R.string.pref_password;
    }

    @Override
    public int getAvatarPreferenceKey() {
        return R.string.pref_gc_avatar;
    }

    @NonNull
    @Override
    public List<UserAction> getUserActions(final UserAction.UAContext user) {
        final List<UserAction> actions = super.getUserActions(user);
        actions.add(new UserAction(R.string.user_menu_open_browser, R.drawable.ic_menu_face, context -> ShareUtils.openUrl(context.getContext(), "https://www.geocaching.com/p/default.aspx?u=" + Network.encode(context.userName))));
        if (StringUtils.isNotBlank(user.userGUID)) {
            actions.add(new UserAction(R.string.user_menu_send_message, R.drawable.ic_menu_message, context -> ShareUtils.openUrl(context.getContext(), "https://www.geocaching.com/account/messagecenter?recipientId=" + context.userGUID + (StringUtils.isNotBlank(context.geocode) ? "&gcCode=" + context.geocode : ""))));
        }
        actions.add(new UserAction(R.string.user_menu_send_email, R.drawable.ic_menu_email, context -> ShareUtils.openUrl(context.getContext(), "https://www.geocaching.com/email/?u=" + Network.encode(context.userName))));
        return actions;
    }

    @Override
    public boolean uploadFieldNotes(@NonNull final File exportFile) {
        if (!GCLogin.getInstance().isActualLoginStatus()) {
            // no need to upload (possibly large file) if we're not logged in
            final StatusCode loginState = GCLogin.getInstance().login();
            if (loginState != StatusCode.NO_ERROR) {
                Log.e("FieldNoteExport.ExportTask upload: Login failed");
                return false;
            }
        }

        final String uploadRes = Network.getResponseData(Network.postRequest("https://www.geocaching.com/api/proxy/web/v1/LogDrafts/upload", new Parameters(), null, "file-0", "text/plain", exportFile));

        if (StringUtils.isBlank(uploadRes)) {
            Log.e("FieldNoteExport.ExportTask upload: No data from server");
            return false;
        }
        return true;
    }

    @Override
    public boolean canIgnoreCache(@NonNull final Geocache cache) {
        return StringUtils.isNotEmpty(cache.getType().wptTypeId) && Settings.isGCPremiumMember();
    }

    @Override
    public boolean canRemoveFromIgnoreCache(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean addToIgnorelist(@NonNull final Geocache cache) {
        GCParser.ignoreCache(cache);
        return true;
    }

    @Override
    public boolean removeFromIgnorelist(@NonNull final Geocache cache) {
        // Not supported for gc.com
        return false;
    }

    @Override
    @Nullable
    public String getCreateAccountUrl() {
        return "https://www.geocaching.com/account/register";
    }

    @Override
    public List<Smiley> getSmileys() {
        return GCSmileysProvider.getSmileys();
    }

    @Override
    @Nullable
    public Smiley getSmiley(final int id) {
        return GCSmileysProvider.getSmiley(id);
    }

    @Override
    public boolean isChallengeCache(@NonNull final Geocache cache) {
        return cache.getType() == CacheType.MYSTERY && StringUtils.containsIgnoreCase(cache.getName(), "challenge");
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes(@NonNull final Geocache geocache) {
        final List<LogType> result = super.getPossibleLogTypes(geocache);
        // since May 2017 finding own caches is not allowed (except for events)
        if (geocache.isOwner()) {
            result.removeAll(Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, LogType.WEBCAM_PHOTO_TAKEN, LogType.NEEDS_ARCHIVE, LogType.NEEDS_MAINTENANCE));
        }
        // since May 2017 only one found log is allowed
        if (geocache.isFound()) {
            result.removeAll(Arrays.asList(LogType.FOUND_IT, LogType.ATTENDED, LogType.WEBCAM_PHOTO_TAKEN));
        }
        // since 2020 (?) needs maintenance and need archive logs are no longer separate log types (but presented in a submenu)
        result.removeAll(Arrays.asList(LogType.NEEDS_MAINTENANCE, LogType.NEEDS_ARCHIVE));
        return result;
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull
        private static final GCConnector INSTANCE = new GCConnector();
    }

}
