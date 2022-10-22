package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.CompassActivityBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.ProximityNotificationByCoords;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.LoggingUI;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.DirectionData;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.GnssStatusProvider.Status;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.WaypointSelectionActionProvider;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.GeoHeightUtils;
import cgeo.geocaching.utils.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;
import org.apache.commons.lang3.StringUtils;

public class CompassActivity extends AbstractActionBarActivity {

    /**
     * Destination cache, may be null
     */
    private Geocache cache = null;
    /**
     * Destination waypoint, may be null
     */
    private Waypoint waypoint = null;
    private Geopoint dstCoords = null;
    private float cacheHeading = 0;
    private String description;
    private ProximityNotificationByCoords proximityNotification = null;
    private final TextSpinner<DirectionData.DeviceOrientation> deviceOrientationMode = new TextSpinner<>();
    private CompassActivityBinding binding;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.compass_activity);
        binding = CompassActivityBinding.bind(getWindow().getDecorView().findViewById(android.R.id.content));

        deviceOrientationMode
                .setValues(Arrays.asList(new DirectionData.DeviceOrientation[]{DirectionData.DeviceOrientation.AUTO, DirectionData.DeviceOrientation.FLAT, DirectionData.DeviceOrientation.UPRIGHT}))
                .setDisplayMapper(d -> getString(R.string.device_orientation) + ": " + getString(d.resId))
                .setCheckedMapper(d -> d == DirectionData.DeviceOrientation.AUTO)
                .setTextClickThrough(true)
                .setChangeListener(Settings::setDeviceOrientationMode);

        reconfigureGui();

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }

        // prepare proximity notification
        proximityNotification = Settings.isSpecificProximityNotificationActive() ? new ProximityNotificationByCoords() : null;
        if (null != proximityNotification) {
            proximityNotification.setTextNotifications(this);
        }

        // cache must exist, except for "any point navigation"
        final String geocode = extras.getString(Intents.EXTRA_GEOCODE);
        if (geocode != null) {
            cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        }

        // find the wanted navigation target
        if (extras.containsKey(Intents.EXTRA_WAYPOINT_ID)) {
            final int waypointId = extras.getInt(Intents.EXTRA_WAYPOINT_ID);
            final Waypoint waypoint = DataStore.loadWaypoint(waypointId);
            if (waypoint != null) {
                setTarget(waypoint, cache);
            }
        } else if (extras.containsKey(Intents.EXTRA_COORDS)) {
            setTarget(extras.getParcelable(Intents.EXTRA_COORDS), extras.getString(Intents.EXTRA_COORD_DESCRIPTION));
        } else if (cache != null) {
            setTarget(cache);
        } else {
            Log.w("CompassActivity.onCreate: no cache was found for geocode " + geocode);
            finish();
        }

        // set activity title just once, independent of what target is switched to
        if (cache != null) {
            setCacheTitleBar(cache);
        } else {
            setTitle(StringUtils.defaultIfBlank(extras.getString(Intents.EXTRA_NAME), res.getString(R.string.navigation)));
        }

        // make sure we can control the TTS volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        super.onResume();

        // resume location access
        PermissionHandler.executeIfLocationPermissionGranted(this,
                new RestartLocationPermissionGrantedCallback(PermissionRequestContext.CompassActivity) {

                    @Override
                    public void executeAfter() {
                        resumeDisposables(geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR),
                                Sensors.getInstance().gpsStatusObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(gpsStatusHandler));
                    }
                });

        forceRefresh();
    }

    @Override
    public void onDestroy() {
        binding.rose.destroyDrawingCache();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.compass_activity);
        binding = CompassActivityBinding.bind(getWindow().getDecorView().findViewById(android.R.id.content));
        setTarget(dstCoords, description);
        forceRefresh();
    }

    private void forceRefresh() {
        reconfigureGui();

        final Sensors sensors = Sensors.getInstance();
        geoDirHandler.updateGeoDir(sensors.currentGeo(), sensors.currentDirection());
    }

    /**
     * refresh GUI elements which can be changed e.g. after a new configuration was issued (e.g. turning from portrait into landscape and vice versa
     */
    private void reconfigureGui() {
        // Force a refresh of location and direction when data is available.
        final Sensors sensors = Sensors.getInstance();

        // reset the visibility of the compass toggle button if the device does not support it.
        if (sensors.hasCompassCapabilities()) {
            binding.useCompass.setChecked(Settings.isUseCompass());
            binding.useCompass.setOnClickListener(view -> {
                Settings.setUseCompass(((ToggleButton) view).isChecked());
                findViewById(R.id.device_orientation_mode).setVisibility(sensors.hasCompassCapabilities() && binding.useCompass.isChecked() ? View.VISIBLE : View.GONE);
            });
        } else {
            binding.useCompass.setVisibility(View.GONE);
        }
        deviceOrientationMode.setTextView(findViewById(R.id.device_orientation_mode)).set(Settings.getDeviceOrientationMode());
        findViewById(R.id.device_orientation_mode).setVisibility(sensors.hasCompassCapabilities() && binding.useCompass.isChecked() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.compass_activity_options, menu);
        if (cache != null) {
            LoggingUI.addMenuItems(this, menu, cache);
            initializeTargetActionProvider(menu);
        }
        return true;
    }

    private void initializeTargetActionProvider(final Menu menu) {
        final MenuItem destinationMenu = menu.findItem(R.id.menu_select_destination);
        WaypointSelectionActionProvider.initialize(destinationMenu, cache, new WaypointSelectionActionProvider.Callback() {

            @Override
            public void onWaypointSelected(final Waypoint waypoint) {
                setTarget(waypoint, null);
            }

            @Override
            public void onGeocacheSelected(final Geocache geocache) {
                setTarget(geocache);
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_hint).setVisible(cache != null && StringUtils.isNotEmpty(cache.getHint()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_map) {
            if (waypoint != null) {
                DefaultMap.startActivityCoords(this, waypoint);
            } else if (cache != null) {
                DefaultMap.startActivityGeoCode(this, cache.getGeocode());
            } else {
                DefaultMap.startActivityCoords(this, dstCoords);
            }
        } else if (id == R.id.menu_tts_toggle) {
            SpeechService.toggleService(this, dstCoords);
        } else if (id == R.id.menu_hint) {
            cache.showHintToast(this);
        } else if (LoggingUI.onMenuItemSelected(item, this, cache, null)) {
            return true; // to satisfy static code analysis
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void setTarget(@NonNull final Geopoint coords, final String newDescription) {
        setDestCoords(coords);
        setTargetDescription(newDescription);
        updateDistanceInfo(Sensors.getInstance().currentGeo());

        Log.d("destination set: " + newDescription + " (" + dstCoords + ")");
    }

    private void setTarget(@NonNull final Waypoint waypointIn, @Nullable final Geocache cache) {
        waypoint = waypointIn;
        final Geopoint coordinates = waypointIn.getCoords();
        if (coordinates != null) { // handled by WaypointSelectionActionProvider, but the compiler doesn't know
            setTarget(coordinates, waypointIn.getName() + (null != cache ? Formatter.SEPARATOR + Formatter.formatCacheInfoShort(cache) : ""));
        }
    }

    private void setTarget(@NonNull final Geocache cache) {
        setTarget(cache.getCoords(), Formatter.formatCacheInfoShort(cache));
    }

    private void setDestCoords(final Geopoint coords) {
        dstCoords = coords;
        if (null != proximityNotification) {
            proximityNotification.setReferencePoint(coords);
        }
        if (dstCoords == null) {
            return;
        }

        binding.destination.setText(dstCoords.toString());
    }

    private void setTargetDescription(@Nullable final String newDescription) {
        description = newDescription;
        if (description == null) {
            binding.cacheinfo.setVisibility(View.GONE);
            return;
        }
        binding.cacheinfo.setVisibility(View.VISIBLE);
        binding.cacheinfo.setText(description);
    }

    @SuppressLint("SetTextI18n")
    private void updateDistanceInfo(final GeoData geo) {
        if (dstCoords == null) {
            return;
        }

        cacheHeading = geo.getCoords().bearingTo(dstCoords);
        binding.distance.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(dstCoords)));
        binding.heading.setText(Math.round(cacheHeading) + "°");
    }

    @SuppressLint("SetTextI18n")
    private final Consumer<Status> gpsStatusHandler = new Consumer<Status>() {
        @Override
        public void accept(final Status gpsStatus) {
            if (gpsStatus.satellitesVisible >= 0) {
                binding.navSatellites.setText(res.getString(R.string.loc_sat) + ": " + gpsStatus.satellitesFixed + "/" + gpsStatus.satellitesVisible);
            } else {
                binding.navSatellites.setText("");
            }
        }
    };

    @SuppressLint("SetTextI18n")
    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoDirData(@NonNull final GeoData geo, final DirectionData dir) {
            try {
                binding.navType.setText(res.getString(geo.getLocationProvider().resourceId));

                if (geo.getAccuracy() >= 0) {
                    binding.navAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()));
                } else {
                    binding.navAccuracy.setText(null);
                }

                binding.navLocation.setText(geo.getCoords() + GeoHeightUtils.getAverageHeight(geo, true, false));

                updateDistanceInfo(geo);

                updateNorthHeading(dir);

                updateDeviceHeadingAndOrientation(dir);

                if (null != proximityNotification) {
                    proximityNotification.onUpdateGeoData(geo);
                }
            } catch (final RuntimeException e) {
                Log.w("Failed to update location", e);
            }
        }
    };

    private void updateNorthHeading(final DirectionData dir) {
        if (binding.rose != null) {
            binding.rose.updateNorth(dir.getDeviceOrientation() == DirectionData.DeviceOrientation.UPRIGHT ?
                    dir.getDirection() : AngleUtils.getDirectionNow(dir.getDirection()), cacheHeading);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateDeviceHeadingAndOrientation(final DirectionData dir) {

        if (dir.hasOrientation()) {
            binding.deviceOrientationAzimuth.setText(formatDecimalFloat(dir.getAzimuth()) + "° /");
            binding.deviceOrientationPitch.setText(formatDecimalFloat(dir.getPitch()) + "° /");
            binding.deviceOrientationRoll.setText(formatDecimalFloat(dir.getRoll()).substring(1) + "°");

            binding.deviceOrientationLabel.setVisibility(View.VISIBLE);
            binding.deviceOrientationAzimuth.setVisibility(View.VISIBLE);
            binding.deviceOrientationPitch.setVisibility(View.VISIBLE);
            binding.deviceOrientationRoll.setVisibility(View.VISIBLE);
        } else {
            binding.deviceOrientationLabel.setVisibility(View.INVISIBLE);
            binding.deviceOrientationAzimuth.setVisibility(View.INVISIBLE);
            binding.deviceOrientationPitch.setVisibility(View.INVISIBLE);
            binding.deviceOrientationRoll.setVisibility(View.INVISIBLE);
        }

        float direction = dir.getDirection();
        while (direction < 0f) {
            direction += 360f;
        }
        while (direction >= 360f) {
            direction -= 360f;
        }
        binding.deviceHeading.setText(String.format(Locale.getDefault(), "%3.1f°", direction));

        if (deviceOrientationMode.get() == DirectionData.DeviceOrientation.AUTO) {
            deviceOrientationMode.setTextDisplayMapper(d -> getString(R.string.device_orientation) + ": " + getString(dir.getDeviceOrientation().resId) + " (" + getString(DirectionData.DeviceOrientation.AUTO.resId) + ")");
        } else {
            deviceOrientationMode.setTextDisplayMapper(d -> getString(R.string.device_orientation) + ": " + getString(d.resId));
        }
    }

    /**
     * formats a float to a decimal with length 4 and no places behind comma. Handles "-0" case.
     */
    private static String formatDecimalFloat(final float value) {
        final String formattedValue = String.format(Locale.US, "% 4.0f", value);
        if (formattedValue.endsWith("-0")) {
            return "   0";
        }
        return formattedValue;
    }

    public static void startActivityWaypoint(final Context context, final Waypoint waypoint) {
        final Intent navigateIntent = new Intent(context, CompassActivity.class);
        navigateIntent.putExtra(Intents.EXTRA_GEOCODE, waypoint.getGeocode());
        navigateIntent.putExtra(Intents.EXTRA_WAYPOINT_ID, waypoint.getId());
        context.startActivity(navigateIntent);
    }

    public static void startActivityPoint(final Context context, final Geopoint coords, final String displayedName) {
        final Intent navigateIntent = new Intent(context, CompassActivity.class);
        navigateIntent.putExtra(Intents.EXTRA_COORDS, coords);
        navigateIntent.putExtra(Intents.EXTRA_NAME, displayedName);
        context.startActivity(navigateIntent);
    }

    public static void startActivityCache(final Context context, final Geocache cache) {
        final Intent navigateIntent = new Intent(context, CompassActivity.class);
        navigateIntent.putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode());
        context.startActivity(navigateIntent);
    }

}
