// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.settings

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractNavigationBarActivity
import cgeo.geocaching.apps.navi.NavigationAppFactory.NavigationAppsEnum
import cgeo.geocaching.brouter.BRouterConstants
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.capability.IAvatar
import cgeo.geocaching.connector.capability.ICredentials
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCConstants
import cgeo.geocaching.connector.gc.GCMemberState
import cgeo.geocaching.enumerations.CacheListInfoItem
import cgeo.geocaching.enumerations.QuickLaunchItem
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.ProximityNotification
import cgeo.geocaching.log.LogTypeTrackable
import cgeo.geocaching.log.TrackableComparator
import cgeo.geocaching.maps.MapMode
import cgeo.geocaching.maps.MapProviderFactory
import cgeo.geocaching.maps.google.v2.GoogleMapProvider
import cgeo.geocaching.maps.interfaces.GeoPointImpl
import cgeo.geocaching.maps.interfaces.MapProvider
import cgeo.geocaching.maps.interfaces.MapSource
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.maps.routing.RoutingMode
import cgeo.geocaching.models.Download
import cgeo.geocaching.models.InfoItem
import cgeo.geocaching.network.HtmlImage
import cgeo.geocaching.playservices.GooglePlayServices
import cgeo.geocaching.sensors.DirectionData
import cgeo.geocaching.sensors.MagnetometerAndAccelerometerProvider
import cgeo.geocaching.sensors.OrientationProvider
import cgeo.geocaching.sensors.RotationProvider
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.storage.PersistableUri
import cgeo.geocaching.ui.AvatarUtils
import cgeo.geocaching.ui.notifications.Notifications
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.OfflineTranslateUtils
import cgeo.geocaching.utils.TranslationUtils
import cgeo.geocaching.maps.MapProviderFactory.MAP_LANGUAGE_DEFAULT_ID

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.res.Configuration
import android.os.Build

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.util.Pair
import androidx.preference.PreferenceManager

import java.io.File
import java.io.FileFilter
import java.util.ArrayList
import java.util.Arrays
import java.util.Calendar
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Set

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.EnumUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair

/**
 * General c:geo preferences/settings set by the user
 */
class Settings {

    private static val LEGACY_UNUSED_MARKER: String = "unused::"

    /**
     * Separator Char for preferences with multiple elements.
     */
    private static val SEPARATOR_CHAR: Char = ','

    public static val MAPROTATION_OFF: Int = 0
    public static val MAPROTATION_MANUAL: Int = 1
    public static val MAPROTATION_AUTO_LOWPOWER: Int = 2; // more power-efficient than #3
    public static val MAPROTATION_AUTO_PRECISE: Int = 3;  // more precise than #2

    public static val COMPACTICON_OFF: Int = 0
    public static val COMPACTICON_ON: Int = 1
    public static val COMPACTICON_AUTO: Int = 2

    public static val CUSTOMBNITEM_PLACEHOLDER: Int = -2
    public static val CUSTOMBNITEM_NONE: Int = -1
    public static val CUSTOMBNITEM_NEARBY: Int = 0

    public static val UNIFIEDMAP_VARIANT_MAPSFORGE: Int = 1
    public static val UNIFIEDMAP_VARIANT_VTM: Int = 2
    public static val UNIFIEDMAP_VARIANT_BOTH: Int = 3

    public static val HOURS_TO_SECONDS: Int = 60 * 60
    public static val DAYS_TO_SECONDS: Int = 24 * HOURS_TO_SECONDS

    private static val HISTORY_SIZE: Int = 50

    private static val MAP_SOURCE_DEFAULT: Int = GoogleMapProvider.GOOGLE_MAP_ID.hashCode()

    private static val PHONE_MODEL_AND_SDK: String = Build.MODEL + "/" + Build.VERSION.SDK_INT

    private static val MAPPER: ObjectMapper = ObjectMapper()

    private static Boolean useCompass = true
    private static DirectionData.DeviceOrientation deviceOrientationMode = DirectionData.DeviceOrientation.AUTO

    /**
     * Cache the mapsource locally. If that is an offline map source, each request would potentially access the
     * underlying map file, leading to delays.
     */
    private static MapSource mapSource
    private static AbstractTileProvider tileProvider

    public static val RENDERTHEMESCALE_DEFAULTKEY: String = "renderthemescale_default"

    enum class class CoordInputFormatEnum {
        Plain,
        Deg,
        Min,
        Sec

        public static val DEFAULT_INT_VALUE: Int = Min.ordinal()

        public static CoordInputFormatEnum fromInt(final Int id) {
            final CoordInputFormatEnum[] values = CoordInputFormatEnum.values()
            if (id < 0 || id >= values.length) {
                return Min
            }
            return values[id]
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
    enum class class DarkModeSetting {

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
        SYSTEM_DEFAULT(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.pref_value_theme_system_default)

        private final @AppCompatDelegate.NightMode
        Int modeId
        private final @StringRes
        Int preferenceValue

        DarkModeSetting(final @AppCompatDelegate.NightMode Int modeId, final @StringRes Int preferenceValue) {
            this.modeId = modeId
            this.preferenceValue = preferenceValue
        }

        public Int getModeId() {
            return modeId
        }

        public String getPreferenceValue(final Context context) {
            return context.getString(preferenceValue)
        }
    }

    public static class PrefLogTemplate {
        private final String key
        private final String title
        private final String text

        @JsonCreator
        public PrefLogTemplate(@JsonProperty("key") final String key, @JsonProperty("title") final String title, @JsonProperty("text") final String text) {
            this.key = key
            this.title = title
            this.text = text
        }

        public String getKey() {
            return key
        }

        public String getTitle() {
            return title
        }

        public String getText() {
            return text
        }

        override         public Boolean equals(final Object o) {
            if (getClass() != o.getClass()) {
                return false
            }
            val p: PrefLogTemplate = (PrefLogTemplate) o
            return p.getKey() == (this.getKey())
        }

        override         public Int hashCode() {
            return key.hashCode()
        }

        override         public String toString() {
            return title
        }
    }

    enum class class RenderThemeScaleType { MAP, TEXT, SYMBOL }

    //NO_APPLICATION_MODE will be true if Settings is used in context of local unit tests
    private static val NO_APPLICATION_MODE: Boolean = CgeoApplication.getInstance() == null

    private static val sharedPrefs: SharedPreferences = NO_APPLICATION_MODE ? null : PreferenceManager
            .getDefaultSharedPreferences(CgeoApplication.getInstance().getBaseContext())

    static {
        migrateSettings()
        Log.setDebug(getBoolean(R.string.pref_debug, false))

    }

    protected Settings() {
        throw InstantiationError()
    }

    public static Int getActualVersion() {
        return getInt(R.string.pref_settingsversion, 0)
    }

    public static Unit setActualVersion(final Int newVersion) {
        putInt(R.string.pref_settingsversion, newVersion)
    }

    public static Int getExpectedVersion() {
        return 11
    }

    private static Unit migrateSettings() {
        //NO migration in NO_APP_MODE
        if (NO_APPLICATION_MODE) {
            return
        }

        val latestPreferencesVersion: Int = getExpectedVersion()
        val currentVersion: Int = getActualVersion()

        // No need to migrate if we are up to date.
        if (currentVersion == latestPreferencesVersion) {
            return
        }

        // No need to migrate if we don't have older settings, defaults will be used instead.
        val preferencesNameV0: String = "cgeo.pref"
        val prefsV0: SharedPreferences = CgeoApplication.getInstance().getSharedPreferences(preferencesNameV0, Context.MODE_PRIVATE)
        if (currentVersion == 0 && prefsV0.getAll().isEmpty()) {
            val e: Editor = sharedPrefs.edit()
            e.putInt(getKey(R.string.pref_settingsversion), latestPreferencesVersion)
            e.apply()
            return
        }

        if (currentVersion < 1) {
            // migrate from non standard file location and integer based Boolean types
            val e: Editor = sharedPrefs.edit()

            e.putBoolean(getKey(R.string.pref_help_shown), prefsV0.getInt(getKey(R.string.pref_help_shown), 0) != 0)
            e.putString(getKey(R.string.pref_webDeviceCode), prefsV0.getString(getKey(R.string.pref_webDeviceCode), null))
            e.putString(getKey(R.string.pref_webDeviceName), prefsV0.getString(getKey(R.string.pref_webDeviceName), null))
            e.putBoolean(getKey(R.string.pref_maplive), prefsV0.getInt(getKey(R.string.pref_maplive), 1) != 0)
            e.putInt(getKey(R.string.pref_mapsource), prefsV0.getInt(getKey(R.string.pref_mapsource), MAP_SOURCE_DEFAULT))
            e.putBoolean(getKey(R.string.pref_showaddress), prefsV0.getInt(getKey(R.string.pref_showaddress), 1) != 0)
            e.putBoolean(getKey(R.string.pref_maptrail), prefsV0.getInt(getKey(R.string.pref_maptrail), 1) != 0)
            e.putInt(getKey(R.string.pref_lastmapzoom), prefsV0.getInt(getKey(R.string.pref_lastmapzoom), 14))
            e.putBoolean(getKey(R.string.pref_livelist), prefsV0.getInt(getKey(R.string.pref_livelist), 1) != 0)
            e.putBoolean(getKey(R.string.pref_units_imperial), prefsV0.getInt(getKey(R.string.pref_units_imperial), 1) != 1)
            e.putBoolean(getKey(R.string.old_pref_skin), prefsV0.getInt(getKey(R.string.old_pref_skin), 0) != 0)
            e.putInt(getKey(R.string.pref_lastusedlist), prefsV0.getInt(getKey(R.string.pref_lastusedlist), StoredList.STANDARD_LIST_ID))
            e.putInt(getKey(R.string.pref_version), prefsV0.getInt(getKey(R.string.pref_version), 0))
            e.putBoolean(getKey(R.string.pref_friendlogswanted), prefsV0.getBoolean(getKey(R.string.pref_friendlogswanted), true))
            e.putBoolean(getKey(R.string.old_pref_useenglish), prefsV0.getBoolean(getKey(R.string.old_pref_useenglish), false))
            e.putBoolean(getKey(R.string.pref_usecompass), prefsV0.getInt(getKey(R.string.pref_usecompass), 1) != 0)
            e.putBoolean(getKey(R.string.pref_trackautovisit), prefsV0.getBoolean(getKey(R.string.pref_trackautovisit), false))
            e.putBoolean(getKey(R.string.pref_sigautoinsert), prefsV0.getBoolean(getKey(R.string.pref_sigautoinsert), false))
            e.putBoolean(getKey(R.string.pref_logimages), prefsV0.getBoolean(getKey(R.string.pref_logimages), false))
            e.putString(getKey(R.string.pref_mapfile), prefsV0.getString(getKey(R.string.pref_mapfile), null))
            e.putString(getKey(R.string.pref_signature), prefsV0.getString(getKey(R.string.pref_signature), null))
            e.putString(getKey(R.string.pref_password), prefsV0.getString(getKey(R.string.pref_password), null))
            e.putString(getKey(R.string.pref_username), prefsV0.getString(getKey(R.string.pref_username), null))
            e.putString(getKey(R.string.pref_memberstatus), prefsV0.getString(getKey(R.string.pref_memberstatus), ""))
            e.putInt(getKey(R.string.pref_coordinputformat), prefsV0.getInt(getKey(R.string.pref_coordinputformat), CoordInputFormatEnum.DEFAULT_INT_VALUE))
            e.putBoolean(getKey(R.string.pref_log_offline), prefsV0.getBoolean(getKey(R.string.pref_log_offline), false))
            e.putBoolean(getKey(R.string.pref_choose_list), prefsV0.getBoolean(getKey(R.string.pref_choose_list), true))
            e.putBoolean(getKey(R.string.pref_loaddirectionimg), prefsV0.getBoolean(getKey(R.string.pref_loaddirectionimg), true))
            e.putString(getKey(R.string.pref_gccustomdate), prefsV0.getString(getKey(R.string.pref_gccustomdate), GCConstants.DEFAULT_GC_DATE))
            e.putInt(getKey(R.string.pref_showwaypointsthreshold), prefsV0.getInt(getKey(R.string.pref_showwaypointsthreshold), getKeyInt(R.integer.waypoint_threshold_default)))
            e.putBoolean(getKey(R.string.pref_opendetailslastpage), prefsV0.getBoolean(getKey(R.string.pref_opendetailslastpage), false))
            e.putInt(getKey(R.string.pref_lastdetailspage), prefsV0.getInt(getKey(R.string.pref_lastdetailspage), 1))
            e.putInt(getKey(R.string.pref_defaultNavigationTool), prefsV0.getInt(getKey(R.string.pref_defaultNavigationTool), NavigationAppsEnum.COMPASS.id))
            e.putInt(getKey(R.string.pref_defaultNavigationTool2), prefsV0.getInt(getKey(R.string.pref_defaultNavigationTool2), NavigationAppsEnum.INTERNAL_MAP.id))
            e.putBoolean(getKey(R.string.pref_debug), prefsV0.getBoolean(getKey(R.string.pref_debug), false))

            e.putInt(getKey(R.string.pref_settingsversion), 1); // mark migrated
            e.apply()
        }

        // changes for settings dialog
        if (currentVersion < 2) {
            val e: Editor = sharedPrefs.edit()

            e.putBoolean(getKey(R.string.pref_units_imperial), useImperialUnits())

            // show waypoints threshold now as a slider
            Int wpThreshold = Math.max(0, getWayPointsThreshold())
            wpThreshold = Math.min(wpThreshold, getKeyInt(R.integer.waypoint_threshold_max))
            e.putInt(getKey(R.string.pref_showwaypointsthreshold), wpThreshold)

            // KEY_MAP_SOURCE must be string, because it is the key for a ListPreference now
            val ms: Int = sharedPrefs.getInt(getKey(R.string.pref_mapsource), MAP_SOURCE_DEFAULT)
            e.remove(getKey(R.string.pref_mapsource))
            e.putString(getKey(R.string.pref_mapsource), String.valueOf(ms))

            // navigation tool ids must be string, because ListPreference uses strings as keys
            val dnt1: Int = sharedPrefs.getInt(getKey(R.string.pref_defaultNavigationTool), NavigationAppsEnum.COMPASS.id)
            val dnt2: Int = sharedPrefs.getInt(getKey(R.string.pref_defaultNavigationTool2), NavigationAppsEnum.INTERNAL_MAP.id)
            e.remove(getKey(R.string.pref_defaultNavigationTool))
            e.remove(getKey(R.string.pref_defaultNavigationTool2))
            e.putString(getKey(R.string.pref_defaultNavigationTool), String.valueOf(dnt1))
            e.putString(getKey(R.string.pref_defaultNavigationTool2), String.valueOf(dnt2))

            e.putInt(getKey(R.string.pref_settingsversion), 2); // mark migrated
            e.apply()
        }

        if (currentVersion < 3) {
            val e: Editor = sharedPrefs.edit()

            Log.i("Moving field-notes")
            FileUtils.move(LocalStorage.getLegacyFieldNotesDirectory(), LocalStorage.getFieldNotesDirectory())

            Log.i("Moving gpx ex- and import dirs")
            FileUtils.move(LocalStorage.getLegacyGpxDirectory(), LocalStorage.getDefaultGpxDirectory())

            Log.i("Moving db files")
            FileUtils.moveTo(File(LocalStorage.getLegacyExternalCgeoDirectory(), DataStore.DB_FILE_NAME), LocalStorage.getExternalDbDirectory())
            FileUtils.moveTo(File(LocalStorage.getLegacyExternalCgeoDirectory(), DataStore.DB_FILE_NAME + DataStore.DB_FILE_CORRUPTED_EXTENSION), LocalStorage.getBackupRootDirectory())
            FileUtils.moveTo(File(LocalStorage.getLegacyExternalCgeoDirectory(), DataStore.DB_FILE_NAME_BACKUP), LocalStorage.getBackupRootDirectory())
            FileUtils.moveTo(File(LocalStorage.getLegacyExternalCgeoDirectory(), DataStore.DB_FILE_NAME_BACKUP + DataStore.DB_FILE_CORRUPTED_EXTENSION), LocalStorage.getBackupRootDirectory())

            Log.i("Moving geocache data")
            val geocacheDirectories: FileFilter = pathname -> {
                val name: String = pathname.getName()
                return pathname.isDirectory() &&
                        (HtmlImage.SHARED == (name) || LocalStorage.GEOCACHE_FILE_PATTERN.matcher(name).find())
            }
            final File[] list = LocalStorage.getLegacyExternalCgeoDirectory().listFiles(geocacheDirectories)
            if (list != null) {
                for (final File file : list) {
                    FileUtils.moveTo(file, LocalStorage.getGeocacheDataDirectory())
                }
            }

            Log.i("Deleting legacy .cgeo dir")
            FileUtils.deleteIgnoringFailure(LocalStorage.getLegacyExternalCgeoDirectory())

            e.putString(getKey(R.string.pref_dataDir), LocalStorage.getExternalPrivateCgeoDirectory().getAbsolutePath())
            e.putInt(getKey(R.string.pref_settingsversion), 3); // mark migrated
            e.apply()
        }

        if (currentVersion < 4) {
            val e: Editor = sharedPrefs.edit()

            if (Integer.parseInt(sharedPrefs.getString(getKey(R.string.pref_defaultNavigationTool), String.valueOf(NavigationAppsEnum.COMPASS.id))) == 25) {
                e.putString(getKey(R.string.pref_defaultNavigationTool), prefsV0.getString(getKey(R.string.pref_defaultNavigationTool), String.valueOf(NavigationAppsEnum.INTERNAL_MAP.id)))
            }

            if (Integer.parseInt(sharedPrefs.getString(getKey(R.string.pref_defaultNavigationTool2), String.valueOf(NavigationAppsEnum.INTERNAL_MAP.id))) == 25) {
                e.putString(getKey(R.string.pref_defaultNavigationTool2), prefsV0.getString(getKey(R.string.pref_defaultNavigationTool2), String.valueOf(NavigationAppsEnum.INTERNAL_MAP.id)))
            }

            e.putInt(getKey(R.string.pref_settingsversion), 4); // mark migrated
            e.apply()
        }

        if (currentVersion < 5) {
            // non-used version which spilled into the nightlies. Just mark as migrated
            setActualVersion(5)
        }

        // the whole range of version numbers from 6 until 8 was "used" in different parts
        // of migration of global exclude settings due to different bugs.
        // Since this spilled into nightlies and beta, they can't be reused.
        if (currentVersion < 8) {
            //migrate global own/found/disable/archived/offlinelog to LIVE filter
            val legacyGlobalSettings: Map<GeocacheFilter.QuickFilter, Boolean> = HashMap<>()
            //see #11311: in some cases, the "exclude found" might not exist yet on user's devices
            val legacyExcludeMine: Boolean = getBooleanDirect("excludemine", false)
            val legacyExcludeFound: Boolean = hasKeyDirect("excludefound") ? getBooleanDirect("excludefound", false) : legacyExcludeMine

            legacyGlobalSettings.put(GeocacheFilter.QuickFilter.OWNED, !legacyExcludeMine)
            legacyGlobalSettings.put(GeocacheFilter.QuickFilter.FOUND, !legacyExcludeFound)
            legacyGlobalSettings.put(GeocacheFilter.QuickFilter.DISABLED, !getBooleanDirect("excludedisabled", false))
            legacyGlobalSettings.put(GeocacheFilter.QuickFilter.ARCHIVED, !getBooleanDirect("excludearchived", false))

            val liveFilterContext: GeocacheFilterContext = GeocacheFilterContext(GeocacheFilterContext.FilterType.LIVE)
            GeocacheFilter liveFilter = liveFilterContext.get()

            if (!liveFilter.hasSameQuickFilter(legacyGlobalSettings)) {
                if (!liveFilter.canSetQuickFilterLossless()) {
                    //settings can't be merged -> remove old filter
                    liveFilter = GeocacheFilter.createEmpty()
                }
                liveFilter.setQuickFilterLossless(legacyGlobalSettings)
                liveFilterContext.set(liveFilter)
            }

            setActualVersion(8)
        }

        if (currentVersion < 9) {
            val e: Editor = sharedPrefs.edit()

            val isMapAutoDownloads: Boolean = sharedPrefs.getBoolean(getKey(R.string.old_pref_mapAutoDownloads), false)
            if (!isMapAutoDownloads) {
                e.putInt(getKey(R.string.pref_mapAutoDownloadsInterval), 0)
            }
            e.remove(getKey(R.string.old_pref_mapAutoDownloads)); // key no longer in use, will fall back to default on downgrade

            val isRoutingTileAutoDownloads: Boolean = sharedPrefs.getBoolean(getKey(R.string.pref_brouterAutoTileDownloads), false)
            if (!isRoutingTileAutoDownloads) {
                e.putInt(getKey(R.string.pref_brouterAutoTileDownloadsInterval), 0)
            }
            // bRouterAutoTileDownloads key is still in use, do NOT remove it

            e.apply()
            setActualVersion(9)
        }

        if (currentVersion < 10) {
            val e: Editor = sharedPrefs.edit()

            val dateFormatString: String = getShortDateFormat()
            if (!dateFormatString.isEmpty()) {
                // fix Short date formatting: "week date" => "date", see #16925
                e.putString(getKey(R.string.pref_short_date_format), dateFormatString.replace("Y", "y"))
            }

            e.apply()
            setActualVersion(10)
        }

        if (currentVersion < 11) {
            val tileprovider: String = sharedPrefs.getString(getKey(R.string.pref_mapsource), "")
                // Google Maps map sources
                .replace("cgeo.geocaching.maps.google.v2.GoogleMapProvider$", "cgeo.geocaching.unifiedmap.tileproviders.")
                // cgeo.geocaching.maps.google.v2.GoogleMapProvider$GoogleMapSource         => cgeo.geocaching.unifiedmap.tileproviders.GoogleMapSource
                // cgeo.geocaching.maps.google.v2.GoogleMapProvider$GoogleSatelliteSource   => cgeo.geocaching.unifiedmap.tileproviders.GoogleSatelliteSource
                // cgeo.geocaching.maps.google.v2.GoogleMapProvider$GoogleTerrainSource     => cgeo.geocaching.unifiedmap.tileproviders.GoogleTerrainSource

                // OSM online map sources
                .replace("cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider$", "cgeo.geocaching.unifiedmap.tileproviders.")
                // cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider$OsmMapSource         => cgeo.geocaching.unifiedmap.tileproviders.OsmOrgSource:null
                .replace(".OsmMapSource", ".OsmOrgSource:null")
                // cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider$OsmdeMapSource       => cgeo.geocaching.unifiedmap.tileproviders.OsmDeSource:null
                .replace(".OsmdeMapSource", ".OsmDeSource:null")
                // cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider$CyclosmMapSource     => cgeo.geocaching.unifiedmap.tileproviders.CyclosmSource:null
                .replace(".CyclosmMapSource", ".CyclosmSource:null")
                // cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider$OpenTopoMapSource    => cgeo.geocaching.unifiedmap.tileproviders.OpenTopoMapSource:null (!!!)
                .replace(".OpenTopoMapSource", ".OpenTopoMapSource:null")

                // OSM offline map sources
                // cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider$OfflineMapSource:primary:cgeo/maps/bremen.map => cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeOfflineTileProvider:primary:cgeo/maps/bremen.map
                .replace(".OfflineMapSource:", ".AbstractMapsforgeOfflineTileProvider:")
                // cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider$OfflineMultiMapSource => cgeo.geocaching.unifiedmap.tileproviders.MapsforgeMultiOfflineTileProvider:null
                .replace(".OfflineMultiMapSource", ".MapsforgeMultiOfflineTileProvider:null")
            

            if (useLegacyMaps()) {
                val e: Editor = sharedPrefs.edit()
                e.putString(getKey(R.string.pref_tileprovider), StringUtils.isBlank(tileprovider) ? "cgeo.geocaching.unifiedmap.tileproviders.GoogleMapSource" : tileprovider)
                e.putBoolean(getKey(R.string.pref_useLegacyMap), false)
                e.putString(getKey(R.string.pref_unifiedMapVariants), String.valueOf(UNIFIEDMAP_VARIANT_MAPSFORGE))
                e.apply()
                Log.e("Migrated map mode to UnifiedMap: " + tileprovider)
            }

            setActualVersion(11)
        }

    }

    private static String getKey(final Int prefKeyId) {
        return CgeoApplication.getInstance() == null ? null : CgeoApplication.getInstance().getString(prefKeyId)
    }

    public static Int getKeyInt(final Int prefKeyId) {
        return CgeoApplication.getInstance() == null ? -1 : CgeoApplication.getInstance().getResources().getInteger(prefKeyId)
    }

    public static String getString(final Int prefKeyId, final String defaultValue) {
        return getStringDirect(getKey(prefKeyId), defaultValue)
    }

    private static Boolean hasKeyDirect(final String prefKey) {
        return sharedPrefs != null && sharedPrefs.contains(prefKey)
    }

    public static String getStringDirect(final String prefKey, final String defaultValue) {
        return sharedPrefs == null ? defaultValue : sharedPrefs.getString(prefKey, defaultValue)
    }


    private static List<String> getStringList(final Int prefKeyId, final String defaultValue) {
        return Arrays.asList(StringUtils.split(getString(prefKeyId, defaultValue), SEPARATOR_CHAR))
    }

    public static Int getInt(final Int prefKeyId, final Int defaultValue) {
        return getIntDirect(getKey(prefKeyId), defaultValue)
    }

    private static Int getIntDirect(final String prefKey, final Int defaultValue) {
        return sharedPrefs == null ? defaultValue : sharedPrefs.getInt(prefKey, defaultValue)
    }

    public static Int getPreferencesCount() {
        return sharedPrefs.getAll().size()
    }

    // workaround for Int prefs, originally saved as string
    private static Int getIntFromString(final Int prefKeyId, final Int defaultValue) {
        try {
            return Integer.parseInt(getString(prefKeyId, String.valueOf(defaultValue)))
        } catch (Exception e) {
            return defaultValue
        }

    }

    public static Long getLong(final Int prefKeyId, final Long defaultValue) {
        return sharedPrefs == null ? defaultValue : sharedPrefs.getLong(getKey(prefKeyId), defaultValue)
    }

    public static Boolean getBoolean(final Int prefKeyId, final Boolean defaultValue) {
        return getBooleanDirect(getKey(prefKeyId), defaultValue)
    }

    private static Boolean getBooleanDirect(final String prefKey, final Boolean defaultValue) {
        try {
            return sharedPrefs == null ? defaultValue : sharedPrefs.getBoolean(prefKey, defaultValue)
        } catch (ClassCastException cce) {
            return defaultValue
        }
    }


    private static Float getFloat(final Int prefKeyId, final Float defaultValue) {
        return sharedPrefs == null ? defaultValue : sharedPrefs.getFloat(getKey(prefKeyId), defaultValue)
    }

    public static Unit putString(final Int prefKeyId, final String value) {
        putStringDirect(getKey(prefKeyId), value)
    }

    public static Unit putStringDirect(final String prefKey, final String value) {
        if (sharedPrefs == null) {
            return
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit()
        edit.putString(prefKey, value)
        edit.apply()
    }

    private static Unit putStringList(final Int prefKeyId, final Iterable<?> elements) {
        putString(prefKeyId, StringUtils.join(elements, SEPARATOR_CHAR))
    }

    public static Unit putBoolean(final Int prefKeyId, final Boolean value) {
        putBooleanDirect(getKey(prefKeyId), value)
    }

    public static Unit putBooleanDirect(final String key, final Boolean value) {

        if (sharedPrefs == null) {
            return
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit()
        edit.putBoolean(key, value)
        edit.apply()
    }

    private static Unit putInt(final Int prefKeyId, final Int value) {
        putIntDirect(getKey(prefKeyId), value)
    }

    public static Unit putIntDirect(final String prefKey, final Int value) {
        if (sharedPrefs == null) {
            return
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit()
        edit.putInt(prefKey, value)
        edit.apply()
    }

    private static Unit putLong(final Int prefKeyId, final Long value) {
        if (sharedPrefs == null) {
            return
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit()
        edit.putLong(getKey(prefKeyId), value)
        edit.apply()
    }

    private static Unit putFloat(final Int prefKeyId, final Float value) {
        if (sharedPrefs == null) {
            return
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit()
        edit.putFloat(getKey(prefKeyId), value)
        edit.apply()
    }

    private static Unit remove(final Int prefKeyId) {
        removeDirect(getKey(prefKeyId))
    }

    private static Unit removeDirect(final String key) {
        if (sharedPrefs == null) {
            return
        }
        final SharedPreferences.Editor edit = sharedPrefs.edit()
        edit.remove(key)
        edit.apply()
    }

    public static Boolean hasGCCredentials() {
        return getGcCredentials().isValid()
    }

    /**
     * Get login and password information of Geocaching.com.
     *
     * @return a pair either with (login, password) or (empty, empty) if no valid information is stored
     */
    public static Credentials getGcCredentials() {
        return getCredentials(GCConnector.getInstance())
    }

    /**
     * Get login and password information from preference key id.
     *
     * @param usernamePreferenceKey Username preference key id
     * @param passwordPreferenceKey Password preference key id
     * @return the credential information
     */
    public static Credentials getCredentials(final Int usernamePreferenceKey, final Int passwordPreferenceKey) {
        val username: String = StringUtils.trim(getString(usernamePreferenceKey, StringUtils.EMPTY))
        val password: String = getString(passwordPreferenceKey, StringUtils.EMPTY)
        return Credentials(username, password)
    }

    /**
     * Get login and password information.
     *
     * @param connector the connector to retrieve the login information from
     * @return the credential information
     */
    public static Credentials getCredentials(final ICredentials connector) {
        return getCredentials(connector.getUsernamePreferenceKey(), connector.getPasswordPreferenceKey())
    }

    /**
     * Set login and password information.
     *
     * @param connector   the connector to retrieve the login information from
     * @param credentials the credential information to store
     */
    public static Unit setCredentials(final ICredentials connector, final Credentials credentials) {
        putString(connector.getUsernamePreferenceKey(), credentials.getUsernameRaw())
        putString(connector.getPasswordPreferenceKey(), credentials.getPasswordRaw())
    }

    public static String getUserName() {
        return StringUtils.trim(getString(R.string.pref_username, StringUtils.EMPTY))
    }

    /** this method is to be solely used for auto-fixing GC username, see MainActivity.UpdateUserInfoHandler */
    public static Unit setGCUserName(final String username) {
        if (StringUtils.isNotBlank(username)) {
            putString(R.string.pref_username, username)
        }
    }

    public static Boolean isGCConnectorActive() {
        return getBoolean(R.string.pref_connectorGCActive, false)
    }

    public static Unit setGCConnectorActive(final Boolean value) {
        putBoolean(R.string.pref_connectorGCActive, value)
    }

    public static Boolean isECConnectorActive() {
        return getBoolean(R.string.pref_connectorECActive, false)
    }

    public static Boolean isALConnectorActive() {
        return getBoolean(R.string.pref_connectorALActive, true)
    }

    public static Boolean isSUConnectorActive() {
        return getBoolean(R.string.pref_connectorSUActive, false)
    }

    public static Boolean isBetterCacherConnectorActive() {
        return getBoolean(R.string.pref_connectorBetterCacherActive, false)
    }

    public static Boolean isGCPremiumMember() {
        return getGCMemberStatus().isPremium()
    }

    public static Boolean isALCAdvanced() {
        return getBoolean(R.string.pref_alc_advanced, false)
    }

    public static Boolean isALCfoundStateManual() {
        return getBoolean(R.string.pref_foundstate_al, false)
    }

    public static Boolean enableVtmSingleMarkerSymbol() {
        return getBoolean(R.string.pref_vtm_single_marker_symbol, false)
    }

    public static Boolean enableVtmMarkerAtlasUsage() {
        return getBoolean(R.string.pref_vtm_marker_atlas_usage, false)
    }

    public static Boolean enableFeatureUnifiedDebug() {
        return getBoolean(R.string.pref_feature_unified_debug, false)
    }

    public static Boolean enableFeatureUnifiedGeoItemLayer() {
        return getBoolean(R.string.pref_feature_unified_geoitem_layer, false)
    }

    public static Boolean enableFeatureWherigoDebug() {
        return getBoolean(R.string.pref_feature_wherigo_debug, false)
    }

    public static Boolean enableFeatureWherigoDebugCartridge(final String code) {
        return getBooleanDirect(getKey(R.string.pref_feature_wherigo_debug) + "_" + code, false)
    }


    public static Boolean isFeatureEnabledDefaultFalse(@StringRes final Int featureKeyId) {
        return getBoolean(featureKeyId, false)
    }

    public static Boolean isFeatureEnabledDefaultTrue(@StringRes final Int featureKeyId) {
        return getBoolean(featureKeyId, true)
    }

    public static String getALCLauncher() {
        return getString(R.string.pref_alc_launcher, "")
    }

    public static GCMemberState getGCMemberStatus() {
        return GCMemberState.fromString(getString(R.string.pref_memberstatus, ""))
    }

    public static Unit setGCMemberStatus(final GCMemberState memberStatus) {
        putString(R.string.pref_memberstatus, memberStatus.id)
    }

    //solely to be used by class Cookies
    public static String getPersistentCookies() {
        return getString(R.string.pref_cookiejar, "")
    }

    //solely to be used by class Cookies
    public static Unit setPersistentCookies(final String cookies) {
        putString(R.string.pref_cookiejar, cookies)
    }

    public static ImmutablePair<String, String> getTokenPair(final Int tokenPublicPrefKey, final Int tokenSecretPrefKey) {
        return ImmutablePair<>(getString(tokenPublicPrefKey, null), getString(tokenSecretPrefKey, null))
    }

    public static Unit setTokens(final Int tokenPublicPrefKey, final String tokenPublic, final Int tokenSecretPrefKey, final String tokenSecret) {
        if (tokenPublic == null) {
            remove(tokenPublicPrefKey)
        } else {
            putString(tokenPublicPrefKey, tokenPublic)
        }
        if (tokenSecret == null) {
            remove(tokenSecretPrefKey)
        } else {
            putString(tokenSecretPrefKey, tokenSecret)
        }
    }

    public static Boolean isOCConnectorActive(final Int isActivePrefKeyId) {
        return getBoolean(isActivePrefKeyId, false)
    }

    public static Boolean hasOAuthAuthorization(final Int tokenPublicPrefKeyId, final Int tokenSecretPrefKeyId) {
        return StringUtils.isNotBlank(getString(tokenPublicPrefKeyId, ""))
                && StringUtils.isNotBlank(getString(tokenSecretPrefKeyId, ""))
    }

    public static String getSignature() {
        return StringUtils.defaultString(getString(R.string.pref_signature, StringUtils.EMPTY))
    }

    public static Unit setUseGooglePlayServices(final Boolean value) {
        putBoolean(R.string.pref_googleplayservices, value)
    }

    public static Boolean useGooglePlayServices() {
        if (!GooglePlayServices.isAvailable()) {
            return false
        }
        return outdatedPhoneModelOrSdk() || getBoolean(R.string.pref_googleplayservices, true)
    }

    public static Boolean useLowPowerMode() {
        return getBoolean(R.string.pref_lowpowermode, false)
    }

    public static Int getLastDisplayedList() {
        return getInt(R.string.pref_lastusedlist, StoredList.STANDARD_LIST_ID)
    }

    /**
     * remember the last displayed cache list
     */
    public static Unit setLastDisplayedList(final Int listId) {
        putInt(R.string.pref_lastusedlist, listId)
    }

    public static Set<Integer> getLastSelectedLists() {
        val lastSelectedLists: Set<Integer> = HashSet<>()
        for (final String lastSelectedListString : getStringList(R.string.pref_last_selected_lists, StringUtils.EMPTY)) {
            try {
                lastSelectedLists.add(Integer.valueOf(lastSelectedListString))
            } catch (final NumberFormatException ignored) {
            }
        }
        return lastSelectedLists
    }

    /**
     * remember the last selection in the dialog that assigns a cache to certain lists
     */
    public static Unit setLastSelectedLists(final Set<Integer> lastSelectedLists) {
        putStringList(R.string.pref_last_selected_lists, lastSelectedLists)
    }

    public static Set<WaypointType> getLastSelectedVisitedWaypointTypes() {
        val lastSelectedVisitedWaypointTypes: Set<WaypointType> = HashSet<>()
        for (final String lastSelectedVisitedWaypointTypesString : getStringList(R.string.pref_last_selected_visited_waypointtypes, StringUtils.EMPTY)) {
            lastSelectedVisitedWaypointTypes.add(WaypointType.findById(lastSelectedVisitedWaypointTypesString))
        }
        return lastSelectedVisitedWaypointTypes
    }

    public static Unit setLastSelectedVisitedWaypointTypes(final Set<WaypointType> lastSelectedVisitedWaypointTypes) {
        val lastSelectedVisitedWaypointTypesAsString: Set<String> = HashSet<>()
        for (final WaypointType wpType : lastSelectedVisitedWaypointTypes) {
            lastSelectedVisitedWaypointTypesAsString.add(wpType.id)
        }
        putStringList(R.string.pref_last_selected_visited_waypointtypes, lastSelectedVisitedWaypointTypesAsString)
    }

    public static Unit setWebNameCode(final String name, final String code) {
        putString(R.string.pref_webDeviceName, name)
        putString(R.string.pref_webDeviceCode, code)
    }

    public static MapProvider getMapProvider() {
        return getMapSource().getMapProvider()
    }

    public static Int getMapRotation() {
        val prefValue: String = getString(R.string.pref_mapRotation, "")
        if (prefValue == (getKey(R.string.pref_maprotation_off))) {
            return MAPROTATION_OFF
        } else if (prefValue == (getKey(R.string.pref_maprotation_auto_lowpower))) {
            return MAPROTATION_AUTO_LOWPOWER
        } else if (prefValue == (getKey(R.string.pref_maprotation_auto_precise))) {
            return MAPROTATION_AUTO_PRECISE
        }
        return MAPROTATION_MANUAL
    }

    public static Unit setMapRotation(final Int mapRotation) {
        switch (mapRotation) {
            case MAPROTATION_OFF:
                putString(R.string.pref_mapRotation, getKey(R.string.pref_maprotation_off))
                break
            case MAPROTATION_MANUAL:
                putString(R.string.pref_mapRotation, getKey(R.string.pref_maprotation_manual))
                break
            case MAPROTATION_AUTO_LOWPOWER:
                putString(R.string.pref_mapRotation, getKey(R.string.pref_maprotation_auto_lowpower))
                break
            case MAPROTATION_AUTO_PRECISE:
                putString(R.string.pref_mapRotation, getKey(R.string.pref_maprotation_auto_precise))
                break
            default:
                // do nothing except satisfy static code analysis
                break
        }
    }

    public static Boolean getBuildings3D() {
        return getBoolean(R.string.pref_buildingLayer3D, true)
    }

    public static Boolean isAutotargetIndividualRoute() {
        return getBoolean(R.string.pref_autotarget_individualroute, false)
    }

    public static Unit setAutotargetIndividualRoute(final Boolean autotargetIndividualRoute) {
        putBoolean(R.string.pref_autotarget_individualroute, autotargetIndividualRoute)
    }

    public static CoordInputFormatEnum getCoordInputFormat() {
        return CoordInputFormatEnum.fromInt(getInt(R.string.pref_coordinputformat, CoordInputFormatEnum.DEFAULT_INT_VALUE))
    }

    public static Unit setCoordInputFormat(final CoordInputFormatEnum format) {
        putInt(R.string.pref_coordinputformat, format.ordinal())
    }

    public static Boolean getLogOffline() {
        return getBoolean(R.string.pref_log_offline, false)
    }

    public static Boolean getChooseList() {
        return getBoolean(R.string.pref_choose_list, true)
    }

    public static Boolean getLoadDirImg() {
        return !isGCPremiumMember() && getBoolean(R.string.pref_loaddirectionimg, true)
    }

    public static Unit setGcCustomDate(final String format) {
        putString(R.string.pref_gccustomdate, format)
    }

    /**
     * @return User selected date format on GC.com
     */
    public static String getGcCustomDate() {
        // We might have some users whose stored value is null, which is invalid. In this case, we use the default.
        return StringUtils.defaultString(getString(R.string.pref_gccustomdate, GCConstants.DEFAULT_GC_DATE),
                GCConstants.DEFAULT_GC_DATE)
    }

    public static Boolean isWallpaper() {
        return getBoolean(R.string.pref_wallpaper, false)
    }

    public static Boolean isShowAddress() {
        return getBoolean(R.string.pref_showaddress, true)
    }

    public static Boolean isExcludeWpOriginal() {
        return getBoolean(R.string.pref_excludeWpOriginal, false)
    }

    public static Boolean isExcludeWpParking() {
        return getBoolean(R.string.pref_excludeWpParking, false)
    }

    public static Boolean isExcludeWpVisited() {
        return getBoolean(R.string.pref_excludeWpVisited, false)
    }

    public static Boolean isStoreLogImages() {
        return getBoolean(R.string.pref_logimages, false)
    }

    public static Boolean isGeokretyConnectorActive() {
        return getBoolean(R.string.pref_connectorGeokretyActive, false)
    }

    public static Boolean hasGeokretyAuthorization() {
        return StringUtils.isNotBlank(getGeokretySecId())
    }

    public static String getGeokretySecId() {
        return getString(R.string.pref_fakekey_geokrety_authorization, null)
    }

    public static Unit setGeokretySecId(final String secid) {
        putString(R.string.pref_fakekey_geokrety_authorization, secid)
    }

    public static String getTokenSecret(final Int prefKeyId) {
        return getString(prefKeyId, StringUtils.EMPTY)
    }

    public static Unit setTokenSecret(final Int prefKeyId, final String secretToken) {
        putString(prefKeyId, secretToken)
    }

    public static Boolean isRegisteredForGeokretyLogging() {
        return getGeokretySecId() != null
    }

    /**
     * Retrieve showed popup counter for warning about logging Trackable recommend Geocode
     *
     * @return number of times the popup has appeared
     */
    public static Int getLogTrackableWithoutGeocodeShowCount() {
        return getInt(R.string.pref_logtrackablewithoutgeocodeshowcount, 0)
    }

    /**
     * Store showed popup counter for warning about logging Trackable recommend Geocode
     *
     * @param showCount the count to save
     */
    public static Unit setLogTrackableWithoutGeocodeShowCount(final Int showCount) {
        putInt(R.string.pref_logtrackablewithoutgeocodeshowcount, showCount)
    }

    public static Boolean isFriendLogsWanted() {
        if (!hasGCCredentials()) {
            // don't show a friends log if the user is anonymous
            return false
        }
        return getBoolean(R.string.pref_friendlogswanted, true)
    }

    public static Int getLogLineLimit() {
        val logLineLimit: Int = getInt(R.string.pref_collapse_log_limit, getKeyInt(R.integer.log_line_limit_default))
        if (logLineLimit == getKeyInt(R.integer.list_load_limit_max)) {
            return 0
        }
        return logLineLimit
    }

    public static Boolean isLiveList() {
        return getBoolean(R.string.pref_livelist, true)
    }

    public static Boolean useLiveCompassInNavigationAction() {
        return getBoolean(R.string.pref_live_compass_in_navigation_action, false)
    }

    public static Boolean isTrackableAutoVisit() {
        return getBoolean(R.string.pref_trackautovisit, false)
    }

    public static Boolean isAutoInsertSignature() {
        return getBoolean(R.string.pref_sigautoinsert, false)
    }

    public static Boolean isDisplayOfflineLogsHomescreen() {
        return getBoolean(R.string.pref_offlinelogs_homescreen, true)
    }

    public static Boolean useImperialUnits() {
        return getBoolean(R.string.pref_units_imperial, useImperialUnitsByDefault())
    }

    private static Boolean useImperialUnitsByDefault() {
        val countryCode: String = Locale.getDefault().getCountry()
        return "US" == (countryCode)  // USA
                || "LR" == (countryCode)  // Liberia
                || "MM" == (countryCode); // Burma
    }

    public static Boolean isLiveMap() {
        return getBoolean(R.string.pref_maplive, true)
    }

    public static Unit setLiveMap(final Boolean live) {
        putBoolean(R.string.pref_maplive, live)
    }

    public static Boolean isMapTrail() {
        return getBoolean(R.string.pref_maptrail, false)
    }

    public static Unit setMapTrail(final Boolean showTrail) {
        putBoolean(R.string.pref_maptrail, showTrail)
    }

    public static Int getMaximumMapTrailLength() {
        return getInt(R.string.pref_maptrail_length, getKeyInt(R.integer.historytrack_length_default))
    }

    public static Int getMapLineValue(final Int prefKeyId, final Int defaultValueKeyId) {
        return getInt(prefKeyId, getKeyInt(defaultValueKeyId))
    }

    public static Boolean hasOSMMultiThreading() {
        return getBoolean(R.string.pref_map_osm_multithreaded, false)
    }

    public static Int getMapOsmThreads() {
        return hasOSMMultiThreading() ? Math.max(1, getInt(R.string.pref_map_osm_threads, Math.min(Runtime.getRuntime().availableProcessors() + 1, 4))) : 1
    }

    public static Int getCompactIconMode() {
        val prefValue: String = getString(R.string.pref_compactIconMode, "")
        if (prefValue == (getKey(R.string.pref_compacticon_on))) {
            return COMPACTICON_ON
        } else if (prefValue == (getKey(R.string.pref_compacticon_auto))) {
            return COMPACTICON_AUTO
        }
        return COMPACTICON_OFF
    }

    public static Unit setCompactIconMode(final Int compactIconMode) {
        switch (compactIconMode) {
            case COMPACTICON_OFF:
                putString(R.string.pref_compactIconMode, getKey(R.string.pref_compacticon_off))
                break
            case COMPACTICON_ON:
                putString(R.string.pref_compactIconMode, getKey(R.string.pref_compacticon_on))
                break
            case COMPACTICON_AUTO:
                putString(R.string.pref_compactIconMode, getKey(R.string.pref_compacticon_auto))
                break
            default:
                // do nothing except satisfy static code analysis
                break
        }
    }

    /**
     * whether to show a direction line on the map
     */
    public static Boolean isMapDirection() {
        return Settings.getRoutingMode() != RoutingMode.OFF
    }

    /**
     * Get last used zoom of the internal map. Differentiate between two use cases for a map of multiple caches (e.g.
     * live map) and the map of a single cache (which is often zoomed in more deep).
     */
    public static Int getMapZoom(final MapMode mapMode) {
        if (mapMode == MapMode.SINGLE || mapMode == MapMode.COORDS) {
            return getCacheZoom()
        }
        return getMapZoom()
    }

    public static Unit setMapZoom(final MapMode mapMode, final Int zoomLevel) {
        if (mapMode == MapMode.SINGLE || mapMode == MapMode.COORDS) {
            setCacheZoom(zoomLevel)
        } else {
            setMapZoom(zoomLevel)
        }
    }

    /**
     * @return zoom used for the (live) map
     */
    private static Int getMapZoom() {
        return Math.max(0, getInt(R.string.pref_lastmapzoom, 14))
    }

    private static Unit setMapZoom(final Int mapZoomLevel) {
        putInt(R.string.pref_lastmapzoom, mapZoomLevel)
    }

    /**
     * @return zoom used for the map of a single cache
     */
    private static Int getCacheZoom() {
        return Math.max(0, getInt(R.string.pref_cache_zoom, 14))
    }

    private static Unit setCacheZoom(final Int zoomLevel) {
        putInt(R.string.pref_cache_zoom, zoomLevel)
    }

    public static GeoPointImpl getMapCenter() {
        return getMapProvider().getMapItemFactory()
                .getGeoPointBase(Geopoint(getInt(R.string.pref_lastmaplat, 0) / 1e6,
                        getInt(R.string.pref_lastmaplon, 0) / 1e6))
    }

    // temporary workaround for UnifiedMap necessary, as it is completely parallel to getMapProvider() currently
    public static Geopoint getUMMapCenter() {
        return Geopoint(getInt(R.string.pref_lastmaplat, 0) / 1e6, getInt(R.string.pref_lastmaplon, 0) / 1e6)
    }

    public static Boolean getZoomIncludingWaypoints() {
        return getBoolean(R.string.pref_zoomincludingwaypoints, false)
    }

    public static Unit setMapCenter(final GeoPointImpl mapViewCenter) {
        if (mapViewCenter == null) {
            return
        }
        putInt(R.string.pref_lastmaplat, mapViewCenter.getLatitudeE6())
        putInt(R.string.pref_lastmaplon, mapViewCenter.getLongitudeE6())
    }

    public static synchronized MapSource getMapSource() {
        if (mapSource != null) {
            return mapSource
        }
        val mapSourceId: String = getString(R.string.pref_mapsource, null)
        mapSource = MapProviderFactory.getMapSource(mapSourceId)
        if (mapSource == null || !mapSource.isAvailable()) {
            mapSource = MapProviderFactory.getAnyMapSource()
        }
        return mapSource
    }

    public static synchronized Unit setMapSource(final MapSource newMapSource) {
        if (newMapSource != null && newMapSource.isAvailable()) {
            putString(R.string.pref_mapsource, newMapSource.getId())
            // cache the value
            mapSource = newMapSource
        }
    }

    public static synchronized AbstractTileProvider getTileProvider() {
        if (tileProvider != null) {
            return tileProvider
        }
        val tileProviderId: String = getString(R.string.pref_tileprovider, null)
        tileProvider = TileProviderFactory.getTileProvider(tileProviderId)
        if (tileProvider == null /* || !tileProvider.isAvailable() */) {
            tileProvider = TileProviderFactory.getAnyTileProvider()
        }
        return tileProvider
    }

    public static synchronized Unit setTileProvider(final AbstractTileProvider newTileProvider) {
        if (newTileProvider != null /* && newTileProvider.isAvailable() */) {
            putString(R.string.pref_tileprovider, newTileProvider.getId())
            tileProvider = newTileProvider
        }
    }

    public static Unit setPreviousTileProvider(final AbstractTileProvider tileProvider) {
        if (tileProvider != null) {
            putString(R.string.pref_previous_tileprovider, tileProvider.getId())
        }
    }

    public static AbstractTileProvider getPreviousTileProvider() {
        val tileProviderId: String = getString(R.string.pref_previous_tileprovider, null)
        tileProvider = TileProviderFactory.getTileProvider(tileProviderId)
        if (tileProvider == null) {
            tileProvider = TileProviderFactory.getAnyTileProvider()
        }
        return tileProvider
    }

    public static Set<String> getHideTileproviders() {
        val empty: Set<String> = Collections.emptySet()
        if (sharedPrefs == null) {
            return empty
        }
        return sharedPrefs.getStringSet(getKey(R.string.pref_tileprovider_hidden), empty)
    }

    public static String getUserDefinedTileProviderUri() {
        return getString(R.string.pref_userDefinedTileProviderUri, null)
    }

    public static Unit setMapLanguage(final String language) {
        putString(R.string.pref_mapLanguage, StringUtils.isBlank(language) ? "" : language)
    }

    public static String getMapLanguage() {
        val language: String = getString(R.string.pref_mapLanguage, null)
        return StringUtils.isBlank(language) ? null : language
    }

    public static Int getMapLanguageId() {
        val language: String = getMapLanguage()
        return StringUtils.isBlank(language) ? MAP_LANGUAGE_DEFAULT_ID : language.hashCode()
    }

    /** use legacy maps **/
    public static Boolean useLegacyMaps() {
        return getBoolean(R.string.pref_useLegacyMap, false)
    }

    /** use Mapsforge as map view for UnifiedMap */
    public static Boolean showMapsforgeInUnifiedMap() {
        return (getUnifiedMapVariant() & UNIFIEDMAP_VARIANT_MAPSFORGE) > 0
    }

    /** use VTM as map view for UnifiedMap */
    public static Boolean showVTMInUnifiedMap() {
        return (getUnifiedMapVariant() & UNIFIEDMAP_VARIANT_VTM) > 0
    }

    /** which variants are enabled for UnifiedMap */
    private static Int getUnifiedMapVariant() {
        try {
            return Integer.parseInt(getString(R.string.pref_unifiedMapVariants, String.valueOf(UNIFIEDMAP_VARIANT_MAPSFORGE)))
        } catch (NumberFormatException ignore) {
            return UNIFIEDMAP_VARIANT_MAPSFORGE
        }
    }

    public static Unit setMapDownloaderSource(final Int source) {
        putInt(R.string.pref_mapdownloader_source, source)
    }

    public static Int getMapDownloaderSource() {
        return getInt(R.string.pref_mapdownloader_source, Download.DownloadType.DOWNLOADTYPE_MAP_MAPSFORGE.id)
    }

    public static Boolean mapAutoDownloadsNeedUpdate() {
        return needsIntervalAction(R.string.pref_mapAutoDownloadsLastCheck, getMapAutoDownloadsInterval() * 24, () -> setMapAutoDownloadsLastCheck(false))
    }

    private static Int getMapAutoDownloadsInterval() {
        return getInt(R.string.pref_mapAutoDownloadsInterval, 30)
    }

    public static Unit setMapAutoDownloadsLastCheck(final Boolean delay) {
        putLong(R.string.pref_mapAutoDownloadsLastCheck, calculateNewTimestamp(delay, getMapAutoDownloadsInterval() * 24))
    }

    public static Boolean getMapDownloadsKeepTemporaryFiles() {
        return getBoolean(R.string.pref_mapDownloadsKeepTemporaryFiles, false)
    }

    public static Boolean getMapDownloaderAutoRename() {
        return getBoolean(R.string.pref_autorenameDownloads, true)
    }

    public static Boolean dbNeedsCleanup() {
        return needsIntervalAction(R.string.pref_dbCleanupLastCheck, 24, () -> setDbCleanupLastCheck(false))
    }

    public static Unit setDbCleanupLastCheck(final Boolean delay) {
        putLong(R.string.pref_dbCleanupLastCheck, calculateNewTimestamp(delay, 24))
    }

    public static Boolean dbNeedsReindex() {
        return needsIntervalAction(R.string.pref_dbReindexLastCheck, 90 * 24, () -> setDbReindexLastCheck(false))
    }

    public static Unit setDbReindexLastCheck(final Boolean delay) {
        putLong(R.string.pref_dbReindexLastCheck, calculateNewTimestamp(delay, 90 * 24))
    }

    public static Boolean pendingDownloadsNeedCheck() {
        return needsIntervalAction(R.string.pref_pendingDownloadsLastCheck, 12, () -> setPendingDownloadsLastCheck(false))
    }

    public static Unit setPendingDownloadsLastCheck(final Boolean delay) {
        putLong(R.string.pref_pendingDownloadsLastCheck, calculateNewTimestamp(delay, 12))
    }

    public static Unit setPqShowDownloadableOnly(final Boolean showDownloadableOnly) {
        putBoolean(R.string.pref_pqShowDownloadableOnly, showDownloadableOnly)
    }

    public static Boolean getPqShowDownloadableOnly() {
        return getBoolean(R.string.pref_pqShowDownloadableOnly, false)
    }

    public static Unit setBookmarklistsShowNewOnly(final Boolean showNewOnly) {
        putBoolean(R.string.pref_bookmarklistsShowNewOnly, showNewOnly)
    }

    public static Boolean getBookmarklistsShowNewOnly() {
        return getBoolean(R.string.pref_bookmarklistsShowNewOnly, false)
    }

    public static Boolean isUseCompass() {
        return useCompass
    }

    public static Unit setDeviceOrientationMode(final DirectionData.DeviceOrientation value) {
        deviceOrientationMode = value
    }

    public static DirectionData.DeviceOrientation getDeviceOrientationMode() {
        return deviceOrientationMode
    }

    public static Unit setUseCompass(final Boolean value) {
        useCompass = value
    }


    public static Unit setAppThemeAutomatically(final Context context) {
        setAppTheme(getAppTheme(context))
    }

    public static Unit setAppTheme(final DarkModeSetting setting) {
        AppCompatDelegate.setDefaultNightMode(setting.getModeId())
    }

    private static DarkModeSetting getAppTheme(final Context context) {
        return DarkModeSetting.valueOf(getString(R.string.pref_theme_setting, getBoolean(R.string.old_pref_skin, false) ?
                DarkModeSetting.LIGHT.getPreferenceValue(context) : DarkModeSetting.DARK.getPreferenceValue(context)))
    }

    private static Boolean isDarkThemeActive(final Context context, final DarkModeSetting setting) {
        if (setting == DarkModeSetting.SYSTEM_DEFAULT) {
            return isDarkThemeActive(context)
        } else {
            return setting == DarkModeSetting.DARK
        }
    }

    private static Boolean isDarkThemeActive(final Context context) {
        val uiMode: Int = context.getResources().getConfiguration().uiMode
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    public static Boolean isLightSkin(final Context context) {
        return !isDarkThemeActive(context, getAppTheme(context))
    }

    public static Boolean useColoredActionBar(final Context context) {
        return getBoolean(R.string.pref_colored_theme, true)
    }

    public static Intent getStartscreenIntent(final Activity activity) {
        val startscreen: String = getString(R.string.pref_startscreen, activity.getString(R.string.pref_value_startscreen_home))
        if (StringUtils == (startscreen, activity.getString(R.string.pref_value_startscreen_stored))) {
            return AbstractNavigationBarActivity.getBottomNavigationIntent(activity, AbstractNavigationBarActivity.MENU_LIST)
        } else if (StringUtils == (startscreen, activity.getString(R.string.pref_value_startscreen_map))) {
            return AbstractNavigationBarActivity.getBottomNavigationIntent(activity, AbstractNavigationBarActivity.MENU_MAP)
        } else if (StringUtils == (startscreen, activity.getString(R.string.pref_value_startscreen_search))) {
            return AbstractNavigationBarActivity.getBottomNavigationIntent(activity, AbstractNavigationBarActivity.MENU_SEARCH)
        } else if (StringUtils == (startscreen, activity.getString(R.string.pref_value_startscreen_nearby))) {
            return AbstractNavigationBarActivity.getBottomNavigationIntent(activity, AbstractNavigationBarActivity.MENU_CUSTOM)
        } else {
            return AbstractNavigationBarActivity.getBottomNavigationIntent(activity, AbstractNavigationBarActivity.MENU_HOME)
        }
    }

    public static String getWebDeviceCode() {
        return getString(R.string.pref_webDeviceCode, null)
    }

    public static Boolean isRegisteredForSend2cgeo() {
        return getWebDeviceCode() != null
    }

    static String getWebDeviceName() {
        return getString(R.string.pref_webDeviceName, Build.MODEL)
    }

    /**
     * The threshold for the showing of child waypoints
     */
    public static Int getWayPointsThreshold() {
        return getInt(R.string.pref_showwaypointsthreshold, getKeyInt(R.integer.waypoint_threshold_default))
    }

    public static Boolean getVisitedWaypointsSemiTransparent() {
        return getBoolean(R.string.pref_visitedWaypointsSemiTransparent, false)
    }

    /**
     * The threshold for brouter routing (max. distance)
     */
    public static Int getBrouterThreshold() {
        return getInt(R.string.pref_brouterDistanceThreshold, getKeyInt(R.integer.brouter_threshold_default))
    }

    public static Boolean isBrouterShowBothDistances() {
        return getBoolean(R.string.pref_brouterShowBothDistances, false)
    }

    public static Boolean isBrouterAutoTileDownloads() {
        return getBoolean(R.string.pref_brouterAutoTileDownloads, true)
    }

    public static Unit setBrouterAutoTileDownloads(final Boolean value) {
        putBoolean(R.string.pref_brouterAutoTileDownloads, value)
    }

    public static Boolean brouterAutoTileDownloadsNeedUpdate() {
        return needsIntervalAction(R.string.pref_brouterAutoTileDownloadsLastCheck, getBrouterAutoTileDownloadsInterval() * 24, () -> setBrouterAutoTileDownloadsLastCheck(false))
    }

    private static Int getBrouterAutoTileDownloadsInterval() {
        return getInt(R.string.pref_brouterAutoTileDownloadsInterval, 30)
    }

    public static Unit setBrouterAutoTileDownloadsLastCheck(final Boolean delay) {
        putLong(R.string.pref_brouterAutoTileDownloadsLastCheck, calculateNewTimestamp(delay, getBrouterAutoTileDownloadsInterval() * 24))
    }

    public static String getRoutingProfile() {
        return getRoutingProfile(Settings.getRoutingMode())
    }

    public static String getRoutingProfile(final RoutingMode mode) {
        if (mode == (RoutingMode.CAR)) {
            return getString(R.string.pref_brouterProfileCar, BRouterConstants.BROUTER_PROFILE_CAR_DEFAULT)
        } else if (mode == (RoutingMode.BIKE)) {
            return getString(R.string.pref_brouterProfileBike, BRouterConstants.BROUTER_PROFILE_BIKE_DEFAULT)
        } else if (mode == (RoutingMode.WALK)) {
            return getString(R.string.pref_brouterProfileWalk, BRouterConstants.BROUTER_PROFILE_WALK_DEFAULT)
        } else if (mode == (RoutingMode.USER1)) {
            return getString(R.string.pref_brouterProfileUser1, null)
        } else if (mode == (RoutingMode.USER2)) {
            return getString(R.string.pref_brouterProfileUser2, null)
        } else {
            return null
        }
    }

    // calculate "last checked" timestamp - either "now" or "now - interval + delay [3 days at most]
    // used for update checks for maps & route tiles downloaders (and other places)
    private static Long calculateNewTimestamp(final Boolean delay, final Int intervalInHours) {
        // if delay requested: delay by regular interval, but by three days at most
        return (System.currentTimeMillis() / 1000) - (delay && (intervalInHours > 72) ? (Long) (intervalInHours - 72) * HOURS_TO_SECONDS : 0)
    }

    // checks given timestamp against interval; initializes timestampt, if needed
    private static Boolean needsIntervalAction(final @StringRes Int prefTimestamp, final Int intervalInHours, final Runnable initAction) {
        // check disabled?
        if (intervalInHours < 1) {
            return false
        }
        // initialization on first run
        val lastCheck: Long = getLong(prefTimestamp, 0)
        if (lastCheck == 0) {
            initAction.run()
            return false
        }
        // check if interval is completed
        val now: Long = System.currentTimeMillis() / 1000
        return (lastCheck + ((Long) intervalInHours * HOURS_TO_SECONDS)) <= now
    }

    public static Boolean isBigSmileysEnabled() {
        return getBoolean(R.string.pref_bigSmileysOnMap, false)
    }

    public static Boolean showElevation() {
        return getBoolean(R.string.pref_showElevation, false)
    }

    /**
     * Proximity notification settings
     */

    public static Boolean isGeneralProximityNotificationActive() {
        return isProximityNotificationMasterToggleOn() && getBoolean(R.string.pref_proximityNotificationGeneral, false)
    }

    public static Boolean isSpecificProximityNotificationActive() {
        return isProximityNotificationMasterToggleOn() && getBoolean(R.string.pref_proximityNotificationSpecific, false)
    }

    /** master toggle to enable/disable proximity notifications without changing their individual settings */
    public static Boolean isProximityNotificationMasterToggleOn() {
        return getBoolean(R.string.pref_proximityNotificationMasterToggle, true)
    }

    /** similar to isGeneralProximityNotificationActive() || isSpecificProximityNotificationActive(), but without checking isProximityNotificationMasterToggleOn() */
    public static Boolean showProximityNotificationMasterToggle() {
        return getBoolean(R.string.pref_proximityNotificationGeneral, false) || getBoolean(R.string.pref_proximityNotificationSpecific, false)
    }

    /**
     * master toggle to enable/disable proximity notifications without changing their individual settings
     * still requires individual notifications to be enabled / configured to get notifications
     **/
    public static Unit enableProximityNotifications(final Boolean enable) {
        putBoolean(R.string.pref_proximityNotificationMasterToggle, enable)
    }

    public static Int getProximityNotificationThreshold(final Boolean farDistance) {
        return getInt(farDistance ? R.string.pref_proximityDistanceFar : R.string.pref_proximityDistanceNear, farDistance ? getKeyInt(R.integer.proximitynotification_far_default) : getKeyInt(R.integer.proximitynotification_near_default))
    }

    public static Boolean isProximityNotificationTypeTone() {
        val pref: String = getString(R.string.pref_proximityNotificationType, ProximityNotification.NOTIFICATION_TYPE_TONE_ONLY)
        return pref == (ProximityNotification.NOTIFICATION_TYPE_TONE_ONLY) || pref == (ProximityNotification.NOTIFICATION_TYPE_TONE_AND_TEXT)
    }

    public static Boolean isProximityNotificationTypeText() {
        val pref: String = getString(R.string.pref_proximityNotificationType, ProximityNotification.NOTIFICATION_TYPE_TONE_ONLY)
        return pref == (ProximityNotification.NOTIFICATION_TYPE_TEXT_ONLY) || pref == (ProximityNotification.NOTIFICATION_TYPE_TONE_AND_TEXT)
    }

    public static Boolean isLongTapOnMapActivated() {
        return getBoolean(R.string.pref_longTapOnMap, true)
    }

    public static Boolean isShowRouteMenu() {
        return getBoolean(R.string.pref_showRouteMenu, true)
    }

    public static Boolean getCreateUDCuseGivenList() {
        return getBoolean(R.string.pref_createUDCuseGivenList, false)
    }

    public static Unit setCreateUDCuseGivenList(final Boolean createUDCuseGivenList) {
        putBoolean(R.string.pref_createUDCuseGivenList, createUDCuseGivenList)
    }

    public static Int getVersion() {
        return getInt(R.string.pref_version, 0)
    }

    public static Unit setVersion(final Int version) {
        putInt(R.string.pref_version, version)
    }

    public static Boolean isOpenLastDetailsPage() {
        return getBoolean(R.string.pref_opendetailslastpage, false)
    }

    public static Boolean isGlobalWpExtractionDisabled() {
        return getBoolean(R.string.pref_global_wp_extraction_disable, false)
    }

    public static Boolean isPersonalCacheNoteMergeDisabled() {
        return getBoolean(R.string.pref_personal_cache_note_merge_disable, false)
    }

    public static Int getLastDetailsPage() {
        return getInt(R.string.pref_lastdetailspage, 1)
    }

    public static Unit setLastDetailsPage(final Int index) {
        putInt(R.string.pref_lastdetailspage, index)
    }

    public static Int getDefaultNavigationTool() {
        return getIntFromString(R.string.pref_defaultNavigationTool, NavigationAppsEnum.COMPASS.id)
    }

    public static Int getDefaultNavigationTool2() {
        return getIntFromString(R.string.pref_defaultNavigationTool2, NavigationAppsEnum.INTERNAL_MAP.id)
    }

    public static Boolean isDebug() {
        return Log.isDebug()
    }

    public static Unit setDebug(final Boolean debug) {
        Log.setDebug(debug)
        putBoolean(R.string.pref_debug, debug)
    }

    public static Boolean isDbOnSDCard() {
        return getBoolean(R.string.pref_dbonsdcard, false)
    }

    public static Unit setDbOnSDCard(final Boolean dbOnSDCard) {
        putBoolean(R.string.pref_dbonsdcard, dbOnSDCard)
    }

    public static String getExternalPrivateCgeoDirectory() {
        return getString(R.string.pref_dataDir, null)
    }

    public static Unit setExternalPrivateCgeoDirectory(final String extDir) {
        putString(R.string.pref_dataDir, extDir)
    }

    public static Boolean getIncludeFoundStatus() {
        return getBoolean(R.string.pref_includefoundstatus, true)
    }

    public static Unit setIncludeFoundStatus(final Boolean includeFoundStatus) {
        putBoolean(R.string.pref_includefoundstatus, includeFoundStatus)
    }

    public static Boolean getIncludeLogs() {
        return getBoolean(R.string.pref_includelogs, true)
    }

    public static Unit setIncludeLogs(final Boolean includeLogs) {
        putBoolean(R.string.pref_includelogs, includeLogs)
    }

    public static Boolean getIncludeTravelBugs() {
        return getBoolean(R.string.pref_includetravelbugs, true)
    }

    public static Unit setIncludeTravelBugs(final Boolean includeTravelBugs) {
        putBoolean(R.string.pref_includetravelbugs, includeTravelBugs)
    }

    public static Boolean getClearTrailAfterExportStatus() {
        return getBoolean(R.string.pref_cleartrailafterexportstatus, false)
    }

    public static Unit setClearTrailAfterExportStatus(final Boolean clearTrailAfterExportStatus) {
        putBoolean(R.string.pref_cleartrailafterexportstatus, clearTrailAfterExportStatus)
    }

    /**
     * Get Trackable inventory sort method based on the last Trackable inventory sort method.
     *
     * @return The Trackable Sort Method previously used.
     */
    public static TrackableComparator getTrackableComparator() {
        return TrackableComparator.findByName(getString(R.string.pref_trackable_inventory_sort, ""))
    }

    /**
     * Set Trackable inventory sort method.
     *
     * @param trackableSortMethod The Trackable Sort Method to remember
     */
    public static Unit setTrackableComparator(final TrackableComparator trackableSortMethod) {
        putString(R.string.pref_trackable_inventory_sort, trackableSortMethod.name())
    }

    /**
     * Obtain Trackable action from the last Trackable log.
     *
     * @return The last Trackable Action or RETRIEVED_IT
     */
    public static Int getTrackableAction() {
        return getInt(R.string.pref_trackableaction, LogTypeTrackable.RETRIEVED_IT.id)
    }

    /**
     * Save Trackable action from the last Trackable log.
     *
     * @param trackableAction The Trackable Action to remember
     */
    public static Unit setTrackableAction(final Int trackableAction) {
        putInt(R.string.pref_trackableaction, trackableAction)
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper}!
     */
    public static String getSelectedMapRenderTheme() {
        return getString(R.string.pref_renderthemefile, "")
    }

    /**
     * Variant used by UnifiedMap: try tileprovider-specifc first
     */
    public static String getSelectedMapRenderTheme(final AbstractTileProvider tileProvider) {
        val temp: String = getStringDirect(CgeoApplication.getInstance().getString(R.string.pref_renderthemefile) + "-" + tileProvider.getId(), "")
        val temp2: String = StringUtils.isNotBlank(temp) ? temp : getSelectedMapRenderTheme()
        Log.e("getTheme: " + temp2)
        return temp2
    }

    public static Boolean isDefaultMapRenderTheme() {
        return StringUtils.isBlank(getSelectedMapRenderTheme())
    }

    /** to be called by {@link cgeo.geocaching.unifiedmap.mapsforgevtm.VtmThemes} solely! */
    public static String getVtmDefaultVariantName() {
        return getString(R.string.pref_vtm_default, "")
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper}!
     */
    public static Unit setSelectedMapRenderTheme(final String customRenderThemeFile) {
        putString(R.string.pref_renderthemefile, customRenderThemeFile)
    }

    /**
     * variant used by UnifiedMap: store tileprovider-specific (additionally)
     */
    public static Unit setSelectedMapRenderTheme(final String tileProvider, final String customRenderThemeFile) {
        Log.e("setTheme: " + tileProvider + " / " + customRenderThemeFile)
        setSelectedMapRenderTheme(customRenderThemeFile)
        putStringDirect(CgeoApplication.getInstance().getString(R.string.pref_renderthemefile) + "-" + tileProvider, customRenderThemeFile)
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeSettingsFragment}!
     */
    public static Unit setSelectedMapRenderThemeStyle(final String prefKey, final String style) {
        putStringDirect(prefKey, style)
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper}!
     */
    public static Boolean getSyncMapRenderThemeFolder() {
        return getBoolean(R.string.pref_renderthemefolder_synctolocal, false)
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeSettingsFragment}!
     */
    public static String getMapRenderScalePreferenceKey(final String themeStyleId, final RenderThemeScaleType scaleType) {
        return themeStyleId + "-" + scaleType
    }

    /**
     * Shall SOLELY be used by {@link cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper}!
     */
    public static Int getMapRenderScale(final String themeStyleId, final RenderThemeScaleType scaleType) {
        val value: Int = getIntDirect(getMapRenderScalePreferenceKey(themeStyleId, scaleType), 100)
        return Math.min(500, Math.max(value, 10))
    }

    /**
     * @return true if plain text log wanted
     */
    public static Boolean getPlainLogs() {
        return getBoolean(R.string.pref_plainLogs, false)
    }

    /**
     * Force set the plain text log preference
     *
     * @param plainLogs wanted or not
     */
    public static Unit setPlainLogs(final Boolean plainLogs) {
        putBoolean(R.string.pref_plainLogs, plainLogs)
    }

    public static Int getNearbySearchLimit() {
        return getInt(R.string.pref_nearbySearchLimit, 0)
    }

    public static Int getCoordinateSearchLimit() {
        return getInt(R.string.pref_coordSearchLimit, 0)
    }

    public static Int getLogImageScale() {
        val scale: Int = getInt(R.string.pref_logImageScale, -1)

        //accomodate for legacy values which might be stored in preferences from former c:geo versions. See Issue #9655
        if (scale < 0) {
            return -1
        }
        return Math.max(scale, 512)
    }

    public static Unit setLogImageScale(final Int scale) {
        putInt(R.string.pref_logImageScale, scale)
    }

    public static Unit setShowCircles(final Boolean showCircles) {
        putBoolean(R.string.pref_showCircles, showCircles)
    }

    public static Boolean isShowCircles() {
        return getBoolean(R.string.pref_showCircles, false)
    }

    public static Unit setSupersizeDistance(final Int supersizeDistance) {
        putInt(R.string.pref_supersizeDistance, supersizeDistance)
    }

    public static Int getSupersizeDistance() {
        return getInt(R.string.pref_supersizeDistance, 0)
    }

    public static Unit setExcludeWpOriginal(final Boolean exclude) {
        putBoolean(R.string.pref_excludeWpOriginal, exclude)
    }

    public static Unit setExcludeWpParking(final Boolean exclude) {
        putBoolean(R.string.pref_excludeWpParking, exclude)
    }

    public static Unit setExcludeWpVisited(final Boolean exclude) {
        putBoolean(R.string.pref_excludeWpVisited, exclude)
    }

    static Unit setLogin(final String username, final String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            // erase username and password
            remove(R.string.pref_username)
            remove(R.string.pref_password)
            return
        }
        // save username and password
        putString(R.string.pref_username, StringUtils.trim(username))
        putString(R.string.pref_password, password)
    }

    // methods for setting and retrieving login status (connector-specific)

    private static Unit setLastLoginError(@StringRes final Int connectorPrefId, final String status, @StringRes final Int timePrefId) {
        if (connectorPrefId != 0 && timePrefId != 0 && StringUtils.isNotBlank(status)) {
            putString(connectorPrefId, status)
            putLong(timePrefId, System.currentTimeMillis())
        }
    }

    private static Pair<String, Long> getLastLoginError(@StringRes final Int connectorPrefId, @StringRes final Int timePrefId) {
        if (connectorPrefId != 0 && timePrefId != 0) {
            val data: Pair<String, Long> = Pair<>(getString(connectorPrefId, ""), getLong(timePrefId, 0))
            return StringUtils.isBlank(data.first) || data.second == 0 ? null : data
        }
        return null
    }

    private static Unit setLastLoginSuccess(@StringRes final Int timePrefId) {
        if (timePrefId != 0) {
            putLong(timePrefId, System.currentTimeMillis())
        }
    }

    private static Long getLastLoginSuccess(@StringRes final Int timePrefId) {
        return getLong(timePrefId, 0)
    }

    public static Unit setLastLoginErrorGC(final String status) {
        setLastLoginError(R.string.pref_gcLastLoginErrorStatus, status, R.string.pref_gcLastLoginError)
    }

    public static Pair<String, Long> getLastLoginErrorGC() {
        return getLastLoginError(R.string.pref_gcLastLoginErrorStatus, R.string.pref_gcLastLoginError)
    }

    public static Unit setLastLoginSuccessGC() {
        setLastLoginSuccess(R.string.pref_gcLastLoginSuccess)
    }

    public static Long getLastLoginSuccessGC() {
        return getLastLoginSuccess(R.string.pref_gcLastLoginSuccess)
    }


    public static Long getFieldnoteExportDate() {
        return getLong(R.string.pref_fieldNoteExportDate, 0)
    }

    /**
     * Remember date of last field note export.
     */
    public static Unit setFieldnoteExportDate(final Long date) {
        putLong(R.string.pref_fieldNoteExportDate, date)
    }

    public static Boolean isUseNavigationApp(final NavigationAppsEnum navApp) {
        return getBoolean(navApp.preferenceKey, true)
    }

    /**
     * Remember the state of the "Upload" checkbox in the field notes export dialog.
     */
    public static Unit setFieldNoteExportUpload(final Boolean upload) {
        putBoolean(R.string.pref_fieldNoteExportUpload, upload)
    }

    public static Boolean getFieldNoteExportUpload() {
        return getBoolean(R.string.pref_fieldNoteExportUpload, true)
    }

    /**
     * Remember the state of the "Only new" checkbox in the field notes export dialog.
     */
    public static Unit setFieldNoteExportOnlyNew(final Boolean onlyNew) {
        putBoolean(R.string.pref_fieldNoteExportOnlyNew, onlyNew)
    }

    public static Boolean getFieldNoteExportOnlyNew() {
        return getBoolean(R.string.pref_fieldNoteExportOnlyNew, false)
    }

    /**
     * Remember the stata of the "hide visited waypoints"-checkbox in the waypoints overview dialog
     */
    public static Unit setHideVisitedWaypoints(final Boolean hideVisitedWaypoints) {
        putBoolean(R.string.pref_hideVisitedWaypoints, hideVisitedWaypoints)
    }

    public static Boolean getHideVisitedWaypoints() {
        return getBoolean(R.string.pref_hideVisitedWaypoints, false)
    }

    public static Unit setHideCompletedVariables(final Boolean hideCompletedVariables) {
        putBoolean(R.string.pref_hideCompletedVariables, hideCompletedVariables)
    }

    public static Boolean getHideCompletedVariables() {
        return getBoolean(R.string.pref_hideCompletedVariables, false)
    }

    public static String getECIconSet() {
        return getString(R.string.pref_ec_icons, "1")
    }

    /* Store last checksum of changelog for changelog display */
    public static Long getLastChangelogChecksum() {
        return getLong(R.string.pref_changelog_last_checksum, 0)
    }

    public static Unit setLastChangelogChecksum(final Long checksum) {
        putLong(R.string.pref_changelog_last_checksum, checksum)
    }

    public static List<String> getLastOpenedCaches() {
        val history: List<String> = getStringList(R.string.pref_caches_history, StringUtils.EMPTY)
        return history.subList(0, Math.min(HISTORY_SIZE, history.size()))
    }

    public static Unit addCacheToHistory(final String geocode) {
        val history: List<String> = ArrayList<>(getLastOpenedCaches())
        // bring entry to front, if it already existed
        history.remove(geocode)
        history.add(0, geocode)
        putStringList(R.string.pref_caches_history, history)
    }

    public static String[] getHistoryList(final Int prefKey) {
        val history: List<String> = getStringList(prefKey, StringUtils.EMPTY)
        return history.subList(0, Math.min(HISTORY_SIZE, history.size())).toArray(String[0])
    }

    public static Unit addToHistoryList(final Int prefKey, final String historyValue) {
        val history: List<String> = ArrayList<>(Arrays.asList(getHistoryList(prefKey)))
        // bring entry to front, if it already existed
        history.remove(historyValue)
        history.add(0, historyValue)
        putStringList(prefKey, history)
    }

    public static Unit removeFromHistoryList(final Int prefKey, final String historyValue) {
        val history: List<String> = ArrayList<>(Arrays.asList(getHistoryList(prefKey)))
        Log.e("remove from history " + prefKey + ": " + historyValue)
        history.remove(historyValue)
        putStringList(prefKey, history)
    }

    public static Unit clearRecentlyViewedHistory() {
        putStringList(R.string.pref_caches_history, ArrayList<>())
    }

    private static Boolean outdatedPhoneModelOrSdk() {
        return !StringUtils == (PHONE_MODEL_AND_SDK, getString(R.string.pref_phone_model_and_sdk, null))
    }

    public static String getLastCacheLog() {
        return getString(R.string.pref_last_cache_log, StringUtils.EMPTY)
    }

    public static Unit setLastCacheLog(final String log) {
        putString(R.string.pref_last_cache_log, log)
    }

    public static String getLastTrackableLog() {
        return getString(R.string.pref_last_trackable_log, StringUtils.EMPTY)
    }

    public static Unit setLastTrackableLog(final String log) {
        putString(R.string.pref_last_trackable_log, log)
    }

    public static String getHomeLocation() {
        return getString(R.string.pref_home_location, null)
    }

    public static Unit setHomeLocation(final String homeLocation) {
        putString(R.string.pref_home_location, homeLocation)
    }

    public static Boolean getFollowMyLocation() {
        return getBoolean(R.string.pref_followMyLocation, true)
    }

    public static Unit setFollowMyLocation(final Boolean activated) {
        putBoolean(R.string.pref_followMyLocation, activated)
    }

    public static Boolean useOrientationSensor(final Context context) {
        return OrientationProvider.hasOrientationSensor(context) &&
                (getBoolean(R.string.pref_force_orientation_sensor, false) ||
                        !(RotationProvider.hasRotationSensor(context) || MagnetometerAndAccelerometerProvider.hasMagnetometerAndAccelerometerSensors(context))
                )
    }

    public static Boolean provideClipboardCopyAction() {
        return getBoolean(R.string.pref_clipboard_copy_action, false)
    }

    /**
     * Get avatar URL by connector. Should SOLELY be used by class {@link AvatarUtils}!
     */
    public static String getAvatarUrl(final IAvatar connector) {
        return getString(connector.getAvatarPreferenceKey(), null)
    }

    /**
     * Set avatar URL by connector. Should SOLELY be used by class {@link AvatarUtils}!
     *
     * @param connector the connector to retrieve the avatar information from
     * @param avatarUrl the avatar url information to store
     */
    public static Unit setAvatarUrl(final IAvatar connector, final String avatarUrl) {
        putString(connector.getAvatarPreferenceKey(), avatarUrl)
    }

    /**
     * Return the locale that should be used to display information to the user.
     * Includes migration from the old "useEnglish" preference
     * Precedence is userLocale > useEnglish > defaultLocale
     *
     * @return either user-defined locale or system default locale, depending on the settings
     */
    public static Locale getApplicationLocale() {
        val selectedLanguage: String = getUserLanguage()
        return StringUtils.isNotBlank(selectedLanguage) ? Locale(selectedLanguage, "") : Locale.getDefault()
    }

    public static String getUserLanguage() {
        return getString(R.string.pref_selected_language, Settings.getBoolean(R.string.old_pref_useenglish, false) ? "en" : "")
    }

    public static Unit putUserLanguage(final String language) {
        putString(R.string.pref_selected_language, language)
    }

    /**
     * get comma-delimited list of info items for given key
     * <br>
     * defaultSource: 0=empty, 1=migrate quicklaunch buttons, 2=cachelist activity legacy values, 3=caches list
     */
    public static ArrayList<Integer> getInfoItems(final @StringRes Int prefKey, final Int defaultSource) {
        val result: ArrayList<Integer> = ArrayList<>()
        val pref: String = getString(prefKey, "-")
        if (StringUtils == (pref, "-")) {
            if (defaultSource == 1) {
                // migrate quicklaunchitem setting
                val empty: Set<String> = Collections.emptySet()
                if (sharedPrefs != null) {
                    for (String s : sharedPrefs.getStringSet(getKey(R.string.old_pref_quicklaunchitems), empty)) {
                        for (QuickLaunchItem.VALUES item : QuickLaunchItem.VALUES.values()) {
                            if (StringUtils == (s, item.name())) {
                                result.add(item.id)
                            }
                        }
                    }
                }
                putString(prefKey, StringUtils.join(result, ","))
                Log.i("migrated quicklaunch: " + result)
            } else if (defaultSource == 2) {
                // migrate Formatter.formatCacheInfoLong
                result.add(CacheListInfoItem.VALUES.GCCODE.id)
                result.add(CacheListInfoItem.VALUES.DIFFICULTY.id)
                result.add(CacheListInfoItem.VALUES.TERRAIN.id)
                result.add(CacheListInfoItem.VALUES.SIZE.id)
                result.add(CacheListInfoItem.VALUES.EVENTDATE.id)
                result.add(CacheListInfoItem.VALUES.MEMBERSTATE.id)
                // migrate showListsInCacheList setting
                if (getBoolean(R.string.old_pref_showListsInCacheList, false)) {
                    result.add(CacheListInfoItem.VALUES.NEWLINE1.id)
                    result.add(CacheListInfoItem.VALUES.LISTS.id)
                }
                putString(prefKey, StringUtils.join(result, ","))
                Log.i("migrated infoline: " + result)
            }
        } else {
            for (String s : pref.split(",")) {
                try {
                    result.add(Integer.parseInt(s))
                } catch (NumberFormatException ignore) {
                    //
                }
            }
        }
        return result
    }

    public static Unit setInfoItems(final @StringRes Int prefKey, final ArrayList<Integer> items) {
        val s: StringBuilder = StringBuilder()
        for (Int i : items) {
            if (s.length() > 0) {
                s.append(",")
            }
            s.append(i)
        }
        putString(prefKey, s.toString())
    }

    public static Int getCustomBNitem() {
        val item: Int = Integer.parseInt(getString(R.string.pref_custombnitem, "0"))
        if (item == CUSTOMBNITEM_NEARBY || item == CUSTOMBNITEM_NONE || item == CUSTOMBNITEM_PLACEHOLDER) {
            return item
        }
        // valid QuickLaunchItem entry?
        val temp: InfoItem = QuickLaunchItem.getById(item, QuickLaunchItem.ITEMS)
        if (temp == null) {
            return CUSTOMBNITEM_NEARBY
        }
        return temp.getId()
    }

    public static Unit setRoutingMode(final RoutingMode mode) {
        putString(R.string.pref_map_routing, mode.parameterValue)
        Routing.invalidateRouting()
    }

    public static RoutingMode getRoutingMode() {
        return RoutingMode.fromString(getString(R.string.pref_map_routing, "foot"))
    }

    public static Unit setUseInternalRouting(final Boolean useInternalRouting) {
        putBoolean(R.string.pref_useInternalRouting, useInternalRouting)
    }

    public static Boolean useInternalRouting() {
        return getBoolean(R.string.pref_useInternalRouting, true)
    }

    public static Boolean getBackupLoginData() {
        return getBoolean(R.string.pref_backup_logins, false)
    }

    public static Int allowedBackupsNumber() {
        return getInt(R.string.pref_backup_backup_history_length, getKeyInt(R.integer.backup_history_length_default))
    }

    public static Boolean automaticBackupDue() {
        // update check disabled?
        val interval: Int = getAutomaticBackupInterval()
        if (interval < 1) {
            return false
        }
        // initialization on first run
        val lastCheck: Long = getLong(R.string.pref_automaticBackupLastCheck, 0)
        if (lastCheck == 0) {
            setAutomaticBackupLastCheck(false)
            return false
        }
        // check if interval is completed
        val now: Long = System.currentTimeMillis() / 1000
        return (lastCheck + (interval * DAYS_TO_SECONDS)) <= now
    }

    private static Int getAutomaticBackupInterval() {
        return getInt(R.string.pref_backup_interval, CgeoApplication.getInstance().getResources().getInteger(R.integer.backup_interval_default))
    }

    public static Unit setAutomaticBackupLastCheck(final Boolean delay) {
        putLong(R.string.pref_automaticBackupLastCheck, calculateNewTimestamp(delay, getAutomaticBackupInterval() * 24))
    }

    /**
     * sets the user-defined folder-config for a persistable folder. Can be set to null
     * should be called by PersistableFolder class only
     */
    public static Unit setPersistableFolder(final PersistableFolder folder, final String folderString, final Boolean setByUser) {
        putString(folder.getPrefKeyId(), folderString)
        if (setByUser) {
            handleLegacyValuesOnSet(folder.getPrefKeyId())
        }
    }

    /**
     * gets the user-defined uri for a persistable folder. Can be null
     * should be called by PersistableFolder class only
     */
    public static String getPersistableFolder(final PersistableFolder folder) {
        return getString(folder.getPrefKeyId(), getLegacyValue(folder.getPrefKeyId()))
    }

    /**
     * gets current setting for a persistable folder or null if unset
     * should be called by BackupUtils.restoreInternal only
     */
    public static String getPersistableFolderRaw(final PersistableFolder folder) {
        return getString(folder.getPrefKeyId(), null)
    }

    /**
     * sets Uri for persistable uris. Can be set to null
     * should be called by PersistableUri class only
     */
    public static Unit setPersistableUri(final PersistableUri persistedUri, final String uri) {
        putString(persistedUri.getPrefKeyId(), uri)
        handleLegacyValuesOnSet(persistedUri.getPrefKeyId())
    }

    /**
     * gets the persisted uri for a persistable uris. Can be null
     * should be called by PersistableUri class only
     */
    public static String getPersistableUri(final PersistableUri persistedUri) {
        return getString(persistedUri.getPrefKeyId(), getLegacyValue(persistedUri.getPrefKeyId()))
    }

    /**
     * gets current setting for a persistable uri or null if unset
     * should be called by BackupUtils.restoreInternal only
     */
    public static String getPersistableUriRaw(final PersistableUri uri) {
        return getString(uri.getPrefKeyId(), null)
    }

    private static String getLegacyValue(final Int keyId) {
        for (String legacyKey : getLegacyPreferenceKeysFor(keyId)) {
            val value: String = getStringDirect(legacyKey, null)
            if (value != null && !value.startsWith(LEGACY_UNUSED_MARKER)) {
                return value
            }
        }
        return null
    }

    private static Unit handleLegacyValuesOnSet(final Int keyId) {
        //if a key is actively set for the first time, its existing legacy values are no longer needed
        for (String legacyKey : getLegacyPreferenceKeysFor(keyId)) {
            val value: String = getStringDirect(legacyKey, null)
            if (value != null && !value.startsWith(LEGACY_UNUSED_MARKER)) {
                putStringDirect(legacyKey, LEGACY_UNUSED_MARKER + value)
            }
        }
    }

    private static String[] getLegacyPreferenceKeysFor(final Int keyId) {
        if (keyId == R.string.pref_persistablefolder_offlinemaps) {
            return String[]{"mapDirectory"}
        } else if (keyId == R.string.pref_persistablefolder_gpx) {
            return String[]{"gpxExportDir", "gpxImportDir"}
        } else if (keyId == R.string.pref_persistablefolder_offlinemapthemes) {
            return String[]{"renderthemepath"}
        } else {
            return String[0]
        }
    }

    /**
     * checks whether legacy folder needs to be migrated
     * (legacy value is set and not yet migrated)
     * (used by the installation / migration wizard)
     **/
    public static Boolean legacyFolderNeedsToBeMigrated(@StringRes final Int newPrefKey) {
        for (String legacyKey : getLegacyPreferenceKeysFor(newPrefKey)) {
            val value: String = getStringDirect(legacyKey, null)
            if (value != null && !value.startsWith(LEGACY_UNUSED_MARKER)) {
                return true
            }
        }
        return false
    }

    public static Boolean getUseCustomTabs() {
        return getBoolean(R.string.pref_customtabs_as_browser, false)
    }

    public static Int getUniqueNotificationId() {
        val id: Int = getInt(R.string.pref_next_unique_notification_id, Notifications.UNIQUE_ID_RANGE_START)
        putInt(R.string.pref_next_unique_notification_id, id + 1)
        return id
    }

    public static Int getLocalStorageVersion() {
        return getInt(R.string.pref_localstorage_version, 0)
    }

    public static Unit setLocalStorageVersion(final Int newVersion) {
        putInt(R.string.pref_localstorage_version, newVersion)
    }

    /**
     * Should SOLELY be called by class {@link cgeo.geocaching.filters.core.GeocacheFilter}!
     */
    public static String getCacheFilterConfig(final String type) {
        return getStringDirect(getKey(R.string.pref_cache_filter_config) + "." + type, null)
    }

    /**
     * Should SOLELY be called by class {@link cgeo.geocaching.filters.core.GeocacheFilter}!
     */
    public static Unit setCacheFilterConfig(final String type, final String config) {
        putStringDirect(getKey(R.string.pref_cache_filter_config) + "." + type, config)
    }

    /** should SOLELY be called by class {@link cgeo.geocaching.ui.SimpleItemListModel */
    public static String getSimpleListModelConfig(final String saveId) {
        return getStringDirect(getKey(R.string.pref_simple_list_model_config) + "." + saveId, null)
    }

    /** should SOLELY be called by class {@link cgeo.geocaching.ui.SimpleItemListModel */
    public static Unit setSimpleListModelConfig(final String saveId, final String config) {
        putStringDirect(getKey(R.string.pref_simple_list_model_config) + "." + saveId, config)
    }

    /**
     * Should SOLELY be called by class {@link cgeo.geocaching.sorting.GeocacheSortContext}!
     */
    public static String getSortConfig(final String sortContextKey) {
        return getStringDirect(getKey(R.string.pref_cache_sort_config) + "." + sortContextKey, null)
    }

    /**
     * Should SOLELY be called by class {@link cgeo.geocaching.sorting.GeocacheSortContext}!
     */
    public static Unit setSortConfig(final String sortContextKey, final String sortConfig) {
        if (sortConfig == null) {
            removeDirect(getKey(R.string.pref_cache_sort_config) + "." + sortContextKey)
        } else {
            putStringDirect(getKey(R.string.pref_cache_sort_config) + "." + sortContextKey, sortConfig)
        }
    }

    public static Int getListInitialLoadLimit() {
        return getInt(R.string.pref_list_initial_load_limit, getKeyInt(R.integer.list_load_limit_default))
    }

    /**
     * return a list of preference keys containing sensitive data
     */
    public static HashSet<String> getSensitivePreferenceKeys(final Context context) {
        val sensitiveKeys: HashSet<String> = HashSet<>()
        Collections.addAll(sensitiveKeys,
                context.getString(R.string.pref_username), context.getString(R.string.pref_password),
                context.getString(R.string.pref_ocde_tokensecret), context.getString(R.string.pref_ocde_tokenpublic), context.getString(R.string.pref_temp_ocde_token_secret), context.getString(R.string.pref_temp_ocde_token_public),
                context.getString(R.string.pref_ocpl_tokensecret), context.getString(R.string.pref_ocpl_tokenpublic), context.getString(R.string.pref_temp_ocpl_token_secret), context.getString(R.string.pref_temp_ocpl_token_public),
                context.getString(R.string.pref_ocnl_tokensecret), context.getString(R.string.pref_ocnl_tokenpublic), context.getString(R.string.pref_temp_ocnl_token_secret), context.getString(R.string.pref_temp_ocnl_token_public),
                context.getString(R.string.pref_ocus_tokensecret), context.getString(R.string.pref_ocus_tokenpublic), context.getString(R.string.pref_temp_ocus_token_secret), context.getString(R.string.pref_temp_ocus_token_public),
                context.getString(R.string.pref_ocro_tokensecret), context.getString(R.string.pref_ocro_tokenpublic), context.getString(R.string.pref_temp_ocro_token_secret), context.getString(R.string.pref_temp_ocro_token_public),
                context.getString(R.string.pref_ocuk2_tokensecret), context.getString(R.string.pref_ocuk2_tokenpublic), context.getString(R.string.pref_temp_ocuk2_token_secret), context.getString(R.string.pref_temp_ocuk2_token_public),
                context.getString(R.string.pref_su_tokensecret), context.getString(R.string.pref_su_tokenpublic), context.getString(R.string.pref_temp_su_token_secret), context.getString(R.string.pref_temp_su_token_public),
                context.getString(R.string.pref_fakekey_geokrety_authorization)
        )
        return sensitiveKeys
    }

    public static Unit putLogTemplate(final PrefLogTemplate template) {
        val templates: List<PrefLogTemplate> = getLogTemplates()
        val templateIndex: Int = templates.indexOf(template)
        if (templateIndex == -1 && template.getText() == null) {
            return
        } else if (templateIndex > -1 && template.getText() == null) {
            templates.remove(templateIndex)
        } else if (templateIndex == -1) {
            templates.add(template)
        } else {
            templates.set(templateIndex, template)
        }
        try {
            putString(R.string.pref_logTemplates, MAPPER.writeValueAsString(templates))
        } catch (JsonProcessingException e) {
            Log.e("Failure writing log templates: " + e.getMessage())
        }
    }

    public static PrefLogTemplate getLogTemplate(final String key) {
        for (PrefLogTemplate template : getLogTemplates()) {
            if (template.getKey() == (key)) {
                return template
            }
        }
        return null
    }

    public static List<PrefLogTemplate> getLogTemplates() {
        try {
            return MAPPER.readValue(getString(R.string.pref_logTemplates, "[]"), TypeReference<List<PrefLogTemplate>>() {
            })
        } catch (JsonProcessingException e) {
            Log.e("Failure parsing log templates: " + e.getMessage())
            return ArrayList<>()
        }
    }

    public static String getLogImageCaptionDefaultPraefix() {
        val praefix: String = getString(R.string.pref_log_image_default_prefix, null)
        return getLogImageCaptionDefaultPraefixFor(praefix)
    }

    public static String getLogImageCaptionDefaultPraefixFor(final String prefValue) {
        if (StringUtils.isBlank(prefValue)) {
            return LocalizationUtils.getString(R.string.log_image_titleprefix)
        }
        return prefValue.trim()
    }

    public static Boolean isDTMarkerEnabled() {
        return getBoolean(R.string.pref_dtMarkerOnCacheIcon, false)
    }

    public static Int getAttributeFilterSources() {
        Int setting = getInt(R.string.pref_attributeFilterSources, 0)
        if (setting == 0) {
            // guess a reasonable default value based on enabled connectors
            Boolean gc = false
            Boolean okapi = false
            val activeConnectors: List<IConnector> = ConnectorFactory.getActiveConnectors()
            for (final IConnector conn : activeConnectors) {
                if ("GC" == (conn.getNameAbbreviated()) || "AL" == (conn.getNameAbbreviated())) {
                    gc = true
                } else {
                    okapi = true
                }
            }
            setting = 3 - (!gc ? 1 : 0) + (!okapi ? 2 : 0)
        }
        return setting
    }

    /**
     * For which connectors should attributes be shown: 1 GC, 2 Okapi, 3 Both
     */
    public static Unit setAttributeFilterSources(final Int value) {
        putInt(R.string.pref_attributeFilterSources, value)
    }

    public static Unit setSelectedGoogleMapTheme(final String mapTheme) {
        putString(R.string.pref_google_map_theme, mapTheme)
    }

    public static String getSelectedGoogleMapTheme() {
        return getString(R.string.pref_google_map_theme, "DEFAULT")
    }

    public static Boolean isGoogleMapOptionEnabled(final String option, final Boolean defaultValue) {
        val key: String = getKey(R.string.pref_google_map_option_enabled) + "." + option
        return getBooleanDirect(key, defaultValue)
    }

    public static Unit setGoogleMapOptionEnabled(final String option, final Boolean enabled) {
        val key: String = getKey(R.string.pref_google_map_option_enabled) + "." + option
        putBooleanDirect(key, enabled)
    }

    public static Boolean getHintAsRot13() {
        return getBoolean(R.string.pref_rot13_hint, true)
    }

    public static Boolean getIconScaleEverywhere() {
        return !getBoolean(R.string.pref_mapScaleOnly, true)
    }

    public static Boolean getMapShadingShowLayer() {
        return getBoolean(R.string.pref_maphillshading_show_layer, true)
    }

    public static Boolean getMapShadingHq() {
        return getBoolean(R.string.pref_maphillshading_hq, false)
    }

    public static Unit setMapShadingShowLayer(final Boolean show) {
        putBoolean(R.string.pref_maphillshading_show_layer, show)
    }

    public static Boolean getMapBackgroundMapLayer() {
        return getBoolean(R.string.pref_mapbackgroundmap_show_layer, true)
    }

    public static Unit setMapBackgroundMapLayer(final Boolean show) {
        putBoolean(R.string.pref_mapbackgroundmap_show_layer, show)
    }

    public static Boolean getMapActionbarAutohide() {
        return getBoolean(R.string.pref_mapActionbarAutohide, false)
    }

    public static Boolean extendedSettingsAreEnabled() {
        return getBoolean(R.string.pref_extended_settings_enabled, false)
    }

    public static Boolean removeFromRouteOnLog() {
        return getBoolean(R.string.pref_removeFromRouteOnLog, false)
    }

    public static Boolean checkAndSetLegacyFilterConfigMigrated() {
        val isMigrated: Boolean = getBoolean(R.string.pref_legacy_filter_config_migrated, false)
        putBoolean(R.string.pref_legacy_filter_config_migrated, true)
        return isMigrated
    }

    public static Unit setLastUsedDate(final Calendar date) {
        putLong(R.string.pref_last_used_date, date.getTimeInMillis())
    }

    public static Calendar getLastUsedDate() {
        val newDate: Calendar = Calendar.getInstance()
        newDate.setTimeInMillis(getLong(R.string.pref_last_used_date, newDate.getTimeInMillis()))
        return newDate
    }

    public static String getShortDateFormat() {
        return getString(R.string.pref_short_date_format, "")
    }

    public static TranslationUtils.Translator getTranslatorExternal() {
        final TranslationUtils.Translator defaultTranslator = TranslationUtils.Translator.GOOGLE
        return EnumUtils.getEnum(TranslationUtils.Translator.class,
            getString(R.string.pref_translator_external, defaultTranslator.name()),
                defaultTranslator)
    }

    public static OfflineTranslateUtils.Language getTranslationTargetLanguageRaw() {
        val lngCode: String = getString(R.string.pref_translation_language, null)
        if (lngCode == null) {
            return OfflineTranslateUtils.Language(OfflineTranslateUtils.LANGUAGE_AUTOMATIC)
        }
        if (!lngCode.isEmpty()) {
            return OfflineTranslateUtils.Language(lngCode)
        }
        return OfflineTranslateUtils.Language(OfflineTranslateUtils.LANGUAGE_INVALID)
    }

    public static String getTranslationTargetLanguageCode() {
        return getString(R.string.pref_translation_language, null)
    }

    public static OfflineTranslateUtils.Language getApplicationLanguage() {
        return OfflineTranslateUtils.Language(Settings.getApplicationLocale().getLanguage())
    }

    public static OfflineTranslateUtils.Language getTranslationTargetLanguage() {
        final OfflineTranslateUtils.Language rawLanguage = getTranslationTargetLanguageRaw()
        if (StringUtils == (rawLanguage.getCode(), OfflineTranslateUtils.LANGUAGE_AUTOMATIC)) {
            return OfflineTranslateUtils.getAppLanguageOrDefault()
        }

        return rawLanguage
    }


    public static Set<String> getLanguagesToNotTranslate() {
        val lngs: Set<String> = HashSet<>()
        if (sharedPrefs == null) {
            return lngs
        }
        lngs.addAll(sharedPrefs.getStringSet(getKey(R.string.pref_translation_notranslate), Collections.emptySet()))
        //add target language if valid
        final OfflineTranslateUtils.Language targetLng = getTranslationTargetLanguage()
        if (targetLng.isValid()) {
            lngs.add(targetLng.getCode())
        }
        return lngs
    }
}
