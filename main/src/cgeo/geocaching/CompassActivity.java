package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.GpsStatusProvider.Status;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.ui.CompassView;
import cgeo.geocaching.ui.LoggingUI;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class CompassActivity extends AbstractActionBarActivity {

    @InjectView(R.id.nav_type) protected TextView navType;
    @InjectView(R.id.nav_accuracy) protected TextView navAccuracy;
    @InjectView(R.id.nav_satellites) protected TextView navSatellites;
    @InjectView(R.id.nav_location) protected TextView navLocation;
    @InjectView(R.id.distance) protected TextView distanceView;
    @InjectView(R.id.heading) protected TextView headingView;
    @InjectView(R.id.rose) protected CompassView compassView;
    @InjectView(R.id.destination) protected TextView destinationTextView;
    @InjectView(R.id.cacheinfo) protected TextView cacheInfoView;

    /**
     * Destination of the compass, or null (if the compass is used for a waypoint only).
     */
    private Geocache cache = null;
    private Geopoint dstCoords = null;
    private float cacheHeading = 0;
    private boolean hasMagneticFieldSensor;
    private String description;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.compass_activity);
        ButterKnife.inject(this);

        final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        hasMagneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null;
        if (!hasMagneticFieldSensor) {
            Settings.setUseCompass(false);
        }

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }

        // cache must exist, except for "any point navigation"
        final String geocode = extras.getString(Intents.EXTRA_GEOCODE);
        if (geocode != null) {
            cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        }

        // find the wanted navigation target
        if (extras.containsKey(Intents.EXTRA_WAYPOINT_ID)) {
            final int waypointId = extras.getInt(Intents.EXTRA_WAYPOINT_ID);
            setTarget(DataStore.loadWaypoint(waypointId));
        }
        else if (extras.containsKey(Intents.EXTRA_COORDS)) {
            final Geopoint coords = extras.getParcelable(Intents.EXTRA_COORDS);
            final String description = extras.getString(Intents.EXTRA_DESCRIPTION);
            setTarget(coords, description);
        }
        else {
            setTarget(cache);
        }

        // set activity title just once, independent of what target is switched to
        if (cache != null) {
            setCacheTitleBar(cache);
        }
        else {
            setTitle(StringUtils.defaultIfBlank(extras.getString(Intents.EXTRA_NAME), res.getString(R.string.navigation)));
        }

        // make sure we can control the TTS volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        super.onResume(geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR),
                app.gpsStatusObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(gpsStatusHandler));
        forceRefresh();
    }

    @Override
    public void onDestroy() {
        compassView.destroyDrawingCache();
        SpeechService.stopService(this);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.compass_activity);
        ButterKnife.inject(this);
        setTarget(dstCoords, description);

        forceRefresh();
    }

    private void forceRefresh() {
        // Force a refresh of location and direction when data is available.
        final CgeoApplication app = CgeoApplication.getInstance();
        final GeoData geo = app.currentGeo();
        geoDirHandler.updateGeoDir(geo, app.currentDirection());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.compass_activity_options, menu);
        menu.findItem(R.id.menu_compass_sensor).setVisible(hasMagneticFieldSensor);
        if (cache != null) {
            LoggingUI.addMenuItems(this, menu, cache);
        }
        addWaypointItems(menu);
        return true;
    }

    private void addWaypointItems(final Menu menu) {
        if (cache != null) {
            final List<Waypoint> waypoints = cache.getWaypoints();
            boolean visible = false;
            final SubMenu subMenu = menu.findItem(R.id.menu_select_destination).getSubMenu();
            for (final Waypoint waypoint : waypoints) {
                if (waypoint.getCoords() != null) {
                    subMenu.add(0, waypoint.getId(), 0, waypoint.getName());
                    visible = true;
                }
            }
            menu.findItem(R.id.menu_select_destination).setVisible(visible);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (Settings.isUseCompass()) {
            menu.findItem(R.id.menu_compass_sensor_magnetic).setChecked(true);
        }
        else {
            menu.findItem(R.id.menu_compass_sensor_gps).setChecked(true);
        }
        menu.findItem(R.id.menu_tts_start).setVisible(!SpeechService.isRunning());
        menu.findItem(R.id.menu_tts_stop).setVisible(SpeechService.isRunning());
        menu.findItem(R.id.menu_compass_cache).setVisible(cache != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_map:
                CGeoMap.startActivityCoords(this, dstCoords, null, null);
                return true;
            case R.id.menu_compass_sensor_gps:
                Settings.setUseCompass(false);
                invalidateOptionsMenuCompatible();
                return true;
            case R.id.menu_compass_sensor_magnetic:
                Settings.setUseCompass(true);
                invalidateOptionsMenuCompatible();
                return true;
            case R.id.menu_tts_start:
                SpeechService.startService(this, dstCoords);
                invalidateOptionsMenuCompatible();
                return true;
            case R.id.menu_tts_stop:
                SpeechService.stopService(this);
                invalidateOptionsMenuCompatible();
                return true;
            case R.id.menu_compass_cache:
                setTarget(cache);
                return true;
            default:
                if (LoggingUI.onMenuItemSelected(item, this, cache)) {
                    return true;
                }
                if (cache != null) {
                    final Waypoint waypoint = cache.getWaypointById(id);
                    if (waypoint != null) {
                        setTarget(waypoint);
                        return true;
                    }
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setTarget(final Geopoint coords, final String description) {
        setDestCoords(coords);
        setTargetDescription(description);
        updateDistanceInfo(app.currentGeo());

        Log.d("destination set: " + description + " (" + dstCoords + ")");
    }

    private void setTarget(final @NonNull Waypoint waypoint) {
        setTarget(waypoint.getCoords(), waypoint.getName());
    }

    private void setTarget(final Geocache cache) {
        setTarget(cache.getCoords(), Formatter.formatCacheInfoShort(cache));
    }

    private void setDestCoords(final Geopoint coords) {
        dstCoords = coords;
        if (dstCoords == null) {
            return;
        }

        destinationTextView.setText(dstCoords.toString());
    }

    private void setTargetDescription(final @Nullable String newDescription) {
        description = newDescription;
        if (description == null) {
            cacheInfoView.setVisibility(View.GONE);
            return;
        }
        cacheInfoView.setVisibility(View.VISIBLE);
        cacheInfoView.setText(description);
    }

    private void updateDistanceInfo(final GeoData geo) {
        if (dstCoords == null) {
            return;
        }

        cacheHeading = geo.getCoords().bearingTo(dstCoords);
        distanceView.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(dstCoords)));
        headingView.setText(Math.round(cacheHeading) + "°");
    }

    private final Action1<Status> gpsStatusHandler = new Action1<Status>() {
        @Override
        public void call(final Status gpsStatus) {
            if (gpsStatus.satellitesVisible >= 0) {
                navSatellites.setText(res.getString(R.string.loc_sat) + ": " + gpsStatus.satellitesFixed + "/" + gpsStatus.satellitesVisible);
            } else {
                navSatellites.setText("");
            }
        }
    };

    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoDir(final GeoData geo, final float dir) {
            try {
                navType.setText(res.getString(geo.getLocationProvider().resourceId));

                if (geo.getAccuracy() >= 0) {
                    navAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()));
                } else {
                    navAccuracy.setText(null);
                }

                navLocation.setText(geo.getCoords().toString());

                updateDistanceInfo(geo);

                updateNorthHeading(AngleUtils.getDirectionNow(dir));
            } catch (final RuntimeException e) {
                Log.w("Failed to update location", e);
            }
        }
    };

    private void updateNorthHeading(final float northHeading) {
        if (compassView != null) {
            compassView.updateNorth(northHeading, cacheHeading);
        }
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
