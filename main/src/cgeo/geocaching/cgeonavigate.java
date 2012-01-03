package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.CGeoMap;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class cgeonavigate extends AbstractActivity {

    private static final String EXTRAS_LONGITUDE = "longitude";
    private static final String EXTRAS_LATITUDE = "latitude";
    private static final String EXTRAS_NAME = "name";
    private static final String EXTRAS_GEOCODE = "geocode";
    private static final List<cgCoord> coordinates = new ArrayList<cgCoord>();
    private static final int MENU_MAP = 0;
    private static final int MENU_SWITCH_COMPASS_GPS = 1;
    private PowerManager pm = null;
    private cgGeo geo = null;
    private cgDirection dir = null;
    private UpdateLocationCallback geoUpdate = new update();
    private UpdateDirectionCallback dirUpdate = new UpdateDirection();
    private Geopoint dstCoords = null;
    private float cacheHeading = 0;
    private Float northHeading = null;
    private String title = null;
    private String name = null;
    private TextView navType = null;
    private TextView navAccuracy = null;
    private TextView navSatellites = null;
    private TextView navLocation = null;
    private TextView distanceView = null;
    private TextView headingView = null;
    private cgCompass compassView = null;
    private updaterThread updater = null;
    private Handler updaterHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (compassView != null && northHeading != null) {
                    compassView.updateNorth(northHeading, cacheHeading);
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeonavigate.updaterHandler: " + e.toString());
            }
        }
    };
    private String geocode;

    public cgeonavigate() {
        super("c:geo-compass");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set layout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setTheme();
        setContentView(R.layout.navigate);
        setTitle(res.getString(R.string.compass_title));

        // sensor & geolocation manager
        if (geo == null) {
            geo = app.startGeo(geoUpdate);
        }
        if (Settings.isUseCompass() && dir == null) {
            dir = app.startDir(this, dirUpdate);
        }

        // get parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(EXTRAS_GEOCODE);
            title = geocode;
            name = extras.getString(EXTRAS_NAME);
            dstCoords = new Geopoint(extras.getDouble(EXTRAS_LATITUDE), extras.getDouble(EXTRAS_LONGITUDE));

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

        setGo4CacheAction();

        // set header
        setTitle();
        setDestCoords();

        if (geo != null) {
            geoUpdate.updateLocation(geo);
        }
        if (dir != null) {
            dirUpdate.updateDirection(dir);
        }

        // get textviews once
        compassView = (cgCompass) findViewById(R.id.rose);

        // start updater thread
        updater = new updaterThread(updaterHandler);
        updater.start();
    }

    @Override
    public void onResume() {
        super.onResume();

        setGo4CacheAction();

        // sensor & geolocation manager
        if (geo == null) {
            geo = app.startGeo(geoUpdate);
        }
        if (Settings.isUseCompass() && dir == null) {
            dir = app.startDir(this, dirUpdate);
        }

        // keep backlight on
        if (pm == null) {
            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }

        // updater thread
        if (updater == null) {
            updater = new updaterThread(updaterHandler);
            updater.start();
        }
    }

    private void setGo4CacheAction() {
        if (StringUtils.isNotBlank(geocode)) {
            app.setAction(geocode);
        } else if (StringUtils.isNotBlank(name)) {
            app.setAction(name);
        }
    }

    @Override
    public void onStop() {
        if (geo != null) {
            geo = app.removeGeo();
        }
        if (dir != null) {
            dir = app.removeDir();
        }

        super.onStop();
    }

    @Override
    public void onPause() {
        if (geo != null) {
            geo = app.removeGeo();
        }
        if (dir != null) {
            dir = app.removeDir();
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (geo != null) {
            geo = app.removeGeo();
        }
        if (dir != null) {
            dir = app.removeDir();
        }

        compassView.destroyDrawingCache();
        compassView = null;

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Settings.isUseCompass()) {
            menu.add(0, MENU_SWITCH_COMPASS_GPS, 0, res.getString(R.string.use_gps)).setIcon(android.R.drawable.ic_menu_compass);
        } else {
            menu.add(0, MENU_SWITCH_COMPASS_GPS, 0, res.getString(R.string.use_compass)).setIcon(android.R.drawable.ic_menu_compass);
        }
        menu.add(0, MENU_MAP, 0, res.getString(R.string.caches_on_map)).setIcon(android.R.drawable.ic_menu_mapmode);
        menu.add(0, 2, 0, res.getString(R.string.destination_set)).setIcon(android.R.drawable.ic_menu_edit);
        if (coordinates.size() > 1) {
            SubMenu subMenu = menu.addSubMenu(0, 3, 0, res.getString(R.string.destination_select)).setIcon(android.R.drawable.ic_menu_myplaces);

            int cnt = 4;
            for (cgCoord coordinate : coordinates) {
                subMenu.add(0, cnt, 0, coordinate.getName() + " (" + coordinate.getCoordType() + ")");
                cnt++;
            }

            return true;
        } else {
            menu.add(0, 3, 0, res.getString(R.string.destination_select)).setIcon(android.R.drawable.ic_menu_myplaces).setEnabled(false);

            return true;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item;
        item = menu.findItem(1);
        if (Settings.isUseCompass()) {
            item.setTitle(res.getString(R.string.use_gps));
        } else {
            item.setTitle(res.getString(R.string.use_compass));
        }

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
            if (oldSetting) {
                if (dir != null) {
                    dir = app.removeDir();
                }
            } else {
                if (dir == null) {
                    dir = app.startDir(this, dirUpdate);
                }
            }
        } else if (id == 2) {
            Intent pointIntent = new Intent(this, cgeopoint.class);
            startActivity(pointIntent);

            finish();
            return true;
        } else if (id > 3 && coordinates.get(id - 4) != null) {
            cgCoord coordinate = coordinates.get(id - 4);

            title = coordinate.getName();
            dstCoords = coordinate.getCoords();
            setTitle();
            setDestCoords();
            updateDistanceInfo();

            Log.d(Settings.tag, "destination set: " + title + " (" + dstCoords + ")");
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

    public void setDest(final Geopoint coords) {
        if (coords == null) {
            return;
        }

        title = "some place";
        setTitle();
        setDestCoords();

        dstCoords = coords;
        updateDistanceInfo();
    }

    public Geopoint getCoordinatesNow() {
        return geo.coordsNow;
    }

    private void updateDistanceInfo() {
        if (geo == null || geo.coordsNow == null || dstCoords == null) {
            return;
        }

        if (distanceView == null) {
            distanceView = (TextView) findViewById(R.id.distance);
        }
        if (headingView == null) {
            headingView = (TextView) findViewById(R.id.heading);
        }

        cacheHeading = geo.coordsNow.bearingTo(dstCoords);
        distanceView.setText(cgBase.getHumanDistance(geo.coordsNow.distanceTo(dstCoords)));
        headingView.setText(Math.round(cacheHeading) + "°");
    }

    private class update implements UpdateLocationCallback {

        @Override
        public void updateLocation(cgGeo geo) {
            if (geo == null) {
                return;
            }

            try {
                if (navType == null || navLocation == null || navAccuracy == null) {
                    navType = (TextView) findViewById(R.id.nav_type);
                    navAccuracy = (TextView) findViewById(R.id.nav_accuracy);
                    navSatellites = (TextView) findViewById(R.id.nav_satellites);
                    navLocation = (TextView) findViewById(R.id.nav_location);
                }

                if (geo.coordsNow != null) {
                    String satellites = null;
                    if (geo.satellitesFixed > 0) {
                        satellites = res.getString(R.string.loc_sat) + ": " + geo.satellitesFixed + "/" + geo.satellitesVisible;
                    } else if (geo.satellitesVisible >= 0) {
                        satellites = res.getString(R.string.loc_sat) + ": 0/" + geo.satellitesVisible;
                    } else {
                        satellites = "";
                    }
                    navSatellites.setText(satellites);
                    navType.setText(res.getString(geo.locationProvider.resourceId));

                    if (geo.accuracyNow >= 0) {
                        if (Settings.isUseMetricUnits()) {
                            navAccuracy.setText("±" + Math.round(geo.accuracyNow) + " m");
                        } else {
                            navAccuracy.setText("±" + Math.round(geo.accuracyNow * 3.2808399) + " ft");
                        }
                    } else {
                        navAccuracy.setText(null);
                    }

                    if (geo.altitudeNow != null) {
                        final String humanAlt = cgBase.getHumanDistance(geo.altitudeNow.floatValue() / 1000);
                        navLocation.setText(geo.coordsNow + " | " + humanAlt);
                    } else {
                        navLocation.setText(geo.coordsNow.toString());
                    }

                    updateDistanceInfo();
                } else {
                    navType.setText(null);
                    navAccuracy.setText(null);
                    navLocation.setText(res.getString(R.string.loc_trying));
                }

                if (!Settings.isUseCompass() || geo.speedNow > 5) { // use GPS when speed is higher than 18 km/h
                    northHeading = geo.bearingNow;
                }
            } catch (Exception e) {
                Log.w(Settings.tag, "Failed to update location.");
            }
        }
    }

    private class UpdateDirection implements UpdateDirectionCallback {

        @Override
        public void updateDirection(cgDirection dir) {
            if (dir == null || dir.directionNow == null) {
                return;
            }

            if (geo == null || geo.speedNow <= 5) { // use compass when speed is lower than 18 km/h
                northHeading = dir.directionNow;
            }
        }
    }

    private static class updaterThread extends Thread {

        private Handler handler = null;

        public updaterThread(Handler handlerIn) {
            handler = handlerIn;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (handler != null) {
                    handler.sendMessage(new Message());
                }

                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static void startActivity(final Context context, final String geocode, final String displayedName, final Geopoint coords, final Collection<cgCoord> coordinatesWithType) {
        coordinates.clear();
        if (coordinatesWithType != null) { // avoid possible NPE
            coordinates.addAll(coordinatesWithType);
        }

        final Intent navigateIntent = new Intent(context, cgeonavigate.class);
        navigateIntent.putExtra(EXTRAS_LATITUDE, coords.getLatitude());
        navigateIntent.putExtra(EXTRAS_LONGITUDE, coords.getLongitude());
        navigateIntent.putExtra(EXTRAS_GEOCODE, geocode.toUpperCase());
        if (null != displayedName) {
            navigateIntent.putExtra(EXTRAS_NAME, displayedName);
        }
        context.startActivity(navigateIntent);
    }
}
