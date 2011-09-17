package cgeo.geocaching.googlemaps;

import cgeo.geocaching.mapcommon.MapBase;
import cgeo.geocaching.mapcommon.cgeomap;
import cgeo.geocaching.mapinterfaces.ActivityImpl;

import com.google.android.maps.MapActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class googleMapActivity extends MapActivity implements ActivityImpl {

    private MapBase mapBase;

    public googleMapActivity() {
        mapBase = new cgeomap(this);
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
    protected void onCreate(Bundle icicle) {
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
