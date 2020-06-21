package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.maps.AbstractMap;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;

import org.mapsforge.v3.android.maps.MapActivity;

public class MapsforgeMapActivity extends MapActivity implements MapActivityImpl, FilteredActivity {

    private final AbstractMap mapBase;

    public MapsforgeMapActivity() {
        mapBase = new CGeoMap(this);
    }

    @Override
    public void setTheme(final int resid) {
        if (Settings.isLightSkin()) {
            super.setTheme(R.style.cgeo_gmap_light);
        } else {
            super.setTheme(R.style.cgeo_gmap);
        }
    }
    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    protected void onCreate(final Bundle icicle) {
        mapBase.onCreate(icicle);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        mapBase.onSaveInstanceState(outState);
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
    public boolean onCreateOptionsMenu(final Menu menu) {
        return mapBase.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        return mapBase.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull final Menu menu) {
        return mapBase.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStop() {
        mapBase.onStop();
    }

    @Override
    public void superOnCreate(@NonNull final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean superOnCreateOptionsMenu(@NonNull final Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void superOnDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean superOnOptionsItemSelected(@NonNull final MenuItem item) {
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
    public boolean superOnPrepareOptionsMenu(@NonNull final Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void navigateUp(final View view) {
        ActivityMixin.navigateUp(this);
    }

    @Override
    public void showFilterMenu(final View view) {
        // do nothing, the filter bar only shows the global filter
    }
}
