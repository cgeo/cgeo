package cgeo.geocaching.connector.oc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.UserInfo;
import cgeo.geocaching.connector.UserInfo.UserInfoStatus;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByFilter;
import cgeo.geocaching.connector.capability.ISearchByFinder;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByOwner;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.capability.IgnoreCapability;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.EnumSet;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class OCApiLiveConnector extends OCApiConnector implements ISearchByCenter, ISearchByViewPort, ILogin, ISearchByKeyword, ISearchByOwner, ISearchByFinder, ISearchByFilter, WatchListCapability, IgnoreCapability, PersonalNoteCapability {

    private final String cS;
    private final int isActivePrefKeyId;
    private final int tokenPublicPrefKeyId;
    private final int tokenSecretPrefKeyId;
    private UserInfo userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfoStatus.NOT_RETRIEVED);

    public OCApiLiveConnector(final String name, final String host, final boolean https, final String prefix, final String licenseString, @StringRes final int cKResId, @StringRes final int cSResId, final int isActivePrefKeyId, final int tokenPublicPrefKeyId, final int tokenSecretPrefKeyId, final ApiSupport apiSupport, @NonNull final String abbreviation) {
        super(name, host, https, prefix, CryptUtils.rot13(CgeoApplication.getInstance().getString(cKResId)), licenseString, apiSupport, abbreviation);

        cS = CryptUtils.rot13(CgeoApplication.getInstance().getString(cSResId));
        this.isActivePrefKeyId = isActivePrefKeyId;
        this.tokenPublicPrefKeyId = tokenPublicPrefKeyId;
        this.tokenSecretPrefKeyId = tokenSecretPrefKeyId;
    }

    @Override
    public boolean isActive() {
        return Settings.isOCConnectorActive(isActivePrefKeyId);
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport) {
        final SearchResult result = new SearchResult(OkapiClient.getCachesBBox(viewport, this));

        Log.d(String.format(Locale.getDefault(), "OC returning %d caches from search by viewport", result.getCount()));

        return result;
    }

    @Override
    public SearchResult searchByCenter(@NonNull final Geopoint center) {
        return new SearchResult(OkapiClient.getCachesAround(center, this));
    }

    @Override
    public SearchResult searchByOwner(@NonNull final String username) {
        return new SearchResult(OkapiClient.getCachesByOwner(username, this));
    }

    @Override
    public SearchResult searchByFinder(@NonNull final String username) {
        return new SearchResult(OkapiClient.getCachesByFinder(username, this));
    }

    @Override
    public OAuthLevel getSupportedAuthLevel() {

        if (Settings.hasOAuthAuthorization(tokenPublicPrefKeyId, tokenSecretPrefKeyId)) {
            return OAuthLevel.Level3;
        }
        return OAuthLevel.Level1;
    }

    @Override
    public String getCS() {
        return CryptUtils.rot13(cS);
    }

    @Override
    public int getTokenPublicPrefKeyId() {
        return tokenPublicPrefKeyId;
    }

    @Override
    public int getTokenSecretPrefKeyId() {
        return tokenSecretPrefKeyId;
    }

    @Override
    public boolean canAddToWatchList(@NonNull final Geocache cache) {
        return getApiSupport() == ApiSupport.current;
    }

    @Override
    public boolean addToWatchlist(@NonNull final Geocache cache) {
        final boolean added = OkapiClient.setWatchState(cache, true, this);

        if (added) {
            DataStore.saveChangedCache(cache);
        }

        return added;
    }

    @Override
    public boolean removeFromWatchlist(@NonNull final Geocache cache) {
        final boolean removed = OkapiClient.setWatchState(cache, false, this);

        if (removed) {
            DataStore.saveChangedCache(cache);
        }

        return removed;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final Geocache cache) {
        return new OkapiLoggingManager(activity, this, cache);
    }

    @Override
    public boolean canLog(@NonNull final Geocache cache) {
        return true;
    }

    private boolean supportsPersonalization() {
        return getSupportedAuthLevel() == OAuthLevel.Level3;
    }

    @Override
    public boolean login(final Handler handler, @Nullable final Activity fromActivity) {
        if (supportsPersonalization()) {
            userInfo = OkapiClient.getUserInfo(this);
        } else {
            userInfo = new UserInfo(StringUtils.EMPTY, 0, UserInfoStatus.NOT_SUPPORTED);
        }
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL;
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return StringUtils.isNotEmpty(getUserName()) && StringUtils.equals(cache.getOwnerDisplayName(), getUserName());
    }

    @Override
    public String getUserName() {
        return userInfo.getName();
    }

    @Override
    public int getCachesFound() {
        return userInfo.getFinds();
    }

    @Override
    public String getLoginStatusString() {
        return CgeoApplication.getInstance().getString(userInfo.getStatus().resId);
    }

    @Override
    public boolean isLoggedIn() {
        return userInfo.getStatus() == UserInfoStatus.SUCCESSFUL;
    }

    @Override
    public SearchResult searchByKeyword(@NonNull final String name) {
        return new SearchResult(OkapiClient.getCachesNamed(Sensors.getInstance().currentGeo().getCoords(), name, this));
    }

    @NonNull
    @Override
    public EnumSet<GeocacheFilterType> getFilterCapabilities() {
        return EnumSet.of(GeocacheFilterType.DISTANCE, GeocacheFilterType.ORIGIN,
            GeocacheFilterType.NAME, GeocacheFilterType.OWNER,
            GeocacheFilterType.TYPE, GeocacheFilterType.SIZE,
            GeocacheFilterType.DIFFICULTY, GeocacheFilterType.TERRAIN, GeocacheFilterType.DIFFICULTY_TERRAIN,
            GeocacheFilterType.FAVORITES, GeocacheFilterType.STATUS, GeocacheFilterType.LOG_ENTRY,
            GeocacheFilterType.LOGS_COUNT);
    }

    @Override
    public SearchResult searchByFilter(@NonNull final GeocacheFilter filter) {
        return new SearchResult(OkapiClient.getCachesByFilter(filter, this));
    }

    @Override
    public boolean isSearchForMyCaches(final String username) {
        return StringUtils.equalsIgnoreCase(username, getUserName());
    }

    @Override
    public boolean canAddPersonalNote(@NonNull final Geocache cache) {
        return this.getApiSupport() == ApiSupport.current && isActive();
    }

    @Override
    public boolean uploadPersonalNote(@NonNull final Geocache cache) {
        return OkapiClient.uploadPersonalNotes(this, cache);
    }

    @Override
    public int getPersonalNoteMaxChars() {
        return 20000;
    }

    @Override
    public boolean canIgnoreCache(@NonNull final Geocache cache) {
        return true;
    }

    @Override
    public void ignoreCache(@NonNull final Geocache cache) {
        final boolean ignored = OkapiClient.setIgnored(cache, this);

        if (ignored) {
            DataStore.saveChangedCache(cache);
        }
    }

}
