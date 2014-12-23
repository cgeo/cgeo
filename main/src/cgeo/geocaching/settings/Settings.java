package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.CGeoMap.MapMode;
import cgeo.geocaching.maps.LiveMapStrategy.Strategy;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.google.v1.GoogleMapProvider;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider.OfflineMapSource;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.FileUtils.FileSelector;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * General c:geo preferences/settings set by the user
 */
public class Settings {

    /**
     * On opening a map, we limit the _initial_ zoom. The user can still zoom out afterwards.
     */
    private static final int INITIAL_MAP_ZOOM_LIMIT = 16;
    private static final char HISTORY_SEPARATOR = ',';
    public static final int SHOW_WP_THRESHOLD_DEFAULT = 10;
    public static final int SHOW_WP_THRESHOLD_MAX = 50;
    private static final int MAP_SOURCE_DEFAULT = GoogleMapProvider.GOOGLE_MAP_ID.hashCode();

    public static final boolean HW_ACCEL_DISABLED_BY_DEFAULT =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ||
            StringUtils.equals(Build.MODEL, "HTC One X") ||    // HTC One X
            StringUtils.equals(Build.MODEL, "HTC One S") ||    // HTC One S
            StringUtils.equals(Build.MODEL, "GT-I8190")  ||    // Samsung S3 mini
            StringUtils.equals(Build.MODEL, "GT-S6310L") ||    // Samsung Galaxy Young
            StringUtils.equals(Build.MODEL, "GT-P5210")  ||    // Samsung Galaxy Tab 3
            StringUtils.equals(Build.MODEL, "GT-S7580")  ||    // Samsung Galaxy Trend Plus
            StringUtils.equals(Build.MODEL, "GT-I9105P") ||    // Samsung Galaxy SII Plus
            StringUtils.equals(Build.MODEL, "ST25i")     ||    // Sony Xperia U
            StringUtils.equals(Build.MODEL, "bq Aquaris 5");   // bq Aquaris 5

    // twitter api keys
    private final static @NonNull String TWITTER_KEY_CONSUMER_PUBLIC = CryptUtils.rot13("ESnsCvAv3kEupF1GCR3jGj");
    private final static @NonNull String TWITTER_KEY_CONSUMER_SECRET = CryptUtils.rot13("7vQWceACV9umEjJucmlpFe9FCMZSeqIqfkQ2BnhV9x");

    private static boolean useCompass = true;

    public enum CoordInputFormatEnum {
        Plain,
        Deg,
        Min,
        Sec;

        static final int DEFAULT_INT_VALUE = Min.ordinal();

        public static CoordInputFormatEnum fromInt(final int id) {
            final CoordInputFormatEnum[] values = CoordInputFormatEnum.values();
            if (id < 0 || id >= values.length) {
                return Min;
            }
            return values[id];
        }
    }

    private static final SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(CgeoApplication.getInstance().getBaseContext());
    static {
        migrateSettings();
        final boolean isDebug = sharedPrefs.getBoolean(getKey(R.string.pref_debug), false);
        Log.setDebug(isDebug);
        CgeoApplication.dumpOnOutOfMemory(isDebug);
    }

    /**
     * Cache the mapsource locally. If that is an offline map source, each request would potentially access the
     * underlying map file, leading to delays.
     */
    private static MapSource mapSource;

    protected Settings() {
        throw new InstantiationError();
    }

    private static void migrateSettings() {
        final int LATEST_PREFERENCES_VERSION = 2;
        final int currentVersion = getInt(R.string.pref_settingsversion, 0);

        // No need to migrate if we are up to date.
        if (currentVersion == LATEST_PREFERENCES_VERSION) {
            return;
        }

        // No need to migrate if we don't have older settings, defaults will be used instead.
        final String preferencesNameV0 = "cgeo.pref";
        final SharedPreferences prefsV0 = CgeoApplication.getInstance().getSharedPreferences(preferencesNameV0, Context.MODE_PRIVATE);
        if (currentVersion == 0 && prefsV0.getAll().isEmpty()) {
            final Editor e = sharedPrefs.edit();
            e.putInt(getKey(R.string.pref_settingsversion), LATEST_PREFERENCES_VERSION);
            e.apply();
            return;
        }

        if (currentVersion < 1) {
            // migrate from non standard file location and integer based boolean types
            final Editor e = sharedPrefs.edit();

            e.putString(getKey(R.string.pref_temp_twitter_token_secret), prefsV0.getString(getKey(R.string.pref_temp_twitter_token_secret), null));
            e.putString(getKey(R.string.pref_temp_twitter_token_public), prefsV0.getString(getKey(R.string.pref_temp_twitter_token_public), null));
            e.putBoolean(getKey(R.string.pref_help_shown), prefsV0.getInt(getKey(R.string.pref_help_shown), 0) != 0);
            e.putFloat(getKey(R.string.pref_anylongitude), prefsV0.getFloat(getKey(R.string.pref_anylongitude), 0));
            e.putFloat(getKey(R.string.pref_anylatitude), prefsV0.getFloat(getKey(R.string.pref_anylatitude), 0));
            e.putBoolean(getKey(R.string.pref_offlinemaps), 0 != prefsV0.getInt(getKey(R.string.pref_offlinemaps), 1));
            e.putBoolean(getKey(R.string.pref_offlinewpmaps), 0 != prefsV0.getInt(getKey(R.string.pref_offlinewpmaps), 0));
            e.putString(getKey(R.string.pref_webDeviceCode), prefsV0.getString(getKey(R.string.pref_webDeviceCode), null));
            e.putString(getKey(R.string.pref_webDeviceName), prefsV0.getString(getKey(R.string.pref_webDeviceName), null));
            e.putBoolean(getKey(R.string.pref_maplive), prefsV0.getInt(getKey(R.string.pref_maplive), 1) != 0);
            e.putInt(getKey(R.string.pref_mapsource), prefsV0.getInt(getKey(R.string.pref_mapsource), MAP_SOURCE_DEFAULT));
            e.putBoolean(getKey(R.string.pref_twitter), 0 != prefsV0.getInt(getKey(R.string.pref_twitter), 0));
            e.putBoolean(getKey(R.string.pref_showaddress), 0 != prefsV0.getInt(getKey(R.string.pref_showaddress), 1));
            e.putBoolean(getKey(R.string.pref_showcaptcha), prefsV0.getBoolean(getKey(R.string.pref_showcaptcha), false));
            e.putBoolean(getKey(R.string.pref_maptrail), prefsV0.getInt(getKey(R.string.pref_maptrail), 1) != 0);
            e.putInt(getKey(R.string.pref_lastmapzoom), prefsV0.getInt(getKey(R.string.pref_lastmapzoom), 14));
            e.putBoolean(getKey(R.string.pref_livelist), 0 != prefsV0.getInt(getKey(R.string.pref_livelist), 1));
            e.putBoolean(getKey(R.string.pref_units_imperial), prefsV0.getInt(getKey(R.string.pref_units_imperial), 1) != 1);
            e.putBoolean(getKey(R.string.pref_skin), prefsV0.getInt(getKey(R.string.pref_skin), 0) != 0);
            e.putInt(getKey(R.string.pref_lastusedlist), prefsV0.getInt(getKey(R.string.pref_lastusedlist), StoredList.STANDARD_LIST_ID));
            e.putString(getKey(R.string.pref_cachetype), prefsV0.getString(getKey(R.string.pref_cachetype), CacheType.ALL.id));
            e.putString(getKey(R.string.pref_twitter_token_secret), prefsV0.getString(getKey(R.string.pref_twitter_token_secret), null));
            e.putString(getKey(R.string.pref_twitter_token_public), prefsV0.getString(getKey(R.string.pref_twitter_token_public), null));
            e.putInt(getKey(R.string.pref_version), prefsV0.getInt(getKey(R.string.pref_version), 0));
            e.putBoolean(getKey(R.string.pref_autoloaddesc), 0 != prefsV0.getInt(getKey(R.string.pref_autoloaddesc), 1));
            e.putBoolean(getKey(R.string.pref_ratingwanted), prefsV0.getBoolean(getKey(R.string.pref_ratingwanted), true));
            e.putBoolean(getKey(R.string.pref_friendlogswanted), prefsV0.getBoolean(getKey(R.string.pref_friendlogswanted), true));
            e.putBoolean(getKey(R.string.pref_useenglish), prefsV0.getBoolean(getKey(R.string.pref_useenglish), false));
            e.putBoolean(getKey(R.string.pref_usecompass), 0 != prefsV0.getInt(getKey(R.string.pref_usecompass), 1));
            e.putBoolean(getKey(R.string.pref_trackautovisit), prefsV0.getBoolean(getKey(R.string.pref_trackautovisit), false));
            e.putBoolean(getKey(R.string.pref_sigautoinsert), prefsV0.getBoolean(getKey(R.string.pref_sigautoinsert), false));
            e.putBoolean(getKey(R.string.pref_logimages), prefsV0.getBoolean(getKey(R.string.pref_logimages), false));
            e.putBoolean(getKey(R.string.pref_excludedisabled), 0 != prefsV0.getInt(getKey(R.string.pref_excludedisabled), 0));
            e.putBoolean(getKey(R.string.pref_excludemine), 0 != prefsV0.getInt(getKey(R.string.pref_excludemine), 0));
            e.putString(getKey(R.string.pref_mapfile), prefsV0.getString(getKey(R.string.pref_mapfile), null));
            e.putString(getKey(R.string.pref_signature), prefsV0.getString(getKey(R.string.pref_signature), null));
            e.putString(getKey(R.string.pref_pass_vote), prefsV0.getString(getKey(R.string.pref_pass_vote), null));
            e.putString(getKey(R.string.pref_password), prefsV0.getString(getKey(R.string.pref_password), null));
            e.putString(getKey(R.string.pref_username), prefsV0.getString(getKey(R.string.pref_username), null));
            e.putString(getKey(R.string.pref_memberstatus), prefsV0.getString(getKey(R.string.pref_memberstatus), ""));
            e.putInt(getKey(R.string.pref_coordinputformat), prefsV0.getInt(getKey(R.string.pref_coordinputformat), CoordInputFormatEnum.DEFAULT_INT_VALUE));
            e.putBoolean(getKey(R.string.pref_log_offline), prefsV0.getBoolean(getKey(R.string.pref_log_offline), false));
            e.putBoolean(getKey(R.string.pref_choose_list), prefsV0.getBoolean(getKey(R.string.pref_choose_list), true));
            e.putBoolean(getKey(R.string.pref_loaddirectionimg), prefsV0.getBoolean(getKey(R.string.pref_loaddirectionimg), true));
            e.putString(getKey(R.string.pref_gccustomdate), prefsV0.getString(getKey(R.string.pref_gccustomdate), GCConstants.DEFAULT_GC_DATE));
            e.putInt(getKey(R.string.pref_showwaypointsthreshold), prefsV0.getInt(getKey(R.string.pref_showwaypointsthreshold), SHOW_WP_THRESHOLD_DEFAULT));
            e.putString(getKey(R.string.pref_cookiestore), prefsV0.getString(getKey(R.string.pref_cookiestore), null));
            e.putBoolean(getKey(R.string.pref_opendetailslastpage), prefsV0.getBoolean(getKey(R.string.pref_opendetailslastpage), false));
            e.putInt(getKey(R.string.pref_lastdetailspage), prefsV0.getInt(getKey(R.string.pref_lastdetailspage), 1));
            e.putInt(getKey(R.string.pref_defaultNavigationTool), prefsV0.getInt(getKey(R.string.pref_defaultNavigationTool), NavigationAppsEnum.COMPASS.id));
            e.putInt(getKey(R.string.pref_defaultNavigationTool2), prefsV0.getInt(getKey(R.string.pref_defaultNavigationTool2), NavigationAppsEnum.INTERNAL_MAP.id));
            e.putInt(getKey(R.string.pref_livemapstrategy), prefsV0.getInt(getKey(R.string.pref_livemapstrategy), Strategy.AUTO.id));
            e.putBoolean(getKey(R.string.pref_debug), prefsV0.getBoolean(getKey(R.string.pref_debug), false));
            e.putInt(getKey(R.string.pref_livemaphintshowcount), prefsV0.getInt(getKey(R.string.pref_livemaphintshowcount), 0));

            e.putInt(getKey(R.string.pref_settingsversion), 1); // mark migrated
            e.apply();
        }

        // changes for new settings dialog
        if (currentVersion < 2) {
            final Editor e = sharedPrefs.edit();

            e.putBoolean(getKey(R.string.pref_units_imperial), useImperialUnits());

            // show waypoints threshold now as a slider
            int wpThreshold = getWayPointsThreshold();
            if (wpThreshold < 0) {
                wpThreshold = 0;
            } else if (wpThreshold > SHOW_WP_THRESHOLD_MAX) {
                wpThreshold = SHOW_WP_THRESHOLD_MAX;
            }
            e.putInt(getKey(R.string.pref_showwaypointsthreshold), wpThreshold);

            // KEY_MAP_SOURCE must be string, because it is the key for a ListPreference now
            final int ms = sharedPrefs.getInt(getKey(R.string.pref_mapsource), MAP_SOURCE_DEFAULT);
            e.remove(getKey(R.string.pref_mapsource));
            e.putString(getKey(R.string.pref_mapsource), String.valueOf(ms));

            // navigation tool ids must be string, because ListPreference uses strings as keys
            final int dnt1 = sharedPrefs.getInt(getKey(R.string.pref_defaultNavigationTool), NavigationAppsEnum.COMPASS.id);
            final int dnt2 = sharedPrefs.getInt(getKey(R.string.pref_defaultNavigationTool2), NavigationAppsEnum.INTERNAL_MAP.id);
            e.remove(getKey(R.string.pref_defaultNavigationTool));
            e.remove(getKey(R.string.pref_defaultNavigationTool2));
            e.putString(getKey(R.string.pref_defaultNavigationTool), String.valueOf(dnt1));
            e.putString(getKey(R.string.pref_defaultNavigationTool2), String.valueOf(dnt2));

            // defaults for gpx directories
            e.putString(getKey(R.string.pref_gpxImportDir), getGpxImportDir());
            e.putString(getKey(R.string.pref_gpxExportDir), getGpxExportDir());

            e.putInt(getKey(R.string.pref_settingsversion), 2); // mark migrated
            e.apply();
        }
    }

    private static String getKey(final int prefKeyId) {
        return CgeoApplication.getInstance().getString(prefKeyId);
    }

    static String getString(final int prefKeyId, final String defaultValue) {
        return sharedPrefs.getString(getKey(prefKeyId), defaultValue);
    }

    private static int getInt(final int prefKeyId, final int defaultValue) {
        return sharedPrefs.getInt(getKey(prefKeyId), defaultValue);
    }

    private static long getLong(final int prefKeyId, final long defaultValue) {
        return sharedPrefs.getLong(getKey(prefKeyId), defaultValue);
    }

    private static boolean getBoolean(final int prefKeyId, final boolean defaultValue) {
        return sharedPrefs.getBoolean(getKey(prefKeyId), defaultValue);
    }

    private static float getFloat(final int prefKeyId, final float defaultValue) {
        return sharedPrefs.getFloat(getKey(prefKeyId), defaultValue);
    }

    protected static void putString(final int prefKeyId, final String value) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putString(getKey(prefKeyId), value);
        edit.apply();
    }

    protected static void putBoolean(final int prefKeyId, final boolean value) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putBoolean(getKey(prefKeyId), value);
        edit.apply();
    }

    private static void putInt(final int prefKeyId, final int value) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putInt(getKey(prefKeyId), value);
        edit.apply();
    }

    private static void putLong(final int prefKeyId, final long value) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putLong(getKey(prefKeyId), value);
        edit.apply();
    }

    private static void putFloat(final int prefKeyId, final float value) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putFloat(getKey(prefKeyId), value);
        edit.apply();
    }

    private static void remove(final int prefKeyId) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.remove(getKey(prefKeyId));
        edit.apply();
    }

    private static boolean contains(final int prefKeyId) {
        return sharedPrefs.contains(getKey(prefKeyId));
    }

    public static void setLanguage(final boolean useEnglish) {
        final Configuration config = new Configuration();
        config.locale = useEnglish ? Locale.ENGLISH : Locale.getDefault();
        final Resources resources = CgeoApplication.getInstance().getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public static boolean isLogin() {
        final String preUsername = getString(R.string.pref_username, null);
        final String prePassword = getString(R.string.pref_password, null);

        return !StringUtils.isBlank(preUsername) && !StringUtils.isBlank(prePassword);
    }

    /**
     * Get login and password information of Geocaching.com.
     *
     * @return a pair either with (login, password) or (empty, empty) if no valid information is stored
     */
    public static ImmutablePair<String, String> getGcCredentials() {
        return getCredentials(GCConnector.getInstance());
    }

    /**
     * Get login and password information.
     *
     * @return a pair either with (login, password) or (empty, empty) if no valid information is stored
     */
    public static ImmutablePair<String, String> getCredentials(final @NonNull ICredentials connector) {
        final String username = getString(connector.getUsernamePreferenceKey(), null);
        final String password = getString(connector.getPasswordPreferenceKey(), null);

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return new ImmutablePair<>(StringUtils.EMPTY, StringUtils.EMPTY);
        }

        return new ImmutablePair<>(username, password);
    }

    public static String getUsername() {
        return getString(R.string.pref_username, null);
    }

    public static boolean isGCConnectorActive() {
        return getBoolean(R.string.pref_connectorGCActive, true);
    }

    public static boolean isECConnectorActive() {
        return getBoolean(R.string.pref_connectorECActive, false);
    }

    public static boolean isOXConnectorActive() {
        return getBoolean(R.string.pref_connectorOXActive, false);
    }

    public static boolean isGCPremiumMember() {
        final String memberStatus = Settings.getGCMemberStatus();
        return StringUtils.equalsIgnoreCase(memberStatus, GCConstants.MEMBER_STATUS_PREMIUM) ||
                StringUtils.equalsIgnoreCase(memberStatus, GCConstants.MEMBER_STATUS_CHARTER);
    }

    public static String getGCMemberStatus() {
        return getString(R.string.pref_memberstatus, "");
    }

    public static void setGCMemberStatus(final String memberStatus) {
        if (StringUtils.isBlank(memberStatus)) {
            remove(R.string.pref_memberstatus);
        }
        putString(R.string.pref_memberstatus, memberStatus);
    }

    public static ImmutablePair<String, String> getTokenPair(final int tokenPublicPrefKey, final int tokenSecretPrefKey) {
        return new ImmutablePair<>(getString(tokenPublicPrefKey, null), getString(tokenSecretPrefKey, null));
    }

    public static void setTokens(final int tokenPublicPrefKey, @Nullable final String tokenPublic, final int tokenSecretPrefKey, @Nullable final String tokenSecret) {
        if (tokenPublic == null) {
            remove(tokenPublicPrefKey);
        } else {
            putString(tokenPublicPrefKey, tokenPublic);
        }
        if (tokenSecret == null) {
            remove(tokenSecretPrefKey);
        } else {
            putString(tokenSecretPrefKey, tokenSecret);
        }
    }

    public static boolean isOCConnectorActive(final int isActivePrefKeyId) {
        return getBoolean(isActivePrefKeyId, false);
    }

    public static boolean hasOCAuthorization(final int tokenPublicPrefKeyId, final int tokenSecretPrefKeyId) {
        return StringUtils.isNotBlank(getString(tokenPublicPrefKeyId, ""))
                && StringUtils.isNotBlank(getString(tokenSecretPrefKeyId, ""));
    }

    public static boolean isGCvoteLogin() {
        return getGCvoteLogin() != null;
    }

    public static ImmutablePair<String, String> getGCvoteLogin() {
        final String username = getString(R.string.pref_username, null);
        final String password = getString(R.string.pref_pass_vote, null);

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }

        return new ImmutablePair<>(username, password);
    }

    public static String getSignature() {
        return getString(R.string.pref_signature, StringUtils.EMPTY);
    }

    public static void setCookieStore(final String cookies) {
        if (StringUtils.isBlank(cookies)) {
            // erase cookies
            remove(R.string.pref_cookiestore);
        }
        // save cookies
        putString(R.string.pref_cookiestore, cookies);
    }

    public static String getCookieStore() {
        return getString(R.string.pref_cookiestore, null);
    }

    public static boolean useGooglePlayServices() {
        return CgeoApplication.getInstance().isGooglePlayServicesAvailable() && getBoolean(R.string.pref_googleplayservices, true);
    }

    public static boolean useLowPowerMode() {
        return getBoolean(R.string.pref_lowpowermode, false);
    }

    /**
     * @param cacheType
     *            The cache type used for future filtering
     */
    public static void setCacheType(final CacheType cacheType) {
        if (cacheType == null) {
            remove(R.string.pref_cachetype);
        } else {
            putString(R.string.pref_cachetype, cacheType.id);
        }
    }

    public static int getLastList() {
        return getInt(R.string.pref_lastusedlist, StoredList.STANDARD_LIST_ID);
    }

    public static void saveLastList(final int listId) {
        putInt(R.string.pref_lastusedlist, listId);
    }

    public static void setWebNameCode(final String name, final String code) {
        putString(R.string.pref_webDeviceName, name);
        putString(R.string.pref_webDeviceCode, code);
    }

    public static MapProvider getMapProvider() {
        return getMapSource().getMapProvider();
    }

    public static String getMapFile() {
        return getString(R.string.pref_mapfile, null);
    }

    public static void setMapFile(final String mapFile) {
        putString(R.string.pref_mapfile, mapFile);
        if (mapFile != null) {
            setMapFileDirectory(new File(mapFile).getParent());
        }
    }

    public static String getMapFileDirectory() {
        final String mapDir = getString(R.string.pref_mapDirectory, null);
        if (mapDir != null) {
            return mapDir;
        }
        final String mapFile = getMapFile();
        if (mapFile != null) {
            return new File(mapFile).getParent();
        }
        return null;
    }

    public static void setMapFileDirectory(final String mapFileDirectory) {
        putString(R.string.pref_mapDirectory, mapFileDirectory);
        MapsforgeMapProvider.getInstance().updateOfflineMaps();
    }

    public static boolean isValidMapFile() {
        return isValidMapFile(getMapFile());
    }

    public static boolean isValidMapFile(final String mapFileIn) {
        return MapsforgeMapProvider.isValidMapFile(mapFileIn);
    }

    public static boolean isScaleMapsforgeText() {
        return getBoolean(R.string.pref_mapsforge_scale_text, true);
    }

    public static CoordInputFormatEnum getCoordInputFormat() {
        return CoordInputFormatEnum.fromInt(getInt(R.string.pref_coordinputformat, CoordInputFormatEnum.DEFAULT_INT_VALUE));
    }

    public static void setCoordInputFormat(final CoordInputFormatEnum format) {
        putInt(R.string.pref_coordinputformat, format.ordinal());
    }

    public static boolean getLogOffline() {
        return getBoolean(R.string.pref_log_offline, false);
    }

    public static boolean getChooseList() {
        return getBoolean(R.string.pref_choose_list, false);
    }

    public static boolean getLoadDirImg() {
        return !isGCPremiumMember() && getBoolean(R.string.pref_loaddirectionimg, true);
    }

    public static void setGcCustomDate(final String format) {
        putString(R.string.pref_gccustomdate, format);
    }

    /**
     * @return User selected date format on GC.com
     */
    public static String getGcCustomDate() {
        // We might have some users whose stored value is null, which is invalid. In this case, we use the default.
        return StringUtils.defaultString(getString(R.string.pref_gccustomdate, GCConstants.DEFAULT_GC_DATE),
                GCConstants.DEFAULT_GC_DATE);
    }

    public static boolean isExcludeMyCaches() {
        return getBoolean(R.string.pref_excludemine, false);
    }

    public static boolean isUseEnglish() {
        return getBoolean(R.string.pref_useenglish, false);
    }

    public static boolean isShowAddress() {
        return getBoolean(R.string.pref_showaddress, true);
    }

    public static boolean isShowCaptcha() {
        return !isGCPremiumMember() && getBoolean(R.string.pref_showcaptcha, false);
    }

    public static boolean isExcludeDisabledCaches() {
        return getBoolean(R.string.pref_excludedisabled, false);
    }

    public static boolean isStoreOfflineMaps() {
        return getBoolean(R.string.pref_offlinemaps, true);
    }

    public static boolean isStoreOfflineWpMaps() {
        return getBoolean(R.string.pref_offlinewpmaps, false);
    }

    public static boolean isStoreLogImages() {
        return getBoolean(R.string.pref_logimages, false);
    }

    public static boolean isAutoLoadDescription() {
        return getBoolean(R.string.pref_autoloaddesc, true);
    }

    public static boolean isRatingWanted() {
        return getBoolean(R.string.pref_ratingwanted, true);
    }

    public static boolean isFriendLogsWanted() {
        if (!isLogin()) {
            // don't show a friends log if the user is anonymous
            return false;
        }
        return getBoolean(R.string.pref_friendlogswanted, true);
    }

    public static boolean isLiveList() {
        return getBoolean(R.string.pref_livelist, true);
    }

    public static boolean isTrackableAutoVisit() {
        return getBoolean(R.string.pref_trackautovisit, false);
    }

    public static boolean isAutoInsertSignature() {
        return getBoolean(R.string.pref_sigautoinsert, false);
    }

    public static void setUseImperialUnits(final boolean useImperialUnits) {
        putBoolean(R.string.pref_units_imperial, useImperialUnits);
    }

    public static boolean useImperialUnits() {
        return getBoolean(R.string.pref_units_imperial, useImperialUnitsByDefault());
    }

    static boolean useImperialUnitsByDefault() {
        final String countryCode = Locale.getDefault().getCountry();
        return "US".equals(countryCode)  // USA
            || "LR".equals(countryCode)  // Liberia
            || "MM".equals(countryCode); // Burma
    }

    public static boolean isLiveMap() {
        return getBoolean(R.string.pref_maplive, true);
    }

    public static void setLiveMap(final boolean live) {
        putBoolean(R.string.pref_maplive, live);
    }

    public static boolean isMapTrail() {
        return getBoolean(R.string.pref_maptrail, true);
    }

    public static void setMapTrail(final boolean showTrail) {
        putBoolean(R.string.pref_maptrail, showTrail);
    }

    /**
     * Get last used zoom of the internal map. Differentiate between two use cases for a map of multiple caches (e.g.
     * live map) and the map of a single cache (which is often zoomed in more deep).
     */
    public static int getMapZoom(final MapMode mapMode) {
        if (mapMode == MapMode.SINGLE || mapMode == MapMode.COORDS) {
            return getCacheZoom();
        }
        return getMapZoom();
    }

    public static void setMapZoom(final MapMode mapMode, final int zoomLevel) {
        if (mapMode == MapMode.SINGLE || mapMode == MapMode.COORDS) {
            setCacheZoom(zoomLevel);
        }
        else {
            setMapZoom(zoomLevel);
        }
    }

    /**
     * @return zoom used for the (live) map
     */
    private static int getMapZoom() {
        return Math.max(getInt(R.string.pref_lastmapzoom, 14), INITIAL_MAP_ZOOM_LIMIT);
    }

    private static void setMapZoom(final int mapZoomLevel) {
        putInt(R.string.pref_lastmapzoom, mapZoomLevel);
    }

    /**
     * @return zoom used for the map of a single cache
     */
    private static int getCacheZoom() {
        return Math.max(getInt(R.string.pref_cache_zoom, 14), INITIAL_MAP_ZOOM_LIMIT);
    }

    private static void setCacheZoom(final int zoomLevel) {
        putInt(R.string.pref_cache_zoom, zoomLevel);
    }

    public static GeoPointImpl getMapCenter() {
        return getMapProvider().getMapItemFactory()
                .getGeoPointBase(new Geopoint(getInt(R.string.pref_lastmaplat, 0) / 1e6,
                        getInt(R.string.pref_lastmaplon, 0) / 1e6));
    }

    public static void setMapCenter(final GeoPointImpl mapViewCenter) {
        putInt(R.string.pref_lastmaplat, mapViewCenter.getLatitudeE6());
        putInt(R.string.pref_lastmaplon, mapViewCenter.getLongitudeE6());
    }

    @NonNull
    public static synchronized MapSource getMapSource() {
        if (mapSource != null) {
            return mapSource;
        }
        final int id = getConvertedMapId();
        mapSource = MapProviderFactory.getMapSource(id);
        if (mapSource != null) {
            // don't use offline maps if the map file is not valid
            if (!(mapSource instanceof OfflineMapSource) || isValidMapFile()) {
                return mapSource;
            }
        }
        // fallback to first available map
        return MapProviderFactory.getDefaultSource();
    }

    private final static int GOOGLEMAP_BASEID = 30;
    private final static int MAP = 1;
    private final static int SATELLITE = 2;

    private final static int MFMAP_BASEID = 40;
    private final static int MAPNIK = 1;
    private final static int CYCLEMAP = 3;
    private final static int OFFLINE = 4;
    private static final int HISTORY_SIZE = 10;

    /**
     * Convert old preference ids for maps (based on constant values) into new hash based ids.
     */
    private static int getConvertedMapId() {
        final int id = Integer.parseInt(getString(R.string.pref_mapsource,
                String.valueOf(MAP_SOURCE_DEFAULT)));
        switch (id) {
            case GOOGLEMAP_BASEID + MAP:
                return GoogleMapProvider.GOOGLE_MAP_ID.hashCode();
            case GOOGLEMAP_BASEID + SATELLITE:
                return GoogleMapProvider.GOOGLE_SATELLITE_ID.hashCode();
            case MFMAP_BASEID + MAPNIK:
                return MapsforgeMapProvider.MAPSFORGE_MAPNIK_ID.hashCode();
            case MFMAP_BASEID + CYCLEMAP:
                return MapsforgeMapProvider.MAPSFORGE_CYCLEMAP_ID.hashCode();
            case MFMAP_BASEID + OFFLINE: {
                final String mapFile = Settings.getMapFile();
                if (StringUtils.isNotEmpty(mapFile)) {
                    return mapFile.hashCode();
                }
                break;
            }
            default:
                break;
        }
        return id;
    }

    public static synchronized void setMapSource(final MapSource newMapSource) {
        putString(R.string.pref_mapsource, String.valueOf(newMapSource.getNumericalId()));
        if (newMapSource instanceof OfflineMapSource) {
            setMapFile(((OfflineMapSource) newMapSource).getFileName());
        }
        // cache the value
        mapSource = newMapSource;
    }

    public static void setAnyCoordinates(final Geopoint coords) {
        if (null != coords) {
            putFloat(R.string.pref_anylatitude, (float) coords.getLatitude());
            putFloat(R.string.pref_anylongitude, (float) coords.getLongitude());
        } else {
            remove(R.string.pref_anylatitude);
            remove(R.string.pref_anylongitude);
        }
    }

    public static Geopoint getAnyCoordinates() {
        if (contains(R.string.pref_anylatitude) && contains(R.string.pref_anylongitude)) {
            final float lat = getFloat(R.string.pref_anylatitude, 0);
            final float lon = getFloat(R.string.pref_anylongitude, 0);
            return new Geopoint(lat, lon);
        }
        return null;
    }

    public static boolean isUseCompass() {
        return useCompass;
    }

    public static void setUseCompass(final boolean value) {
        useCompass = value;
    }

    public static boolean isLightSkin() {
        return getBoolean(R.string.pref_skin, false);
    }

    @NonNull
    public static String getTwitterKeyConsumerPublic() {
        return TWITTER_KEY_CONSUMER_PUBLIC;
    }

    @NonNull
    public static String getTwitterKeyConsumerSecret() {
        return TWITTER_KEY_CONSUMER_SECRET;
    }

    public static String getWebDeviceCode() {
        return getString(R.string.pref_webDeviceCode, null);
    }

    public static boolean isRegisteredForSend2cgeo() {
        return getWebDeviceCode() != null;
    }

    public static String getWebDeviceName() {
        return getString(R.string.pref_webDeviceName, Build.MODEL);
    }

    /**
     * @return The cache type used for filtering or ALL if no filter is active.
     *         Returns never null
     */
    public static CacheType getCacheType() {
        return CacheType.getById(getString(R.string.pref_cachetype, CacheType.ALL.id));
    }

    /**
     * The Threshold for the showing of child waypoints
     */
    public static int getWayPointsThreshold() {
        return getInt(R.string.pref_showwaypointsthreshold, SHOW_WP_THRESHOLD_DEFAULT);
    }

    public static void setShowWaypointsThreshold(final int threshold) {
        putInt(R.string.pref_showwaypointsthreshold, threshold);
    }

    public static boolean isUseTwitter() {
        return getBoolean(R.string.pref_twitter, false);
    }

    public static void setUseTwitter(final boolean useTwitter) {
        putBoolean(R.string.pref_twitter, useTwitter);
    }

    public static boolean isTwitterLoginValid() {
        return !StringUtils.isBlank(getTokenPublic())
                && !StringUtils.isBlank(getTokenSecret());
    }

    public static String getTokenPublic() {
        return getString(R.string.pref_twitter_token_public, null);
    }

    public static String getTokenSecret() {
        return getString(R.string.pref_twitter_token_secret, null);

    }

    public static boolean hasTwitterAuthorization() {
        return StringUtils.isNotBlank(getTokenPublic())
                && StringUtils.isNotBlank(getTokenSecret());
    }

    public static void setTwitterTokens(@Nullable final String tokenPublic,
            @Nullable final String tokenSecret, final boolean enableTwitter) {
        putString(R.string.pref_twitter_token_public, tokenPublic);
        putString(R.string.pref_twitter_token_secret, tokenSecret);
        if (tokenPublic != null) {
            remove(R.string.pref_temp_twitter_token_public);
            remove(R.string.pref_temp_twitter_token_secret);
        }
        setUseTwitter(enableTwitter);
    }

    public static void setTwitterTempTokens(@Nullable final String tokenPublic,
            @Nullable final String tokenSecret) {
        putString(R.string.pref_temp_twitter_token_public, tokenPublic);
        putString(R.string.pref_temp_twitter_token_secret, tokenSecret);
    }

    public static ImmutablePair<String, String> getTempToken() {
        final String tokenPublic = getString(R.string.pref_temp_twitter_token_public, null);
        final String tokenSecret = getString(R.string.pref_temp_twitter_token_secret, null);
        return new ImmutablePair<>(tokenPublic, tokenSecret);
    }

    public static int getVersion() {
        return getInt(R.string.pref_version, 0);
    }

    public static void setVersion(final int version) {
        putInt(R.string.pref_version, version);
    }

    public static boolean isOpenLastDetailsPage() {
        return getBoolean(R.string.pref_opendetailslastpage, false);
    }

    public static int getLastDetailsPage() {
        return getInt(R.string.pref_lastdetailspage, 1);
    }

    public static void setLastDetailsPage(final int index) {
        putInt(R.string.pref_lastdetailspage, index);
    }

    public static int getDefaultNavigationTool() {
        return Integer.parseInt(getString(
                R.string.pref_defaultNavigationTool,
                String.valueOf(NavigationAppsEnum.COMPASS.id)));
    }

    public static int getDefaultNavigationTool2() {
        return Integer.parseInt(getString(
                R.string.pref_defaultNavigationTool2,
                String.valueOf(NavigationAppsEnum.INTERNAL_MAP.id)));
    }

    public static Strategy getLiveMapStrategy() {
        return Strategy.getById(getInt(R.string.pref_livemapstrategy, Strategy.AUTO.id));
    }

    public static void setLiveMapStrategy(final Strategy strategy) {
        putInt(R.string.pref_livemapstrategy, strategy.id);
    }

    public static boolean isDebug() {
        return Log.isDebug();
    }

    public static int getLiveMapHintShowCount() {
        return getInt(R.string.pref_livemaphintshowcount, 0);
    }

    public static void setLiveMapHintShowCount(final int showCount) {
        putInt(R.string.pref_livemaphintshowcount, showCount);
    }

    public static boolean isDbOnSDCard() {
        return getBoolean(R.string.pref_dbonsdcard, false);
    }

    public static void setDbOnSDCard(final boolean dbOnSDCard) {
        putBoolean(R.string.pref_dbonsdcard, dbOnSDCard);
    }

    public static String getGpxExportDir() {
        return getString(R.string.pref_gpxExportDir,
                Environment.getExternalStorageDirectory().getPath() + "/gpx");
    }

    public static String getGpxImportDir() {
        return getString(R.string.pref_gpxImportDir,
                Environment.getExternalStorageDirectory().getPath() + "/gpx");
    }

    public static boolean getShareAfterExport() {
        return getBoolean(R.string.pref_shareafterexport, true);
    }

    public static void setShareAfterExport(final boolean shareAfterExport) {
        putBoolean(R.string.pref_shareafterexport, shareAfterExport);
    }

    public static int getTrackableAction() {
        return getInt(R.string.pref_trackableaction, LogType.RETRIEVED_IT.id);
    }

    public static void setTrackableAction(final int trackableAction) {
        putInt(R.string.pref_trackableaction, trackableAction);
    }

    public static String getCustomRenderThemeBaseFolder() {
        return getString(R.string.pref_renderthemepath, "");
    }

    public static String getCustomRenderThemeFilePath() {
        return getString(R.string.pref_renderthemefile, "");
    }

    public static void setCustomRenderThemeFile(final String customRenderThemeFile) {
        putString(R.string.pref_renderthemefile, customRenderThemeFile);
    }

    public static File[] getMapThemeFiles() {
        final File directory = new File(Settings.getCustomRenderThemeBaseFolder());
        final List<File> result = new ArrayList<>();
        FileUtils.listDir(result, directory, new ExtensionsBasedFileSelector(new String[] { "xml" }), null);

        return result.toArray(new File[result.size()]);
    }

    private static class ExtensionsBasedFileSelector extends FileSelector {
        private final String[] extensions;
        public ExtensionsBasedFileSelector(final String[] extensions) {
            this.extensions = extensions;
        }
        @Override
        public boolean isSelected(final File file) {
            final String filename = file.getName();
            for (final String ext : extensions) {
                if (StringUtils.endsWithIgnoreCase(filename, ext)) {
                    return true;
                }
            }
            return false;
        }
        @Override
        public boolean shouldEnd() {
            return false;
        }
    }

    public static boolean getPlainLogs() {
        return getBoolean(R.string.pref_plainLogs, false);
    }

    public static boolean getUseNativeUa() {
        return getBoolean(R.string.pref_nativeUa, false);
    }

    public static String getCacheTwitterMessage() {
        return getString(R.string.pref_twitter_cache_message, "I found [NAME] ([URL]).");
    }

    public static String getTrackableTwitterMessage() {
        return getString(R.string.pref_twitter_trackable_message, "I touched [NAME] ([URL]).");
    }

    public static int getLogImageScale() {
        return getInt(R.string.pref_logImageScale, -1);
    }

    public static void setLogImageScale(final int scale) {
        putInt(R.string.pref_logImageScale, scale);
    }

    public static void setExcludeMine(final boolean exclude) {
        putBoolean(R.string.pref_excludemine, exclude);
    }

    static void setLogin(final String username, final String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            // erase username and password
            remove(R.string.pref_username);
            remove(R.string.pref_password);
            return;
        }
        // save username and password
        putString(R.string.pref_username, username);
        putString(R.string.pref_password, password);
    }

    public static long getFieldnoteExportDate() {
        return getLong(R.string.pref_fieldNoteExportDate, 0);
    }

    /**
     * Remember date of last field note export.
     */
    public static void setFieldnoteExportDate(final long date) {
        putLong(R.string.pref_fieldNoteExportDate, date);
    }

    public static boolean isUseNavigationApp(final NavigationAppsEnum navApp) {
        return getBoolean(navApp.preferenceKey, true);
    }

    /**
     * Remember the state of the "Upload" checkbox in the field notes export dialog.
     */
    public static void setFieldNoteExportUpload(final boolean upload) {
        putBoolean(R.string.pref_fieldNoteExportUpload, upload);
    }

    public static boolean getFieldNoteExportUpload() {
        return getBoolean(R.string.pref_fieldNoteExportUpload, true);
    }

    /**
     * Remember the state of the "Only new" checkbox in the field notes export dialog.
     */
    public static void setFieldNoteExportOnlyNew(final boolean onlyNew) {
        putBoolean(R.string.pref_fieldNoteExportOnlyNew, onlyNew);
    }

    public static boolean getFieldNoteExportOnlyNew() {
        return getBoolean(R.string.pref_fieldNoteExportOnlyNew, false);
    }

    public static String getECIconSet() {
        return getString(R.string.pref_ec_icons, "1");
    }

    /* Store last checksum of changelog for changelog display */
    public static long getLastChangelogChecksum() {
        return getLong(R.string.pref_changelog_last_checksum, 0);
    }

    public static void setLastChangelogChecksum(final long checksum) {
        putLong(R.string.pref_changelog_last_checksum, checksum);
    }

    public static List<String> getLastOpenedCaches() {
        final List<String> history = Arrays.asList(StringUtils.split(getString(R.string.pref_caches_history, StringUtils.EMPTY), HISTORY_SEPARATOR));
        return history.subList(0, Math.min(HISTORY_SIZE, history.size()));
    }

    public static void addCacheToHistory(@NonNull final String geocode) {
        final ArrayList<String> history = new ArrayList<>(getLastOpenedCaches());
        // bring entry to front, if it already existed
        history.remove(geocode);
        history.add(0, geocode);
        putString(R.string.pref_caches_history, StringUtils.join(history, HISTORY_SEPARATOR));
    }

    public static boolean useHardwareAcceleration() {
        return getBoolean(R.string.pref_hardware_acceleration, !HW_ACCEL_DISABLED_BY_DEFAULT);
    }

    public static void setUseHardwareAcceleration(final boolean useHardwareAcceleration) {
        putBoolean(R.string.pref_hardware_acceleration, useHardwareAcceleration);
    }

    public static String getLastCacheLog() {
        return getString(R.string.pref_last_cache_log, StringUtils.EMPTY);
    }

    public static void setLastCacheLog(final String log) {
        putString(R.string.pref_last_cache_log, log);
    }

    public static String getLastTrackableLog() {
        return getString(R.string.pref_last_trackable_log, StringUtils.EMPTY);
    }

    public static void setLastTrackableLog(final String log) {
        putString(R.string.pref_last_trackable_log, log);
    }

    @Nullable
    public static String getHomeLocation() {
        return getString(R.string.pref_home_location, null);
    }

    public static void setHomeLocation(@NonNull final String homeLocation) {
        putString(R.string.pref_home_location, homeLocation);
    }
}
