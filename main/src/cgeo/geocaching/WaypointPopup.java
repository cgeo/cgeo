package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteRating;
import cgeo.geocaching.geopoint.HumanDistance;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

public class WaypointPopup extends AbstractActivity {

    private static final String EXTRA_GEOCODE = "geocode";
    private static final String EXTRA_WAYPOINT_ID = "waypoint_id";
    private LayoutInflater inflater = null;
    private int waypoint_id = 0;
    private String geocode = null;
    private cgCache cache = null;
    private cgWaypoint waypoint = null;
    private UpdateLocation geoUpdate = new UpdateLocation();
    private TextView cacheDistance = null;
    private Handler ratingHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                final Bundle data = msg.getData();

                setRating(data.getFloat("rating"), data.getInt("votes"));
            } catch (Exception e) {
                // nothing
            }
        }
    };

    private class UpdateLocation extends GeoObserver {

        @Override
        public void updateLocation(final IGeoData geo) {
            try {
                if (geo.getCoords() != null && cache != null && cache.getCoords() != null) {
                    cacheDistance.setText(HumanDistance.getHumanDistance(geo.getCoords().distanceTo(cache.getCoords())));
                    cacheDistance.bringToFront();
                }
            } catch (Exception e) {
                Log.w("Failed to UpdateLocation location.");
            }
        }
    }

    public WaypointPopup() {
        super("c:geo-waypoint-info");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set layout
        setTheme(R.style.transparent);
        setContentView(R.layout.waypoint_popup);
        setTitle(res.getString(R.string.detail));

        // get parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(EXTRA_GEOCODE);
            waypoint_id = extras.getInt(EXTRA_WAYPOINT_ID);
        }

        if (StringUtils.isBlank(geocode)) {
            showToast(res.getString(R.string.err_detail_cache_find));

            finish();
            return;
        }

        ImageView defaultNavigationImageView = (ImageView) findViewById(R.id.defaultNavigation);
        defaultNavigationImageView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startDefaultNavigation2();
                return true;
            }
        });

    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            final Rect r = new Rect(0, 0, 0, 0);
            getWindow().getDecorView().getHitRect(r);
            if (!r.contains((int) event.getX(), (int) event.getY())) {
                finish();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 2, 0, NavigationAppFactory.getDefaultNavigationApplication(this).getName()).setIcon(android.R.drawable.ic_menu_compass); // default navigation tool
        menu.add(0, 3, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_mapmode);
        addVisitMenu(menu, cache);
        menu.add(0, 5, 0, res.getString(R.string.cache_menu_around)).setIcon(android.R.drawable.ic_menu_rotate); // caches around
        menu.add(0, 7, 0, res.getString(R.string.cache_menu_browser)).setIcon(android.R.drawable.ic_menu_info_details); // browser

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            if (cache != null && cache.getCoords() != null) {
                menu.findItem(2).setVisible(true);
                menu.findItem(3).setVisible(true);
                menu.findItem(5).setVisible(true);
            } else {
                menu.findItem(2).setVisible(false);
                menu.findItem(3).setVisible(false);
                menu.findItem(5).setVisible(false);
            }

            menu.findItem(MENU_LOG_VISIT).setEnabled(Settings.isLogin());
        } catch (Exception e) {
            // nothing
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int menuItem = item.getItemId();

        if (menuItem == 2) {
            navigateTo();
            return true;
        } else if (menuItem == 3) {
            NavigationAppFactory.showNavigationMenu(app.currentGeo(), this, cache, null, null);
            return true;
        } else if (menuItem == 5) {
            cachesAround();
            return true;
        } else if (menuItem == MENU_LOG_VISIT) {
            cache.logVisit(this);
            finish();
            return true;
        } else if (menuItem == 7) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.getGeocode())));
            return true;
        }

        int logType = menuItem - MENU_LOG_VISIT_OFFLINE;
        cache.logOffline(this, LogType.getById(logType));
        return true;
    }

    private void init() {
        app.setAction(geocode);

        cache = app.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        waypoint = app.loadWaypoint(waypoint_id);
        if (cache == null) {
            showToast(res.getString(R.string.err_detail_cache_find));

            finish();
            return;
        }

        if (CacheType.UNKNOWN == cache.getType()) {
            Set<String> geocodes = new HashSet<String>();
            geocodes.add(geocode);
            SearchResult search = ConnectorFactory.searchByGeocodes(geocodes);
            cache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_ONLY);
        }

        try {
            RelativeLayout itemLayout;
            TextView itemName;
            TextView itemValue;
            LinearLayout itemStars;

            if (StringUtils.isNotBlank(waypoint.getName())) {
                setTitle(waypoint.getName());
            } else {
                setTitle(waypoint.getGeocode().toUpperCase());
            }

            inflater = getLayoutInflater();
            geocode = cache.getGeocode().toUpperCase();

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

                public void onClick(View arg0) {
                    Intent editIntent = new Intent(WaypointPopup.this, cgeowaypointadd.class);
                    editIntent.putExtra("waypoint", waypoint.getId());
                    startActivity(editIntent);
                    restartActivity();
                }
            });

            //Start filling cache details
            LinearLayout cacheDetailsList = (LinearLayout) findViewById(R.id.cache_details_list);
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
                itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
                itemName = (TextView) itemLayout.findViewById(R.id.name);
                itemValue = (TextView) itemLayout.findViewById(R.id.value);

                itemName.setText(res.getString(R.string.cache_status));

                StringBuilder state = new StringBuilder();
                if (cache.isFound()) {
                    if (state.length() > 0) {
                        state.append(", ");
                    }
                    state.append(res.getString(R.string.cache_status_found));
                }
                if (cache.isArchived()) {
                    if (state.length() > 0) {
                        state.append(", ");
                    }
                    state.append(res.getString(R.string.cache_status_archived));
                }
                if (cache.isDisabled()) {
                    if (state.length() > 0) {
                        state.append(", ");
                    }
                    state.append(res.getString(R.string.cache_status_disabled));
                }
                if (cache.isPremiumMembersOnly()) {
                    if (state.length() > 0) {
                        state.append(", ");
                    }
                    state.append(res.getString(R.string.cache_status_premium));
                }

                itemValue.setText(state.toString());
                cacheDetailsList.addView(itemLayout);

                state = null;
            }

            // distance
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);

            itemName.setText(res.getString(R.string.cache_distance));
            itemValue.setText("--");
            cacheDetailsList.addView(itemLayout);
            cacheDistance = itemValue;

            // difficulty
            if (cache.getDifficulty() > 0f) {
                itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_layout, null);
                itemName = (TextView) itemLayout.findViewById(R.id.name);
                itemValue = (TextView) itemLayout.findViewById(R.id.value);
                itemStars = (LinearLayout) itemLayout.findViewById(R.id.stars);

                itemName.setText(res.getString(R.string.cache_difficulty));
                itemValue.setText(String.format("%.1f", cache.getDifficulty()) + ' ' + res.getString(R.string.cache_rating_of) + " 5");
                for (int i = 0; i <= 4; i++) {
                    ImageView star = (ImageView) inflater.inflate(R.layout.star, null);
                    if ((cache.getDifficulty() - i) >= 1.0) {
                        star.setImageResource(R.drawable.star_on);
                    } else if ((cache.getDifficulty() - i) > 0.0) {
                        star.setImageResource(R.drawable.star_half);
                    } else {
                        star.setImageResource(R.drawable.star_off);
                    }
                    itemStars.addView(star);
                }
                cacheDetailsList.addView(itemLayout);
            }

            // terrain
            if (cache.getTerrain() > 0f) {
                itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_layout, null);
                itemName = (TextView) itemLayout.findViewById(R.id.name);
                itemValue = (TextView) itemLayout.findViewById(R.id.value);
                itemStars = (LinearLayout) itemLayout.findViewById(R.id.stars);

                itemName.setText(res.getString(R.string.cache_terrain));
                itemValue.setText(String.format("%.1f", cache.getTerrain()) + ' ' + res.getString(R.string.cache_rating_of) + " 5");
                for (int i = 0; i <= 4; i++) {
                    ImageView star = (ImageView) inflater.inflate(R.layout.star, null);
                    if ((cache.getTerrain() - i) >= 1.0) {
                        star.setImageResource(R.drawable.star_on);
                    } else if ((cache.getTerrain() - i) > 0.0) {
                        star.setImageResource(R.drawable.star_half);
                    } else {
                        star.setImageResource(R.drawable.star_off);
                    }
                    itemStars.addView(star);
                }
                cacheDetailsList.addView(itemLayout);
            }

            // rating
            if (cache.getRating() > 0) {
                setRating(cache.getRating(), cache.getVotes());
            } else {
                if (Settings.isRatingWanted() && cache.supportsGCVote()) {
                    (new Thread() {

                        @Override
                        public void run() {
                            GCVoteRating rating = GCVote.getRating(cache.getGuid(), geocode);

                            if (rating == null) {
                                return;
                            }

                            Message msg = Message.obtain();
                            Bundle bundle = new Bundle();
                            bundle.putFloat("rating", rating.getRating());
                            bundle.putInt("votes", rating.getVotes());
                            msg.setData(bundle);

                            ratingHandler.sendMessage(msg);
                        }
                    }).start();
                }
            }

            // more details
            Button buttonMore = (Button) findViewById(R.id.more_details);
            buttonMore.setOnClickListener(new OnClickListener() {

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        app.addGeoObserver(geoUpdate);
        init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        app.deleteGeoObserver(geoUpdate);
        super.onPause();
    }

    private void navigateTo() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        NavigationAppFactory.startDefaultNavigationApplication(app.currentGeo(), this, cache, null, null);
    }

    private void cachesAround() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        cgeocaches.startActivityCachesAround(this, cache.getCoords());

        finish();
    }

    private void setRating(float rating, int votes) {
        if (rating <= 0) {
            return;
        }

        RelativeLayout itemLayout;
        TextView itemName;
        TextView itemValue;
        LinearLayout itemStars;
        LinearLayout detailsList = (LinearLayout) findViewById(R.id.cache_details_list);

        itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_layout, null);
        itemName = (TextView) itemLayout.findViewById(R.id.name);
        itemValue = (TextView) itemLayout.findViewById(R.id.value);
        itemStars = (LinearLayout) itemLayout.findViewById(R.id.stars);

        itemName.setText(res.getString(R.string.cache_rating));
        itemValue.setText(String.format("%.1f", rating) + ' ' + res.getString(R.string.cache_rating_of) + " 5");
        itemStars.addView(createStarRating(rating, 5, this), 1);

        if (votes > 0) {
            final TextView itemAddition = (TextView) itemLayout.findViewById(R.id.addition);
            itemAddition.setText("(" + votes + ")");
            itemAddition.setVisibility(View.VISIBLE);
        }
        detailsList.addView(itemLayout);
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void goDefaultNavigation(View view) {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));
            return;
        }
        NavigationAppFactory.startDefaultNavigationApplication(app.currentGeo(), this, cache, null, null);
        finish();
    }

    /**
     * Tries to navigate to the {@link cgCache} of this activity.
     */
    private void startDefaultNavigation2() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));
            return;
        }
        NavigationAppFactory.startDefaultNavigationApplication2(app.currentGeo(), this, cache, null, null);
        finish();
    }

    @Override
    public void goManual(View view) {
        super.goManual(view);
        finish();
    }

    public static void startActivity(final Context context, int id, final String geocode) {
        final Intent popupIntent = new Intent(context, WaypointPopup.class);
        popupIntent.putExtra(EXTRA_WAYPOINT_ID, id);
        popupIntent.putExtra(EXTRA_GEOCODE, geocode);
        context.startActivity(popupIntent);
    }
}
