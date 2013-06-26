package cgeo.geocaching.settings;

import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LiveMapStrategy.Strategy;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.google.GoogleMapProvider;
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * General c:geo preferences/settings set by the user
 */
public final class Settings {

    private static final String KEY_TEMP_TWITTER_TOKEN_SECRET = "temp-token-secret";
    private static final String KEY_TEMP_TWITTER_TOKEN_PUBLIC = "temp-token-public";
    private static final String KEY_HELP_SHOWN = "helper";
    private static final String KEY_ANYLONGITUDE = "anylongitude";
    private static final String KEY_ANYLATITUDE = "anylatitude";
    private static final String KEY_USE_OFFLINEMAPS = "offlinemaps";
    private static final String KEY_USE_OFFLINEWPMAPS = "offlinewpmaps";
    private static final String KEY_WEB_DEVICE_CODE = "webDeviceCode";
    static final String KEY_WEBDEVICE_NAME = "webDeviceName";
    private static final String KEY_MAP_LIVE = "maplive";
    static final String KEY_MAP_SOURCE = "mapsource";
    private static final String KEY_USE_TWITTER = "twitter";
    private static final String KEY_SHOW_ADDRESS = "showaddress";
    static final String KEY_SHOW_CAPTCHA = "showcaptcha";
    private static final String KEY_MAP_TRAIL = "maptrail";
    private static final String KEY_LAST_MAP_ZOOM = "mapzoom";
    private static final String KEY_LAST_MAP_LAT = "maplat";
    private static final String KEY_LAST_MAP_LON = "maplon";
    private static final String KEY_LIVE_LIST = "livelist";
    private static final String KEY_IMPERIAL_UNITS = "units";
    private static final String KEY_SKIN = "skin";
    private static final String KEY_LAST_USED_LIST = "lastlist";
    private static final String KEY_CACHE_TYPE = "cachetype";
    private static final String KEY_TWITTER_TOKEN_SECRET = "tokensecret";
    private static final String KEY_TWITTER_TOKEN_PUBLIC = "tokenpublic";
    private static final String KEY_VERSION = "version";
    private static final String KEY_LOAD_DESCRIPTION = "autoloaddesc";
    private static final String KEY_RATING_WANTED = "ratingwanted";
    private static final String KEY_ELEVATION_WANTED = "elevationwanted";
    private static final String KEY_FRIENDLOGS_WANTED = "friendlogswanted";
    static final String KEY_USE_ENGLISH = "useenglish";
    private static final String KEY_USE_COMPASS = "usecompass";
    private static final String KEY_AUTO_VISIT_TRACKABLES = "trackautovisit";
    private static final String KEY_AUTO_INSERT_SIGNATURE = "sigautoinsert";
    static final String KEY_ALTITUDE_CORRECTION = "altcorrection";
    private static final String KEY_STORE_LOG_IMAGES = "logimages";
    private static final String KEY_EXCLUDE_DISABLED = "excludedisabled";
    private static final String KEY_EXCLUDE_OWN = "excludemine";
    private static final String KEY_MAPFILE = "mfmapfile";
    static final String KEY_SIGNATURE = "signature";
    static final String KEY_GCVOTE_PASSWORD = "pass-vote";
    static final String KEY_PASSWORD = "password";
    static final String KEY_USERNAME = "username";
    private static final String KEY_MEMBER_STATUS = "memberstatus";
    private static final String KEY_COORD_INPUT_FORMAT = "coordinputformat";
    private static final String KEY_LOG_OFFLINE = "log_offline";
    private static final String KEY_CHOOSE_LIST = "choose_list";
    static final String KEY_LOAD_DIRECTION_IMG = "loaddirectionimg";
    private static final String KEY_GC_CUSTOM_DATE = "gccustomdate";
    private static final String KEY_SHOW_WAYPOINTS_THRESHOLD = "gcshowwaypointsthreshold";
    private static final String KEY_COOKIE_STORE = "cookiestore";
    private static final String KEY_OPEN_LAST_DETAILS_PAGE = "opendetailslastpage";
    private static final String KEY_LAST_DETAILS_PAGE = "lastdetailspage";
    static final String KEY_DEFAULT_NAVIGATION_TOOL = "defaultNavigationTool";
    static final String KEY_DEFAULT_NAVIGATION_TOOL_2 = "defaultNavigationTool2";
    private static final String KEY_LIVE_MAP_STRATEGY = "livemapstrategy";
    static final String KEY_DEBUG = "debug";
    private static final String KEY_HIDE_LIVE_MAP_HINT = "hidelivemaphint";
    private static final String KEY_LIVE_MAP_HINT_SHOW_COUNT = "livemaphintshowcount";
    private static final String KEY_SETTINGS_VERSION = "settingsversion";
    static final String KEY_DB_ON_SDCARD = "dbonsdcard";
    private static final String KEY_LAST_TRACKABLE_ACTION = "trackableaction";
    private static final String KEY_SHARE_AFTER_EXPORT = "shareafterexport";
    public static final String KEY_RENDER_THEME_BASE_FOLDER = "renderthemepath";
    static final String KEY_RENDER_THEME_FILE_PATH = "renderthemefile";
    public static final String KEY_GPX_EXPORT_DIR = "gpxExportDir";
    public static final String KEY_GPX_IMPORT_DIR = "gpxImportDir";
    private static final String KEY_PLAIN_LOGS = "plainLogs";
    private static final String KEY_NATIVE_UA = "nativeUa";
    static final String KEY_MAP_DIRECTORY = "mapDirectory";
    private static final String KEY_CONNECTOR_GC_ACTIVE = "connectorGCActive";
    private static final String KEY_CONNECTOR_OC_ACTIVE = "connectorOCActive";
    private static final String KEY_LOG_IMAGE_SCALE = "logImageScale";
    private static final String KEY_OCDE_TOKEN_SECRET = "ocde_tokensecret";
    private static final String KEY_OCDE_TOKEN_PUBLIC = "ocde_tokenpublic";
    private static final String KEY_TEMP_OCDE_TOKEN_SECRET = "ocde-temp-token-secret";
    private static final String KEY_TEMP_OCDE_TOKEN_PUBLIC = "ocde-temp-token-public";

    /*
     * fake keys are for finding preference objects only, because preferences
     * don't have an id.
     */
    static final String FAKEKEY_PREFERENCE_BACKUP_INFO = "fakekey_preference_backup_info";
    static final String FAKEKEY_PREFERENCE_BACKUP = "fakekey_preference_backup";
    static final String FAKEKEY_PREFERENCE_RESTORE = "fakekey_preference_restore";

    public static final int SHOW_WP_THRESHOLD_DEFAULT = 5;
    public static final int SHOW_WP_THRESHOLD_MAX = 50;
    private static final int MAP_SOURCE_DEFAULT = GoogleMapProvider.GOOGLE_MAP_ID.hashCode();

    private final static int unitsMetric = 1;

    // twitter api keys
    private final static String keyConsumerPublic = CryptUtils.rot13("ESnsCvAv3kEupF1GCR3jGj");
    private final static String keyConsumerSecret = CryptUtils.rot13("7vQWceACV9umEjJucmlpFe9FCMZSeqIqfkQ2BnhV9x");

    public enum coordInputFormatEnum {
        Plain,
        Deg,
        Min,
        Sec;

        public static coordInputFormatEnum fromInt(int id) {
            final coordInputFormatEnum[] values = coordInputFormatEnum.values();
            if (id < 0 || id >= values.length) {
                return Min;
            }
            return values[id];
        }
    }

    private static String username = null;
    private static String password = null;

    private static final SharedPreferences sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(cgeoapplication.getInstance().getBaseContext());
    static {
        migrateSettings();
        Log.setDebug(sharedPrefs.getBoolean(KEY_DEBUG, false));
    }

    // maps
    private static MapProvider mapProvider = null;
    private static String cacheTwitterMessage = "I found [NAME] ([URL])";

    private Settings() {
        // this class is not to be instantiated;
    }

    private static void migrateSettings() {
        // migrate from non standard file location and integer based boolean types
        int oldVersion = sharedPrefs.getInt(KEY_SETTINGS_VERSION, 0);
        if (oldVersion < 1) {
            final String oldPreferencesName = "cgeo.pref";
            final SharedPreferences old = cgeoapplication.getInstance().getSharedPreferences(oldPreferencesName, Context.MODE_PRIVATE);
            final Editor e = sharedPrefs.edit();

            e.putString(KEY_TEMP_TWITTER_TOKEN_SECRET, old.getString(KEY_TEMP_TWITTER_TOKEN_SECRET, null));
            e.putString(KEY_TEMP_TWITTER_TOKEN_PUBLIC, old.getString(KEY_TEMP_TWITTER_TOKEN_PUBLIC, null));
            e.putBoolean(KEY_HELP_SHOWN, old.getInt(KEY_HELP_SHOWN, 0) != 0);
            e.putFloat(KEY_ANYLONGITUDE, old.getFloat(KEY_ANYLONGITUDE, 0));
            e.putFloat(KEY_ANYLATITUDE, old.getFloat(KEY_ANYLATITUDE, 0));
            e.putBoolean(KEY_USE_OFFLINEMAPS, 0 != old.getInt(KEY_USE_OFFLINEMAPS, 1));
            e.putBoolean(KEY_USE_OFFLINEWPMAPS, 0 != old.getInt(KEY_USE_OFFLINEWPMAPS, 0));
            e.putString(KEY_WEB_DEVICE_CODE, old.getString(KEY_WEB_DEVICE_CODE, null));
            e.putString(KEY_WEBDEVICE_NAME, old.getString(KEY_WEBDEVICE_NAME, null));
            e.putBoolean(KEY_MAP_LIVE, old.getInt(KEY_MAP_LIVE, 1) != 0);
            e.putInt(KEY_MAP_SOURCE, old.getInt(KEY_MAP_SOURCE, MAP_SOURCE_DEFAULT));
            e.putBoolean(KEY_USE_TWITTER, 0 != old.getInt(KEY_USE_TWITTER, 0));
            e.putBoolean(KEY_SHOW_ADDRESS, 0 != old.getInt(KEY_SHOW_ADDRESS, 1));
            e.putBoolean(KEY_SHOW_CAPTCHA, old.getBoolean(KEY_SHOW_CAPTCHA, false));
            e.putBoolean(KEY_MAP_TRAIL, old.getInt(KEY_MAP_TRAIL, 1) != 0);
            e.putInt(KEY_LAST_MAP_ZOOM, old.getInt(KEY_LAST_MAP_ZOOM, 14));
            e.putBoolean(KEY_LIVE_LIST, 0 != old.getInt(KEY_LIVE_LIST, 1));
            e.putBoolean(KEY_IMPERIAL_UNITS, old.getInt(KEY_IMPERIAL_UNITS, unitsMetric) == unitsMetric);
            e.putBoolean(KEY_SKIN, old.getInt(KEY_SKIN, 0) != 0);
            e.putInt(KEY_LAST_USED_LIST, old.getInt(KEY_LAST_USED_LIST, StoredList.STANDARD_LIST_ID));
            e.putString(KEY_CACHE_TYPE, old.getString(KEY_CACHE_TYPE, CacheType.ALL.id));
            e.putString(KEY_TWITTER_TOKEN_SECRET, old.getString(KEY_TWITTER_TOKEN_SECRET, null));
            e.putString(KEY_TWITTER_TOKEN_PUBLIC, old.getString(KEY_TWITTER_TOKEN_PUBLIC, null));
            e.putInt(KEY_VERSION, old.getInt(KEY_VERSION, 0));
            e.putBoolean(KEY_LOAD_DESCRIPTION, 0 != old.getInt(KEY_LOAD_DESCRIPTION, 1));
            e.putBoolean(KEY_RATING_WANTED, old.getBoolean(KEY_RATING_WANTED, true));
            e.putBoolean(KEY_ELEVATION_WANTED, old.getBoolean(KEY_ELEVATION_WANTED, false));
            e.putBoolean(KEY_FRIENDLOGS_WANTED, old.getBoolean(KEY_FRIENDLOGS_WANTED, true));
            e.putBoolean(KEY_USE_ENGLISH, old.getBoolean(KEY_USE_ENGLISH, false));
            e.putBoolean(KEY_USE_COMPASS, 0 != old.getInt(KEY_USE_COMPASS, 1));
            e.putBoolean(KEY_AUTO_VISIT_TRACKABLES, old.getBoolean(KEY_AUTO_VISIT_TRACKABLES, false));
            e.putBoolean(KEY_AUTO_INSERT_SIGNATURE, old.getBoolean(KEY_AUTO_INSERT_SIGNATURE, false));
            e.putInt(KEY_ALTITUDE_CORRECTION, old.getInt(KEY_ALTITUDE_CORRECTION, 0));
            e.putBoolean(KEY_STORE_LOG_IMAGES, old.getBoolean(KEY_STORE_LOG_IMAGES, false));
            e.putBoolean(KEY_EXCLUDE_DISABLED, 0 != old.getInt(KEY_EXCLUDE_DISABLED, 0));
            e.putBoolean(KEY_EXCLUDE_OWN, 0 != old.getInt(KEY_EXCLUDE_OWN, 0));
            e.putString(KEY_MAPFILE, old.getString(KEY_MAPFILE, null));
            e.putString(KEY_SIGNATURE, old.getString(KEY_SIGNATURE, null));
            e.putString(KEY_GCVOTE_PASSWORD, old.getString(KEY_GCVOTE_PASSWORD, null));
            e.putString(KEY_PASSWORD, old.getString(KEY_PASSWORD, null));
            e.putString(KEY_USERNAME, old.getString(KEY_USERNAME, null));
            e.putString(KEY_MEMBER_STATUS, old.getString(KEY_MEMBER_STATUS, ""));
            e.putInt(KEY_COORD_INPUT_FORMAT, old.getInt(KEY_COORD_INPUT_FORMAT, 0));
            e.putBoolean(KEY_LOG_OFFLINE, old.getBoolean(KEY_LOG_OFFLINE, false));
            e.putBoolean(KEY_CHOOSE_LIST, old.getBoolean(KEY_CHOOSE_LIST, false));
            e.putBoolean(KEY_LOAD_DIRECTION_IMG, old.getBoolean(KEY_LOAD_DIRECTION_IMG, true));
            e.putString(KEY_GC_CUSTOM_DATE, old.getString(KEY_GC_CUSTOM_DATE, null));
            e.putInt(KEY_SHOW_WAYPOINTS_THRESHOLD, old.getInt(KEY_SHOW_WAYPOINTS_THRESHOLD, 0));
            e.putString(KEY_COOKIE_STORE, old.getString(KEY_COOKIE_STORE, null));
            e.putBoolean(KEY_OPEN_LAST_DETAILS_PAGE, old.getBoolean(KEY_OPEN_LAST_DETAILS_PAGE, false));
            e.putInt(KEY_LAST_DETAILS_PAGE, old.getInt(KEY_LAST_DETAILS_PAGE, 1));
            e.putInt(KEY_DEFAULT_NAVIGATION_TOOL, old.getInt(KEY_DEFAULT_NAVIGATION_TOOL, NavigationAppsEnum.COMPASS.id));
            e.putInt(KEY_DEFAULT_NAVIGATION_TOOL_2, old.getInt(KEY_DEFAULT_NAVIGATION_TOOL_2, NavigationAppsEnum.INTERNAL_MAP.id));
            e.putInt(KEY_LIVE_MAP_STRATEGY, old.getInt(KEY_LIVE_MAP_STRATEGY, Strategy.AUTO.id));
            e.putBoolean(KEY_DEBUG, old.getBoolean(KEY_DEBUG, false));
            e.putBoolean(KEY_HIDE_LIVE_MAP_HINT, old.getInt(KEY_HIDE_LIVE_MAP_HINT, 0) != 0);
            e.putInt(KEY_LIVE_MAP_HINT_SHOW_COUNT, old.getInt(KEY_LIVE_MAP_HINT_SHOW_COUNT, 0));

            e.putInt(KEY_SETTINGS_VERSION, 1); // mark migrated
            e.commit();
        }

        // changes for new settings dialog
        if (oldVersion < 2) {
            final Editor e = sharedPrefs.edit();

            e.putBoolean(KEY_IMPERIAL_UNITS, !isUseImperialUnits());

            // show waypoints threshold now as a slider
            int wpThreshold = getWayPointsThreshold();
            if (wpThreshold < 0) {
                wpThreshold = 0;
            } else if (wpThreshold > SHOW_WP_THRESHOLD_MAX) {
                wpThreshold = SHOW_WP_THRESHOLD_MAX;
            }
            e.putInt(KEY_SHOW_WAYPOINTS_THRESHOLD, wpThreshold);

            // KEY_MAP_SOURCE must be string, because it is the key for a ListPreference now
            int ms = sharedPrefs.getInt(KEY_MAP_SOURCE, MAP_SOURCE_DEFAULT);
            e.remove(KEY_MAP_SOURCE);
            e.putString(KEY_MAP_SOURCE, String.valueOf(ms));

            // navigation tool ids must be string, because ListPreference uses strings as keys
            int dnt1 = sharedPrefs.getInt(KEY_DEFAULT_NAVIGATION_TOOL, NavigationAppsEnum.COMPASS.id);
            int dnt2 = sharedPrefs.getInt(KEY_DEFAULT_NAVIGATION_TOOL_2, NavigationAppsEnum.INTERNAL_MAP.id);
            e.remove(KEY_DEFAULT_NAVIGATION_TOOL);
            e.remove(KEY_DEFAULT_NAVIGATION_TOOL_2);
            e.putString(KEY_DEFAULT_NAVIGATION_TOOL, String.valueOf(dnt1));
            e.putString(KEY_DEFAULT_NAVIGATION_TOOL_2, String.valueOf(dnt2));

            // defaults for gpx directories
            e.putString(KEY_GPX_IMPORT_DIR, getGpxImportDir());
            e.putString(KEY_GPX_EXPORT_DIR, getGpxExportDir());

            e.putInt(KEY_SETTINGS_VERSION, 2); // mark migrated
            e.commit();
        }
    }

    static String getString(final String key, final String defaultValue) {
        return sharedPrefs.getString(key, defaultValue);
    }

    static boolean putString(final String key, final String value) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putString(key, value);
        return edit.commit();
    }

    static boolean putBoolean(final String key, final boolean value) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putBoolean(key, value);
        return edit.commit();
    }

    static boolean putInt(final String key, final int value) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putInt(key, value);
        return edit.commit();
    }

    static boolean putFloat(final String key, final float value) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putFloat(key, value);
        return edit.commit();
    }

    static boolean remove(final String key) {
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.remove(key);
        return edit.commit();
    }

    public static void setLanguage(boolean useEnglish) {
        final Configuration config = new Configuration();
        config.locale = useEnglish ? Locale.ENGLISH : Locale.getDefault();
        final Resources resources = cgeoapplication.getInstance().getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public static boolean isLogin() {
        final String preUsername = sharedPrefs.getString(KEY_USERNAME, null);
        final String prePassword = sharedPrefs.getString(KEY_PASSWORD, null);

        return !StringUtils.isBlank(preUsername) && !StringUtils.isBlank(prePassword);
    }

    /**
     * Get login and password information.
     *
     * @return a pair (login, password) or null if no valid information is stored
     */
    public static ImmutablePair<String, String> getGcLogin() {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            final String preUsername = sharedPrefs.getString(KEY_USERNAME, null);
            final String prePassword = sharedPrefs.getString(KEY_PASSWORD, null);

            if (StringUtils.isBlank(preUsername) || StringUtils.isBlank(prePassword)) {
                return null;
            }

            username = preUsername;
            password = prePassword;
        }
        return new ImmutablePair<String, String>(username, password);
    }

    public static String getUsername() {
        return username != null ? username : sharedPrefs.getString(KEY_USERNAME, null);
    }

    // TODO: remove with SettingsActivity
    public static boolean setLogin(final String username, final String password) {
        Settings.username = username;
        Settings.password = password;
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            // erase username and password
            boolean a = remove(KEY_USERNAME);
            boolean b = remove(KEY_PASSWORD);
            return a && b;
        }
        // save username and password
        boolean a = putString(KEY_USERNAME, username);
        boolean b = putString(KEY_PASSWORD, password);
        return a && b;
    }

    public static boolean isGCConnectorActive() {
        return sharedPrefs.getBoolean(KEY_CONNECTOR_GC_ACTIVE, true);
    }

    // TODO: remove with SettingsActivity
    public static boolean setGCConnectorActive(final boolean isActive) {
        return putBoolean(KEY_CONNECTOR_GC_ACTIVE, isActive);
    }

    public static boolean isPremiumMember() {
        // Basic Member, Premium Member, ???
        String memberStatus = Settings.getMemberStatus();
        if (memberStatus == null) {
            return false;
        }
        return GCConstants.MEMBER_STATUS_PM.equalsIgnoreCase(memberStatus);
    }

    public static String getMemberStatus() {
        return sharedPrefs.getString(KEY_MEMBER_STATUS, "");
    }

    public static boolean setMemberStatus(final String memberStatus) {
        if (StringUtils.isBlank(memberStatus)) {
            return remove(KEY_MEMBER_STATUS);
        }
        return putString(KEY_MEMBER_STATUS, memberStatus);
    }

    public static boolean isOCConnectorActive() {
        return sharedPrefs.getBoolean(KEY_CONNECTOR_OC_ACTIVE, false);
    }

    public static boolean setOCConnectorActive(final boolean isActive) {
        return putBoolean(KEY_CONNECTOR_OC_ACTIVE, isActive);
    }

    public static String getOCDETokenPublic() {
        return sharedPrefs.getString(KEY_OCDE_TOKEN_PUBLIC, "");
    }

    public static String getOCDETokenSecret() {
        return sharedPrefs.getString(KEY_OCDE_TOKEN_SECRET, "");
    }

    public static void setOCDETokens(final String tokenPublic,
            final String tokenSecret, boolean enableOcDe) {
        putString(KEY_OCDE_TOKEN_PUBLIC, tokenPublic);
        putString(KEY_OCDE_TOKEN_SECRET, tokenSecret);
        if (tokenPublic != null) {
            remove(KEY_TEMP_OCDE_TOKEN_PUBLIC);
            remove(KEY_TEMP_OCDE_TOKEN_SECRET);
        }
        setOCConnectorActive(enableOcDe);
    }

    public static void setOCDETempTokens(final String tokenPublic, final String tokenSecret) {
        putString(KEY_TEMP_OCDE_TOKEN_PUBLIC, tokenPublic);
        putString(KEY_TEMP_OCDE_TOKEN_SECRET, tokenSecret);
    }

    public static ImmutablePair<String, String> getTempOCDEToken() {
        String tokenPublic = sharedPrefs.getString(KEY_TEMP_OCDE_TOKEN_PUBLIC, null);
        String tokenSecret = sharedPrefs.getString(KEY_TEMP_OCDE_TOKEN_SECRET, null);
        return new ImmutablePair<String, String>(tokenPublic, tokenSecret);
    }

    public static boolean isGCvoteLogin() {
        final String preUsername = sharedPrefs.getString(KEY_USERNAME, null);
        final String prePassword = sharedPrefs.getString(KEY_GCVOTE_PASSWORD, null);

        return !StringUtils.isBlank(preUsername) && !StringUtils.isBlank(prePassword);
    }

    // TODO: remove with SettingsActivity
    public static boolean setGCvoteLogin(final String password) {
        if (StringUtils.isBlank(password)) {
            // erase password
            return remove(KEY_GCVOTE_PASSWORD);
        }
        // save password
        return putString(KEY_GCVOTE_PASSWORD, password);
    }

    public static ImmutablePair<String, String> getGCvoteLogin() {
        final String username = sharedPrefs.getString(KEY_USERNAME, null);
        final String password = sharedPrefs.getString(KEY_GCVOTE_PASSWORD, null);

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }

        return new ImmutablePair<String, String>(username, password);
    }

    // TODO: remove with SettingsActivity
    public static boolean setSignature(final String signature) {
        if (StringUtils.isBlank(signature)) {
            // erase signature
            return remove(KEY_SIGNATURE);
        }
        // save signature
        return putString(KEY_SIGNATURE, signature);
    }

    public static String getSignature() {
        return sharedPrefs.getString(KEY_SIGNATURE, null);
    }

    public static boolean setCookieStore(final String cookies) {
        if (StringUtils.isBlank(cookies)) {
            // erase cookies
            return remove(KEY_COOKIE_STORE);
        }
        // save cookies
        return putString(KEY_COOKIE_STORE, cookies);
    }

    public static String getCookieStore() {
        return sharedPrefs.getString(KEY_COOKIE_STORE, null);
    }

    /**
     * @param cacheType
     *            The cache type used for future filtering
     */
    public static void setCacheType(final CacheType cacheType) {
        if (cacheType == null) {
            remove(KEY_CACHE_TYPE);
        } else {
            putString(KEY_CACHE_TYPE, cacheType.id);
        }
    }

    public static int getLastList() {
        return sharedPrefs.getInt(KEY_LAST_USED_LIST, StoredList.STANDARD_LIST_ID);
    }

    public static void saveLastList(final int listId) {
        putInt(KEY_LAST_USED_LIST, listId);
    }

    public static void setWebNameCode(final String name, final String code) {
        putString(KEY_WEBDEVICE_NAME, name);
        putString(KEY_WEB_DEVICE_CODE, code);
    }

    public static MapProvider getMapProvider() {
        if (mapProvider == null) {
            mapProvider = getMapSource().getMapProvider();
        }
        return mapProvider;
    }

    public static String getMapFile() {
        return sharedPrefs.getString(KEY_MAPFILE, null);
    }

    public static boolean setMapFile(final String mapFile) {
        boolean result = putString(KEY_MAPFILE, mapFile);
        if (mapFile != null) {
            setMapFileDirectory(new File(mapFile).getParent());
        }
        return result;
    }

    public static String getMapFileDirectory() {
        final String mapDir = sharedPrefs.getString(KEY_MAP_DIRECTORY, null);
        if (mapDir != null) {
            return mapDir;
        }
        final String mapFile = getMapFile();
        if (mapFile != null) {
            return new File(mapFile).getParent();
        }
        return null;
    }

    public static boolean setMapFileDirectory(final String mapFileDirectory) {
        boolean result = putString(KEY_MAP_DIRECTORY, mapFileDirectory);
        MapsforgeMapProvider.getInstance().updateOfflineMaps();
        return result;
    }

    public static boolean isValidMapFile() {
        return isValidMapFile(getMapFile());
    }

    public static boolean isValidMapFile(final String mapFileIn) {
        return MapsforgeMapProvider.isValidMapFile(mapFileIn);
    }

    public static coordInputFormatEnum getCoordInputFormat() {
        return coordInputFormatEnum.fromInt(sharedPrefs.getInt(KEY_COORD_INPUT_FORMAT, 0));
    }

    public static void setCoordInputFormat(final coordInputFormatEnum format) {
        putInt(KEY_COORD_INPUT_FORMAT, format.ordinal());
    }

    static void setLogOffline(final boolean offline) {
        putBoolean(KEY_LOG_OFFLINE, offline);
    }

    public static boolean getLogOffline() {
        return sharedPrefs.getBoolean(KEY_LOG_OFFLINE, false);
    }

    // TODO: remove with SettingsActivity
    static void setChooseList(final boolean choose) {
        putBoolean(KEY_CHOOSE_LIST, choose);
    }

    public static boolean getChooseList() {
        return sharedPrefs.getBoolean(KEY_CHOOSE_LIST, false);
    }

    // TODO: remove with SettingsActivity
    static void setLoadDirImg(final boolean value) {
        putBoolean(KEY_LOAD_DIRECTION_IMG, value);
    }

    public static boolean getLoadDirImg() {
        return !isPremiumMember() && sharedPrefs.getBoolean(KEY_LOAD_DIRECTION_IMG, true);
    }

    public static void setGcCustomDate(final String format) {
        putString(KEY_GC_CUSTOM_DATE, format);
    }

    /**
     * @return User selected date format on GC.com
     * @see Login#gcCustomDateFormats
     */
    public static String getGcCustomDate() {
        return sharedPrefs.getString(KEY_GC_CUSTOM_DATE, null);
    }

    public static boolean isExcludeMyCaches() {
        return sharedPrefs.getBoolean(KEY_EXCLUDE_OWN, false);
    }

    // TODO: remove with SettingsActivity
    public static void setExcludeMine(final boolean exclude) {
        putBoolean(KEY_EXCLUDE_OWN, exclude);
    }

    public static void setUseEnglish(final boolean english) {
        putBoolean(KEY_USE_ENGLISH, english);
        setLanguage(english);
    }

    public static boolean isUseEnglish() {
        return sharedPrefs.getBoolean(KEY_USE_ENGLISH, false);
    }

    public static boolean isShowAddress() {
        return sharedPrefs.getBoolean(KEY_SHOW_ADDRESS, true);
    }

    // TODO: remove with SettingsActivity
    public static void setShowAddress(final boolean showAddress) {
        putBoolean(KEY_SHOW_ADDRESS, showAddress);
    }

    public static boolean isShowCaptcha() {
        return !isPremiumMember() && sharedPrefs.getBoolean(KEY_SHOW_CAPTCHA, false);
    }

    // TODO: remove with SettingsActivity
    public static void setShowCaptcha(final boolean showCaptcha) {
        putBoolean(KEY_SHOW_CAPTCHA, showCaptcha);
    }

    public static boolean isExcludeDisabledCaches() {
        return sharedPrefs.getBoolean(KEY_EXCLUDE_DISABLED, false);
    }

    // TODO: remove with SettingsActivity
    public static void setExcludeDisabledCaches(final boolean exclude) {
        putBoolean(KEY_EXCLUDE_DISABLED, exclude);
    }

    public static boolean isStoreOfflineMaps() {
        return sharedPrefs.getBoolean(KEY_USE_OFFLINEMAPS, true);
    }

    // TODO: remove with SettingsActivity
    public static void setStoreOfflineMaps(final boolean offlineMaps) {
        putBoolean(KEY_USE_OFFLINEMAPS, offlineMaps);
    }

    public static boolean isStoreOfflineWpMaps() {
        return sharedPrefs.getBoolean(KEY_USE_OFFLINEWPMAPS, false);
    }

    // TODO: remove with SettingsActivity
    public static void setStoreOfflineWpMaps(final boolean offlineMaps) {
        putBoolean(KEY_USE_OFFLINEWPMAPS, offlineMaps);
    }

    public static boolean isStoreLogImages() {
        return sharedPrefs.getBoolean(KEY_STORE_LOG_IMAGES, false);
    }

    // TODO: remove with SettingsActivity
    public static void setStoreLogImages(final boolean storeLogImages) {
        putBoolean(KEY_STORE_LOG_IMAGES, storeLogImages);
    }

    public static boolean isAutoLoadDescription() {
        return sharedPrefs.getBoolean(KEY_LOAD_DESCRIPTION, true);
    }

    // TODO: remove with SettingsActivity
    public static void setAutoLoadDesc(final boolean autoLoad) {
        putBoolean(KEY_LOAD_DESCRIPTION, autoLoad);
    }

    public static boolean isRatingWanted() {
        return sharedPrefs.getBoolean(KEY_RATING_WANTED, true);
    }

    // TODO: remove with SettingsActivity
    public static void setRatingWanted(final boolean ratingWanted) {
        putBoolean(KEY_RATING_WANTED, ratingWanted);
    }

    public static boolean isElevationWanted() {
        return sharedPrefs.getBoolean(KEY_ELEVATION_WANTED, false);
    }

    // TODO: remove with SettingsActivity
    public static void setElevationWanted(final boolean elevationWanted) {
        putBoolean(KEY_ELEVATION_WANTED, elevationWanted);
    }

    public static boolean isFriendLogsWanted() {
        if (!isLogin()) {
            // don't show a friends log if the user is anonymous
            return false;
        }
        return sharedPrefs.getBoolean(KEY_FRIENDLOGS_WANTED, true);
    }

    // TODO: remove with SettingsActivity
    public static void setFriendLogsWanted(final boolean friendLogsWanted) {
        putBoolean(KEY_FRIENDLOGS_WANTED, friendLogsWanted);
    }

    public static boolean isLiveList() {
        return sharedPrefs.getBoolean(KEY_LIVE_LIST, true);
    }

    // TODO: remove with SettingsActivity
    public static void setLiveList(final boolean liveList) {
        putBoolean(KEY_LIVE_LIST, liveList);
    }

    public static boolean isTrackableAutoVisit() {
        return sharedPrefs.getBoolean(KEY_AUTO_VISIT_TRACKABLES, false);
    }

    // TODO: remove with SettingsActivity
    public static void setTrackableAutoVisit(final boolean autoVisit) {
        putBoolean(KEY_AUTO_VISIT_TRACKABLES, autoVisit);
    }

    public static boolean isAutoInsertSignature() {
        return sharedPrefs.getBoolean(KEY_AUTO_INSERT_SIGNATURE, false);
    }

    // TODO: remove with SettingsActivity
    public static void setAutoInsertSignature(final boolean autoInsert) {
        putBoolean(KEY_AUTO_INSERT_SIGNATURE, autoInsert);
    }

    public static boolean isUseImperialUnits() {
        return sharedPrefs.getBoolean(KEY_IMPERIAL_UNITS, false);
    }

    // TODO: remove with SettingsActivity
    public static void setUseImperialUnits(final boolean imperial) {
        putBoolean(KEY_IMPERIAL_UNITS, imperial);
    }

    public static boolean isLiveMap() {
        return sharedPrefs.getBoolean(KEY_MAP_LIVE, true);
    }

    public static void setLiveMap(final boolean live) {
        putBoolean(KEY_MAP_LIVE, live);
    }

    public static boolean isMapTrail() {
        return sharedPrefs.getBoolean(KEY_MAP_TRAIL, true);
    }

    public static void setMapTrail(final boolean showTrail) {
        putBoolean(KEY_MAP_TRAIL, showTrail);
    }

    public static int getMapZoom() {
        return sharedPrefs.getInt(KEY_LAST_MAP_ZOOM, 14);
    }

    public static void setMapZoom(final int mapZoomLevel) {
        putInt(KEY_LAST_MAP_ZOOM, mapZoomLevel);
    }

    public static GeoPointImpl getMapCenter() {
        return getMapProvider().getMapItemFactory()
                .getGeoPointBase(new Geopoint(sharedPrefs.getInt(KEY_LAST_MAP_LAT, 0) / 1e6,
                        sharedPrefs.getInt(KEY_LAST_MAP_LON, 0) / 1e6));
    }

    public static void setMapCenter(final GeoPointImpl mapViewCenter) {
        putInt(KEY_LAST_MAP_LAT, mapViewCenter.getLatitudeE6());
        putInt(KEY_LAST_MAP_LON, mapViewCenter.getLongitudeE6());
    }

    public static MapSource getMapSource() {
        final int id = getConvertedMapId();
        final MapSource map = MapProviderFactory.getMapSource(id);
        if (map != null) {
            // don't use offline maps if the map file is not valid
            if ((!(map instanceof OfflineMapSource)) || (isValidMapFile())) {
                return map;
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

    /**
     * convert old preference ids for maps (based on constant values) into new hash based ids
     *
     * @return
     */
    private static int getConvertedMapId() {
        // what the heck is happening here?? hashCodes of Strings?
        // why not strings?
        final int id = Integer.parseInt(sharedPrefs.getString(KEY_MAP_SOURCE,
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

    public static void setMapSource(final MapSource newMapSource) {
        if (!MapProviderFactory.isSameActivity(getMapSource(), newMapSource)) {
            mapProvider = null;
        }
        putString(KEY_MAP_SOURCE, String.valueOf(newMapSource.getNumericalId()));
        if (newMapSource instanceof OfflineMapSource) {
            setMapFile(((OfflineMapSource) newMapSource).getFileName());
        }
    }

    public static void setAnyCoordinates(final Geopoint coords) {
        if (null != coords) {
            putFloat(KEY_ANYLATITUDE, (float) coords.getLatitude());
            putFloat(KEY_ANYLONGITUDE, (float) coords.getLongitude());
        } else {
            remove(KEY_ANYLATITUDE);
            remove(KEY_ANYLONGITUDE);
        }
    }

    public static Geopoint getAnyCoordinates() {
        if (sharedPrefs.contains(KEY_ANYLATITUDE) && sharedPrefs.contains(KEY_ANYLONGITUDE)) {
            float lat = sharedPrefs.getFloat(KEY_ANYLATITUDE, 0);
            float lon = sharedPrefs.getFloat(KEY_ANYLONGITUDE, 0);
            return new Geopoint(lat, lon);
        }
        return null;
    }

    public static boolean isUseCompass() {
        return sharedPrefs.getBoolean(KEY_USE_COMPASS, true);
    }

    public static void setUseCompass(final boolean useCompass) {
        putBoolean(KEY_USE_COMPASS, useCompass);
    }

    public static boolean isLightSkin() {
        return sharedPrefs.getBoolean(KEY_SKIN, false);
    }

    // TODO: remove with SettingsActivity
    public static void setLightSkin(final boolean lightSkin) {
        putBoolean(KEY_SKIN, lightSkin);
    }

    public static String getKeyConsumerPublic() {
        return keyConsumerPublic;
    }

    public static String getKeyConsumerSecret() {
        return keyConsumerSecret;
    }

    public static int getAltitudeCorrection() {
        return sharedPrefs.getInt(KEY_ALTITUDE_CORRECTION, 0);
    }

    // TODO: remove with SettingsActivity
    public static boolean setAltitudeCorrection(final int altitude) {
        return putInt(KEY_ALTITUDE_CORRECTION, altitude);
    }

    public static String getWebDeviceCode() {
        return sharedPrefs.getString(KEY_WEB_DEVICE_CODE, null);
    }

    public static String getWebDeviceName() {
        return sharedPrefs.getString(KEY_WEBDEVICE_NAME, android.os.Build.MODEL);
    }

    /**
     * @return The cache type used for filtering or ALL if no filter is active.
     *         Returns never null
     */
    public static CacheType getCacheType() {
        return CacheType.getById(sharedPrefs.getString(KEY_CACHE_TYPE, CacheType.ALL.id));
    }

    /**
     * The Threshold for the showing of child waypoints
     */
    public static int getWayPointsThreshold() {
        return sharedPrefs.getInt(KEY_SHOW_WAYPOINTS_THRESHOLD, SHOW_WP_THRESHOLD_DEFAULT);
    }

    // TODO: remove with SettingsActivity
    public static void setShowWaypointsThreshold(final int threshold) {
        putInt(KEY_SHOW_WAYPOINTS_THRESHOLD, threshold);
    }

    public static boolean isUseTwitter() {
        return sharedPrefs.getBoolean(KEY_USE_TWITTER, false);
    }

    public static void setUseTwitter(final boolean useTwitter) {
        putBoolean(KEY_USE_TWITTER, useTwitter);
    }

    public static boolean isTwitterLoginValid() {
        return !StringUtils.isBlank(getTokenPublic())
                && !StringUtils.isBlank(getTokenSecret());
    }

    public static String getTokenPublic() {
        return sharedPrefs.getString(KEY_TWITTER_TOKEN_PUBLIC, null);
    }

    public static String getTokenSecret() {
        return sharedPrefs.getString(KEY_TWITTER_TOKEN_SECRET, null);

    }

    public static void setTwitterTokens(final String tokenPublic,
            final String tokenSecret, boolean enableTwitter) {
        putString(KEY_TWITTER_TOKEN_PUBLIC, tokenPublic);
        putString(KEY_TWITTER_TOKEN_SECRET, tokenSecret);
        if (tokenPublic != null) {
            remove(KEY_TEMP_TWITTER_TOKEN_PUBLIC);
            remove(KEY_TEMP_TWITTER_TOKEN_SECRET);
        }
        setUseTwitter(enableTwitter);
    }

    public static void setTwitterTempTokens(final String tokenPublic,
            final String tokenSecret) {
        putString(KEY_TEMP_TWITTER_TOKEN_PUBLIC, tokenPublic);
        putString(KEY_TEMP_TWITTER_TOKEN_SECRET, tokenSecret);
    }

    public static ImmutablePair<String, String> getTempToken() {
        String tokenPublic = sharedPrefs.getString(KEY_TEMP_TWITTER_TOKEN_PUBLIC, null);
        String tokenSecret = sharedPrefs.getString(KEY_TEMP_TWITTER_TOKEN_SECRET, null);
        return new ImmutablePair<String, String>(tokenPublic, tokenSecret);
    }

    public static int getVersion() {
        return sharedPrefs.getInt(KEY_VERSION, 0);
    }

    public static void setVersion(final int version) {
        putInt(KEY_VERSION, version);
    }

    public static boolean isOpenLastDetailsPage() {
        return sharedPrefs.getBoolean(KEY_OPEN_LAST_DETAILS_PAGE, false);
    }

    // TODO: remove with SettingsActivity
    public static void setOpenLastDetailsPage(final boolean openLastPage) {
        putBoolean(KEY_OPEN_LAST_DETAILS_PAGE, openLastPage);
    }

    public static int getLastDetailsPage() {
        return sharedPrefs.getInt(KEY_LAST_DETAILS_PAGE, 1);
    }

    public static void setLastDetailsPage(final int index) {
        putInt(KEY_LAST_DETAILS_PAGE, index);
    }

    public static int getDefaultNavigationTool() {
        return Integer.parseInt(sharedPrefs.getString(
                KEY_DEFAULT_NAVIGATION_TOOL,
                String.valueOf(NavigationAppsEnum.COMPASS.id)));
    }

    public static void setDefaultNavigationTool(final int defaultNavigationTool) {
        putString(KEY_DEFAULT_NAVIGATION_TOOL,
                String.valueOf(defaultNavigationTool));
    }

    public static int getDefaultNavigationTool2() {
        return Integer.parseInt(sharedPrefs.getString(
                KEY_DEFAULT_NAVIGATION_TOOL_2,
                String.valueOf(NavigationAppsEnum.INTERNAL_MAP.id)));
    }

    public static void setDefaultNavigationTool2(final int defaultNavigationTool) {
        putString(KEY_DEFAULT_NAVIGATION_TOOL_2,
                String.valueOf(defaultNavigationTool));
    }

    public static Strategy getLiveMapStrategy() {
        return Strategy.getById(sharedPrefs.getInt(KEY_LIVE_MAP_STRATEGY, Strategy.AUTO.id));
    }

    public static void setLiveMapStrategy(final Strategy strategy) {
        putInt(KEY_LIVE_MAP_STRATEGY, strategy.id);
    }

    public static boolean isDebug() {
        return Log.isDebug();
    }

    // TODO: remove with SettingsActivity
    public static void setDebug(final boolean debug) {
        putBoolean(KEY_DEBUG, debug);
        Log.setDebug(debug);
    }

    public static boolean getHideLiveMapHint() {
        return sharedPrefs.getBoolean(KEY_HIDE_LIVE_MAP_HINT, false);
    }

    public static void setHideLiveHint(final boolean hide) {
        putBoolean(KEY_HIDE_LIVE_MAP_HINT, hide);
    }

    public static int getLiveMapHintShowCount() {
        return sharedPrefs.getInt(KEY_LIVE_MAP_HINT_SHOW_COUNT, 0);
    }

    public static void setLiveMapHintShowCount(final int showCount) {
        putInt(KEY_LIVE_MAP_HINT_SHOW_COUNT, showCount);
    }

    public static boolean isDbOnSDCard() {
        return sharedPrefs.getBoolean(KEY_DB_ON_SDCARD, false);
    }

    public static void setDbOnSDCard(final boolean dbOnSDCard) {
        putBoolean(KEY_DB_ON_SDCARD, dbOnSDCard);
    }

    public static String getGpxExportDir() {
        return sharedPrefs.getString(KEY_GPX_EXPORT_DIR,
                Environment.getExternalStorageDirectory().getPath() + "/gpx");
    }

    // TODO: remove with SettingsActivity
    public static void setGpxExportDir(final String gpxExportDir) {
        putString(KEY_GPX_EXPORT_DIR, gpxExportDir);
    }

    public static String getGpxImportDir() {
        return sharedPrefs.getString(KEY_GPX_IMPORT_DIR,
                Environment.getExternalStorageDirectory().getPath() + "/gpx");
    }

    // TODO: remove with SettingsActivity
    public static void setGpxImportDir(final String gpxImportDir) {
        putString(KEY_GPX_IMPORT_DIR, gpxImportDir);
    }

    public static boolean getShareAfterExport() {
        return sharedPrefs.getBoolean(KEY_SHARE_AFTER_EXPORT, true);
    }

    public static void setShareAfterExport(final boolean shareAfterExport) {
        putBoolean(KEY_SHARE_AFTER_EXPORT, shareAfterExport);
    }

    public static int getTrackableAction() {
        return sharedPrefs.getInt(KEY_LAST_TRACKABLE_ACTION, LogType.RETRIEVED_IT.id);
    }

    public static void setTrackableAction(final int trackableAction) {
        putInt(KEY_LAST_TRACKABLE_ACTION, trackableAction);
    }

    public static String getCustomRenderThemeBaseFolder() {
        return sharedPrefs.getString(KEY_RENDER_THEME_BASE_FOLDER, "");
    }

    // TODO: remove with SettingsActivity
    public static boolean setCustomRenderThemeBaseFolder(final String folder) {
        return putString(KEY_RENDER_THEME_BASE_FOLDER, folder);
    }

    public static String getCustomRenderThemeFilePath() {
        return sharedPrefs.getString(KEY_RENDER_THEME_FILE_PATH, "");
    }

    public static void setCustomRenderThemeFile(final String customRenderThemeFile) {
        putString(KEY_RENDER_THEME_FILE_PATH, customRenderThemeFile);
    }

    public static File[] getMapThemeFiles() {
        File directory = new File(Settings.getCustomRenderThemeBaseFolder());
        List<File> result = new ArrayList<File>();
        FileUtils.listDir(result, directory, new ExtensionsBasedFileSelector(new String[] { "xml" }), null);

        return result.toArray(new File[result.size()]);
    }

    private static class ExtensionsBasedFileSelector extends FileSelector {
        private final String[] extensions;
        public ExtensionsBasedFileSelector(String[] extensions) {
            this.extensions = extensions;
        }
        @Override
        public boolean isSelected(File file) {
            String filename = file.getName();
            for (String ext : extensions) {
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
        return sharedPrefs.getBoolean(KEY_PLAIN_LOGS, false);
    }

    // TODO: remove with SettingsActivity
    public static void setPlainLogs(final boolean plainLogs) {
        putBoolean(KEY_PLAIN_LOGS, plainLogs);
    }

    public static boolean getUseNativeUa() {
        return sharedPrefs.getBoolean(KEY_NATIVE_UA, false);
    }

    // TODO: remove with SettingsActivity
    public static void setUseNativeUa(final boolean useNativeUa) {
        putBoolean(KEY_NATIVE_UA, useNativeUa);
    }

    public static String getCacheTwitterMessage() {
        // TODO make customizable from UI
        return cacheTwitterMessage;
    }

    public static String getTrackableTwitterMessage() {
        // TODO make customizable from UI
        return "I touched [NAME] ([URL])!";
    }

    public static int getLogImageScale() {
        return sharedPrefs.getInt(KEY_LOG_IMAGE_SCALE, -1);
    }

    public static void setLogImageScale(final int scale) {
        putInt(KEY_LOG_IMAGE_SCALE, scale);
    }
}
