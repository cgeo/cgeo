package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.AbstractDialogFragment;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.maps.AbstractMap;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.mapsforge.v6.TargetView;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.IndividualRouteUtils;
import cgeo.geocaching.utils.TrackUtils;
import static cgeo.geocaching.filters.gui.GeocacheFilterActivity.EXTRA_FILTER_CONTEXT;
import static cgeo.geocaching.maps.google.v2.GoogleMapUtils.isGoogleMapsAvailable;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;
import static cgeo.geocaching.settings.Settings.MAPROTATION_OFF;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.lang3.StringUtils;

@SuppressLint("MissingSuperCall") // super calls are handled via mapBase (mapBase.onCreate, mapBase.onSaveInstanceState, ...) TODO: Why is it done like that?
public class GoogleMapActivity extends AppCompatActivity implements MapActivityImpl, FilteredActivity {

    private static final String STATE_INDIVIDUAlROUTEUTILS = "indrouteutils";
    private static final String STATE_TRACKUTILS = "trackutils";

    private final AbstractMap mapBase;

    private TrackUtils trackUtils = null;
    private IndividualRouteUtils individualRouteUtils = null;

    public GoogleMapActivity() {
        mapBase = new CGeoMap(this);
    }

    public void setTheme(final int resid) {
        super.setTheme(R.style.cgeo);
    }

    public TrackUtils getTrackUtils() {
        return trackUtils;
    }

    public IndividualRouteUtils getIndividualRouteUtils() {
        return individualRouteUtils;
    }

    @Override
    public AppCompatActivity getActivity() {
        return this;
    }

    @Override
    protected void onCreate(final Bundle icicle) {
        mapBase.onCreate(icicle);
        individualRouteUtils = new IndividualRouteUtils(this, icicle == null ? null : icicle.getBundle(STATE_INDIVIDUAlROUTEUTILS),
            mapBase::clearIndividualRoute, mapBase::reloadIndividualRoute);
        trackUtils = new TrackUtils(this, icicle == null ? null : icicle.getBundle(STATE_TRACKUTILS),
            mapBase::setTracks, mapBase::centerOnPosition);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        mapBase.onSaveInstanceState(outState);
        outState.putBundle(STATE_INDIVIDUAlROUTEUTILS, individualRouteUtils.getState());
        outState.putBundle(STATE_TRACKUTILS, trackUtils.getState());
    }

    @Override
    public void onLowMemory() {
        mapBase.onLowMemory();
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
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
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
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        invalidateOptionsMenu();
    }


    @Override
    protected void onStart() {
        //Target view
        mapBase.targetView = new TargetView((TextView) findViewById(R.id.target), (TextView) findViewById(R.id.targetSupersize), StringUtils.EMPTY, StringUtils.EMPTY);
        final Geocache target = mapBase.getCurrentTargetCache();
        if (target != null) {
            mapBase.targetView.setTarget(target.getGeocode(), target.getName());
        }
        mapBase.onStart();
    }

    @Override
    protected void onStop() {
        mapBase.onStop();
    }

    @Override
    public void superOnCreate(final Bundle savedInstanceState) {
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
    public void navigateUp(final View view) {
        ActivityMixin.navigateUp(this);
    }

    @Override
    public void superOnResume() {
        super.onResume();
    }

    @Override
    public void superOnStart() {
        super.onStart();
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
        final boolean result = super.onPrepareOptionsMenu(menu);
        final boolean isGoogleMapsAvailable = isGoogleMapsAvailable(this);

        menu.findItem(R.id.menu_map_rotation).setVisible(isGoogleMapsAvailable);
        if (isGoogleMapsAvailable) {
            final int mapRotation = Settings.getMapRotation();
            switch (mapRotation) {
                case MAPROTATION_OFF:
                    menu.findItem(R.id.menu_map_rotation_off).setChecked(true);
                    break;
                case MAPROTATION_MANUAL:
                    menu.findItem(R.id.menu_map_rotation_manual).setChecked(true);
                    break;
                case MAPROTATION_AUTO:
                    menu.findItem(R.id.menu_map_rotation_auto).setChecked(true);
                    break;
                default:
                    break;
            }
        }
        this.trackUtils.onPrepareOptionsMenu(menu);

        return result;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AbstractDialogFragment.REQUEST_CODE_TARGET_INFO && resultCode == AbstractDialogFragment.RESULT_CODE_SET_TARGET) {
            final AbstractDialogFragment.TargetInfo targetInfo = data.getExtras().getParcelable(Intents.EXTRA_TARGET_INFO);
            if (targetInfo != null) {
                if (Settings.isAutotargetIndividualRoute()) {
                    Settings.setAutotargetIndividualRoute(false);
                    Toast.makeText(this, R.string.map_disable_autotarget_individual_route, Toast.LENGTH_SHORT).show();
                }
                mapBase.setTarget(targetInfo.coords, targetInfo.geocode);
            }
            /* @todo: Clarify if needed in GMv2
            final List<String> changedGeocodes = new ArrayList<>();
            String geocode = popupGeocodes.poll();
            while (geocode != null) {
                changedGeocodes.add(geocode);
                geocode = popupGeocodes.poll();
            }
            if (caches != null) {
                caches.invalidate(changedGeocodes);
            }
            */
        }
        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            mapBase.getMapOptions().filterContext = data.getParcelableExtra(EXTRA_FILTER_CONTEXT);
            mapBase.onMapSettingsPopupFinished(false);
        }

        this.trackUtils.onActivityResult(requestCode, resultCode, data);
        this.individualRouteUtils.onActivityResult(requestCode, resultCode, data);
        DownloaderUtils.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void showFilterMenu(final View view) {
        MapUtils.openFilterActivity(this, mapBase.getFilterContext(), mapBase.getCaches());
    }

}
