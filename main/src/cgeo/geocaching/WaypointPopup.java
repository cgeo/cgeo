package cgeo.geocaching;

import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
        waypoint = app.loadWaypoint(waypointId);
        try {
            RelativeLayout itemLayout;
            TextView itemName;
            TextView itemValue;

            if (StringUtils.isNotBlank(waypoint.getName())) {
                setTitle(waypoint.getName());
            } else {
                setTitle(waypoint.getGeocode().toUpperCase());
            }

            // actionbar icon
            ((TextView) findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(waypoint.getWaypointType().markerId), null, null, null);

            //Start filling waypoint details
            LinearLayout waypointDetailsList = (LinearLayout) findViewById(R.id.waypoint_details_list);
            waypointDetailsList.removeAllViews();

            //Waypoint geocode
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);

            itemName.setText(res.getString(R.string.cache_geocode));

            itemValue.setText(waypoint.getPrefix().toUpperCase() + waypoint.getGeocode().toUpperCase().substring(2));
            waypointDetailsList.addView(itemLayout);

            // Edit Button
            Button buttonEdit = (Button) findViewById(R.id.edit);
            buttonEdit.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Intent editIntent = new Intent(WaypointPopup.this, cgeowaypointadd.class);
                    editIntent.putExtra("waypoint", waypoint.getId());
                    startActivity(editIntent);
                    restartActivity();
                }
            });

            //Start filling cache details
            LinearLayout cacheDetailsList = (LinearLayout) findViewById(R.id.details_list);
            cacheDetailsList.removeAllViews();

            // Cache name
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);
            itemValue.setLines(1);
            itemName.setText(res.getString(R.string.cache_name));

            itemValue.setText(cache.getName());
            cacheDetailsList.addView(itemLayout);

            // cache type
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);
            itemName.setText(res.getString(R.string.cache_type));

            String cacheType = cache.getType().getL10n();
            String cacheSize = cache.getSize() != CacheSize.UNKNOWN ? " (" + cache.getSize().getL10n() + ")" : "";
            itemValue.setText(cacheType + cacheSize);
            cacheDetailsList.addView(itemLayout);

            // gc-code
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);

            itemName.setText(res.getString(R.string.cache_geocode));
            itemValue.setText(cache.getGeocode().toUpperCase());
            cacheDetailsList.addView(itemLayout);

            // cache state
            if (cache.isArchived() || cache.isDisabled() || cache.isPremiumMembersOnly() || cache.isFound()) {
                itemLayout = createCacheState();
                cacheDetailsList.addView(itemLayout);

            }

            // distance
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);

            itemName.setText(res.getString(R.string.cache_distance));
            itemValue.setText("--");
            cacheDistance = itemValue;
            cacheDetailsList.addView(itemLayout);
            // difficulty
            if (cache.getDifficulty() > 0f) {
                itemLayout = createDifficulty();
                cacheDetailsList.addView(itemLayout);
            }

            // terrain
            if (cache.getTerrain() > 0f) {
                itemLayout = createTerrain();
                cacheDetailsList.addView(itemLayout);
            }

            // rating
            if (cache.getRating() > 0) {
                setRating(cache.getRating(), cache.getVotes());
            } else {
                aquireGCVote();
            }

            // more details
            Button buttonMore = (Button) findViewById(R.id.more_details);
            buttonMore.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Intent cachesIntent = new Intent(WaypointPopup.this, CacheDetailActivity.class);
                    cachesIntent.putExtra(EXTRA_GEOCODE, geocode.toUpperCase());
                    startActivity(cachesIntent);

                    finish();
                }
            });

        } catch (Exception e) {
            Log.e("cgeopopup.init: " + e.toString());
        }
    }

    @Override
    protected void navigateTo() {
        if (waypoint == null || waypoint.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        NavigationAppFactory.startDefaultNavigationApplication(app.currentGeo(), this, null, waypoint, null);
    }

    @Override
    protected void cachesAround() {
        if (waypoint == null || waypoint.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        cgeocaches.startActivityCoordinates(this, waypoint.getCoords());

        finish();
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void goDefaultNavigation(final View view) {
        if (waypoint == null || waypoint.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));
            return;
        }
        NavigationAppFactory.startDefaultNavigationApplication(app.currentGeo(), this, null, waypoint, null);
        finish();
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
        NavigationAppFactory.startDefaultNavigationApplication2(app.currentGeo(), this, null, waypoint, null);
        finish();
    }

    public static void startActivity(final Context context, final int waypointId, final String geocode) {
        final Intent popupIntent = new Intent(context, WaypointPopup.class);
        popupIntent.putExtra(EXTRA_WAYPOINT_ID, waypointId);
        popupIntent.putExtra(EXTRA_GEOCODE, geocode);
        context.startActivity(popupIntent);
    }

    @Override
    protected void logOffline(int menuItem) {
        cache.logOffline(this, LogType.getById(menuItem - MENU_LOG_VISIT_OFFLINE));
    }

    @Override
    protected void logVisit() {
        cache.logVisit(this);
        finish();
    }

    @Override
    protected void showInBrowser() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.getGeocode())));
    }

    @Override
    protected void showNavigationMenu() {
        NavigationAppFactory.showNavigationMenu(app.currentGeo(), this, null, waypoint, null);
    }
}
