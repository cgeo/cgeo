package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WaypointPopupFragment extends AbstractDialogFragment {
    @InjectView(R.id.actionbar_title) protected TextView actionBarTitle;
    @InjectView(R.id.waypoint_details_list) protected LinearLayout waypointDetailsLayout;
    @InjectView(R.id.edit) protected Button buttonEdit;
    @InjectView(R.id.details_list) protected LinearLayout cacheDetailsLayout;

    private int waypointId = 0;
    private Waypoint waypoint = null;
    private TextView waypointDistance = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View  v = inflater.inflate(R.layout.waypoint_popup, container, false);
        initCustomActionBar(v);
        ButterKnife.inject(this,v);

        return v;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        waypointId = getArguments().getInt(WAYPOINT_ARG);
    }

    @Override
    protected void onUpdateGeoData(GeoData geo) {
        if (waypoint != null && waypoint.getCoords() != null) {
            waypointDistance.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(waypoint.getCoords())));
            waypointDistance.bringToFront();
        }
    }

    @Override
    protected void init() {
        super.init();

        waypoint = DataStore.loadWaypoint(waypointId);
        try {
            if (StringUtils.isNotBlank(waypoint.getName())) {
                setTitle(waypoint.getName());
            } else {
                setTitle(waypoint.getGeocode());
            }


            actionBarTitle.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(waypoint.getWaypointType().markerId), null, null, null);

            //getSupportActionBar().setIcon(getResources().getDrawable(waypoint.getWaypointType().markerId));

            details = new CacheDetailsCreator(getActivity(), waypointDetailsLayout);

            //Waypoint geocode
            details.add(R.string.cache_geocode, waypoint.getPrefix() + waypoint.getGeocode().substring(2));
            details.addDistance(waypoint, waypointDistance);
            waypointDistance = details.getValueView();
            details.add(R.string.waypoint_note, waypoint.getNote());

            buttonEdit.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    EditWaypointActivity.startActivityEditWaypoint(getActivity(), cache, waypoint.getId());
                    getActivity().finish();
                }
            });

            details = new CacheDetailsCreator(getActivity(), cacheDetailsLayout);
            details.add(R.string.cache_name, cache.getName());

            addCacheDetails();

        } catch (Exception e) {
            Log.e("WaypointPopup.init", e);
        }
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
    protected Geopoint getCoordinates() {
        if (waypoint == null) {
            return null;
        }
        return waypoint.getCoords();
    }

    public static DialogFragment newInstance(String geocode, int waypointId) {

        Bundle args = new Bundle();
        args.putInt(WAYPOINT_ARG, waypointId);
        args.putString(GEOCODE_ARG, geocode);

        DialogFragment f = new WaypointPopupFragment();
        f.setArguments(args);
        f.setStyle(DialogFragment.STYLE_NO_TITLE,0);

        return f;
    }
}
