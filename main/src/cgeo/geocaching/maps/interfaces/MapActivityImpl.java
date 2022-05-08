package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.maps.RouteTrackUtils;
import cgeo.geocaching.maps.Tracks;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Defines the common functions of the provider-specific
 * MapActivity implementations.
 */
public interface MapActivityImpl extends FilteredActivity {

    Resources getResources();

    AppCompatActivity getActivity();

    void superOnCreate(Bundle savedInstanceState);

    void superOnResume();

    void superOnDestroy();

    void superOnStart();

    void superOnStop();

    void superOnPause();

    boolean superOnCreateOptionsMenu(Menu menu);

    boolean superOnPrepareOptionsMenu(Menu menu);

    boolean superOnOptionsItemSelected(MenuItem item);

    RouteTrackUtils getRouteTrackUtils();

    Tracks getTracks();

}
