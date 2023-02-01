package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.IAvatar;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.GCMemberState;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.ProximityNotification;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableComparator;
import cgeo.geocaching.maps.MapMode;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.google.v2.GoogleMapProvider;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.playservices.GooglePlayServices;
import cgeo.geocaching.sensors.DirectionData;
import cgeo.geocaching.sensors.MagnetometerAndAccelerometerProvider;
import cgeo.geocaching.sensors.OrientationProvider;
import cgeo.geocaching.sensors.RotationProvider;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.PersistableUri;
import cgeo.geocaching.ui.AvatarUtils;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.maps.MapProviderFactory.MAP_LANGUAGE_DEFAULT_ID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * General c:geo preferences/settings set by the user
 */
public class Settings {

    private static final String LEGACY_UNUSED_MARKER = "unused::";

    /**
     * Separator char for preferences with multiple elements.
     */
    private static final char SEPARATOR_CHAR = ',';

    public static final int MAPROTATION_OFF = 0;
    public static final int MAPROTATION_MANUAL = 1;
    public static final int MAPROTATION_AUTO = 2;

    public static final int COMPACTICON_OFF = 0;
    public static final int COMPACTICON_ON = 1;
    public static final int COMPACTICON_AUTO = 2;

    public static final int HOURS_TO_SECONDS = 60 * 60;
    public static final int DAYS_TO_SECONDS = 24 * HOURS_TO_SECONDS;

    private static final int HISTORY_SIZE = 50;

    private static final int MAP_SOURCE_DEFAULT = GoogleMapProvider.GOOGLE_MAP_ID.hashCode();

    private static final String PHONE_MODEL_AND_SDK = Build.MODEL + "/" + Build.VERSION.SDK_INT;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // twitter api keys
    @NonNull private static final String TWITTER_KEY_CONSUMER_PUBLIC = CryptUtils.rot13("ESnsCvAv3kEupF1GCR3jGj");
    @NonNull private static final String TWITTER_KEY_CONSUMER_SECRET = CryptUtils.rot13("7vQWceACV9umEjJucmlpFe9FCMZSeqIqfkQ2BnhV9x");

    private static boolean useCompass = true;
    private static DirectionData.DeviceOrientation deviceOrientationMode = DirectionData.DeviceOrientation.AUTO;

    /**
     * Cache the mapsource locally. If that is an offline map source, each request would potentially access the
     * underlying map file, leading to delays.
     */
    private static MapSource mapSource;
    private static AbstractTileProvider tileProvider;

    public static final String RENDERTHEMESCALE_DEFAULTKEY = "renderthemescale_default";

    public enum CoordInputFormatEnum {
        Plain,
        Deg,
        Min,
        Sec;

        public static final int DEFAULT_INT_VALUE = Min.ordinal();

        public static CoordInputFormatEnum fromInt(final int id) {
            final CoordInputFormatEnum[] values = CoordInputFormatEnum.values();
            if (id < 0 || id >= values.length) {
                return Min;
            }
            return values[id];
        }
    }

    /**
     * Possible values of the Dark Mode Setting.
     * <p>
     * The Dark Mode Setting can be stored in {@link android.content.SharedPreferences} as String by using {@link DarkModeSetting#getPreferenceValue(Context)} and received via {@link DarkModeSetting#valueOf(String)}.
     * <p>
     * Additionally, the equivalent {@link AppCompatDelegate}-Mode can be received via {@link #getModeId()}.
     *
     * @see AppCompatDelegate#MODE_NIGHT_YES
     * @see AppCompatDelegate#MODE_NIGHT_NO
     * @see AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM
     */
    public enum DarkModeSetting {

        /**
         * Always use light mode.
         */
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO, R.string.pref_value_theme_light),
        /**
         * Always use dark mode.
         */
        DARK(AppCompatDelegate.MODE_NIGHT_YES, R.string.pref_value_theme_dark),
        /**
         * Follow the global system setting for dark mode.
         */
        SYSTEM_DEFAULT(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.pref_value_theme_system_default);

        private final @AppCompatDelegate.NightMode
        int modeId;
        private final @StringRes
        int preferenceValue;

        DarkModeSetting(final @AppCompatDelegate.NightMode int modeId, final @StringRes int preferenceValue) {
            this.modeId = modeId;
            this.preferenceValue = preferenceValue;
        }

        public int getModeId() {
            return modeId;
        }

        public String getPreferenceValue(final @NonNull Context context) {
            return context.getString(preferenceValue);
        }
    }

    public static class PrefLogTemplate {
        private final @NonNull String key;
        private final String title;
        private final String text;

        @JsonCreator
        public PrefLogTemplate(@JsonProperty("key") final String key, @JsonProperty("title") final String title, @JsonProperty("text") final String text) {
            this.key = key;
            this.title = title;
            this.text = text;
        }

        @NonNull
        public String getKey() {
            return key;
        }

        public String getTitle() {
            return title;
        }

        public String getText() {
            return text;
        }

        @Override
        public boolean equals(final Object o) {
            if (getClass() != o.getClass()) {
                return false;
            }
            final PrefLogTemplate p = (PrefLogTemplate) o;
            return p.getKey().equals(this.getKey());
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        @NonNull
        public String toString() {
            return title;
        }
    }

    public enum RenderThemeScaleType { MAP, TEXT, SYMBOL }

    //NO_APPLICATION_MODE will be true if Settings is used in context of local unit tests
    private static final boolean NO_APPLICATION_MODE = CgeoApplication.getInstance() == null;

    private static final SharedPreferences sharedPrefs = NO_APPLICATION_MODE ? null : PreferenceManager
            .getDefaultSharedPreferences(CgeoApplication.getInstance().getBaseContext());

    static {
        migrateSettings();
        Log.setDebug(getBoolean(R.string.pref_debug, false));

    }

    protected Settings() {
        throw new InstantiationError();
    }

    public static int getActualVersion() {
        return getInt(R.string.pref_settingsversion, 0);
    }

    public static void setActualVersion(final int newVersion) {
        putInt(R.string.pref_settingsversion, newVersion);
    }

    public static int getExpectedVersion() {
        return 9;
    }

    private static void migrateSettings() {
        //NO migration in NO_APP_MODE
        if (NO_APPLICATION_MODE) {
            return;
        }

        final int latestPreferencesVersion = getExpectedVersion();
        final int currentVersion = getActualVersion();

        // No need to migrate if we are up to date.
        if (currentVersion == latestPreferencesVersion) {
            return;
        }

        // No need to migrate if we don't have older settings, defaults will be used instead.
        final String preferencesNameV0 = "cgeo.pref";
        final SharedPreferences prefsV0 = CgeoApplication.getInstance().getSharedPreferences(preferencesNameV0, Context.MODE_PRIVATE);
        if (currentVersion == 0 && prefsV0.getAll().isEmpty()) {
            final Editor e = sharedPrefs.edit();
            e.putInt(getKey(R.string.pref_settingsversion), latestPreferencesVersion);
            e.apply();
            return;
        }

        if (currentVersion < 1) {
            // migrate from non standard file location and integer based boolean types
            final Editor e = sharedPrefs.edit();

            e.putString(getKey(R.string.pref_temp_twitter_token_secret), prefsV0.getString(getKey(R.string.pref_temp_twitter_token_secret), null));
            e.putString(getKey(R.string.pref_temp_twitter_token_public), prefsV0.getString(getKey(R.string.pref_temp_twitter_token_public), null));
            e.putBoolean(getKey(R.string.pref_help_shown), prefsV0.getInt(getKey(R.string.pref_help_shown), 0) != 0);
            e.putString(getKey(R.string.pref_webDeviceCode), prefsV0.getString(getKey(R.string.pref_webDeviceCode), null));
            e.putString(getKey(R.string.pref_webDeviceName), prefsV0.getString(getKey(R.string.pref_webDeviceName), null));
            e.putBoolean(getKey(R.string.pref_maplive), prefsV0.getInt(getKey(R.string.pref_maplive), 1) != 0);
            e.putInt(getKey(R.string.pref_mapsource), prefsV0.getInt(getKey(R.string.pref_mapsource), MAP_SOURCE_DEFAULT));
            e.putBoolean(getKey(R.string.pref_twitter), prefsV0.getInt(getKey(R.string.pref_twitter), 0) != 0);
            e.putBoolean(getKey(R.string.pref_showaddress), prefsV0.getInt(getKey(R.string.pref_showaddress), 1) != 0);
            e.putBoolean(getKey(R.string.pref_maptrail), prefsV0.getInt(getKey(R.string.pref_maptrail), 1) != 0);
            e.putInt(getKey(R.string.pref_lastmapzoom), prefsV0.getInt(getKey(R.string.pref_lastmapzoom), 14));
            e.putBoolean(getKey(R.string.pref_livelist), prefsV0.getInt(getKey(R.string.pref_livelist), 1) != 0);
            e.putBoolean(getKey(R.string.pref_units_imperial), prefsV0.getInt(getKey(R.string.pref_units_imperial), 1) != 1);
            e.putBoolean(getKey(R.string.old_pref_skin), prefsV0.getInt(getKey(R.string.old_pref_skin), 0) != 0);
            e.putInt(getKey(R.string.pref_lastusedlist), prefsV0.getInt(getKey(R.string.pref_lastusedlist), StoredList.STANDARD_LIST_ID));
            e.putString(getKey(R.string.pref_twitter_token_secret), prefsV0.getString(getKey(R.string.pref_twitter_token_secret), null));
            e.putString(getKey(R.string.pref_twitter_token_public), prefsV0.getString(getKey(R.string.pref_twitter_token_public), null));
            e.putInt(getKey(R.string.pref_version), prefsV0.getInt(getKey(R.string.pref_version), 0));
            e.putBoolean(getKey(R.string.pref_ratingwanted), prefsV0.getBoolean(getKey(R.string.pref_ratingwanted), true));
            e.putBoolean(getKey(R.string.pref_friendlogswanted), prefsV0.getBoolean(getKey(R.string.pref_friendlogswanted), true));
            e.putBoolean(getKey(R.string.old_pref_useenglish), prefsV0.getBoolean(getKey(R.string.old_pref_useenglish), false));
            e.putBoolean(getKey(R.string.pref_usecompass), prefsV0.getInt(getKey(R.string.pref_usecompass), 1) != 0);
            e.putBoolean(getKey(R.string.pref_trackautovisit), prefsV0.getBoolean(getKey(R.string.pref_trackautovisit), false));
            e.putBoolean(getKey(R.string.pref_sigautoinsert), prefsV0.getBoolean(getKey(R.string.pref_sigautoinsert), false));
            e.putBoolean(getKey(R.string.pref_logimages), prefsV0.getBoolean(getKey(R.string.pref_logimages), false));
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
            e.putInt(getKey(R.string.pref_showwaypointsthreshold), prefsV0.getInt(getKey(R.string.pref_showwaypointsthreshold), getKeyInt(R.integer.waypoint_threshold_default)));
            e.putBoolean(getKey(R.string.pref_opendetailslastpage), prefsV0.getBoolean(getKey(R.string.pref_opendetailslastpage), false));
            e.putInt(getKey(R.string.pref_lastdetailspage), prefsV0.getInt(getKey(R.string.pref_lastdetailspage), 1));
            e.putInt(getKey(R.string.pref_defaultNavigationTool), prefsV0.getInt(getKey(R.string.pref_defaultNavigationTool), NavigationAppsEnum.COMPASS.id));
            e.putInt(getKey(R.string.pref_defaultNavigationTool2), prefsV0.getInt(getKey(R.string.pref_defaultNavigationTool2), NavigationAppsEnum.INTERNAL_MAP.id));
            e.putBoolean(getKey(R.string.pref_debug), prefsV0.getBoolean(getKey(R.string.pref_debug), false));

            e.putInt(getKey(R.string.pref_settingsversion), 1); // mark migrated
            e.apply();
        }

        // changes for new settings dialog
        if (currentVersion < 2) {
            final Editor e = sharedPrefs.edit();

            e.putBoolean(getKey(R.string.pref_units_imperial), useImperialUnits());

            // show waypoints threshold now as a slider
            int wpThreshold = Math.max(0, getWayPointsThreshold());
            wpThreshold = Math.min(wpThreshold, getKeyInt(R.integer.waypoint_threshold_max));
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

            e.putInt(getKey(R.string.pref_settingsversion), 2); // mark migrated
            e.apply();
        }

        if (currentVersion < 3) {
            final Editor e = sharedPrefs.edit();

            Log.i("Moving field-notes");
            FileUtils.move(LocalStorage.getLegacyFieldNotesDirectory(), LocalStorage.getFieldNotesDirectory());

            Log.i("Moving gpx ex- and import dirs");
            FileUtils.move(LocalStorage.getLegacyGpxDirectory(), LocalStorage.getDefaultGpxDirectory());

            Log.i("Moving db files");
            FileUtils.moveTo(new File(LocalStorage.getLegacyExternalCgeoDirectory(), DataStore.DB_FILE_NAME), LocalStorage.getExternalDbDirectory());
            FileUtils.moveTo(new File(LocalStorage.getLegacyExternalCgeoDirectory(), DataStore.DB_FILE_NAME + DataStore.DB_FILE_CORRUPTED_EXTENSION), LocalStorage.getBackupRootDirectory());
            FileUtils.moveTo(new File(LocalStorage.getLegacyExternalCgeoDirectory(), DataStore.DB_FILE_NAME_BACKUP), LocalStorage.getBackupRootDirectory());
            FileUtils.moveTo(new File(LocalStorage.getLegacyExternalCgeoDirectory(), DataStore.DB_FILE_NAME_BACKUP + DataStore.DB_FILE_CORRUPTED_EXTENSION), LocalStorage.getBackupRootDirectory());

            Log.i("Moving geocache data");
            final FileFilter geocacheDirectories = pathname -> {
                final String name = pathname.getName();
                return pathname.isDirectory() &&
                        (HtmlImage.SHARED.equals(name) || LocalStorage.GEOCACHE_FILE_PATTERN.matcher(name).find());
            };
            final File[] list = LocalStorage.getLegacyExternalCgeoDirectory().listFiles(geocacheDirectories);
            if (list != null) {
                for (final File file : list) {
                    FileUtils.moveTo(file, LocalStorage.getGeocacheDataDirectory());
                }
            }

            Log.i("Deleting legacy .cgeo dir");
            FileUtils.deleteIgnoringFailure(LocalStorage.getLegacyExternalCgeoDirectory());

            e.putString(getKey(R.string.pref_dataDir), LocalStorage.getExternalPrivateCgeoDirectory().getAbsolutePath());
            e.putInt(getKey(R.string.pref_settingsversion), 3); // mark migrated
            e.apply();
        }

        if (currentVersion < 4) {
            final Editor e = sharedPrefs.edit();

            if (Integer.parseInt(sharedPrefs.getString(getKey(R.string.pref_defaultNavigationTool), String.valueOf(NavigationAppsEnum.COMPASS.id))) == 25) {
                e.putString(getKey(R.string.pref_defaultNavigationTool), prefsV0.getString(getKey(R.string.pref_defaultNavigationTool), String.valueOf(NavigationAppsEnum.INTERNAL_MAP.id)));
            }

            if (Integer.parseInt(sharedPrefs.getString(getKey(R.string.pref_defaultNavigationTool2), String.valueOf(NavigationAppsEnum.INTERNAL_MAP.id))) == 25) {
                e.putString(getKey(R.string.pref_defaultNavigationTool2), prefsV0.getString(getKey(R.string.pref_defaultNavigationTool2), String.valueOf(NavigationAppsEnum.INTERNAL_MAP.id)));
            }

            e.putInt(getKey(R.string.pref_settingsversion), 4); // mark migrated
            e.apply();
        }

        if (currentVersion < 5) {
            // non-used version which spilled into the nightlies. Just mark as migrated
            setActualVersion(5);
        }

        // the whole range of version numbers from 6 until 8 was "used" in different parts
        // of migration of global exclude settings due to different bugs.
        // Since this spilled into nightlies and beta, they can't be reused.
        if (currentVersion < 8) {
            //migrate global own/found/disable/archived/offlinelog to LIVE filter
            final Map<GeocacheFilter.QuickFilter, Boolean> legacyGlobalSettings = new HashMap<>();
            //see #11311: in some cases, the "exclude found" might not exist yet on user's devices
            final boolean legacyExcludeMine = getBooleanDirect("excludemine", false);
            final boolean legacyExcludeFound = hasKeyDirect("excludefound") ? getBooleanDirect("excludefound", false) : legacyExcludeMine;

            legacyGlobalSettings.put(GeocacheFilter.QuickFilter.OWNED, !legacyExcludeMine);
            legacyGlobalSettings.put(GeocacheFilter.QuickFilter.FOUND, !legacyExcludeFound);
            legacyGlobalSettings.put(GeocacheFilter.QuickFilter.DISABLED, !getBooleanDirect("excludedisabled", false));
            legacyGlobalSettings.put(GeocacheFilter.QuickFilter.ARCHIVED, !getBooleanDirect("excludearchived", false));

            final GeocacheFilterContext liveFilterContext = new GeocacheFilterContext(GeocacheFilterContext.FilterType.LIVE);
            GeocacheFilter liveFilter = liveFilterContext.get();

            if (!liveFilter.hasSameQuickFilter(legacyGlobalSettings)) {
                if (!liveFilter.canSetQuickFilterLossless()) {
                    //settings can't be merged -> remove old filter
                    liveFilter = GeocacheFilter.createEmpty();
                }
                liveFilter.setQuickFilterLossless(legacyGlobalSettings);
                liveFilterContext.set(liveFilter);
            }

            setActualVersion(8);
        }

        if (currentVersion < 9) {
            final Editor e = sharedPrefs.edit();

            final boolean isMapAutoDownloads = sharedPrefs.getBoolean(getKey(R.string.old_pref_mapAutoDownloads), false);
            if (!isMapAutoDownloads) {
                e.putInt(getKey(R.string.pref_mapAutoDownloadsInterval), 0);
            }
            e.remove(getKey(R.string.old_pref_mapAutoDownloads)); // key no longer in use, will fall back to default on downgrade

            final boolean isRoutingTileAutoDownloads = sharedPrefs.getBoolean(getKey(R.string.pref_brouterAutoTileDownloads), false);
            if (!isRoutingTileAutoDownloads) {
                e.putInt(getKey(R.string.pref_brouterAutoTileDownloadsInterval), 0);
            }
            // bRouterAutoTileDownloads key is still in use, do NOT remove it

            e.apply();
            setActualVersion(9);
        }
    }

    private static String getKey(final int prefKeyId) {
        return CgeoApplication.getInstance() == null ? null : CgeoApplication.getInstance().getString(prefKeyId);
    }

    public static int getKeyInt(final int prefKeyId) {
        return CgeoApplication.getInstance() == null ? -1 : CgeoApplication.getInstance().getResources().getInteger(prefKeyId);
    }

    protected static String getString(final int prefKeyId, final String defaultValue) {
        return getStringDirect(getKey(prefKeyId), defaultValue);
    }

    private static boolean hasKeyDirect(final String prefKey) {
        return sharedPrefs != null && sharedPrefs.contains(prefKey);
    }

    private static String getStringDirect(final String prefKey, final String defaultValue) {
        return sharedPrefs == null ? defaultValue : sharedPrefs.getString(prefKey, defaultValue);
    }


    private static List<String> getStringList(final int prefKeyId, final String defaultValue) {
        return Arrays.asList(StringUtils.split(getString(prefKeyId, defaultValue), SEPARATOR_CHAR));
    }

    private static int getInt(final int prefKeyId, final int defaultValue) {
        return getIntDirect(getKey(prefKeyId), defaultValue);
    }

    private static int getIntDirect(final String prefKey, final int defaultValue) {
        return sharedPrefs == null ? defaultValue : sharedPrefs.getInt(prefKey, defaultValue);
    }

    public static int getPreferencesCount() {
        return sharedPrefs.getAll().size();
    }

    // workaround for int prefs, originally saved as string
    private static int getIntFromString(final int prefKeyId, final int defaultValue) {
        try {
            return Integer.parseInt(getString(prefKeyId, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }

    }

    private static long getLong(final int prefKeyId, final long defaultValue) {
        return sharedPrefs == null ? defaultValue : sharedPrefs.getLong(getKey(prefKeyId), defaultValue);
    }

    public static boolean getBoolean(final int prefKeyId, final boolean defaultValue) {
        return getBooleanDirect(getKey(prefKeyId), defaultValue);
    }

    private static boolean getBooleanDirect(final String prefKey, final boolean defaultValue) {
        try {
            return sharedPrefs == null ? defaultValue : sharedPrefs.getBoolean(prefKey, defaultValue);
        } catch (ClassCastException cce) {
            return defaultValue;
        }
    }


    private static float getFloat(final int prefKeyId, final float defaultValue) {
        return sharedPrefs == null ? defaultValue : sharedPrefs.getFloat(getKey(prefKeyId), defaultValue);
    }

    protected static void putString(final int prefKeyId, final String value) {
        putStringDirect(getKey(prefKeyId), value);
    }

    private static void putStringDirect(final String prefKey, final String value) {
        if (sharedPrefs == null) {
            return;
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putString(prefKey, value);
        edit.apply();
    }

    private static void putStringList(final int prefKeyId, final Iterable<?> elements) {
        putString(prefKeyId, StringUtils.join(elements, SEPARATOR_CHAR));
    }

    protected static void putBoolean(final int prefKeyId, final boolean value) {
        if (sharedPrefs == null) {
            return;
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putBoolean(getKey(prefKeyId), value);
        edit.apply();
    }

    private static void putInt(final int prefKeyId, final int value) {
        if (sharedPrefs == null) {
            return;
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putInt(getKey(prefKeyId), value);
        edit.apply();
    }

    private static void putLong(final int prefKeyId, final long value) {
        if (sharedPrefs == null) {
            return;
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putLong(getKey(prefKeyId), value);
        edit.apply();
    }

    private static void putFloat(final int prefKeyId, final float value) {
        if (sharedPrefs == null) {
            return;
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putFloat(getKey(prefKeyId), value);
        edit.apply();
    }

    private static void remove(final int prefKeyId) {
        removeDirect(getKey(prefKeyId));
    }

    private static void removeDirect(final String key) {
        if (sharedPrefs == null) {
            return;
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.remove(key);
        edit.apply();
    }

    private static boolean contains(final int prefKeyId) {
        return sharedPrefs != null && sharedPrefs.contains(getKey(prefKeyId));
    }

    public static boolean hasGCCredentials() {
        return getGcCredentials().isValid();
    }

    /**
     * Get login and password information of Geocaching.com.
     *
     * @return a pair either with (login, password) or (empty, empty) if no valid information is stored
     */
    public static Credentials getGcCredentials() {
        return getCredentials(GCConnector.getInstance());
    }

    /**
     * Get login and password information from preference key id.
     *
     * @param usernamePreferenceKey Username preference key id
     * @param passwordPreferenceKey Password preference key id
     * @return the credential information
     */
    @NonNull
    public static Credentials getCredentials(final int usernamePreferenceKey, final int passwordPreferenceKey) {
        final String username = StringUtils.trim(getString(usernamePreferenceKey, StringUtils.EMPTY));
        final String password = getString(passwordPreferenceKey, StringUtils.EMPTY);
        return new Credentials(username, password);
    }

    /**
     * Get login and password information.
     *
     * @param connector the connector to retrieve the login information from
     * @return the credential information
     */
    @NonNull
    public static Credentials getCredentials(@NonNull final ICredentials connector) {
        return getCredentials(connector.getUsernamePreferenceKey(), connector.getPasswordPreferenceKey());
    }

    /**
     * Set login and password information.
     *
     * @param connector   the connector to retrieve the login information from
     * @param credentials the credential information to store
     */
    public static void setCredentials(@NonNull final ICredentials connector, final Credentials credentials) {
        putString(connector.getUsernamePreferenceKey(), credentials.getUsernameRaw());
        putString(connector.getPasswordPreferenceKey(), credentials.getPasswordRaw());
    }

    public static String getUserName() {
        return StringUtils.trim(getString(R.string.pref_username, StringUtils.EMPTY));
    }

    public static boolean isGCConnectorActive() {
        return getBoolean(R.string.pref_connectorGCActive, false);
    }

    public static void setGCConnectorActive(final boolean value) {
        putBoolean(R.string.pref_connectorGCActive, value);
    }

    public static boolean isECConnectorActive() {
        return getBoolean(R.string.pref_connectorECActive, false);
    }

    public static boolean isALConnectorActive() {
        return getBoolean(R.string.pref_connectorALActive, true);
    }

    public static boolean isSUConnectorActive() {
        return getBoolean(R.string.pref_connectorSUActive, false);
    }

    public static boolean isGCPremiumMember() {
        return getGCMemberStatus().isPremium();
    }

    public static boolean isALCAdvanced() {
        return getBoolean(R.string.pref_alc_advanced, false);
    }

    public static boolean enableFeatureNewImageGallery() {
        if (!contains(R.string.pref_feature_new_image_gallery)) {
            //return !BranchDetectionHelper.isProductionBuild();
            return true;
        }
        return getBoolean(R.string.pref_feature_new_image_gallery, false);
    }

    public static String getALCLauncher() {
        return getString(R.string.pref_alc_launcher, "");
    }

    public static GCMemberState getGCMemberStatus() {
        return GCMemberState.fromString(getString(R.string.pref_memberstatus, ""));
    }

    public static void setGCMemberStatus(final GCMemberState memberStatus) {
        putString(R.string.pref_memberstatus, memberStatus.id);
    }

    @NonNull
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

    public static boolean hasOAuthAuthorization(final int tokenPublicPrefKeyId, final int tokenSecretPrefKeyId) {
        return StringUtils.isNotBlank(getString(tokenPublicPrefKeyId, ""))
                && StringUtils.isNotBlank(getString(tokenSecretPrefKeyId, ""));
    }

    public static boolean isGCVoteLoginValid() {
        return getGCVoteLogin().isValid();
    }

    @NonNull
    public static Credentials getGCVoteLogin() {
        final String username = StringUtils.trimToNull(getString(R.string.pref_username, null));
        final String password = getString(R.string.pref_pass_vote, null);
        return new Credentials(username, password);
    }

    @NonNull
    public static String getSignature() {
        return StringUtils.defaultString(getString(R.string.pref_signature, StringUtils.EMPTY));
    }

    public static void setUseGooglePlayServices(final boolean value) {
        putBoolean(R.string.pref_googleplayservices, value);
    }

    public static boolean useGooglePlayServices() {
        if (!GooglePlayServices.isAvailable()) {
            return false;
        }
        return outdatedPhoneModelOrSdk() || getBoolean(R.string.pref_googleplayservices, true);
    }

    public static boolean useLowPowerMode() {
        return getBoolean(R.string.pref_lowpowermode, false);
    }

    public static int getLastDisplayedList() {
        return getInt(R.string.pref_lastusedlist, StoredList.STANDARD_LIST_ID);
    }

    /**
     * remember the last displayed cache list
     */
    public static void setLastDisplayedList(final int listId) {
        putInt(R.string.pref_lastusedlist, listId);
    }

    public static Set<Integer> getLastSelectedLists() {
        final Set<Integer> lastSelectedLists = new HashSet<>();
        for (final String lastSelectedListString : getStringList(R.string.pref_last_selected_lists, StringUtils.EMPTY)) {
            try {
                lastSelectedLists.add(Integer.valueOf(lastSelectedListString));
            } catch (final NumberFormatException ignored) {
            }
        }
        return lastSelectedLists;
    }

    /**
     * remember the last selection in the dialog that assigns a cache to certain lists
     */
    public static void setLastSelectedLists(final Set<Integer> lastSelectedLists) {
        putStringList(R.string.pref_last_selected_lists, lastSelectedLists);
    }

    public static void setWebNameCode(final String name, final String code) {
        putString(R.string.pref_webDeviceName, name);
        putString(R.string.pref_webDeviceCode, code);
    }

    public static MapProvider getMapProvider() {
        return getMapSource().getMapProvider();
    }

    public static int getMapRotation() {
        final String prefValue = getString(R.string.pref_mapRotation, "");
        if (prefValue.equals(getKey(R.string.pref_maprotation_off))) {
            return MAPROTATION_OFF;
        } else if (prefValue.equals(getKey(R.string.pref_maprotation_auto))) {
            return MAPROTATION_AUTO;
        }
        return MAPROTATION_MANUAL;
    }

    public static void setMapRotation(final int mapRotation) {
        switch (mapRotation) {
            case MAPROTATION_OFF:
                putString(R.string.pref_mapRotation, getKey(R.string.pref_maprotation_off));
                break;
            case MAPROTATION_MANUAL:
                putString(R.string.pref_mapRotation, getKey(R.string.pref_maprotation_manual));
                break;
            case MAPROTATION_AUTO:
                putString(R.string.pref_mapRotation, getKey(R.string.pref_maprotation_auto));
                break;
            default:
                // do nothing except satisfy static code analysis
                break;
        }
    }

    public static boolean isAutotargetIndividualRoute() {
        return getBoolean(R.string.pref_autotarget_individualroute, false);
    }

    public static void setAutotargetIndividualRoute(final boolean autotargetIndividualRoute) {
        putBoolean(R.string.pref_autotarget_individualroute, autotargetIndividualRoute);
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
        return getBoolean(R.string.pref_choose_list, true);
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

    public static boolean isWallpaper() {
        return getBoolean(R.string.pref_wallpaper, false);
    }

    public static boolean isShowAddress() {
        return getBoolean(R.string.pref_showaddress, true);
    }

    public static boolean isExcludeWpOriginal() {
        return getBoolean(R.string.pref_excludeWpOriginal, false);
    }

    public static boolean isExcludeWpParking() {
        return getBoolean(R.string.pref_excludeWpParking, false);
    }

    public static boolean isExcludeWpVisited() {
        return getBoolean(R.string.pref_excludeWpVisited, false);
    }

    public static boolean isStoreLogImages() {
        return getBoolean(R.string.pref_logimages, false);
    }

    public static boolean isRatingWanted() {
        return getBoolean(R.string.pref_ratingwanted, false);
    }

    public static boolean isGeokretyConnectorActive() {
        return getBoolean(R.string.pref_connectorGeokretyActive, false);
    }

    public static boolean hasGeokretyAuthorization() {
        return StringUtils.isNotBlank(getGeokretySecId());
    }

    public static String getGeokretySecId() {
        return getString(R.string.pref_fakekey_geokrety_authorization, null);
    }

    public static void setGeokretySecId(final String secid) {
        putString(R.string.pref_fakekey_geokrety_authorization, secid);
    }

    public static String getTokenSecret(final int prefKeyId) {
        return getString(prefKeyId, StringUtils.EMPTY);
    }

    public static void setTokenSecret(final int prefKeyId, final String secretToken) {
        putString(prefKeyId, secretToken);
    }

    public static boolean isRegisteredForGeokretyLogging() {
        return getGeokretySecId() != null;
    }

    /**
     * Retrieve showed popup counter for warning about logging Trackable recommend Geocode
     *
     * @return number of times the popup has appeared
     */
    public static int getLogTrackableWithoutGeocodeShowCount() {
        return getInt(R.string.pref_logtrackablewithoutgeocodeshowcount, 0);
    }

    /**
     * Store showed popup counter for warning about logging Trackable recommend Geocode
     *
     * @param showCount the count to save
     */
    public static void setLogTrackableWithoutGeocodeShowCount(final int showCount) {
        putInt(R.string.pref_logtrackablewithoutgeocodeshowcount, showCount);
    }

    public static boolean isFriendLogsWanted() {
        if (!hasGCCredentials()) {
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

    public static boolean isDisplayOfflineLogsHomescreen() {
        return getBoolean(R.string.pref_offlinelogs_homescreen, true);
    }

    static void setUseImperialUnits(final boolean useImperialUnits) {
        putBoolean(R.string.pref_units_imperial, useImperialUnits);
    }

    public static boolean useImperialUnits() {
        return getBoolean(R.string.pref_units_imperial, useImperialUnitsByDefault());
    }

    private static boolean useImperialUnitsByDefault() {
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
        return getBoolean(R.string.pref_maptrail, false);
    }

    public static void setMapTrail(final boolean showTrail) {
        putBoolean(R.string.pref_maptrail, showTrail);
    }

    public static int getMaximumMapTrailLength() {
        return getInt(R.string.pref_maptrail_length, getKeyInt(R.integer.historytrack_length_default));
    }

    public static boolean showListsInCacheList() {
        return getBoolean(R.string.pref_showListsInCacheList, true);
    }

    public static void setShowListsInCacheList(final boolean showListsInCacheList) {
        putBoolean(R.string.pref_showListsInCacheList, showListsInCacheList);
    }

    public static int getMapLineValue(final int prefKeyId, final int defaultValueKeyId) {
        return getInt(prefKeyId, getKeyInt(defaultValueKeyId));
    }

    public static boolean hasOSMMultiThreading() {
        return getBoolean(R.string.pref_map_osm_multithreaded, false);
    }

    public static int getMapOsmThreads() {
        return hasOSMMultiThreading() ? Math.max(1, getInt(R.string.pref_map_osm_threads, Math.min(Runtime.getRuntime().availableProcessors() + 1, 4))) : 1;
    }

    public static int getCompactIconMode() {
        final String prefValue = getString(R.string.pref_compactIconMode, "");
        if (prefValue.equals(getKey(R.string.pref_compacticon_on))) {
            return COMPACTICON_ON;
        } else if (prefValue.equals(getKey(R.string.pref_compacticon_auto))) {
            return COMPACTICON_AUTO;
        }
        return COMPACTICON_OFF;
    }

    public static void setCompactIconMode(final int compactIconMode) {
        switch (compactIconMode) {
            case COMPACTICON_OFF:
                putString(R.string.pref_compactIconMode, getKey(R.string.pref_compacticon_off));
                break;
            case COMPACTICON_ON:
                putString(R.string.pref_compactIconMode, getKey(R.string.pref_compacticon_on));
                break;
            case COMPACTICON_AUTO:
                putString(R.string.pref_compactIconMode, getKey(R.string.pref_compacticon_auto));
                break;
            default:
                // do nothing except satisfy static code analysis
                break;
        }
    }

    /**
     * whether to show a direction line on the map
     */
    public static boolean isMapDirection() {
        return Settings.getRoutingMode() != RoutingMode.OFF;
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
        } else {
            setMapZoom(zoomLevel);
        }
    }

    /**
     * @return zoom used for the (live) map
     */
    private static int getMapZoom() {
        return Math.max(0, getInt(R.string.pref_lastmapzoom, 14));
    }

    private static void setMapZoom(final int mapZoomLevel) {
        putInt(R.string.pref_lastmapzoom, mapZoomLevel);
    }

    /**
     * @return zoom used for the map of a single cache
     */
    private static int getCacheZoom() {
        return Math.max(0, getInt(R.string.pref_cache_zoom, 14));
    }

    private static void setCacheZoom(final int zoomLevel) {
        putInt(R.string.pref_cache_zoom, zoomLevel);
    }

    public static GeoPointImpl getMapCenter() {
        return getMapProvider().getMapItemFactory()
                .getGeoPointBase(new Geopoint(getInt(R.string.pref_lastmaplat, 0) / 1e6,
                        getInt(R.string.pref_lastmaplon, 0) / 1e6));
    }

    // temporary workaround for UnifiedMap necessary, as it is completely parallel to getMapProvider() currently
    public static Geopoint getUMMapCenter() {
        return new Geopoint(getInt(R.string.pref_lastmaplat, 0) / 1e6, getInt(R.string.pref_lastmaplon, 0) / 1e6);
    }

    public static boolean getZoomIncludingWaypoints() {
        return getBoolean(R.string.pref_zoomincludingwaypoints, false);
    }

    public static void setMapCenter(final GeoPointImpl mapViewCenter) {
        if (mapViewCenter == null) {
            return;
        }
        putInt(R.string.pref_lastmaplat, mapViewCenter.getLatitudeE6());
        putInt(R.string.pref_lastmaplon, mapViewCenter.getLongitudeE6());
    }

    @NonNull
    public static synchronized MapSource getMapSource() {
        if (mapSource != null) {
            return mapSource;
        }
        final String mapSourceId = getString(R.string.pref_mapsource, null);
        mapSource = MapProviderFactory.getMapSource(mapSourceId);
        if (mapSource == null || !mapSource.isAvailable()) {
            mapSource = MapProviderFactory.getAnyMapSource();
        }
        return mapSource;
    }

    public static synchronized void setMapSource(final MapSource newMapSource) {
        if (newMapSource != null && newMapSource.isAvailable()) {
            putString(R.string.pref_mapsource, newMapSource.getId());
            // cache the value
            mapSource = newMapSource;
        }
    }

    @NonNull
    public static synchronized AbstractTileProvider getTileProvider() {
        if (tileProvider != null) {
            return tileProvider;
        }
        final String tileProviderId = getString(R.string.pref_tileprovider, null);
        tileProvider = TileProviderFactory.getTileProvider(tileProviderId);
        if (tileProvider == null /* || !tileProvider.isAvailable() */) {
            tileProvider = TileProviderFactory.getAnyTileProvider();
        }
        return tileProvider;
    }

    public static synchronized void setTileProvider(final AbstractTileProvider newTileProvider) {
        if (newTileProvider != null /* && newTileProvider.isAvailable() */) {
            putString(R.string.pref_tileprovider, newTileProvider.getId());
            tileProvider = newTileProvider;
        }
    }

    public static Set<String> getHideTileproviders() {
        final Set<String> empty = Collections.emptySet();
        if (sharedPrefs == null) {
            return empty;
        }
        return sharedPrefs.getStringSet(getKey(R.string.pref_tileprovider_hidden), empty);
    }

    public static void setMapLanguage(@Nullable final String language) {
        putString(R.string.pref_mapLanguage, StringUtils.isBlank(language) ? "" : language);
    }

    @Nullable
    public static String getMapLanguage() {
        final String language = getString(R.string.pref_mapLanguage, null);
        return StringUtils.isBlank(language) ? null : language;
    }

    public static int getMapLanguageId() {
        final String language = getMapLanguage();
        return StringUtils.isBlank(language) ? MAP_LANGUAGE_DEFAULT_ID : language.hashCode();
    }

    /** display UnifiedMap icon on home screen? */
    public static boolean showUnifiedMap() {
        return getBoolean(R.string.pref_showUnifiedMap, false);
    }

    /** use UnifiedMap as default map in certain places */
    public static boolean useUnifiedMap() {
        return getBoolean(R.string.pref_useUnifiedMap, false);
    }

    public static void setMapDownloaderSource(final int source) {
        putInt(R.string.pref_mapdownloader_source, source);
    }

    public static int getMapDownloaderSource() {
        return getInt(R.string.pref_mapdownloader_source, Download.DownloadType.DOWNLOADTYPE_MAP_MAPSFORGE.id);
    }

    public static boolean mapAutoDownloadsNeedUpdate() {
        return needsIntervalAction(R.string.pref_mapAutoDownloadsLastCheck, getMapAutoDownloadsInterval() * 24, () -> setMapAutoDownloadsLastCheck(false));
    }

    private static int getMapAutoDownloadsInterval() {
        return getInt(R.string.pref_mapAutoDownloadsInterval, 30);
    }

    public static void setMapAutoDownloadsLastCheck(final boolean delay) {
        putLong(R.string.pref_mapAutoDownloadsLastCheck, calculateNewTimestamp(delay, getMapAutoDownloadsInterval() * 24));
    }

    public static boolean dbNeedsCleanup() {
        return needsIntervalAction(R.string.pref_dbCleanupLastCheck, 24, () -> setDbCleanupLastCheck(false));
    }

    public static void setDbCleanupLastCheck(final boolean delay) {
        putLong(R.string.pref_dbCleanupLastCheck, calculateNewTimestamp(delay, 24));
    }

    public static boolean dbNeedsReindex() {
        return needsIntervalAction(R.string.pref_dbReindexLastCheck, 90 * 24, () -> setDbReindexLastCheck(false));
    }

    public static void setDbReindexLastCheck(final boolean delay) {
        putLong(R.string.pref_dbReindexLastCheck, calculateNewTimestamp(delay, 90 * 24));
    }

    public static boolean pendingDownloadsNeedCheck() {
        return needsIntervalAction(R.string.pref_pendingDownloadsLastCheck, 12, () -> setPendingDownloadsLastCheck(false));
    }

    public static void setPendingDownloadsLastCheck(final boolean delay) {
        putLong(R.string.pref_pendingDownloadsLastCheck, calculateNewTimestamp(delay, 12));
    }

    public static void setPqShowDownloadableOnly(final boolean showDownloadableOnly) {
        putBoolean(R.string.pref_pqShowDownloadableOnly, showDownloadableOnly);
    }

    public static boolean getPqShowDownloadableOnly() {
        return getBoolean(R.string.pref_pqShowDownloadableOnly, false);
    }

    public static void setBookmarklistsShowNewOnly(final boolean showNewOnly) {
        putBoolean(R.string.pref_bookmarklistsShowNewOnly, showNewOnly);
    }

    public static boolean getBookmarklistsShowNewOnly() {
        return getBoolean(R.string.pref_bookmarklistsShowNewOnly, false);
    }

    public static boolean isUseCompass() {
        return useCompass;
    }

    public static void setDeviceOrientationMode(final DirectionData.DeviceOrientation value) {
        deviceOrientationMode = value;
    }

    public static DirectionData.DeviceOrientation getDeviceOrientationMode() {
        return deviceOrientationMode;
    }

    public static void setUseCompass(final boolean value) {
        useCompass = value;
    }


    public static void setAppThemeAutomatically(final @NonNull Context context) {
        setAppTheme(getAppTheme(context));
    }

    public static void setAppTheme(final DarkModeSetting setting) {
        AppCompatDelegate.setDefaultNightMode(setting.getModeId());
    }

    private static DarkModeSetting getAppTheme(final @NonNull Context context) {
        return DarkModeSetting.valueOf(getString(R.string.pref_theme_setting, getBoolean(R.string.old_pref_skin, false) ?
                DarkModeSetting.LIGHT.getPreferenceValue(context) : DarkModeSetting.DARK.getPreferenceValue(context)));
    }

    private static boolean isDarkThemeActive(final @NonNull Context context, final DarkModeSetting setting) {
        if (setting == DarkModeSetting.SYSTEM_DEFAULT) {
            return isDarkThemeActive(context);
        } else {
            return setting == DarkModeSetting.DARK;
        }
    }

    private static boolean isDarkThemeActive(final @NonNull Context context) {
        final int uiMode = context.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean isLightSkin(final @NonNull Context context) {
        return !isDarkThemeActive(context, getAppTheme(context));
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

    static String getWebDeviceName() {
        return getString(R.string.pref_webDeviceName, Build.MODEL);
    }

    /**
     * The threshold for the showing of child waypoints
     */
    public static int getWayPointsThreshold() {
        return getInt(R.string.pref_showwaypointsthreshold, getKeyInt(R.integer.waypoint_threshold_default));
    }

    /**
     * The threshold for brouter routing (max. distance)
     */
    public static int getBrouterThreshold() {
        return getInt(R.string.pref_brouterDistanceThreshold, getKeyInt(R.integer.brouter_threshold_default));
    }

    public static boolean isBrouterShowBothDistances() {
        return getBoolean(R.string.pref_brouterShowBothDistances, false);
    }

    public static boolean isBrouterAutoTileDownloads() {
        return getBoolean(R.string.pref_brouterAutoTileDownloads, false);
    }

    public static void setBrouterAutoTileDownloads(final boolean value) {
        putBoolean(R.string.pref_brouterAutoTileDownloads, value);
    }

    public static boolean brouterAutoTileDownloadsNeedUpdate() {
        return needsIntervalAction(R.string.pref_brouterAutoTileDownloadsLastCheck, getBrouterAutoTileDownloadsInterval() * 24, () -> setBrouterAutoTileDownloadsLastCheck(false));
    }

    private static int getBrouterAutoTileDownloadsInterval() {
        return getInt(R.string.pref_brouterAutoTileDownloadsInterval, 30);
    }

    public static void setBrouterAutoTileDownloadsLastCheck(final boolean delay) {
        putLong(R.string.pref_brouterAutoTileDownloadsLastCheck, calculateNewTimestamp(delay, getBrouterAutoTileDownloadsInterval() * 24));
    }

    public static String getRoutingProfile() {
        return getRoutingProfile(Settings.getRoutingMode());
    }

    public static String getRoutingProfile(final RoutingMode mode) {
        if (mode.equals(RoutingMode.CAR)) {
            return getString(R.string.pref_brouterProfileCar, BRouterConstants.BROUTER_PROFILE_CAR_DEFAULT);
        } else if (mode.equals(RoutingMode.BIKE)) {
            return getString(R.string.pref_brouterProfileBike, BRouterConstants.BROUTER_PROFILE_BIKE_DEFAULT);
        } else if (mode.equals(RoutingMode.WALK)) {
            return getString(R.string.pref_brouterProfileWalk, BRouterConstants.BROUTER_PROFILE_WALK_DEFAULT);
        } else {
            return null;
        }
    }

    // calculate new "last checked" timestamp - either "now" or "now - interval + delay [3 days at most]
    // used for update checks for maps & route tiles downloaders (and other places)
    private static long calculateNewTimestamp(final boolean delay, final int intervalInHours) {
        // if delay requested: delay by regular interval, but by three days at most
        return (System.currentTimeMillis() / 1000) - (delay && (intervalInHours > 72) ? (long) (intervalInHours - 72) * HOURS_TO_SECONDS : 0);
    }

    // checks given timestamp against interval; initializes timestampt, if needed
    private static boolean needsIntervalAction(final @StringRes int prefTimestamp, final int intervalInHours, final Runnable initAction) {
        // check disabled?
        if (intervalInHours < 1) {
            return false;
        }
        // initialization on first run
        final long lastCheck = getLong(prefTimestamp, 0);
        if (lastCheck == 0) {
            initAction.run();
            return false;
        }
        // check if interval is completed
        final long now = System.currentTimeMillis() / 1000;
        return (lastCheck + ((long) intervalInHours * HOURS_TO_SECONDS)) <= now;
    }

    public static boolean isBigSmileysEnabled() {
        return getBoolean(R.string.pref_bigSmileysOnMap, false);
    }

    /**
     * Proximity notification settings
     */

    public static boolean isGeneralProximityNotificationActive() {
        return getBoolean(R.string.pref_proximityNotificationGeneral, false);
    }

    public static boolean isSpecificProximityNotificationActive() {
        return getBoolean(R.string.pref_proximityNotificationSpecific, false);
    }

    public static int getProximityNotificationThreshold(final boolean farDistance) {
        return getInt(farDistance ? R.string.pref_proximityDistanceFar : R.string.pref_proximityDistanceNear, farDistance ? getKeyInt(R.integer.proximitynotification_far_default) : getKeyInt(R.integer.proximitynotification_near_default));
    }

    public static boolean isProximityNotificationTypeTone() {
        final String pref = getString(R.string.pref_proximityNotificationType, ProximityNotification.NOTIFICATION_TYPE_TONE_ONLY);
        return pref.equals(ProximityNotification.NOTIFICATION_TYPE_TONE_ONLY) || pref.equals(ProximityNotification.NOTIFICATION_TYPE_TONE_AND_TEXT);
    }

    public static boolean isProximityNotificationTypeText() {
        final String pref = getString(R.string.pref_proximityNotificationType, ProximityNotification.NOTIFICATION_TYPE_TONE_ONLY);
        return pref.equals(ProximityNotification.NOTIFICATION_TYPE_TEXT_ONLY) || pref.equals(ProximityNotification.NOTIFICATION_TYPE_TONE_AND_TEXT);
    }

    public static boolean isLongTapOnMapActivated() {
        return getBoolean(R.string.pref_longTapOnMap, true);
    }

    public static boolean getCreateUDCuseGivenList() {
        return getBoolean(R.string.pref_createUDCuseGivenList, false);
    }

    public static void setCreateUDCuseGivenList(final boolean createUDCuseGivenList) {
        putBoolean(R.string.pref_createUDCuseGivenList, createUDCuseGivenList);
    }

    public static boolean isUseTwitter() {
        return getBoolean(R.string.pref_twitter, false);
    }

    private static void setUseTwitter(final boolean useTwitter) {
        putBoolean(R.string.pref_twitter, useTwitter);
    }

    public static boolean isTwitterLoginValid() {
        return StringUtils.isNotBlank(getTokenPublic())
                && StringUtils.isNotBlank(getTokenSecret());
    }

    public static String getTokenPublic() {
        return getString(R.string.pref_twitter_token_public, null);
    }

    public static String getTokenSecret() {
        return getString(R.string.pref_twitter_token_secret, null);

    }

    static boolean hasTwitterAuthorization() {
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

    @NonNull
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

    public static boolean isGlobalWpExtractionDisabled() {
        return getBoolean(R.string.pref_global_wp_extraction_disable, false);
    }

    public static boolean isPersonalCacheNoteMergeDisabled() {
        return getBoolean(R.string.pref_personal_cache_note_merge_disable, false);
    }

    public static int getLastDetailsPage() {
        return getInt(R.string.pref_lastdetailspage, 1);
    }

    public static void setLastDetailsPage(final int index) {
        putInt(R.string.pref_lastdetailspage, index);
    }

    public static int getDefaultNavigationTool() {
        return getIntFromString(R.string.pref_defaultNavigationTool, NavigationAppsEnum.COMPASS.id);
    }

    public static int getDefaultNavigationTool2() {
        return getIntFromString(R.string.pref_defaultNavigationTool2, NavigationAppsEnum.INTERNAL_MAP.id);
    }

    public static boolean isDebug() {
        return Log.isDebug();
    }

    public static void setDebug(final boolean debug) {
        Log.setDebug(debug);
        putBoolean(R.string.pref_debug, debug);
    }

    public static boolean isDbOnSDCard() {
        return getBoolean(R.string.pref_dbonsdcard, false);
    }

    public static void setDbOnSDCard(final boolean dbOnSDCard) {
        putBoolean(R.string.pref_dbonsdcard, dbOnSDCard);
    }

    public static String getExternalPrivateCgeoDirectory() {
        return getString(R.string.pref_dataDir, null);
    }

    public static void setExternalPrivateCgeoDirectory(final String extDir) {
        putString(R.string.pref_dataDir, extDir);
    }

    public static boolean getIncludeFoundStatus() {
        return getBoolean(R.string.pref_includefoundstatus, true);
    }

    public static void setIncludeFoundStatus(final boolean includeFoundStatus) {
        putBoolean(R.string.pref_includefoundstatus, includeFoundStatus);
    }

    public static boolean getClearTrailAfterExportStatus() {
        return getBoolean(R.string.pref_cleartrailafterexportstatus, false);
    }

    public static void setClearTrailAfterExportStatus(final boolean clearTrailAfterExportStatus) {
        putBoolean(R.string.pref_cleartrailafterexportstatus, clearTrailAfterExportStatus);
    }

    /**
     * Get Trackable inventory sort method based on the last Trackable inventory sort method.
     *
     * @return The Trackable Sort Method previously used.
     */
    public static TrackableComparator getTrackableComparator() {
        return TrackableComparator.findByName(getString(R.string.pref_trackable_inventory_sort, ""));
    }

    /**
     * Set Trackable inventory sort method.
     *
     * @param trackableSortMethod The Trackable Sort Method to remember
     */
    public static void setTrackableComparator(final TrackableComparator trackableSortMethod) {
        putString(R.string.pref_trackable_inventory_sort, trackableSortMethod.name());
    }

    /**
     * Obtain Trackable action from the last Trackable log.
     *
     * @return The last Trackable Action or RETRIEVED_IT
     */
    public static int getTrackableAction() {
        return getInt(R.string.pref_trackableaction, LogTypeTrackable.RETRIEVED_IT.id);
    }

    /**
     * Save Trackable action from the last Trackable log.
     *
     * @param trackableAction The Trackable Action to remember
     */
    public static void setTrackableAction(final int trackableAction) {
        putInt(R.string.pref_trackableaction, trackableAction);
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper}!
     */
    public static String getSelectedMapRenderTheme() {
        return getString(R.string.pref_renderthemefile, "");
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper}!
     */
    public static void setSelectedMapRenderTheme(final String customRenderThemeFile) {
        putString(R.string.pref_renderthemefile, customRenderThemeFile);
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeSettingsFragment}!
     */
    public static void setSelectedMapRenderThemeStyle(final String prefKey, final String style) {
        putStringDirect(prefKey, style);
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper}!
     */
    public static boolean getSyncMapRenderThemeFolder() {
        return getBoolean(R.string.pref_renderthemefolder_synctolocal, false);
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper}!
     */
    public static void setSyncMapRenderThemeFolder(final boolean syncMapRenderThemeFolder) {
        putBoolean(R.string.pref_renderthemefolder_synctolocal, syncMapRenderThemeFolder);
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeSettingsFragment}!
     */
    public static String getMapRenderScalePreferenceKey(final String themeStyleId, final RenderThemeScaleType scaleType) {
        return themeStyleId + "-" + scaleType;
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper}!
     */
    public static int getMapRenderScale(final String themeStyleId, final RenderThemeScaleType scaleType) {
        final int value = getIntDirect(getMapRenderScalePreferenceKey(themeStyleId, scaleType), 100);
        return Math.min(500, Math.max(value, 10));
    }

    /**
     * @return true if plain text log wanted
     */
    public static boolean getPlainLogs() {
        return getBoolean(R.string.pref_plainLogs, false);
    }

    /**
     * Force set the plain text log preference
     *
     * @param plainLogs wanted or not
     */
    public static void setPlainLogs(final boolean plainLogs) {
        putBoolean(R.string.pref_plainLogs, plainLogs);
    }

    public static int getNearbySearchLimit() {
        return getInt(R.string.pref_nearbySearchLimit, 0);
    }

    @NonNull
    public static String getCacheTwitterMessage() {
        return StringUtils.defaultString(getString(R.string.pref_twitter_cache_message, "I found [NAME] ([URL])."));
    }

    @NonNull
    public static String getTrackableTwitterMessage() {
        return StringUtils.defaultString(getString(R.string.pref_twitter_trackable_message, "I touched [NAME] ([URL])."));
    }

    public static int getLogImageScale() {
        final int scale = getInt(R.string.pref_logImageScale, -1);

        //accomodate for legacy values which might be stored in preferences from former c:geo versions. See Issue #9655
        if (scale < 0) {
            return -1;
        }
        return Math.max(scale, 512);
    }

    public static void setLogImageScale(final int scale) {
        putInt(R.string.pref_logImageScale, scale);
    }

    public static void setShowCircles(final boolean showCircles) {
        putBoolean(R.string.pref_showCircles, showCircles);
    }

    public static boolean isShowCircles() {
        return getBoolean(R.string.pref_showCircles, false);
    }

    public static void setSupersizeDistance(final int supersizeDistance) {
        putInt(R.string.pref_supersizeDistance, supersizeDistance);
    }

    public static int getSupersizeDistance() {
        return getInt(R.string.pref_supersizeDistance, 0);
    }

    public static void setExcludeWpOriginal(final boolean exclude) {
        putBoolean(R.string.pref_excludeWpOriginal, exclude);
    }

    public static void setExcludeWpParking(final boolean exclude) {
        putBoolean(R.string.pref_excludeWpParking, exclude);
    }

    public static void setExcludeWpVisited(final boolean exclude) {
        putBoolean(R.string.pref_excludeWpVisited, exclude);
    }

    static void setLogin(final String username, final String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            // erase username and password
            remove(R.string.pref_username);
            remove(R.string.pref_password);
            return;
        }
        // save username and password
        putString(R.string.pref_username, StringUtils.trim(username));
        putString(R.string.pref_password, password);
    }

    // methods for setting and retrieving login status (connector-specific)

    private static void setLastLoginError(@StringRes final int connectorPrefId, final String status, @StringRes final int timePrefId) {
        if (connectorPrefId != 0 && timePrefId != 0 && StringUtils.isNotBlank(status)) {
            putString(connectorPrefId, status);
            putLong(timePrefId, System.currentTimeMillis());
        }
    }

    @Nullable
    private static Pair<String, Long> getLastLoginError(@StringRes final int connectorPrefId, @StringRes final int timePrefId) {
        if (connectorPrefId != 0 && timePrefId != 0) {
            final Pair<String, Long> data = new Pair<>(getString(connectorPrefId, ""), getLong(timePrefId, 0));
            return StringUtils.isBlank(data.first) || data.second == 0 ? null : data;
        }
        return null;
    }

    private static void setLastLoginSuccess(@StringRes final int timePrefId) {
        if (timePrefId != 0) {
            putLong(timePrefId, System.currentTimeMillis());
        }
    }

    private static long getLastLoginSuccess(@StringRes final int timePrefId) {
        return getLong(timePrefId, 0);
    }

    public static void setLastLoginErrorGC(final String status) {
        setLastLoginError(R.string.pref_gcLastLoginErrorStatus, status, R.string.pref_gcLastLoginError);
    }

    @Nullable
    public static Pair<String, Long> getLastLoginErrorGC() {
        return getLastLoginError(R.string.pref_gcLastLoginErrorStatus, R.string.pref_gcLastLoginError);
    }

    public static void setLastLoginSuccessGC() {
        setLastLoginSuccess(R.string.pref_gcLastLoginSuccess);
    }

    public static long getLastLoginSuccessGC() {
        return getLastLoginSuccess(R.string.pref_gcLastLoginSuccess);
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

    /**
     * Remember the stata of the "hide visited waypoints"-checkbox in the waypoints overview dialog
     */
    public static void setHideVisitedWaypoints(final boolean hideVisitedWaypoints) {
        putBoolean(R.string.pref_hideVisitedWaypoints, hideVisitedWaypoints);
    }

    public static boolean getHideVisitedWaypoints() {
        return getBoolean(R.string.pref_hideVisitedWaypoints, false);
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
        final List<String> history = getStringList(R.string.pref_caches_history, StringUtils.EMPTY);
        return history.subList(0, Math.min(HISTORY_SIZE, history.size()));
    }

    public static void addCacheToHistory(@NonNull final String geocode) {
        final List<String> history = new ArrayList<>(getLastOpenedCaches());
        // bring entry to front, if it already existed
        history.remove(geocode);
        history.add(0, geocode);
        putStringList(R.string.pref_caches_history, history);
    }

    private static boolean outdatedPhoneModelOrSdk() {
        return !StringUtils.equals(PHONE_MODEL_AND_SDK, getString(R.string.pref_phone_model_and_sdk, null));
    }

    private static void storePhoneModelAndSdk() {
        putString(R.string.pref_phone_model_and_sdk, PHONE_MODEL_AND_SDK);
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

    public static boolean getFollowMyLocation() {
        return getBoolean(R.string.pref_followMyLocation, true);
    }

    public static void setFollowMyLocation(final boolean activated) {
        putBoolean(R.string.pref_followMyLocation, activated);
    }

    public static void setForceOrientationSensor(final boolean forceOrientationSensor) {
        putBoolean(R.string.pref_force_orientation_sensor, forceOrientationSensor);
    }

    public static boolean useOrientationSensor(final Context context) {
        return OrientationProvider.hasOrientationSensor(context) &&
                (getBoolean(R.string.pref_force_orientation_sensor, false) ||
                        !(RotationProvider.hasRotationSensor(context) || MagnetometerAndAccelerometerProvider.hasMagnetometerAndAccelerometerSensors(context))
                );
    }

    /**
     * Get avatar URL by connector. Should SOLELY be used by class {@link AvatarUtils}!
     */
    @Nullable
    public static String getAvatarUrl(@NonNull final IAvatar connector) {
        return getString(connector.getAvatarPreferenceKey(), null);
    }

    /**
     * Set avatar URL by connector. Should SOLELY be used by class {@link AvatarUtils}!
     *
     * @param connector the connector to retrieve the avatar information from
     * @param avatarUrl the avatar url information to store
     */
    public static void setAvatarUrl(@NonNull final IAvatar connector, final String avatarUrl) {
        putString(connector.getAvatarPreferenceKey(), avatarUrl);
    }

    /**
     * Return the locale that should be used to display information to the user.
     * Includes migration from the old "useEnglish" preference
     * Precedence is userLocale > useEnglish > defaultLocale
     *
     * @return either user-defined locale or system default locale, depending on the settings
     */
    public static Locale getApplicationLocale() {
        final String selectedLanguage = getUserLanguage();
        return StringUtils.isNotBlank(selectedLanguage) ? new Locale(selectedLanguage, "") : Locale.getDefault();
    }

    public static String getUserLanguage() {
        return getString(R.string.pref_selected_language, Settings.getBoolean(R.string.old_pref_useenglish, false) ? "en" : "");
    }

    public static void putUserLanguage(final String language) {
        putString(R.string.pref_selected_language, language);
    }

    public static Set<String> getQuicklaunchitems() {
        final Set<String> empty = Collections.emptySet();
        if (sharedPrefs == null) {
            return empty;
        }
        return sharedPrefs.getStringSet(getKey(R.string.pref_quicklaunchitems), empty);
    }

    public static void setRoutingMode(@NonNull final RoutingMode mode) {
        putString(R.string.pref_map_routing, mode.parameterValue);
        Routing.invalidateRouting();
    }

    public static RoutingMode getRoutingMode() {
        return RoutingMode.fromString(getString(R.string.pref_map_routing, "foot"));
    }

    public static void setUseInternalRouting(final boolean useInternalRouting) {
        putBoolean(R.string.pref_useInternalRouting, useInternalRouting);
    }

    public static boolean useInternalRouting() {
        return getBoolean(R.string.pref_useInternalRouting, false);
    }

    public static boolean getBackupLoginData() {
        return getBoolean(R.string.pref_backup_logins, false);
    }

    public static int allowedBackupsNumber() {
        return getInt(R.string.pref_backup_backup_history_length, getKeyInt(R.integer.backup_history_length_default));
    }

    public static boolean automaticBackupDue() {
        // update check disabled?
        final int interval = getAutomaticBackupInterval();
        if (interval < 1) {
            return false;
        }
        // initialization on first run
        final long lastCheck = getLong(R.string.pref_automaticBackupLastCheck, 0);
        if (lastCheck == 0) {
            setAutomaticBackupLastCheck(false);
            return false;
        }
        // check if interval is completed
        final long now = System.currentTimeMillis() / 1000;
        return (lastCheck + (interval * DAYS_TO_SECONDS)) <= now;
    }

    private static int getAutomaticBackupInterval() {
        return getInt(R.string.pref_backup_interval, CgeoApplication.getInstance().getResources().getInteger(R.integer.backup_interval_default));
    }

    public static void setAutomaticBackupLastCheck(final boolean delay) {
        putLong(R.string.pref_automaticBackupLastCheck, calculateNewTimestamp(delay, getAutomaticBackupInterval() * 24));
    }

    /**
     * sets the user-defined folder-config for a persistable folder. Can be set to null
     * should be called by PersistableFolder class only
     */
    public static void setPersistableFolder(@NonNull final PersistableFolder folder, @Nullable final String folderString, final boolean setByUser) {
        putString(folder.getPrefKeyId(), folderString);
        if (setByUser) {
            handleLegacyValuesOnSet(folder.getPrefKeyId());
        }
    }

    /**
     * gets the user-defined uri for a persistable folder. Can be null
     * should be called by PersistableFolder class only
     */
    @Nullable
    public static String getPersistableFolder(@NonNull final PersistableFolder folder) {
        return getString(folder.getPrefKeyId(), getLegacyValue(folder.getPrefKeyId()));
    }

    /**
     * gets current setting for a persistable folder or null if unset
     * should be called by BackupUtils.restoreInternal only
     */
    @Nullable
    public static String getPersistableFolderRaw(@NonNull final PersistableFolder folder) {
        return getString(folder.getPrefKeyId(), null);
    }

    /**
     * sets Uri for persistable uris. Can be set to null
     * should be called by PersistableUri class only
     */
    public static void setPersistableUri(@NonNull final PersistableUri persistedUri, @Nullable final String uri) {
        putString(persistedUri.getPrefKeyId(), uri);
        handleLegacyValuesOnSet(persistedUri.getPrefKeyId());
    }

    /**
     * gets the persisted uri for a persistable uris. Can be null
     * should be called by PersistableUri class only
     */
    @Nullable
    public static String getPersistableUri(@NonNull final PersistableUri persistedUri) {
        return getString(persistedUri.getPrefKeyId(), getLegacyValue(persistedUri.getPrefKeyId()));
    }

    /**
     * gets current setting for a persistable uri or null if unset
     * should be called by BackupUtils.restoreInternal only
     */
    @Nullable
    public static String getPersistableUriRaw(@NonNull final PersistableUri uri) {
        return getString(uri.getPrefKeyId(), null);
    }

    private static String getLegacyValue(final int keyId) {
        for (String legacyKey : getLegacyPreferenceKeysFor(keyId)) {
            final String value = getStringDirect(legacyKey, null);
            if (value != null && !value.startsWith(LEGACY_UNUSED_MARKER)) {
                return value;
            }
        }
        return null;
    }

    private static void handleLegacyValuesOnSet(final int keyId) {
        //if a new key is actively set for the first time, its existing legacy values are no longer needed
        for (String legacyKey : getLegacyPreferenceKeysFor(keyId)) {
            final String value = getStringDirect(legacyKey, null);
            if (value != null && !value.startsWith(LEGACY_UNUSED_MARKER)) {
                putStringDirect(legacyKey, LEGACY_UNUSED_MARKER + value);
            }
        }
    }

    private static String[] getLegacyPreferenceKeysFor(final int keyId) {
        if (keyId == R.string.pref_persistablefolder_offlinemaps) {
            return new String[]{"mapDirectory"};
        } else if (keyId == R.string.pref_persistablefolder_gpx) {
            return new String[]{"gpxExportDir", "gpxImportDir"};
        } else if (keyId == R.string.pref_persistableuri_track) {
            return new String[]{"pref_trackfile"};
        } else if (keyId == R.string.pref_persistablefolder_offlinemapthemes) {
            return new String[]{"renderthemepath"};
        } else {
            return new String[0];
        }
    }

    /**
     * checks whether legacy folder needs to be migrated
     * (legacy value is set and not yet migrated)
     * (used by the installation / migration wizard)
     **/
    public static boolean legacyFolderNeedsToBeMigrated(@StringRes final int newPrefKey) {
        for (String legacyKey : getLegacyPreferenceKeysFor(newPrefKey)) {
            final String value = getStringDirect(legacyKey, null);
            if (value != null && !value.startsWith(LEGACY_UNUSED_MARKER)) {
                return true;
            }
        }
        return false;
    }

    public static boolean getUseCustomTabs() {
        return getBoolean(R.string.pref_customtabs_as_browser, false);
    }

    public static int getUniqueNotificationId() {
        final int id = getInt(R.string.pref_next_unique_notification_id, Notifications.UNIQUE_ID_RANGE_START);
        putInt(R.string.pref_next_unique_notification_id, id + 1);
        return id;
    }

    public static int getLocalStorageVersion() {
        return getInt(R.string.pref_localstorage_version, 0);
    }

    public static void setLocalStorageVersion(final int newVersion) {
        putInt(R.string.pref_localstorage_version, newVersion);
    }

    /**
     * Should SOLELY be called by class {@link cgeo.geocaching.filters.core.GeocacheFilter}!
     */
    public static String getCacheFilterConfig(final String type) {
        return getStringDirect(getKey(R.string.pref_cache_filter_config) + "." + type, null);
    }

    /**
     * Should SOLELY be called by class {@link cgeo.geocaching.filters.core.GeocacheFilter}!
     */
    public static void setCacheFilterConfig(final String type, final String config) {
        putStringDirect(getKey(R.string.pref_cache_filter_config) + "." + type, config);
    }

    /**
     * Should SOLELY be called by class {@link cgeo.geocaching.sorting.GeocacheSortContext}!
     */
    public static String getSortConfig(final String sortContextKey) {
        return getStringDirect(getKey(R.string.pref_cache_sort_config) + "." + sortContextKey, null);
    }

    /**
     * Should SOLELY be called by class {@link cgeo.geocaching.sorting.GeocacheSortContext}!
     */
    public static void setSortConfig(final String sortContextKey, final String sortConfig) {
        if (sortConfig == null) {
            removeDirect(getKey(R.string.pref_cache_sort_config) + "." + sortContextKey);
        } else {
            putStringDirect(getKey(R.string.pref_cache_sort_config) + "." + sortContextKey, sortConfig);
        }
    }

    public static int getListInitialLoadLimit() {
        return getInt(R.string.pref_list_initial_load_limit, getKeyInt(R.integer.list_load_limit_default));
    }

    /**
     * return a list of preference keys containing sensitive data
     */
    public static HashSet<String> getSensitivePreferenceKeys(final Context context) {
        final HashSet<String> sensitiveKeys = new HashSet<>();
        Collections.addAll(sensitiveKeys,
                context.getString(R.string.pref_username), context.getString(R.string.pref_password),
                context.getString(R.string.pref_ecusername), context.getString(R.string.pref_ecpassword),
                context.getString(R.string.pref_user_vote), context.getString(R.string.pref_pass_vote),
                context.getString(R.string.pref_twitter), context.getString(R.string.pref_temp_twitter_token_secret), context.getString(R.string.pref_temp_twitter_token_public), context.getString(R.string.pref_twitter_token_secret), context.getString(R.string.pref_twitter_token_public),
                context.getString(R.string.pref_ocde_tokensecret), context.getString(R.string.pref_ocde_tokenpublic), context.getString(R.string.pref_temp_ocde_token_secret), context.getString(R.string.pref_temp_ocde_token_public),
                context.getString(R.string.pref_ocpl_tokensecret), context.getString(R.string.pref_ocpl_tokenpublic), context.getString(R.string.pref_temp_ocpl_token_secret), context.getString(R.string.pref_temp_ocpl_token_public),
                context.getString(R.string.pref_ocnl_tokensecret), context.getString(R.string.pref_ocnl_tokenpublic), context.getString(R.string.pref_temp_ocnl_token_secret), context.getString(R.string.pref_temp_ocnl_token_public),
                context.getString(R.string.pref_ocus_tokensecret), context.getString(R.string.pref_ocus_tokenpublic), context.getString(R.string.pref_temp_ocus_token_secret), context.getString(R.string.pref_temp_ocus_token_public),
                context.getString(R.string.pref_ocro_tokensecret), context.getString(R.string.pref_ocro_tokenpublic), context.getString(R.string.pref_temp_ocro_token_secret), context.getString(R.string.pref_temp_ocro_token_public),
                context.getString(R.string.pref_ocuk2_tokensecret), context.getString(R.string.pref_ocuk2_tokenpublic), context.getString(R.string.pref_temp_ocuk2_token_secret), context.getString(R.string.pref_temp_ocuk2_token_public),
                context.getString(R.string.pref_su_tokensecret), context.getString(R.string.pref_su_tokenpublic), context.getString(R.string.pref_temp_su_token_secret), context.getString(R.string.pref_temp_su_token_public),
                context.getString(R.string.pref_fakekey_geokrety_authorization)
        );
        return sensitiveKeys;
    }

    public static void putLogTemplate(final PrefLogTemplate template) {
        final List<PrefLogTemplate> templates = getLogTemplates();
        final int templateIndex = templates.indexOf(template);
        if (templateIndex == -1 && template.getText() == null) {
            return;
        } else if (templateIndex > -1 && template.getText() == null) {
            templates.remove(templateIndex);
        } else if (templateIndex == -1) {
            templates.add(template);
        } else {
            templates.set(templateIndex, template);
        }
        try {
            putString(R.string.pref_logTemplates, MAPPER.writeValueAsString(templates));
        } catch (JsonProcessingException e) {
            Log.e("Failure writing log templates: " + e.getMessage());
        }
    }

    @Nullable
    public static PrefLogTemplate getLogTemplate(final String key) {
        for (PrefLogTemplate template : getLogTemplates()) {
            if (template.getKey().equals(key)) {
                return template;
            }
        }
        return null;
    }

    @Nullable
    public static List<PrefLogTemplate> getLogTemplates() {
        try {
            return MAPPER.readValue(getString(R.string.pref_logTemplates, "[]"), new TypeReference<List<PrefLogTemplate>>() {
            });
        } catch (JsonProcessingException e) {
            Log.e("Failure parsing log templates: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static boolean isDTMarkerEnabled() {
        return getBoolean(R.string.pref_dtMarkerOnCacheIcon, false);
    }

    public static int getAttributeFilterSources() {
        int setting = getInt(R.string.pref_attributeFilterSources, 0);
        if (setting == 0) {
            // guess a reasonable default value based on enabled connectors
            boolean gc = false;
            boolean okapi = false;
            final List<IConnector> activeConnectors = ConnectorFactory.getActiveConnectors();
            for (final IConnector conn : activeConnectors) {
                if ("GC".equals(conn.getNameAbbreviated()) || "AL".equals(conn.getNameAbbreviated())) {
                    gc = true;
                } else {
                    okapi = true;
                }
            }
            setting = 3 - (!gc ? 1 : 0) + (!okapi ? 2 : 0);
        }
        return setting;
    }

    /**
     * For which connectors should attributes be shown: 1 GC, 2 Okapi, 3 Both
     */
    public static void setAttributeFilterSources(final int value) {
        putInt(R.string.pref_attributeFilterSources, value);
    }

    public static void setSelectedGoogleMapTheme(final String mapTheme) {
        putString(R.string.pref_google_map_theme, mapTheme);
    }

    public static String getSelectedGoogleMapTheme() {
        return getString(R.string.pref_google_map_theme, "DEFAULT");
    }

    public static boolean getHintAsRot13() {
        return getBoolean(R.string.pref_rot13_hint, true);
    }

    public static double getMapShadingScale() {
        return ((double) getInt(R.string.pref_mapShadingScale, 100)) / 100;
    }

    public static double getMapShadingLinearity() {
        return ((double) getInt(R.string.pref_mapShadingLinearity, 5)) / 100;
    }

}
