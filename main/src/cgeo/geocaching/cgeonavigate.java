package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.ui.CompassView;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class cgeonavigate extends AbstractActivity {

    private static final String EXTRAS_COORDS = "coords";
    private static final String EXTRAS_NAME = "name";
    private static final String EXTRAS_GEOCODE = "geocode";
    private static final String EXTRAS_CACHE_INFO = "cacheinfo";
    private static final List<IWaypoint> coordinates = new ArrayList<IWaypoint>();
    private static final int MENU_MAP = 0;
    private static final int MENU_SWITCH_COMPASS_GPS = 1;
    private PowerManager pm = null;
    private Geopoint dstCoords = null;
    private float cacheHeading = 0;
    private String title = null;
    private String info = null;
    private TextView navType = null;
    private TextView navAccuracy = null;
    private TextView navSatellites = null;
    private TextView navLocation = null;
    private TextView distanceView = null;
    private TextView headingView = null;
    private CompassView compassView = null;

    public cgeonavigate() {
        super("c:geo-compass", true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.navigate);
        setTitle(res.getString(R.string.compass_title));

        // get parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            title = extras.getString(EXTRAS_GEOCODE);
            final String name = extras.getString(EXTRAS_NAME);
            dstCoords = (Geopoint) extras.getParcelable(EXTRAS_COORDS);
            info = extras.getString(EXTRAS_CACHE_INFO);

            if (StringUtils.isNotBlank(name)) {
                if (StringUtils.isNotBlank(title)) {
                    title += ": " + name;
                } else {
                    title = name;
                }
            }
        } else {
            Intent pointIntent = new Intent(this, cgeopoint.class);
            startActivity(pointIntent);

            finish();
            return;
        }

        // set header
        setTitle();
        setDestCoords();
        setCacheInfo();

        // get textviews once
        compassView = (CompassView) findViewById(R.id.rose);
    }

    @Override
    public void onResume() {
        super.onResume();

        // sensor & geolocation manager
        geoDirHandler.startGeoAndDir();

        // keep backlight on
        if (pm == null) {
            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
    }

    @Override
    public void onPause() {
        geoDirHandler.stopGeoAndDir();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        compassView.destroyDrawingCache();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.add(0, MENU_SWITCH_COMPASS_GPS, 0, res.getString(Settings.isUseCompass() ? R.string.use_gps : R.string.use_compass)).setIcon(R.drawable.ic_menu_compass);
        menu.add(0, MENU_MAP, 0, res.getString(R.string.caches_on_map)).setIcon(R.drawable.ic_menu_mapmode);
        menu.add(0, 2, 0, res.getString(R.string.destination_set)).setIcon(R.drawable.ic_menu_edit);
        if (coordinates.size() > 1) {
            final SubMenu subMenu = menu.addSubMenu(0, 3, 0, res.getString(R.string.destination_select)).setIcon(R.drawable.ic_menu_myplaces);
            int cnt = 4;
            for (final IWaypoint coordinate : coordinates) {
                subMenu.add(0, cnt, 0, coordinate.getName() + " (" + coordinate.getCoordType() + ")");
                cnt++;
            }
        } else {
            menu.add(0, 3, 0, res.getString(R.string.destination_select)).setIcon(R.drawable.ic_menu_myplaces).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(MENU_SWITCH_COMPASS_GPS).setTitle(res.getString(Settings.isUseCompass() ? R.string.use_gps : R.string.use_compass));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == MENU_MAP) {
            CGeoMap.startActivityCoords(this, dstCoords, null, null);
        } else if (id == MENU_SWITCH_COMPASS_GPS) {
            boolean oldSetting = Settings.isUseCompass();
            Settings.setUseCompass(!oldSetting);
            invalidateOptionsMenuCompatible();
            if (oldSetting) {
                geoDirHandler.stopDir();
            } else {
                geoDirHandler.startDir();
            }
        } else if (id == 2) {
            Intent pointIntent = new Intent(this, cgeopoint.class);
            startActivity(pointIntent);

            finish();
            return true;
        } else if (id > 3 && coordinates.get(id - 4) != null) {
            final IWaypoint coordinate = coordinates.get(id - 4);

            title = coordinate.getName();
            dstCoords = coordinate.getCoords();
            setTitle();
            setDestCoords();
            setCacheInfo();
            updateDistanceInfo(app.currentGeo());

            Log.d("destination set: " + title + " (" + dstCoords + ")");
            return true;
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

        ((TextView) findViewById(R.id.destination)).setText(dstCoords.toString());
    }

    private void setCacheInfo() {
        final TextView cacheInfoView = (TextView) findViewById(R.id.cacheinfo);
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

        if (distanceView == null) {
            distanceView = (TextView) findViewById(R.id.distance);
        }
        if (headingView == null) {
            headingView = (TextView) findViewById(R.id.heading);
        }

        cacheHeading = geo.getCoords().bearingTo(dstCoords);
        distanceView.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(dstCoords)));
        headingView.setText(Math.round(cacheHeading) + "°");
    }

    private GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoData(final IGeoData geo) {
            try {
                if (navType == null || navLocation == null || navAccuracy == null) {
                    navType = (TextView) findViewById(R.id.nav_type);
                    navAccuracy = (TextView) findViewById(R.id.nav_accuracy);
                    navSatellites = (TextView) findViewById(R.id.nav_satellites);
                    navLocation = (TextView) findViewById(R.id.nav_location);
                }

                if (geo.getCoords() != null) {
                    if (geo.getSatellitesVisible() >= 0) {
                        navSatellites.setText(res.getString(R.string.loc_sat) + ": " + geo.getSatellitesFixed() + "/" + geo.getSatellitesVisible());
                    }
                    else {
                        navSatellites.setText("");
                    }
                    navType.setText(res.getString(geo.getLocationProvider().resourceId));

                    if (geo.getAccuracy() >= 0) {
                        navAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()));
                    } else {
                        navAccuracy.setText(null);
                    }

                    if (geo.getAltitude() != 0.0f) {
                        final String humanAlt = Units.getDistanceFromMeters((float) geo.getAltitude());
                        navLocation.setText(geo.getCoords() + " | " + humanAlt);
                    } else {
                        navLocation.setText(geo.getCoords().toString());
                    }

                    updateDistanceInfo(geo);
                } else {
                    navType.setText(null);
                    navAccuracy.setText(null);
                    navLocation.setText(res.getString(R.string.loc_trying));
                }

                if (!Settings.isUseCompass() || geo.getSpeed() > 5) { // use GPS when speed is higher than 18 km/h
                    updateNorthHeading(geo.getBearing());
                }
            } catch (Exception e) {
                Log.w("Failed to LocationUpdater location.");
            }
        }

        @Override
        public void updateDirection(final float direction) {
            if (app.currentGeo().getSpeed() <= 5) { // use compass when speed is lower than 18 km/h
                updateNorthHeading(DirectionProvider.getDirectionNow(cgeonavigate.this, direction));
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
        if (coordinatesWithType != null) { // avoid possible NPE
            coordinates.addAll(coordinatesWithType);
        }

        final Intent navigateIntent = new Intent(context, cgeonavigate.class);
        navigateIntent.putExtra(EXTRAS_COORDS, coords);
        navigateIntent.putExtra(EXTRAS_GEOCODE, geocode);
        if (null != displayedName) {
            navigateIntent.putExtra(EXTRAS_NAME, displayedName);
        }
        navigateIntent.putExtra(EXTRAS_CACHE_INFO, info);
        context.startActivity(navigateIntent);
    }

    public static void startActivity(final Context context, final String geocode, final String displayedName, final Geopoint coords, final Collection<IWaypoint> coordinatesWithType) {
        cgeonavigate.startActivity(context, geocode, displayedName, coords, coordinatesWithType, null);
    }

}
