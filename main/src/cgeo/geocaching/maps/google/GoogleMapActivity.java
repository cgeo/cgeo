package cgeo.geocaching.maps.google;

import cgeo.geocaching.Settings;
import cgeo.geocaching.maps.AbstractMap;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.utils.Log;

import com.google.android.maps.MapActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class GoogleMapActivity extends MapActivity implements MapActivityImpl {

    private AbstractMap mapBase;
    private MapProvider mapProvider;

    public GoogleMapActivity() {
        mapBase = new CGeoMap(this);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public MapProvider getMapProvider() {
        // This should never happen!
        if (mapProvider == null) {
            mapProvider = new GoogleMapProvider(0);
            Log.e("GoogleMapActivity: Uninitialized MapProvider access attempted");
        }
        return mapProvider;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        if (icicle != null && icicle.containsKey(AbstractMap.EXTRAS_MAP_SOURCE)) {
            mapProvider = MapProviderFactory.getMapProvider(icicle.getInt(AbstractMap.EXTRAS_MAP_SOURCE));
        } else {
            mapProvider = Settings.getMapProvider();
        }

        // this should not ever happen,
        // it's just a safeguard that we do not end up with the wrong MapProvider
        if (!(mapProvider instanceof GoogleMapProvider)) {
            mapProvider = new GoogleMapProvider(0);
            Log.e("GoogleMapActivity: Got the wrong MapProvider during onCreate");
        }

        mapBase.onCreate(icicle);
    }

    @Override
    protected void onDestroy() {
        mapBase.onDestroy();
    }

    @Override
    protected void onPause() {
        mapBase.onPause();
    }

    @Override
    protected void onResume() {
        mapBase.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mapBase.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mapBase.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return mapBase.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStop() {
        mapBase.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(AbstractMap.EXTRAS_MAP_SOURCE, mapBase.getMapSource());
    }

    @Override
    public void superOnCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean superOnCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void superOnDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean superOnOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void superOnResume() {
        super.onResume();
    }

    @Override
    public void superOnStop() {
        super.onStop();
    }

    @Override
    public void superOnPause() {
        super.onPause();
    }

    @Override
    public boolean superOnPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    // close activity and open homescreen
    public void goHome(View view) {
        mapBase.goHome(view);
    }

    // open manual entry
    public void goManual(View view) {
        mapBase.goManual(view);
    }

}
