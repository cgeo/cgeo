package cgeo.geocaching.connector.su;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.utils.DisposableHandler;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class SuConnector extends AbstractConnector implements ISearchByCenter, ISearchByGeocode, ISearchByViewPort, ILogin, ICredentials {

    private static final CharSequence PREFIX_MULTISTEP_VIRTUAL = "MV";
    private static final CharSequence PREFIX_TRADITIONAL = "TR";
    private static final CharSequence PREFIX_VIRTUAL = "VI";
    private static final CharSequence PREFIX_MULTISTEP = "MS";
    private static final CharSequence PREFIX_EVENT = "EV";
    private static final CharSequence PREFIX_CONTEST = "CT";
    private static final CharSequence PREFIX_MYSTERY = "LT";
    private static final CharSequence PREFIX_MYSTERY_VIRTUAL = "LV";

    // Let just add this general prefix, since for search prefix is not important at all,
    // all IDs are unique at SU
    private static final CharSequence PREFIX_GENERAL = "SU";


    @NonNull
    private final SuLogin suLogin = SuLogin.getInstance();

    private SuConnector() {
        // singleton
    }

    public static SuConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean login(final Handler handler, @Nullable final Activity fromActivity) {
        final StatusCode status = suLogin.login();

        if (ConnectorFactory.showLoginToast && handler != null) {
            handler.sendMessage(handler.obtainMessage(0, status));
            ConnectorFactory.showLoginToast = false;

            // invoke settings activity to insert login details
            if (status == StatusCode.NO_LOGIN_INFO_STORED && fromActivity != null) {
                SettingsActivity.openForScreen(R.string.preference_screen_su, fromActivity);
            }
        }
        return status == StatusCode.NO_ERROR;
    }

    @Override
    public String getUserName() {
        return suLogin.getActualUserName();
    }

    @Override
    public int getCachesFound() {
        return suLogin.getActualCachesFound();
    }

    @Override
    public String getLoginStatusString() {
        return suLogin.getActualStatus();
    }

    @Override
    public boolean isLoggedIn() {
        return suLogin.isActualLoginStatus();
    }

    @Override
    public Credentials getCredentials() {
        return Settings.getCredentials(R.string.pref_suusername, R.string.pref_supassword);
    }

    @Override
    public int getUsernamePreferenceKey() {
        return R.string.pref_suusername;
    }

    @Override
    public int getPasswordPreferenceKey() {
        return R.string.pref_supassword;
    }

    @Override
    public int getAvatarPreferenceKey() {
        return R.string.pref_su_avatar;
    }

    @Override
    @NonNull
    public String getName() {
        return "Geocaching.su";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return "GCSU";
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + "&cid=" + cache.getCacheId();
    }

    @Override
    public boolean getHttps() {
        return false;
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.geocaching.su";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return StringUtils.isNotEmpty(getUserName()) && StringUtils.equals(cache.getOwnerDisplayName(), getUserName());
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return StringUtils.startsWithAny(StringUtils.upperCase(geocode), PREFIX_GENERAL, PREFIX_TRADITIONAL, PREFIX_MULTISTEP_VIRTUAL, PREFIX_VIRTUAL, PREFIX_MULTISTEP, PREFIX_EVENT, PREFIX_CONTEST, PREFIX_MYSTERY, PREFIX_MYSTERY_VIRTUAL) && isNumericId(geocode.substring(2));
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return getHostUrl() + "/?pn=101";
    }

    @Override
    public boolean isActive() {
        return Settings.isSUConnectorActive();
    }

    @Override
    public SearchResult searchByCenter(@NonNull final Geopoint center) {
        return new SearchResult();
        //return searchCaches("cache", new Parameters(PARAMETER_REQUEST_TYPE, REQUEST_TYPE_CENTER, "radius", "40", "clng", GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, center), "clat", GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, center), PARAMETER_RESULT_FIELDS, RESULT_FIELDS_SEARCH));
    }

    public final String getConsumerKey() {
        return CgeoApplication.getInstance().getString(R.string.su_consumer_key);
    }

    public final String getConsumerSecret() {
        return CgeoApplication.getInstance().getString(R.string.su_consumer_secret);
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final DisposableHandler handler) {
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final Geocache cache;
        try {
            cache = SuApi.searchByGeocode(geocode);
        } catch (final SuApi.CacheNotFoundException e) {
            return new SearchResult(StatusCode.CACHE_NOT_FOUND);
        } catch (final SuApi.NotAuthorizedException e) {
            return new SearchResult(StatusCode.NOT_LOGGED_IN);
        } catch (final SuApi.ConnectionErrorException e) {
            return new SearchResult(StatusCode.CONNECTION_FAILED_SU);
        }

        return new SearchResult(cache);
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport, @Nullable final MapTokens tokens) {
        final List<Geocache> caches = SuApi.searchByBBox(viewport, this);

        return new SearchResult(caches);
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes(@NonNull final Geocache geocache) {
        final List<LogType> logTypes = new ArrayList<>();

        logTypes.add(LogType.FOUND_IT);
        logTypes.add(LogType.DIDNT_FIND_IT);
        logTypes.add(LogType.NOTE);

        if (geocache.isOwner()) {
            logTypes.add(LogType.OWNER_MAINTENANCE);
        }
        return logTypes;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    @NonNull
    public ILoggingManager getLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final Geocache cache) {
        return new SuLoggingManager(activity, this, cache);
    }

    @Override
    public boolean canLog(@NonNull final Geocache cache) {
        return true;
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull
        private static final SuConnector INSTANCE = new SuConnector();
    }


}
