package cgeo.geocaching;

import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.utils.Log;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.apache.commons.lang3.StringUtils;

public class WaypointPopupFragment extends AbstractDialogFragmentWithProximityNotification {
    @BindView(R.id.actionbar_title) protected TextView actionBarTitle;
    @BindView(R.id.waypoint_details_list) protected LinearLayout waypointDetailsLayout;
    @BindView(R.id.toggle_visited) protected CheckBox toggleVisited;
    @BindView(R.id.edit) protected Button buttonEdit;
    @BindView(R.id.details_list) protected LinearLayout cacheDetailsLayout;

    private int waypointId = 0;
    private Waypoint waypoint = null;
    private TextView waypointDistance = null;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.waypoint_popup, container, false);
        initCustomActionBar(v);
        ButterKnife.bind(this, v);

        return v;
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
        if (null != proximityNotification) {
            proximityNotification.setReferencePoint(waypoint.getCoords());
        }

        if (waypoint == null) {
            Log.e("WaypointPopupFragment.init: unable to get waypoint " + waypointId);
            getActivity().finish();
            return;
        }

        try {
            if (StringUtils.isNotBlank(waypoint.getName())) {
                setTitle(waypoint.getName());
            } else {
                setTitle(waypoint.getGeocode());
            }


            actionBarTitle.setCompoundDrawablesWithIntrinsicBounds(Compatibility.getDrawable(getResources(), waypoint.getWaypointType().markerId), null, null, null);

            //getSupportActionBar().setIcon(getResources().getDrawable(waypoint.getWaypointType().markerId));

            details = new CacheDetailsCreator(getActivity(), waypointDetailsLayout);

            //Waypoint geocode
            details.add(R.string.cache_geocode, waypoint.getPrefix() + waypoint.getGeocode().substring(2));
            waypointDistance = details.addDistance(waypoint, waypointDistance);
            details.addHtml(R.string.waypoint_note, waypoint.getNote(), waypoint.getGeocode());

            toggleVisited.setChecked(waypoint.isVisited());
            toggleVisited.setOnClickListener(arg1 -> {
                waypoint.setVisited(!waypoint.isVisited());
                DataStore.saveWaypoint(waypoint.getId(), waypoint.getGeocode(), waypoint);
                Toast.makeText(getActivity(), waypoint.isVisited() ? R.string.waypoint_set_visited : R.string.waypoint_unset_visited, Toast.LENGTH_SHORT).show();
            });

            buttonEdit.setOnClickListener(arg0 -> {
                EditWaypointActivity.startActivityEditWaypoint(getActivity(), cache, waypoint.getId());
                getActivity().finish();
            });

            details = new CacheDetailsCreator(getActivity(), cacheDetailsLayout);
            details.add(R.string.cache_name, cache.getName());

            addCacheDetails();

            final View view = getView();
            assert view != null;

        } catch (final Exception e) {
            Log.e("WaypointPopup.init", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == R.id.menu_tts_toggle) {
            SpeechService.toggleService(getActivity(), waypoint.getCoords());
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        SpeechService.stopService(getActivity());
        super.onDestroy();
    }

    @Override
    public void navigateTo() {
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
        getActivity().finish();
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

    public static DialogFragment newInstance(final String geocode, final int waypointId) {

        final Bundle args = new Bundle();
        args.putInt(WAYPOINT_ARG, waypointId);
        args.putString(GEOCODE_ARG, geocode);

        final DialogFragment f = new WaypointPopupFragment();
        f.setArguments(args);
        f.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        return f;
    }
}
