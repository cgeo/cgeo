package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.GpsStatusProvider.Status;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.ui.CompassView;
import cgeo.geocaching.ui.LoggingUI;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
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

    private static final String EXTRAS_COORDS = "coords";
    private static final String EXTRAS_NAME = "name";
    private static final String EXTRAS_GEOCODE = "geocode";
    private static final String EXTRAS_CACHE_INFO = "cacheinfo";

    /**
     * Destination of the compass, or null (if the compass is used for a waypoint only).
     */
    private @Nullable Geocache cache = null;
    private Geopoint dstCoords = null;
    private float cacheHeading = 0;
    private String title = null;
    private String info = null;
    private boolean hasMagneticFieldSensor;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.compass_activity);

        final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        hasMagneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null;
        if (!hasMagneticFieldSensor) {
            Settings.setUseCompass(false);
        }

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final String geocode = extras.getString(EXTRAS_GEOCODE);
            if (StringUtils.isNotEmpty(geocode)) {
                cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            }
            title = geocode;
            final String name = extras.getString(EXTRAS_NAME);
            dstCoords = extras.getParcelable(EXTRAS_COORDS);
            info = extras.getString(EXTRAS_CACHE_INFO);

            if (StringUtils.isNotBlank(name)) {
                if (StringUtils.isNotBlank(title)) {
                    title += ": " + name;
                } else {
                    title = name;
                }
            }
        } else {
            final Intent pointIntent = new Intent(this, NavigateAnyPointActivity.class);
            startActivity(pointIntent);

            finish();
            return;
        }

        // set header
        setTitle();
        setDestCoords();
        setCacheInfo();

        ButterKnife.inject(this);

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

        setTitle();
        setDestCoords();
        setCacheInfo();
        forceRefresh();
    }

    private void forceRefresh() {
        // Force a refresh of location and direction when data is available.
        final CgeoApplication app = CgeoApplication.getInstance();
        final IGeoData geo = app.currentGeo();
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
            default:
                if (LoggingUI.onMenuItemSelected(item, this, cache)) {
                    return true;
                }
                if (cache != null) {
                    final Waypoint waypoint = cache.getWaypointById(id);
                    if (waypoint != null) {
                        title = waypoint.getName();
                        dstCoords = waypoint.getCoords();
                        setTitle();
                        setDestCoords();
                        setCacheInfo();
                        updateDistanceInfo(app.currentGeo());

                        Log.d("destination set: " + title + " (" + dstCoords + ")");
                        return true;
                    }
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setTitle() {
        if (StringUtils.isNotBlank(title)) {
            setTitle(title);
        } else {
            setTitle(res.getString(R.string.navigation));
        }
    }

    private void setDestCoords() {
        if (dstCoords == null) {
            return;
        }

        destinationTextView.setText(dstCoords.toString());
    }

    private void setCacheInfo() {
        if (info == null) {
            cacheInfoView.setVisibility(View.GONE);
            return;
        }
        cacheInfoView.setVisibility(View.VISIBLE);
        cacheInfoView.setText(info);
    }

    private void updateDistanceInfo(final IGeoData geo) {
        if (geo.getCoords() == null || dstCoords == null) {
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
        public void updateGeoDir(final IGeoData geo, final float dir) {
            try {
                if (geo.getCoords() != null) {
                    navType.setText(res.getString(geo.getLocationProvider().resourceId));

                    if (geo.getAccuracy() >= 0) {
                        navAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()));
                    } else {
                        navAccuracy.setText(null);
                    }

                    navLocation.setText(geo.getCoords().toString());

                    updateDistanceInfo(geo);
                } else {
                    navType.setText(null);
                    navAccuracy.setText(null);
                    navLocation.setText(res.getString(R.string.loc_trying));
                }

                updateNorthHeading(AngleUtils.getDirectionNow(dir));
            } catch (final RuntimeException e) {
                Log.w("Failed to LocationUpdater location.");
            }
        }
    };

    private void updateNorthHeading(final float northHeading) {
        if (compassView != null) {
            compassView.updateNorth(northHeading, cacheHeading);
        }
    }

    public static void startActivity(final Context context, final String geocode, final String displayedName, final Geopoint coords,
            final String info) {
        final Intent navigateIntent = new Intent(context, CompassActivity.class);
        navigateIntent.putExtra(EXTRAS_COORDS, coords);
        navigateIntent.putExtra(EXTRAS_GEOCODE, geocode);
        if (null != displayedName) {
            navigateIntent.putExtra(EXTRAS_NAME, displayedName);
        }
        navigateIntent.putExtra(EXTRAS_CACHE_INFO, info);
        context.startActivity(navigateIntent);
    }

    public static void startActivity(final Context context, final String geocode, final String displayedName, final Geopoint coords) {
        startActivity(context, geocode, displayedName, coords, null);
    }

    public static void startActivityCache(final Context context, final Geocache cache) {
        startActivity(context, cache.getGeocode(), cache.getName(), cache.getCoords(),
                Formatter.formatCacheInfoShort(cache));
    }

}
