package cgeo.geocaching;

import butterknife.InjectView;
import butterknife.Views;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.ui.CompassView;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.ui.LoggingUI;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CompassActivity extends AbstractActivity {

    private static final int COORDINATES_OFFSET = 10;

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
    private static final List<IWaypoint> coordinates = new ArrayList<IWaypoint>();

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.compass_activity);

        final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        hasMagneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null;
        if (!hasMagneticFieldSensor) {
            Settings.setUseCompass(false);
        }

        // get parameters
        Bundle extras = getIntent().getExtras();
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
            Intent pointIntent = new Intent(this, NavigateAnyPointActivity.class);
            startActivity(pointIntent);

            finish();
            return;
        }

        // set header
        setTitle();
        setDestCoords();
        setCacheInfo();

        Views.inject(this);

        // set the shortcut to map by clicking on the destination coordinates or cache info at the top of the compass
        final View info = findViewById(R.id.info1);
        info.setClickable(true);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CGeoMap.startActivityCoords(CompassActivity.this, dstCoords, null, title);
            }
        });

        // make sure we can control the TTS volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        super.onResume();

        // sensor and geolocation manager
        geoDirHandler.startGeoAndDir();
    }

    @Override
    public void onPause() {
        geoDirHandler.stopGeoAndDir();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        compassView.destroyDrawingCache();
        SpeechService.stopService(this);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.compass_activity);
        Views.inject(this);

        setTitle();
        setDestCoords();
        setCacheInfo();

        // Force a refresh of location and direction when data is available.
        final CgeoApplication app = CgeoApplication.getInstance();
        final IGeoData geo = app.currentGeo();
        if (geo != null) {
            geoDirHandler.update(geo);
        }
        final Float dir = app.currentDirection();
        if (dir != null) {
            geoDirHandler.update(dir);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.compass_activity_options, menu);
        menu.findItem(R.id.menu_switch_compass_gps).setVisible(hasMagneticFieldSensor);
        final SubMenu subMenu = menu.findItem(R.id.menu_select_destination).getSubMenu();
        if (coordinates.size() > 1) {
            for (int i = 0; i < coordinates.size(); i++) {
                final IWaypoint coordinate = coordinates.get(i);
                subMenu.add(0, COORDINATES_OFFSET + i, 0, coordinate.getName() + " (" + coordinate.getCoordType() + ")");
            }
        } else {
            menu.findItem(R.id.menu_select_destination).setVisible(false);
        }
        if (cache != null) {
            LoggingUI.addMenuItems(this, menu, cache);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_switch_compass_gps).setTitle(res.getString(Settings.isUseCompass() ? R.string.use_gps : R.string.use_compass));
        menu.findItem(R.id.menu_tts_start).setVisible(!SpeechService.isRunning());
        menu.findItem(R.id.menu_tts_stop).setVisible(SpeechService.isRunning());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_map:
                CGeoMap.startActivityCoords(this, dstCoords, null, null);
                return true;
            case R.id.menu_switch_compass_gps:
                boolean oldSetting = Settings.isUseCompass();
                Settings.setUseCompass(!oldSetting);
                invalidateOptionsMenuCompatible();
                if (oldSetting) {
                    geoDirHandler.stopDir();
                } else {
                    geoDirHandler.startDir();
                }
                return true;
            case R.id.menu_edit_destination:
                Intent pointIntent = new Intent(this, NavigateAnyPointActivity.class);
                startActivity(pointIntent);

                finish();
                return true;
            case R.id.menu_tts_start:
                SpeechService.startService(this, dstCoords);
                return true;
            case R.id.menu_tts_stop:
                SpeechService.stopService(this);
                return true;
            default:
                if (LoggingUI.onMenuItemSelected(item, this, cache)) {
                    return true;
                }
                int coordinatesIndex = id - COORDINATES_OFFSET;
                if (coordinatesIndex >= 0 && coordinatesIndex < coordinates.size()) {
                    final IWaypoint coordinate = coordinates.get(coordinatesIndex);
                    title = coordinate.getName();
                    dstCoords = coordinate.getCoords();
                    setTitle();
                    setDestCoords();
                    setCacheInfo();
                    updateDistanceInfo(app.currentGeo());

                    Log.d("destination set: " + title + " (" + dstCoords + ")");
                    return true;
                }
        }
        return false;
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

    private GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoData(final IGeoData geo) {
            try {
                if (geo.getCoords() != null) {
                    if (geo.getSatellitesVisible() >= 0) {
                        navSatellites.setText(res.getString(R.string.loc_sat) + ": " + geo.getSatellitesFixed() + "/" + geo.getSatellitesVisible());
                    } else {
                        navSatellites.setText("");
                    }
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

                if (!Settings.isUseCompass() || geo.getSpeed() > 5) { // use GPS when speed is higher than 18 km/h
                    updateNorthHeading(geo.getBearing());
                }
            } catch (RuntimeException e) {
                Log.w("Failed to LocationUpdater location.");
            }
        }

        @Override
        public void updateDirection(final float direction) {
            if (app.currentGeo().getSpeed() <= 5) { // use compass when speed is lower than 18 km/h
                updateNorthHeading(DirectionProvider.getDirectionNow(CompassActivity.this, direction));
            }
        }
    };

    private void updateNorthHeading(final float northHeading) {
        if (compassView != null) {
            compassView.updateNorth(northHeading, cacheHeading);
        }
    }

    public static void startActivity(final Context context, final String geocode, final String displayedName, final Geopoint coords, final Collection<IWaypoint> coordinatesWithType,
            final String info) {
        coordinates.clear();
        if (coordinatesWithType != null) {
            for (IWaypoint coordinate : coordinatesWithType) {
                if (coordinate != null) {
                    coordinates.add(coordinate);
                }
            }
        }

        final Intent navigateIntent = new Intent(context, CompassActivity.class);
        navigateIntent.putExtra(EXTRAS_COORDS, coords);
        navigateIntent.putExtra(EXTRAS_GEOCODE, geocode);
        if (null != displayedName) {
            navigateIntent.putExtra(EXTRAS_NAME, displayedName);
        }
        navigateIntent.putExtra(EXTRAS_CACHE_INFO, info);
        context.startActivity(navigateIntent);
    }

    public static void startActivity(final Context context, final String geocode, final String displayedName, final Geopoint coords, final Collection<IWaypoint> coordinatesWithType) {
        CompassActivity.startActivity(context, geocode, displayedName, coords, coordinatesWithType, null);
    }

    public static void startActivity(final Context context, final Geocache cache) {
        startActivity(context, cache.getGeocode(), cache.getName(), cache.getCoords(), null,
                Formatter.formatCacheInfoShort(cache));
    }

}
