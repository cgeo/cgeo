package cgeo.geocaching.connector.wm;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.extension.FoundNumCounter;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.LocalizationUtils;

import cgeo.geocaching.utils.ShareUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WMConnector extends AbstractConnector implements ILogin, ISearchByGeocode /*TODO ISearchByFilter, ISearchByViewPort*/ {

    private static final String USER_PROFILE_URI = "https://www.waymarking.com/users/profile.aspx?f=1&guid=%s";
    private static final String USER_EMAIL_URI = "https://www.waymarking.com/users/email.aspx?f=1&guid=%s";

    private final String name;

    private WMConnector() {
        // singleton
        name = LocalizationUtils.getString(R.string.init_wm);
        prefKey = R.string.preference_screen_wm;
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull private static final WMConnector INSTANCE = new WMConnector();
    }

    @NonNull
    public static WMConnector getInstance() {
        return WMConnector.Holder.INSTANCE;
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
    public boolean supportsDifficultyTerrain() {
        return false;
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
        return R.drawable.marker_wm;
    }

    @Override
    public int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_wm;
    }

    @Override
    public int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker_wm;
    }

    @Override
    public int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_wm;
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

    @Override
    public boolean login() {
        // login
        final StatusCode status = WMLogin.getInstance().login();
        // update cache counter
        FoundNumCounter.getAndUpdateFoundNum(this);

        return status == StatusCode.NO_ERROR;
    }

    @Override
    public void logout() {
        WMLogin.getInstance().logout();
    }

    @Override
    public boolean isLoggedIn() {
        return WMLogin.getInstance().isActualLoginStatus();
    }

    @Override
    public String getLoginStatusString() {
        return WMLogin.getInstance().getActualStatus();
    }

    @Override
    public String getUserName() {
        return WMLogin.getInstance().getActualUserName();
    }

    @Override
    public int getCachesFound() {
        return WMLogin.getInstance().getActualCachesFound();
    }

    @Override
    public void increaseCachesFound(final int by) {
        WMLogin.getInstance().increaseActualCachesFound(by);
    }

    @Override
    public int getFindsQuantityString() {
        return R.plurals.user_visits;
    }

    @NonNull
    @Override
    public List<UserAction> getUserActions(final UserAction.UAContext user) {
        final List<UserAction> actions = super.getUserActions(user);
        actions.add(new UserAction(R.string.user_menu_open_browser, R.drawable.ic_menu_face, context -> ShareUtils.openUrl(context.getContext(), String.format(USER_PROFILE_URI, context.userGUID))));
        actions.add(new UserAction(R.string.user_menu_send_email, R.drawable.ic_menu_email, context -> ShareUtils.openUrl(context.getContext(), String.format(USER_EMAIL_URI, context.userGUID))));
        return actions;
    }

    @Override
    public SearchResult searchByGeocode(@Nullable String geocode, @Nullable String guid, DisposableHandler handler) {
        if (geocode == null) {
            return null;
        }

        final Geocache cache = WMApi.searchByGeocode(geocode, handler);
        return cache != null ? new SearchResult(cache) : null;
    }
}
