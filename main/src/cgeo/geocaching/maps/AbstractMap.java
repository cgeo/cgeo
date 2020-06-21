package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.utils.TrackUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

/**
 * Base class for the map activity. Delegates base class calls to the
 * provider-specific implementation.
 */
public abstract class AbstractMap {

    final MapActivityImpl mapActivity;

    protected AbstractMap(final MapActivityImpl activity) {
        mapActivity = activity;
    }

    public Resources getResources() {
        return mapActivity.getResources();
    }

    public Activity getActivity() {
        return mapActivity.getActivity();
    }

    public void onCreate(final Bundle savedInstanceState) {
        mapActivity.superOnCreate(savedInstanceState);
        Routing.connect(null);
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
        Routing.disconnect();
    }

    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        final boolean result = mapActivity.superOnCreateOptionsMenu(menu);
        mapActivity.getActivity().getMenuInflater().inflate(R.menu.map_activity, menu);
        return result;
    }

    public boolean onPrepareOptionsMenu(@NonNull final Menu menu) {
        return mapActivity.superOnPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        return mapActivity.superOnOptionsItemSelected(item);
    }

    public abstract void onSaveInstanceState(@NonNull Bundle outState);

    public abstract void onLowMemory();

    public void setTracks(final TrackUtils.Tracks tracks) {
        //
    }
}
