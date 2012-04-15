package cgeo.geocaching.maps;

import cgeo.geocaching.Settings;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Base class for the map activity. Delegates base class calls to the
 * provider-specific implementation.
 *
 * @author rsudev
 *
 */
public abstract class AbstractMap {

    public static final String EXTRAS_MAP_SOURCE = "mapSource";
    public static final String EXTRAS_MAP_FILE = "mapFile";

    final MapActivityImpl mapActivity;
    int mapSource = 0;
    String mapFile = "";

    protected AbstractMap(MapActivityImpl activity) {
        mapActivity = activity;
    }

    public Resources getResources() {
        return mapActivity.getResources();
    }

    public Activity getActivity() {
        return mapActivity.getActivity();
    }

    public MapProvider getMapProvider() {
        return mapActivity.getMapProvider();
    }

    public int getMapSource() {
        return mapSource;
    }

    public void setMapSource(int sourceId) {
        Settings.setMapSource(sourceId);
        mapSource = sourceId;
    }

    public void onCreate(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRAS_MAP_FILE)) {
                mapFile = savedInstanceState.getString(EXTRAS_MAP_FILE);
            }
            mapSource = savedInstanceState.getInt(EXTRAS_MAP_SOURCE);
        }

        if (StringUtils.isEmpty(mapFile)) {
            mapFile = Settings.getMapFile();
        }
        if (mapSource == 0) {
            mapSource = Settings.getMapSource();
        }

        mapActivity.superOnCreate(savedInstanceState);
    }

    public void onResume() {
        mapActivity.superOnResume();
    }

    public void onStop() {
        mapActivity.superOnStop();
    }

    public void onPause() {
        mapActivity.superOnPause();
    }

    public void onDestroy() {
        mapActivity.superOnDestroy();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return mapActivity.superOnCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return mapActivity.superOnPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return mapActivity.superOnOptionsItemSelected(item);
    }

    public abstract void goHome(View view);

    public abstract void goManual(View view);

}
