package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LiveMapStrategy.Strategy;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mapsforge.android.maps.MapDatabase;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

/**
 * General c:geo preferences/settings set by the user
 */
public final class Settings {

    private static final String KEY_TEMP_TOKEN_SECRET = "temp-token-secret";
    private static final String KEY_TEMP_TOKEN_PUBLIC = "temp-token-public";
    private static final String KEY_HELP_SHOWN = "helper";
    private static final String KEY_ANYLONGITUDE = "anylongitude";
    private static final String KEY_ANYLATITUDE = "anylatitude";
    private static final String KEY_PUBLICLOC = "publicloc";
    private static final String KEY_USE_OFFLINEMAPS = "offlinemaps";
    private static final String KEY_USE_OFFLINEWPMAPS = "offlinewpmaps";
    private static final String KEY_WEB_DEVICE_CODE = "webDeviceCode";
    private static final String KEY_WEBDEVICE_NAME = "webDeviceName";
    private static final String KEY_MAP_LIVE = "maplive";
    private static final String KEY_MAP_SOURCE = "mapsource";
    private static final String KEY_USE_TWITTER = "twitter";
    private static final String KEY_SHOW_ADDRESS = "showaddress";
    private static final String KEY_SHOW_CAPTCHA = "showcaptcha";
    private static final String KEY_MAP_TRAIL = "maptrail";
    private static final String KEY_LAST_MAP_ZOOM = "mapzoom";
    private static final String KEY_LIVE_LIST = "livelist";
    private static final String KEY_METRIC_UNITS = "units";
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
    private static final String KEY_USE_ENGLISH = "useenglish";
    private static final String KEY_USE_COMPASS = "usecompass";
    private static final String KEY_AUTO_VISIT_TRACKABLES = "trackautovisit";
    private static final String KEY_AUTO_INSERT_SIGNATURE = "sigautoinsert";
    private static final String KEY_ALTITUDE_CORRECTION = "altcorrection";
    private static final String KEY_USE_GOOGLE_NAVIGATION = "usegnav";
    private static final String KEY_STORE_LOG_IMAGES = "logimages";
    private static final String KEY_EXCLUDE_DISABLED = "excludedisabled";
    private static final String KEY_EXCLUDE_OWN = "excludemine";
    private static final String KEY_MAPFILE = "mfmapfile";
    private static final String KEY_SIGNATURE = "signature";
    private static final String KEY_GCVOTE_PASSWORD = "pass-vote";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_MEMBER_STATUS = "memberstatus";
    private static final String KEY_COORD_INPUT_FORMAT = "coordinputformat";
    private static final String KEY_LOG_OFFLINE = "log_offline";
    private static final String KEY_LOAD_DIRECTION_IMG = "loaddirectionimg";
    private static final String KEY_GC_CUSTOM_DATE = "gccustomdate";
    private static final String KEY_SHOW_WAYPOINTS_THRESHOLD = "gcshowwaypointsthreshold";
    private static final String KEY_COOKIE_STORE = "cookiestore";
    private static final String KEY_OPEN_LAST_DETAILS_PAGE = "opendetailslastpage";
    private static final String KEY_LAST_DETAILS_PAGE = "lastdetailspage";
    private static final String KEY_DEFAULT_NAVIGATION_TOOL = "defaultNavigationTool";
    private static final String KEY_DEFAULT_NAVIGATION_TOOL_2 = "defaultNavigationTool2";
    private static final String KEY_LIVE_MAP_STRATEGY = "livemapstrategy";
    private static final String KEY_DEBUG = "debug";
    private static final String KEY_HIDE_LIVE_MAP_HINT = "hidelivemaphint";
    private static final String KEY_LIVE_MAP_HINT_SHOW_COUNT = "livemaphintshowcount";

    private final static int unitsMetric = 1;
    private final static int unitsImperial = 2;

    // twitter api keys
    private final static String keyConsumerPublic = CryptUtils.rot13("ESnsCvAv3kEupF1GCR3jGj");
    private final static String keyConsumerSecret = CryptUtils.rot13("7vQWceACV9umEjJucmlpFe9FCMZSeqIqfkQ2BnhV9x");

    private interface PrefRunnable {
        void edit(final Editor edit);
    }

    public enum coordInputFormatEnum {
        Plain,
        Deg,
        Min,
        Sec;

        static coordInputFormatEnum fromInt(int id) {
            final coordInputFormatEnum[] values = coordInputFormatEnum.values();
            if (id >= 0 && id < values.length) {
                return values[id];
            } else {
                return Min;
            }
        }
    }

    // usable values
    public static final String tag = "cgeo";

    /** Name of the preferences file */
    public static final String preferences = "cgeo.pref";

    private static final SharedPreferences sharedPrefs = cgeoapplication.getInstance().getSharedPreferences(Settings.preferences, Context.MODE_PRIVATE);
    private static String username = null;
    private static String password = null;

    // maps
    private static MapProvider mapProvider = null;

    private Settings() {
        // this class is not to be instantiated;
    }

    public static void setLanguage(boolean useEnglish) {
        final Configuration config = new Configuration();
        config.locale = useEnglish ? new Locale("en") : Locale.getDefault();
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
    public static ImmutablePair<String, String> getLogin() {
        if (username == null || password == null) {
            final String preUsername = sharedPrefs.getString(KEY_USERNAME, null);
            final String prePassword = sharedPrefs.getString(KEY_PASSWORD, null);

            if (preUsername == null || prePassword == null) {
                return null;
            }

            username = preUsername;
            password = prePassword;
        }
        return new ImmutablePair<String, String>(username, password);
    }

    public static String getUsername() {
        if (null == username) {
            return sharedPrefs.getString(KEY_USERNAME, null);
        } else {
            return username;
        }
    }

    public static boolean setLogin(final String username, final String password) {
        Settings.username = username;
        Settings.password = password;
        return editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                    // erase username and password
                    edit.remove(KEY_USERNAME);
                    edit.remove(KEY_PASSWORD);
                } else {
                    // save username and password
                    edit.putString(KEY_USERNAME, username);
                    edit.putString(KEY_PASSWORD, password);
                }
            }
        });
    }

    public static boolean isPremiumMember() {
        // Basic Member, Premium Member, ???
        String memberStatus = Settings.getMemberStatus();
        if (memberStatus == null) {
            return false;
        }
        return "Premium Member".equalsIgnoreCase(memberStatus);
    }

    public static String getMemberStatus() {
        return sharedPrefs.getString(KEY_MEMBER_STATUS, "");
    }

    public static boolean setMemberStatus(final String memberStatus) {
        return editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                if (StringUtils.isBlank(memberStatus)) {
                    edit.remove(KEY_MEMBER_STATUS);
                } else {
                    edit.putString(KEY_MEMBER_STATUS, memberStatus);
                }
            }
        });
    }

    public static boolean isGCvoteLogin() {
        final String preUsername = sharedPrefs.getString(KEY_USERNAME, null);
        final String prePassword = sharedPrefs.getString(KEY_GCVOTE_PASSWORD, null);

        return !StringUtils.isBlank(preUsername) && !StringUtils.isBlank(prePassword);
    }

    public static boolean setGCvoteLogin(final String password) {
        return editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                if (StringUtils.isBlank(password)) {
                    // erase password
                    edit.remove(KEY_GCVOTE_PASSWORD);
                } else {
                    // save password
                    edit.putString(KEY_GCVOTE_PASSWORD, password);
                }
            }
        });
    }

    public static ImmutablePair<String, String> getGCvoteLogin() {
        final String username = sharedPrefs.getString(KEY_USERNAME, null);
        final String password = sharedPrefs.getString(KEY_GCVOTE_PASSWORD, null);

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }

        return new ImmutablePair<String, String>(username, password);
    }

    public static boolean setSignature(final String signature) {
        return editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                if (StringUtils.isBlank(signature)) {
                    // erase signature
                    edit.remove(KEY_SIGNATURE);
                } else {
                    // save signature
                    edit.putString(KEY_SIGNATURE, signature);
                }
            }
        });
    }

    public static String getSignature() {
        return sharedPrefs.getString(KEY_SIGNATURE, null);
    }

    public static boolean setCookieStore(final String cookies) {
        return editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(final Editor edit) {
                if (StringUtils.isBlank(cookies)) {
                    // erase cookies
                    edit.remove(KEY_COOKIE_STORE);
                } else {
                    // save cookies
                    edit.putString(KEY_COOKIE_STORE, cookies);
                }
            }
        });
    }

    public static String getCookieStore() {
        return sharedPrefs.getString(KEY_COOKIE_STORE, null);
    }

    /**
     * @param cacheType
     *            The cache type used for future filtering
     */
    public static void setCacheType(final CacheType cacheType) {
        editSharedSettings(new PrefRunnable() {
            @Override
            public void edit(Editor edit) {
                if (cacheType == null) {
                    edit.remove(KEY_CACHE_TYPE);
                } else {
                    edit.putString(KEY_CACHE_TYPE, cacheType.id);
                }
            }
        });
    }

    public static void setLiveMap(final boolean live) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_MAP_LIVE, live ? 1 : 0);
            }
        });
    }

    public static int getLastList() {
        final int listId = sharedPrefs.getInt(KEY_LAST_USED_LIST, StoredList.STANDARD_LIST_ID);

        return listId;
    }

    public static void saveLastList(final int listId) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_LAST_USED_LIST, listId);
            }
        });
    }

    public static void setWebNameCode(final String name, final String code) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {

                edit.putString(KEY_WEBDEVICE_NAME, name);
                edit.putString(KEY_WEB_DEVICE_CODE, code);
            }
        });
    }

    public static MapProvider getMapProvider() {
        if (mapProvider == null) {
            mapProvider = MapProviderFactory.getMapProvider(getMapSource());
        }
        return mapProvider;
    }

    public static String getMapFile() {
        return sharedPrefs.getString(KEY_MAPFILE, null);
    }

    public static boolean setMapFile(final String mapFile) {
        final boolean commitResult = editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putString(KEY_MAPFILE, mapFile);
            }
        });

        return commitResult;
    }

    public static boolean isValidMapFile() {
        return checkMapfile(getMapFile());
    }

    private static boolean checkMapfile(final String mapFileIn) {
        if (null == mapFileIn) {
            return false;
        }
        return MapDatabase.isValidMapFile(mapFileIn);
    }

    public static coordInputFormatEnum getCoordInputFormat() {
        return coordInputFormatEnum.fromInt(sharedPrefs.getInt(KEY_COORD_INPUT_FORMAT, 0));
    }

    public static void setCoordInputFormat(final coordInputFormatEnum format) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_COORD_INPUT_FORMAT, format.ordinal());
            }
        });
    }

    static void setLogOffline(final boolean offline) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_LOG_OFFLINE, offline);
            }
        });
    }

    public static boolean getLogOffline() {
        return sharedPrefs.getBoolean(KEY_LOG_OFFLINE, false);
    }

    static void setLoadDirImg(final boolean value) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_LOAD_DIRECTION_IMG, value);
            }
        });
    }

    public static boolean getLoadDirImg() {
        return isPremiumMember() ? false : sharedPrefs.getBoolean(KEY_LOAD_DIRECTION_IMG, true);
    }

    public static void setGcCustomDate(final String format) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putString(KEY_GC_CUSTOM_DATE, format);
            }
        });
    }

    /**
     * @return User selected date format on GC.com
     * @see cgBase.gcCustomDateFormats
     */
    public static String getGcCustomDate() {
        return sharedPrefs.getString(KEY_GC_CUSTOM_DATE, null);
    }

    public static boolean isExcludeMyCaches() {
        return 0 != sharedPrefs.getInt(KEY_EXCLUDE_OWN, 0);
    }

    /**
     * edit some settings without knowing how to get the settings editor or how to commit
     *
     * @param runnable
     * @return
     */
    private static boolean editSharedSettings(final PrefRunnable runnable) {
        final SharedPreferences.Editor prefsEdit = sharedPrefs.edit();
        runnable.edit(prefsEdit);
        return prefsEdit.commit();
    }

    public static void setExcludeMine(final boolean exclude) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_EXCLUDE_OWN, exclude ? 1 : 0);
            }
        });
    }

    public static void setUseEnglish(final boolean english) {
        editSharedSettings(new PrefRunnable() {
            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_USE_ENGLISH, english);
                setLanguage(english);
            }
        });
    }

    public static boolean isUseEnglish() {
        return sharedPrefs.getBoolean(KEY_USE_ENGLISH, false);
    }

    public static boolean isShowAddress() {
        return 0 != sharedPrefs.getInt(KEY_SHOW_ADDRESS, 1);
    }

    public static void setShowAddress(final boolean showAddress) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_SHOW_ADDRESS, showAddress ? 1 : 0);
            }
        });
    }

    public static boolean isShowCaptcha() {
        return isPremiumMember() ? false : sharedPrefs.getBoolean(KEY_SHOW_CAPTCHA, false);
    }

    public static void setShowCaptcha(final boolean showCaptcha) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_SHOW_CAPTCHA, showCaptcha);
            }
        });
    }

    public static boolean isExcludeDisabledCaches() {
        return 0 != sharedPrefs.getInt(KEY_EXCLUDE_DISABLED, 0);
    }

    public static void setExcludeDisabledCaches(final boolean exclude) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_EXCLUDE_DISABLED, exclude ? 1 : 0);
            }
        });
    }

    public static boolean isStoreOfflineMaps() {
        return 0 != sharedPrefs.getInt(KEY_USE_OFFLINEMAPS, 1);
    }

    public static void setStoreOfflineMaps(final boolean offlineMaps) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_USE_OFFLINEMAPS, offlineMaps ? 1 : 0);
            }
        });
    }

    public static boolean isStoreOfflineWpMaps() {
        return 0 != sharedPrefs.getInt(KEY_USE_OFFLINEWPMAPS, 1);
    }

    public static void setStoreOfflineWpMaps(final boolean offlineMaps) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_USE_OFFLINEWPMAPS, offlineMaps ? 1 : 0);
            }
        });
    }

    public static boolean isStoreLogImages() {
        return sharedPrefs.getBoolean(KEY_STORE_LOG_IMAGES, false);
    }

    public static void setStoreLogImages(final boolean storeLogImages) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_STORE_LOG_IMAGES, storeLogImages);
            }
        });
    }

    public static boolean isUseGoogleNavigation() {
        return 0 != sharedPrefs.getInt(KEY_USE_GOOGLE_NAVIGATION, 1);
    }

    public static void setUseGoogleNavigation(final boolean useGoogleNavigation) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_USE_GOOGLE_NAVIGATION, useGoogleNavigation ? 1 : 0);
            }
        });
    }

    public static boolean isAutoLoadDescription() {
        return 0 != sharedPrefs.getInt(KEY_LOAD_DESCRIPTION, 0);
    }

    public static void setAutoLoadDesc(final boolean autoLoad) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_LOAD_DESCRIPTION, autoLoad ? 1 : 0);
            }
        });
    }

    public static boolean isRatingWanted() {
        return sharedPrefs.getBoolean(KEY_RATING_WANTED, true);
    }

    public static void setRatingWanted(final boolean ratingWanted) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_RATING_WANTED, ratingWanted);
            }
        });
    }

    public static boolean isElevationWanted() {
        return sharedPrefs.getBoolean(KEY_ELEVATION_WANTED, true);
    }

    public static void setElevationWanted(final boolean elevationWanted) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_ELEVATION_WANTED, elevationWanted);
            }
        });
    }

    public static boolean isFriendLogsWanted() {
        if (!isLogin()) {
            // don't show a friends log if the user is anonymous
            return false;
        }
        return sharedPrefs.getBoolean(KEY_FRIENDLOGS_WANTED, true);
    }

    public static void setFriendLogsWanted(final boolean friendLogsWanted) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_FRIENDLOGS_WANTED, friendLogsWanted);
            }
        });
    }

    public static boolean isLiveList() {
        return 0 != sharedPrefs.getInt(KEY_LIVE_LIST, 1);
    }

    public static void setLiveList(final boolean liveList) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_LIVE_LIST, liveList ? 1 : 0);
            }
        });
    }

    public static boolean isPublicLoc() {
        return 0 != sharedPrefs.getInt(KEY_PUBLICLOC, 0);
    }

    public static void setPublicLoc(final boolean publicLocation) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_PUBLICLOC, publicLocation ? 1 : 0);
            }
        });
    }

    public static boolean isTrackableAutoVisit() {
        return sharedPrefs.getBoolean(KEY_AUTO_VISIT_TRACKABLES, false);
    }

    public static void setTrackableAutoVisit(final boolean autoVisit) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_AUTO_VISIT_TRACKABLES, autoVisit);
            }
        });
    }

    public static boolean isAutoInsertSignature() {
        return sharedPrefs.getBoolean(KEY_AUTO_INSERT_SIGNATURE, false);
    }

    public static void setAutoInsertSignature(final boolean autoInsert) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_AUTO_INSERT_SIGNATURE, autoInsert);
            }
        });
    }

    public static boolean isUseMetricUnits() {
        return sharedPrefs.getInt(KEY_METRIC_UNITS, unitsMetric) == unitsMetric;
    }

    public static void setUseMetricUnits(final boolean metric) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_METRIC_UNITS, metric ? unitsMetric : unitsImperial);
            }
        });
    }

    public static boolean isLiveMap() {
        return sharedPrefs.getInt(KEY_MAP_LIVE, 1) != 0;
    }

    public static boolean isMapTrail() {
        return sharedPrefs.getInt(KEY_MAP_TRAIL, 1) != 0;
    }

    public static void setMapTrail(final boolean showTrail) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_MAP_TRAIL, showTrail ? 1 : 0);
            }
        });
    }

    public static int getMapZoom() {
        return sharedPrefs.getInt(KEY_LAST_MAP_ZOOM, 14);
    }

    public static void setMapZoom(final int mapZoomLevel) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_LAST_MAP_ZOOM, mapZoomLevel);
            }
        });
    }

    public static int getMapSource() {
        return sharedPrefs.getInt(KEY_MAP_SOURCE, 0);
    }

    public static void setMapSource(final int newMapSource) {
        if (!MapProviderFactory.isSameProvider(getMapSource(), newMapSource)) {
            mapProvider = null;
        }
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_MAP_SOURCE, newMapSource);
            }
        });
    }

    public static void setAnyCoordinates(final Geopoint coords) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                if (null != coords) {
                    edit.putFloat(KEY_ANYLATITUDE, (float) coords.getLatitude());
                    edit.putFloat(KEY_ANYLONGITUDE, (float) coords.getLongitude());
                } else {
                    edit.remove(KEY_ANYLATITUDE);
                    edit.remove(KEY_ANYLONGITUDE);
                }
            }
        });

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
        return 0 != sharedPrefs.getInt(KEY_USE_COMPASS, 1);
    }

    public static void setUseCompass(final boolean useCompass) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_USE_COMPASS, useCompass ? 1 : 0);
            }
        });
    }

    public static boolean isHelpShown() {
        return sharedPrefs.getInt(KEY_HELP_SHOWN, 0) != 0;
    }

    public static void setHelpShown() {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_HELP_SHOWN, 1);
            }
        });
    }

    public static boolean isLightSkin() {
        return sharedPrefs.getInt(KEY_SKIN, 0) != 0;
    }

    public static void setLightSkin(final boolean lightSkin) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_SKIN, lightSkin ? 1 : 0);
            }
        });
    }

    public static String getKeyConsumerPublic() {
        return keyConsumerPublic;
    }

    public static String getKeyConsumerSecret() {
        return keyConsumerSecret;
    }

    public static int getAltCorrection() {
        return sharedPrefs.getInt(KEY_ALTITUDE_CORRECTION, 0);
    }

    public static boolean setAltCorrection(final int altitude) {
        return editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_ALTITUDE_CORRECTION, altitude);
            }
        });
    }

    public static String getWebDeviceCode() {
        return sharedPrefs.getString(KEY_WEB_DEVICE_CODE, null);
    }

    public static String getWebDeviceName() {
        return sharedPrefs.getString(KEY_WEBDEVICE_NAME, null);
    }

    /**
     * @return The cache type used for filtering or ALL if no filter is active. Returns never null
     */
    public static CacheType getCacheType() {
        return CacheType.getById(sharedPrefs.getString(KEY_CACHE_TYPE, CacheType.ALL.id));
    }

    public static int getWayPointsThreshold() {
        return sharedPrefs.getInt(KEY_SHOW_WAYPOINTS_THRESHOLD, 0);
    }

    public static void setShowWaypointsThreshold(final int threshold) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_SHOW_WAYPOINTS_THRESHOLD, threshold);
            }
        });
    }

    public static boolean isUseTwitter() {
        return 0 != sharedPrefs.getInt(KEY_USE_TWITTER, 0);
    }

    public static void setUseTwitter(final boolean useTwitter) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_USE_TWITTER, useTwitter ? 1 : 0);
            }
        });
    }

    public static boolean isTwitterLoginValid() {
        return !StringUtils.isBlank(getTokenPublic()) && !StringUtils.isBlank(getTokenSecret());
    }

    public static String getTokenPublic() {
        return sharedPrefs.getString(KEY_TWITTER_TOKEN_PUBLIC, null);
    }

    public static String getTokenSecret() {
        return sharedPrefs.getString(KEY_TWITTER_TOKEN_SECRET, null);

    }

    public static int getVersion() {
        return sharedPrefs.getInt(KEY_VERSION, 0);
    }

    public static void setTwitterTokens(final String tokenPublic, final String tokenSecret, boolean enableTwitter) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putString(KEY_TWITTER_TOKEN_PUBLIC, tokenPublic);
                edit.putString(KEY_TWITTER_TOKEN_SECRET, tokenSecret);
                if (tokenPublic != null) {
                    edit.remove(KEY_TEMP_TOKEN_PUBLIC);
                    edit.remove(KEY_TEMP_TOKEN_SECRET);
                }
            }
        });
        setUseTwitter(enableTwitter);
    }

    public static void setTwitterTempTokens(final String tokenPublic, final String tokenSecret) {
        editSharedSettings(new PrefRunnable() {
            @Override
            public void edit(Editor edit) {
                edit.putString(KEY_TEMP_TOKEN_PUBLIC, tokenPublic);
                edit.putString(KEY_TEMP_TOKEN_SECRET, tokenSecret);
            }
        });
    }

    public static ImmutablePair<String, String> getTempToken() {
        String tokenPublic = sharedPrefs.getString(KEY_TEMP_TOKEN_PUBLIC, null);
        String tokenSecret = sharedPrefs.getString(KEY_TEMP_TOKEN_SECRET, null);
        return new ImmutablePair<String, String>(tokenPublic, tokenSecret);
    }

    public static void setVersion(final int version) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_VERSION, version);
            }
        });
    }

    public static boolean isOpenLastDetailsPage() {
        return sharedPrefs.getBoolean(KEY_OPEN_LAST_DETAILS_PAGE, false);
    }

    public static void setOpenLastDetailsPage(final boolean openLastPage) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putBoolean(KEY_OPEN_LAST_DETAILS_PAGE, openLastPage);
            }
        });
    }

    public static int getLastDetailsPage() {
        return sharedPrefs.getInt(KEY_LAST_DETAILS_PAGE, 1);
    }

    public static void setLastDetailsPage(final int index) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_LAST_DETAILS_PAGE, index);
            }
        });
    }

    public static int getDefaultNavigationTool() {
        return sharedPrefs.getInt(KEY_DEFAULT_NAVIGATION_TOOL, 0);
    }

    public static void setDefaultNavigationTool(final int defaultNavigationTool) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_DEFAULT_NAVIGATION_TOOL, defaultNavigationTool);
            }
        });
    }

    public static int getDefaultNavigationTool2() {
        return sharedPrefs.getInt(KEY_DEFAULT_NAVIGATION_TOOL_2, 0);
    }

    public static void setDefaultNavigationTool2(final int defaultNavigationTool) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_DEFAULT_NAVIGATION_TOOL_2, defaultNavigationTool);
            }
        });
    }

    public static Strategy getLiveMapStrategy() {
        return Strategy.getById(sharedPrefs.getInt(KEY_LIVE_MAP_STRATEGY, Strategy.AUTO.id));
    }

    public static void setLiveMapStrategy(final Strategy strategy) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_LIVE_MAP_STRATEGY, strategy.id);
            }
        });
    }

    public static boolean isDebug() {
        return sharedPrefs.getBoolean(KEY_DEBUG, false);
    }

    public static void setDebug(final boolean debug) {
        editSharedSettings(new PrefRunnable() {

            @Override public void edit(Editor edit) {
                edit.putBoolean(KEY_DEBUG, debug);
            }
        });
    }

    public static boolean getHideLiveMapHint() {
        return sharedPrefs.getInt(KEY_HIDE_LIVE_MAP_HINT, 0) == 0 ? false : true;
    }

    public static void setHideLiveHint(final boolean hide) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_HIDE_LIVE_MAP_HINT, hide ? 1 : 0);
            }
        });
    }

    public static int getLiveMapHintShowCount() {
        return sharedPrefs.getInt(KEY_LIVE_MAP_HINT_SHOW_COUNT, 0);
    }

    public static void setLiveMapHintShowCount(final int showCount) {
        editSharedSettings(new PrefRunnable() {

            @Override
            public void edit(Editor edit) {
                edit.putInt(KEY_LIVE_MAP_HINT_SHOW_COUNT, showCount);
            }
        });
    }
}
