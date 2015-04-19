package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ShowcaseViewBuilder;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.GpsStatusProvider.Status;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.ui.CompassView;
import cgeo.geocaching.ui.LoggingUI;
import cgeo.geocaching.ui.WaypointSelectionActionProvider;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import com.github.amlcurran.showcaseview.targets.ActionItemTarget;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

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
    @InjectView(R.id.use_compass) protected ToggleButton useCompassSwitch;

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

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        onCreate(savedInstanceState, R.layout.compass_activity);
        ButterKnife.inject(this);

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
            final Waypoint waypoint = DataStore.loadWaypoint(waypointId);
            if (waypoint != null) {
                setTarget(waypoint);
            }
        }
        else if (extras.containsKey(Intents.EXTRA_COORDS)) {
            setTarget(extras.<Geopoint> getParcelable(Intents.EXTRA_COORDS), extras.getString(Intents.EXTRA_DESCRIPTION));
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

        if (Sensors.getInstance().hasCompassCapabilities()) {
            useCompassSwitch.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View view) {
                    Settings.setUseCompass(((ToggleButton) view).isChecked());
                }
            });
            useCompassSwitch.setVisibility(View.VISIBLE);
        } else {
            useCompassSwitch.setVisibility(View.GONE);
        }

        // make sure we can control the TTS volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        presentShowcase();
    }

    @Override
    public void onResume() {
        onResume(geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR),
                Sensors.getInstance().gpsStatusObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(gpsStatusHandler));
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
        useCompassSwitch.setChecked(Settings.isUseCompass());
        // Force a refresh of location and direction when data is available.
        final Sensors sensors = Sensors.getInstance();
        geoDirHandler.updateGeoDir(sensors.currentGeo(), sensors.currentDirection());
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
                setTarget(waypoint);
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
        menu.findItem(R.id.menu_tts_start).setVisible(!SpeechService.isRunning());
        menu.findItem(R.id.menu_tts_stop).setVisible(SpeechService.isRunning());
        menu.findItem(R.id.menu_hint).setVisible(cache != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_map:
                if (waypoint != null) {
                    CGeoMap.startActivityCoords(this, waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getName());
                }
                else if (cache != null) {
                    CGeoMap.startActivityGeoCode(this, cache.getGeocode());
                }
                else {
                    CGeoMap.startActivityCoords(this, dstCoords, null, null);
                }
                return true;
            case R.id.menu_tts_start:
                SpeechService.startService(this, dstCoords);
                invalidateOptionsMenuCompatible();
                return true;
            case R.id.menu_tts_stop:
                SpeechService.stopService(this);
                invalidateOptionsMenuCompatible();
                return true;
            case R.id.menu_hint:
                cache.showHintToast(this);
                return true;
            default:
                if (LoggingUI.onMenuItemSelected(item, this, cache)) {
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public ShowcaseViewBuilder getShowcase() {
        return new ShowcaseViewBuilder(this)
                .setTarget(new ActionItemTarget(this, R.id.menu_hint))
                .setContent(R.string.showcase_compass_hint_title, R.string.showcase_compass_hint_text);
    }

    private void setTarget(@NonNull final Geopoint coords, final String newDescription) {
        setDestCoords(coords);
        setTargetDescription(newDescription);
        updateDistanceInfo(Sensors.getInstance().currentGeo());

        Log.d("destination set: " + newDescription + " (" + dstCoords + ")");
    }

    private void setTarget(final @NonNull Waypoint waypointIn) {
        waypoint = waypointIn;
        setTarget(waypointIn.getCoords(), waypointIn.getName());
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
