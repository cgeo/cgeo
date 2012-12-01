package cgeo.geocaching;

import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.geopoint.Geopoint;
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
    private static final String EXTRA_WAYPOINT_ID = "waypoint_id";
    private int waypointId = 0;
    private cgWaypoint waypoint = null;

    public WaypointPopup() {
        super("c:geo-waypoint-info", R.layout.waypoint_popup);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            waypointId = extras.getInt(EXTRA_WAYPOINT_ID);
        }
    }

    @Override
    protected void init() {
        super.init();
        waypoint = cgData.loadWaypoint(waypointId);
        try {
            if (StringUtils.isNotBlank(waypoint.getName())) {
                setTitle(waypoint.getName());
            } else {
                setTitle(waypoint.getGeocode());
            }

            // actionbar icon
            ((TextView) findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(waypoint.getWaypointType().markerId), null, null, null);

            //Start filling waypoint details
            details = new CacheDetailsCreator(this, (LinearLayout) findViewById(R.id.waypoint_details_list));

            //Waypoint geocode
            details.add(R.string.cache_geocode, waypoint.getPrefix() + waypoint.getGeocode().substring(2));

            // Edit Button
            final Button buttonEdit = (Button) findViewById(R.id.edit);
            buttonEdit.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    EditWaypointActivity.startActivityEditWaypoint(WaypointPopup.this, waypoint.getId());
                    finish();
                }
            });

            //Start filling cache details
            details = new CacheDetailsCreator(this, (LinearLayout) findViewById(R.id.details_list));
            details.add(R.string.cache_name, cache.getName());

            addCacheDetails();

        } catch (Exception e) {
            Log.e("cgeopopup.init: " + e.toString());
        }
    }

    @Override
    protected void navigateTo() {
        NavigationAppFactory.startDefaultNavigationApplication(1, this, waypoint);
    }

    /**
     * Tries to navigate to the {@link cgCache} of this activity.
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
        popupIntent.putExtra(EXTRA_WAYPOINT_ID, waypointId);
        popupIntent.putExtra(EXTRA_GEOCODE, geocode);
        context.startActivity(popupIntent);
    }

    @Override
    protected void showNavigationMenu() {
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
