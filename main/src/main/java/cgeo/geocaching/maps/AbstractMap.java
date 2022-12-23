package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.MapActivityImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.PositionAndHistory;
import cgeo.geocaching.maps.mapsforge.v6.TargetView;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.storage.DataStore;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class for the map activity. Delegates base class calls to the
 * provider-specific implementation.
 */
public abstract class AbstractMap {

    @NonNull
    final MapActivityImpl mapActivity;
    protected MapViewImpl<CachesOverlayItemImpl> mapView;

    protected PositionAndHistory overlayPositionAndScale;
    public String targetGeocode = null;
    public Geopoint lastNavTarget = null;
    public TargetView targetView;

    protected AbstractMap(@NonNull final MapActivityImpl activity) {
        mapActivity = activity;
    }

    public Resources getResources() {
        return mapActivity.getResources();
    }

    public MapActivityImpl getMapActivity() {
        return mapActivity;
    }

    public AppCompatActivity getActivity() {
        return mapActivity.getActivity();
    }

    public void onCreate(final Bundle savedInstanceState) {
        mapActivity.superOnCreate(savedInstanceState);
        Routing.connect(getActivity());
    }

    public void onStart() {
        mapActivity.superOnStart();
    }

    public void onResume() {
        mapActivity.superOnResume();
    }

    public void onPause() {
        mapActivity.superOnPause();
    }

    public void onStop() {
        mapActivity.superOnStop();
    }

    public void onDestroy() {
        mapActivity.superOnDestroy();
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

    public void setTrack(final String key, final Route track) {
        //
    }

    public void centerOnPosition(final double latitude, final double longitude, final Viewport viewport) {
        //
    }


    public void reloadIndividualRoute() {
        //
    }

    public void clearIndividualRoute() {
        //
    }

    public abstract void refreshMapData(boolean circlesSwitched);

    public boolean isTargetSet() {
        return StringUtils.isNotBlank(targetGeocode) && null != lastNavTarget;
    }

    @Nullable
    public Geocache getCurrentTargetCache() {
        if (StringUtils.isNotBlank(targetGeocode)) {
            return DataStore.loadCache(targetGeocode, LoadFlags.LOAD_CACHE_OR_DB);
        }
        return null;
    }

    public void setTarget(final Geopoint coords, final String geocode) {
        lastNavTarget = coords;
        mapView.setCoordinates(overlayPositionAndScale.getCoordinates());
        if (StringUtils.isNotBlank(geocode)) {
            targetGeocode = geocode;
            final Geocache target = getCurrentTargetCache();
            targetView.setTarget(targetGeocode, target != null ? target.getName() : StringUtils.EMPTY);
            if (lastNavTarget == null && target != null) {
                lastNavTarget = target.getCoords();
            }
        } else {
            targetGeocode = null;
            targetView.setTarget(null, null);
        }
        mapView.setDestinationCoords(lastNavTarget);
        ActivityMixin.invalidateOptionsMenu(getActivity());
    }

    public abstract Collection<Geocache> getCaches();

    public abstract GeocacheFilterContext getFilterContext();

    public abstract MapOptions getMapOptions();

}
