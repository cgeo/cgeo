package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WaypointPopup extends AbstractPopupActivity {
    @InjectView(R.id.actionbar_title) protected TextView actionBarTitle;
    @InjectView(R.id.waypoint_details_list) protected LinearLayout waypointDetailsLayout;
    @InjectView(R.id.edit) protected Button buttonEdit;
    @InjectView(R.id.details_list) protected LinearLayout cacheDetailsLayout;

    private int waypointId = 0;
    private Waypoint waypoint = null;
    private TextView waypointDistance = null;

    public WaypointPopup() {
        super(R.layout.waypoint_popup);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.inject(this);
        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            waypointId = extras.getInt(Intents.EXTRA_WAYPOINT_ID);
        }
    }

    @Override
    public void onUpdateGeoData(IGeoData geo) {
        if (geo.getCoords() != null && waypoint != null && waypoint.getCoords() != null) {
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

            details = new CacheDetailsCreator(this, waypointDetailsLayout);

            //Waypoint geocode
            details.add(R.string.cache_geocode, waypoint.getPrefix() + waypoint.getGeocode().substring(2));
            details.addDistance(waypoint, waypointDistance);
            waypointDistance = details.getValueView();
            details.add(R.string.waypoint_note, waypoint.getNote());

            buttonEdit.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    EditWaypointActivity.startActivityEditWaypoint(WaypointPopup.this, cache, waypoint.getId());
                    finish();
                }
            });

            details = new CacheDetailsCreator(this, cacheDetailsLayout);
            details.add(R.string.cache_name, cache.getName());

            addCacheDetails();

        } catch (Exception e) {
            Log.e("WaypointPopup.init", e);
        }
    }

    @Override
    public void navigateTo() {
        NavigationAppFactory.startDefaultNavigationApplication(1, this, waypoint);
    }

    /**
     * Tries to navigate to the {@link Geocache} of this activity.
     */
    @Override
    protected void startDefaultNavigation2() {
        if (waypoint == null || waypoint.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));
            return;
        }
        NavigationAppFactory.startDefaultNavigationApplication(2, this, waypoint);
        finish();
    }

    public static void startActivity(final Context context, final int waypointId, final String geocode) {
        final Intent popupIntent = new Intent(context, WaypointPopup.class);
        popupIntent.putExtra(Intents.EXTRA_WAYPOINT_ID, waypointId);
        popupIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        context.startActivity(popupIntent);
    }

    @Override
    public void showNavigationMenu() {
        NavigationAppFactory.showNavigationMenu(this, null, waypoint, null);
    }

    @Override
    protected Geopoint getCoordinates() {
        if (waypoint == null) {
            return null;
        }
        return waypoint.getCoords();
    }
}
