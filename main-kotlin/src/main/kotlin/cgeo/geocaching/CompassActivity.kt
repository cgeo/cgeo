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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.databinding.CompassActivityBinding
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.ProximityNotificationByCoords
import cgeo.geocaching.location.Units
import cgeo.geocaching.log.LoggingUI
import cgeo.geocaching.maps.DefaultMap
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.permission.PermissionAction
import cgeo.geocaching.permission.PermissionContext
import cgeo.geocaching.sensors.DirectionData
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.sensors.GnssStatusProvider.Status
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.speech.SpeechService
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.TextSpinner
import cgeo.geocaching.ui.ToggleItemType
import cgeo.geocaching.ui.WaypointSelectionActionProvider
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ShareUtils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ToggleButton

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Arrays
import java.util.Locale


import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.functions.Consumer
import org.apache.commons.lang3.StringUtils

class CompassActivity : AbstractActionBarActivity() {

    /**
     * Destination cache, may be null
     */
    private var cache: Geocache = null
    /**
     * Destination waypoint, may be null
     */
    private var waypoint: Waypoint = null
    private var dstCoords: Geopoint = null
    private var cacheHeading: Float = 0
    private String description
    private var proximityNotification: ProximityNotificationByCoords = null
    private val deviceOrientationMode: TextSpinner<DirectionData.DeviceOrientation> = TextSpinner<>()
    private CompassActivityBinding binding

    private val askLocationPermissionAction: PermissionAction<Void> = PermissionAction.register(this, PermissionContext.LOCATION, b -> binding.hint.locationStatus.updatePermissions())

    private static val REQUEST_CODE_LOG: Int = 1001

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setThemeAndContentView(R.layout.compass_activity)
        binding = CompassActivityBinding.bind(getWindow().getDecorView().findViewById(android.R.id.content))

        deviceOrientationMode
                .setValues(Arrays.asList(DirectionData.DeviceOrientation.AUTO, DirectionData.DeviceOrientation.FLAT, DirectionData.DeviceOrientation.UPRIGHT))
                .setDisplayMapperPure(d -> getString(R.string.device_orientation) + ": " + getString(d.resId))
                .setCheckedMapper(d -> d == DirectionData.DeviceOrientation.AUTO)
                .setTextClickThrough(true)
                .setChangeListener(Settings::setDeviceOrientationMode)

        reconfigureGui()

        // get parameters
        val extras: Bundle = getIntent().getExtras()
        if (extras == null) {
            finish()
            return
        }

        // prepare proximity notification
        proximityNotification = Settings.isSpecificProximityNotificationActive() ? ProximityNotificationByCoords() : null
        if (null != proximityNotification) {
            proximityNotification.setTextNotifications(this)
        }

        // cache must exist, except for "any point navigation"
        val geocode: String = extras.getString(Intents.EXTRA_GEOCODE)
        if (geocode != null) {
            cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        }

        // find the wanted navigation target
        if (extras.containsKey(Intents.EXTRA_WAYPOINT_ID)) {
            val waypointId: Int = extras.getInt(Intents.EXTRA_WAYPOINT_ID)
            val waypoint: Waypoint = DataStore.loadWaypoint(waypointId)
            if (waypoint != null) {
                setTarget(waypoint, cache)
            }
        } else if (extras.containsKey(Intents.EXTRA_COORDS)) {
            setTarget(extras.getParcelable(Intents.EXTRA_COORDS), extras.getString(Intents.EXTRA_COORD_DESCRIPTION))
        } else if (cache != null) {
            setTarget(cache)
        } else {
            Log.w("CompassActivity.onCreate: no cache was found for geocode " + geocode)
            finish()
        }

        // set activity title just once, independent of what target is switched to
        if (cache != null) {
            setCacheTitleBar(cache)
        } else {
            setTitle(StringUtils.defaultIfBlank(extras.getString(Intents.EXTRA_NAME), res.getString(R.string.navigation)))
        }

        // make sure we can control the TTS volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC)

        binding.hint.locationStatus.setPermissionRequestCallback(askLocationPermissionAction::launch)
    }

    override     public Unit onResume() {
        super.onResume()

        // resume location access
        resumeDisposables(geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR),
                LocationDataProvider.getInstance().gpsStatusObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(gpsStatusHandler))

        forceRefresh()
    }

    override     public Unit onDestroy() {
        binding.rose.destroyDrawingCache()
        super.onDestroy()
    }

    override     public Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.compass_activity)
        binding = CompassActivityBinding.bind(getWindow().getDecorView().findViewById(android.R.id.content))
        setTarget(dstCoords, description)
        forceRefresh()
    }

    private Unit forceRefresh() {
        reconfigureGui()

        val locationDataProvider: LocationDataProvider = LocationDataProvider.getInstance()
        geoDirHandler.updateGeoDir(locationDataProvider.currentGeo(), locationDataProvider.currentDirection())
    }

    /**
     * refresh GUI elements which can be changed e.g. after a configuration was issued (e.g. turning from portrait into landscape and vice versa
     */
    private Unit reconfigureGui() {
        // Force a refresh of location and direction when data is available.
        val locationDataProvider: LocationDataProvider = LocationDataProvider.getInstance()

        // reset the visibility of the compass toggle button if the device does not support it.
        if (locationDataProvider.hasCompassCapabilities()) {
            binding.useCompass.setChecked(Settings.isUseCompass())
            binding.useCompass.setOnClickListener(view -> {
                Settings.setUseCompass(((ToggleButton) view).isChecked())
                findViewById(R.id.device_orientation_mode).setVisibility(locationDataProvider.hasCompassCapabilities() && binding.useCompass.isChecked() ? View.VISIBLE : View.GONE)
            })
        } else {
            binding.useCompass.setVisibility(View.GONE)
        }
        deviceOrientationMode.setTextView(findViewById(R.id.device_orientation_mode)).set(Settings.getDeviceOrientationMode())
        findViewById(R.id.device_orientation_mode).setVisibility(locationDataProvider.hasCompassCapabilities() && binding.useCompass.isChecked() ? View.VISIBLE : View.GONE)
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.compass_activity_options, menu)
        if (cache != null) {
            LoggingUI.addMenuItems(this, menu, cache)
            initializeTargetActionProvider(menu)
        }
        return true
    }

    private Unit initializeTargetActionProvider(final Menu menu) {
        val destinationMenu: MenuItem = menu.findItem(R.id.menu_select_destination)
        WaypointSelectionActionProvider.initialize(destinationMenu, cache, WaypointSelectionActionProvider.Callback() {

            override             public Unit onWaypointSelected(final Waypoint waypoint) {
                setTarget(waypoint, null)
            }

            override             public Unit onGeocacheSelected(final Geocache geocache) {
                setTarget(geocache)
            }
        })
    }

    override     public Boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_hint).setVisible(cache != null && StringUtils.isNotEmpty(cache.getHint()))

        val ttsMenuItem: MenuItem = menu.findItem(R.id.menu_tts_toggle)
        ttsMenuItem.setVisible(cache != null)
        ToggleItemType.TOGGLE_SPEECH.toggleMenuItem(ttsMenuItem, SpeechService.isRunning())

        return true
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val id: Int = item.getItemId()
        if (id == R.id.menu_map) {
            if (waypoint != null) {
                DefaultMap.startActivityCoords(this, waypoint)
            } else if (cache != null) {
                DefaultMap.startActivityGeoCode(this, cache.getGeocode())
            } else {
                DefaultMap.startActivityCoords(this, dstCoords)
            }
        } else if (id == R.id.menu_tts_toggle) {
            SpeechService.toggleService(this, dstCoords)
            ToggleItemType.TOGGLE_SPEECH.toggleMenuItem(item, SpeechService.isRunning())
        } else if (id == R.id.menu_hint) {
            if (binding.hint.offlineHintText.getVisibility() == View.VISIBLE) {
                binding.hint.offlineHintSeparator1.setVisibility(View.GONE)
                binding.hint.offlineHintSeparator2.setVisibility(View.GONE)
                binding.hint.offlineHintText.setVisibility(View.GONE)
            } else {
                binding.hint.offlineHintSeparator1.setVisibility(View.VISIBLE)
                binding.hint.offlineHintSeparator2.setVisibility(View.VISIBLE)
                binding.hint.offlineHintText.setVisibility(View.VISIBLE)
                binding.hint.offlineHintText.setText(cache.getHint())
            }
        } else if (LoggingUI.onMenuItemSelected(item, this, cache, null)) {
            return true; // to satisfy static code analysis
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_LOG && resultCode == Activity.RESULT_OK && data != null) {
            ShareUtils.showLogPostedSnackbar(this, data, findViewById(R.id.location_status))
        }
    }



    private Unit setTarget(final Geopoint coords, final String newDescription) {
        setDestCoords(coords)
        setTargetDescription(newDescription)
        updateDistanceInfo(LocationDataProvider.getInstance().currentGeo())

        Log.d("destination set: " + newDescription + " (" + dstCoords + ")")
    }

    private Unit setTarget(final Waypoint waypointIn, final Geocache cache) {
        waypoint = waypointIn
        val coordinates: Geopoint = waypointIn.getCoords()
        if (coordinates != null) { // handled by WaypointSelectionActionProvider, but the compiler doesn't know
            setTarget(coordinates, waypointIn.getName() + (null != cache ? Formatter.SEPARATOR + Formatter.formatCacheInfoShort(cache) : ""))
        }
    }

    private Unit setTarget(final Geocache cache) {
        setTarget(cache.getCoords(), Formatter.formatCacheInfoShort(cache))
    }

    private Unit setDestCoords(final Geopoint coords) {
        dstCoords = coords
        if (null != proximityNotification) {
            proximityNotification.setReferencePoint(coords)
        }
        if (dstCoords == null) {
            return
        }

        binding.destination.setText(dstCoords.toString())
    }

    private Unit setTargetDescription(final String newDescription) {
        description = newDescription
        if (description == null) {
            binding.cacheinfo.setVisibility(View.GONE)
            return
        }
        binding.cacheinfo.setVisibility(View.VISIBLE)
        binding.cacheinfo.setText(description)
    }

    @SuppressLint("SetTextI18n")
    private Unit updateDistanceInfo(final GeoData geo) {
        if (dstCoords == null) {
            return
        }

        cacheHeading = geo.getCoords().bearingTo(dstCoords)
        binding.distance.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(dstCoords)))
        binding.heading.setText(Math.round(cacheHeading) + "°")
    }

    @SuppressLint("SetTextI18n")
    private val gpsStatusHandler: Consumer<Status> = Consumer<Status>() {
        override         public Unit accept(final Status gpsStatus) {
            if (binding == null || binding.hint.locationStatus == null) {
                //activity is not initialized yet, ignore update
                return
            }
            binding.hint.locationStatus.updateSatelliteStatus(gpsStatus)
        }
    }

    @SuppressLint("SetTextI18n")
    private val geoDirHandler: GeoDirHandler = GeoDirHandler() {
        override         public Unit updateGeoDirData(final GeoData geo, final DirectionData dir) {
            if (binding == null || binding.hint.locationStatus == null) {
                //activity is not initialized yet, ignore update
                return
            }
            try {
                binding.hint.locationStatus.updateGeoData(geo)

                updateDistanceInfo(geo)

                updateNorthHeading(dir)

                updateDeviceHeadingAndOrientation(dir)

                if (null != proximityNotification) {
                    proximityNotification.onUpdateGeoData(geo)
                }
            } catch (final RuntimeException e) {
                Log.w("Failed to update location", e)
            }
        }
    }

    private Unit updateNorthHeading(final DirectionData dir) {
        if (binding.rose != null) {
            binding.rose.updateNorth(dir.getDeviceOrientation() == DirectionData.DeviceOrientation.UPRIGHT ?
                    dir.getDirection() : AngleUtils.getDirectionNow(dir.getDirection()), cacheHeading)
        }
    }

    @SuppressLint("SetTextI18n")
    private Unit updateDeviceHeadingAndOrientation(final DirectionData dir) {

        if (dir.hasOrientation()) {
            binding.deviceOrientationAzimuth.setText(formatDecimalFloat(dir.getAzimuth()) + "° /")
            binding.deviceOrientationPitch.setText(formatDecimalFloat(dir.getPitch()) + "° /")
            binding.deviceOrientationRoll.setText(formatDecimalFloat(dir.getRoll()).substring(1) + "°")

            binding.deviceOrientationLabel.setVisibility(View.VISIBLE)
            binding.deviceOrientationAzimuth.setVisibility(View.VISIBLE)
            binding.deviceOrientationPitch.setVisibility(View.VISIBLE)
            binding.deviceOrientationRoll.setVisibility(View.VISIBLE)
        } else {
            binding.deviceOrientationLabel.setVisibility(View.INVISIBLE)
            binding.deviceOrientationAzimuth.setVisibility(View.INVISIBLE)
            binding.deviceOrientationPitch.setVisibility(View.INVISIBLE)
            binding.deviceOrientationRoll.setVisibility(View.INVISIBLE)
        }

        Float direction = dir.getDirection()
        while (direction < 0f) {
            direction += 360f
        }
        while (direction >= 360f) {
            direction -= 360f
        }
        binding.deviceHeading.setText(String.format(Locale.getDefault(), "%3.1f°", direction))

        if (deviceOrientationMode.get() == DirectionData.DeviceOrientation.AUTO) {
            deviceOrientationMode.setTextDisplayMapperPure(d -> getString(R.string.device_orientation) + ": " + getString(dir.getDeviceOrientation().resId) + " (" + getString(DirectionData.DeviceOrientation.AUTO.resId) + ")")
        } else {
            deviceOrientationMode.setTextDisplayMapperPure(d -> getString(R.string.device_orientation) + ": " + getString(d.resId))
        }
    }

    /**
     * formats a Float to a decimal with length 4 and no places behind comma. Handles "-0" case.
     */
    private static String formatDecimalFloat(final Float value) {
        val formattedValue: String = String.format(Locale.US, "% 4.0f", value)
        if (formattedValue.endsWith("-0")) {
            return "   0"
        }
        return formattedValue
    }

    public static Unit startActivityWaypoint(final Context context, final Waypoint waypoint) {
        val navigateIntent: Intent = Intent(context, CompassActivity.class)
        navigateIntent.putExtra(Intents.EXTRA_GEOCODE, waypoint.getGeocode())
        navigateIntent.putExtra(Intents.EXTRA_WAYPOINT_ID, waypoint.getId())
        context.startActivity(navigateIntent)
    }

    public static Unit startActivityPoint(final Context context, final Geopoint coords, final String displayedName) {
        val navigateIntent: Intent = Intent(context, CompassActivity.class)
        navigateIntent.putExtra(Intents.EXTRA_COORDS, coords)
        navigateIntent.putExtra(Intents.EXTRA_NAME, displayedName)
        context.startActivity(navigateIntent)
    }

    public static Unit startActivityCache(final Context context, final Geocache cache) {
        val navigateIntent: Intent = Intent(context, CompassActivity.class)
        navigateIntent.putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode())
        context.startActivity(navigateIntent)
    }

}
