package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractNavigationBarMapActivity;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.databinding.WaypointPopupBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.StringUtils;

public class WaypointPopupFragment extends AbstractDialogFragmentWithProximityNotification {

    private int waypointId = 0;
    private Waypoint waypoint = null;
    private TextView waypointDistance = null;
    private WaypointPopupBinding binding;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        binding = WaypointPopupBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        waypointId = getArguments().getInt(WAYPOINT_ARG);
    }

    @Override
    protected void onUpdateGeoData(final GeoData geo) {
        super.onUpdateGeoData(geo);
        if (waypoint != null) {
            final Geopoint coordinates = waypoint.getCoords();
            if (coordinates != null) {
                waypointDistance.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(coordinates)));
                waypointDistance.bringToFront();
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        waypoint = DataStore.loadWaypoint(waypointId);
        if (waypoint == null) {
            Log.e("WaypointPopupFragment.init: unable to get waypoint " + waypointId);
            ((AbstractNavigationBarMapActivity) requireActivity()).sheetRemoveFragment();
            return;
        }

        if (null != proximityNotification) {
            proximityNotification.setReferencePoint(waypoint.getCoords());
            proximityNotification.setTextNotifications(getContext());
        }

        try {
            final String wpCode = waypoint.getPrefix() + waypoint.getShortGeocode().substring(2);
            binding.toolbar.toolbar.setTitle(wpCode);
            binding.toolbar.toolbar.setLogo(MapMarkerUtils.getWaypointMarker(res, waypoint, false, Settings.getIconScaleEverywhere()).getDrawable());
            onCreatePopupOptionsMenu(binding.toolbar.toolbar, this, cache);
            binding.toolbar.toolbar.setOnMenuItemClickListener(this::onPopupOptionsItemSelected);

            binding.title.setText(TextUtils.coloredCacheText(getActivity(), cache, cache.getName()));
            details = new CacheDetailsCreator(getActivity(), binding.waypointDetailsList);

            // Cache name and type
            final String cacheType = cache.getType().getL10n();
            final String cacheSize = cache.showSize() ? " (" + cache.getSize().getL10n() + ")" : "";
            details.add(R.string.cache_type, cacheType + cacheSize + " (" + cache.getShortGeocode() + ")");

            //Waypoint name
            if (StringUtils.isNotBlank(waypoint.getName())) {
                details.add(R.string.cache_name, waypoint.getName());
            }
            waypointDistance = details.addDistance(waypoint, waypointDistance);
            details.addLatestLogs(cache);

            // addWideHTML should go to the end of the list
            final String note = waypoint.getNote();
            if (StringUtils.isNotBlank(note)) {
                details.addWideHtml(-1, note, waypoint.getShortGeocode());
            }
            final String userNote = waypoint.getUserNote();
            if (StringUtils.isNotBlank(userNote)) {
                details.addWideHtml(R.string.waypoint_user_note, userNote, waypoint.getShortGeocode());
            }

            binding.toggleVisited.setChecked(waypoint.isVisited());
            binding.toggleVisited.setOnClickListener(arg1 -> {
                waypoint.setVisited(!waypoint.isVisited());
                DataStore.saveWaypoint(waypoint.getId(), waypoint.getGeocode(), waypoint);
                ViewUtils.showShortToast(getActivity(), waypoint.isVisited() ? R.string.waypoint_set_visited : R.string.waypoint_unset_visited);
            });

            binding.edit.setOnClickListener(arg0 -> EditWaypointActivity.startActivityEditWaypoint(getActivity(), cache, waypoint.getId()));

            binding.moreDetails.setOnClickListener(arg0 -> {
                CacheDetailActivity.startActivity(getActivity(), geocode);
                ((AbstractNavigationBarMapActivity) requireActivity()).sheetRemoveFragment();
            });

            final View view = getView();
            assert view != null;

        } catch (final Exception e) {
            Log.e("WaypointPopup.init", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == R.id.menu_tts_toggle) {
            SpeechService.toggleService(getActivity(), waypoint.getCoords());
            return true;
        }
        return false;
    }

    /**
     * Tries to navigate to the {@link Geocache} of this activity.
     */
    @Override
    public void startDefaultNavigation() {
        NavigationAppFactory.startDefaultNavigationApplication(1, getActivity(), waypoint);
    }

    /**
     * Tries to navigate to the {@link Geocache} of this activity.
     */
    @Override
    public void startDefaultNavigation2() {
        if (waypoint == null || waypoint.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));
            return;
        }
        NavigationAppFactory.startDefaultNavigationApplication(2, getActivity(), waypoint);
    }


    @Override
    public void showNavigationMenu() {
        NavigationAppFactory.showNavigationMenu(getActivity(), null, waypoint, null);
    }

    @Override
    protected TargetInfo getTargetInfo() {
        if (waypoint == null) {
            return null;
        }
        return new TargetInfo(waypoint.getCoords(), cache.getGeocode());
    }

    public static Fragment newInstance(final String geocode, final int waypointId) {

        final Bundle args = new Bundle();
        args.putInt(WAYPOINT_ARG, waypointId);
        args.putString(GEOCODE_ARG, geocode);

        final Fragment f = new WaypointPopupFragment();
        f.setArguments(args);
        // f.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        return f;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        GeocacheChangedBroadcastReceiver.sendBroadcast(getContext(), geocode);
    }
}
