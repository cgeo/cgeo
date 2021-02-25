package cgeo.geocaching.connector.lc;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class LCConnector extends AbstractConnector implements ISearchByGeocode, ISearchByCenter, ISearchByViewPort, ILogin, ICredentials {

    @NonNull
    private static final String CACHE_URL = "https://adventurelab.page.link/";

    /**
     * Pattern for LC codes
     */
    @NonNull
    private static final Pattern PATTERN_LC_CODE = Pattern.compile("LC[0-9a-zA-Z]+", Pattern.CASE_INSENSITIVE);

    @NonNull
    private final LCLogin lcLogin = LCLogin.getInstance();

    private LCConnector() {
        // singleton
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull private static final LCConnector INSTANCE = new LCConnector();
    }

    @NonNull
    public static LCConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return PATTERN_LC_CODE.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return CACHE_URL + cache.getGeocode().replace("LC", "");
    }

    @Override
    @NonNull
    public String getName() {
        return "labs.geocaching.com";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return "LC";
    }

    @Override
    @NonNull
    public String getHost() {
        return "labs.geocaching.com";
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final DisposableHandler handler) {
        Log.e("searchByGeocode: gecode = " + geocode);
        Log.e("searchByGeocode: guid   = " + guid);
        if (guid == null) {
            return null;
        }
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final Geocache cache = LCApi.searchByGeocode(guid);

        return cache != null ? new SearchResult(cache) : null;
    }
    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport) {
        final Collection<Geocache> caches = LCApi.searchByBBox(viewport);
        final SearchResult searchResult = new SearchResult(caches);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    @NonNull
    public SearchResult searchByCenter(@NonNull final Geopoint center) {
        final Collection<Geocache> caches = LCApi.searchByCenter(center);
        final SearchResult searchResult = new SearchResult(caches);
        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return CACHE_URL;
    }

    @Override
    public boolean isActive() {
        return Settings.isLCConnectorActive();
    }

    @Override
    public boolean login(final Handler handler, @Nullable final Activity fromActivity) {
        // login
        final StatusCode status = lcLogin.login();

        return status == StatusCode.NO_ERROR;
    }

    @Override
    public String getUserName() {
        return lcLogin.getActualUserName();
    }

    @Override
    public Credentials getCredentials() {
        return Settings.getCredentials(R.string.pref_lcusername, R.string.pref_lcpassword);
    }

    @Override
    public int getCachesFound() {
        return lcLogin.getActualCachesFound();
    }

    @Override
    public String getLoginStatusString() {
        return lcLogin.getActualStatus();
    }

    @Override
    public boolean isLoggedIn() {
        return lcLogin.isActualLoginStatus();
    }

    @Override
    public int getCacheMapMarkerId(final boolean disabled) {
        final String icons = Settings.getLCIconSet();
        if (StringUtils.equals(icons, "1")) {
            return disabled ? R.drawable.marker_disabled_other : R.drawable.marker_other;
        }
        return disabled ? R.drawable.marker_disabled : R.drawable.marker;
    }


    @Override
    public int getMaxTerrain() {
        return 1;
    }

    @Override
    public int getUsernamePreferenceKey() {
        return R.string.pref_lcusername;
    }

    @Override
    public int getPasswordPreferenceKey() {
        return R.string.pref_lcpassword;
    }

    @Override
    public int getAvatarPreferenceKey() {
        return R.string.pref_lc_avatar;
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final String geocode = "LC" + StringUtils.substringAfter(url, "https://adventurelab.page.link/");
        if (canHandle(geocode)) {
            return geocode;
        }
        return super.getGeocodeFromUrl(url);
    }

    @Override
    @NonNull
    public String getCreateAccountUrl() {
        return "https://www.geocaching.com/account/join";
    }
}
