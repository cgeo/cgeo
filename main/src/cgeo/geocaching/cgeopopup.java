package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteRating;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class cgeopopup extends AbstractActivity {

    private boolean fromDetail = false;
    private LayoutInflater inflater = null;
    private String geocode = null;
    private cgCache cache = null;
    private cgGeo geo = null;
    private UpdateLocationCallback geoUpdate = new update();
    private ProgressDialog storeDialog = null;
    private ProgressDialog dropDialog = null;
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
    private CancellableHandler storeCacheHandler = new CancellableHandler() {

        @Override
        public void handleRegularMessage(Message msg) {
            try {
                if (storeDialog != null) {
                    storeDialog.dismiss();
                }

                finish();
                return;
            } catch (Exception e) {
                showToast(res.getString(R.string.err_store));

                Log.e(Settings.tag, "cgeopopup.storeCacheHandler: " + e.toString());
            }

            if (storeDialog != null) {
                storeDialog.dismiss();
            }
            init();
        }
    };
    private Handler dropCacheHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (dropDialog != null) {
                    dropDialog.dismiss();
                }

                finish();
                return;
            } catch (Exception e) {
                showToast(res.getString(R.string.err_drop));

                Log.e(Settings.tag, "cgeopopup.dropCacheHandler: " + e.toString());
            }

            if (dropDialog != null) {
                dropDialog.dismiss();
            }
            init();
        }
    };

    public cgeopopup() {
        super("c:geo-cache-info");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set layout
        setTheme(R.style.transparent);
        setContentView(R.layout.popup);
        setTitle(res.getString(R.string.detail));

        // get parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            fromDetail = extras.getBoolean("fromdetail");
            geocode = extras.getString("geocode");
        }

        if (StringUtils.isBlank(geocode)) {
            showToast(res.getString(R.string.err_detail_cache_find));

            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 2, 0, res.getString(R.string.cache_menu_compass)).setIcon(android.R.drawable.ic_menu_compass); // compass

        SubMenu subMenu = menu.addSubMenu(1, 0, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_mapmode);
        NavigationAppFactory.addMenuItems(subMenu, this, res);
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
                menu.findItem(0).setVisible(true);
                menu.findItem(2).setVisible(true);
                menu.findItem(5).setVisible(true);
            } else {
                menu.findItem(0).setVisible(false);
                menu.findItem(2).setVisible(false);
                menu.findItem(5).setVisible(false);
            }

            boolean visitPossible = !fromDetail && Settings.isLogin();
            menu.findItem(MENU_LOG_VISIT).setEnabled(visitPossible);
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

        if (NavigationAppFactory.onMenuItemSelected(item, geo, this, res, cache, null, null, null)) {
            return true;
        }

        int logType = menuItem - MENU_LOG_VISIT_OFFLINE;
        cache.logOffline(this, logType);
        return true;
    }

    private void init() {
        if (geo == null) {
            geo = app.startGeo(geoUpdate);
        }

        app.setAction(geocode);

        cache = app.getCacheByGeocode(geocode);

        if (cache == null) {
            showToast(res.getString(R.string.err_detail_cache_find));

            finish();
            return;
        }

        try {
            RelativeLayout itemLayout;
            TextView itemName;
            TextView itemValue;
            LinearLayout itemStars;

            if (StringUtils.isNotBlank(cache.getName())) {
                setTitle(cache.getName());
            } else {
                setTitle(geocode.toUpperCase());
            }

            inflater = getLayoutInflater();
            geocode = cache.getGeocode().toUpperCase();

            ((ScrollView) findViewById(R.id.details_list_box)).setVisibility(View.VISIBLE);
            LinearLayout detailsList = (LinearLayout) findViewById(R.id.details_list);
            detailsList.removeAllViews();

            // actionbar icon
            ((TextView) findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(cgBase.getCacheIcon(cache.getType())), null, null, null);

            // cache type
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);

            itemName.setText(res.getString(R.string.cache_type));

            String cacheType = cache.getType() != CacheType.UNKNOWN ? cache.getType().getL10n() : CacheType.MYSTERY.getL10n();
            if (cache.getSize() != null) {
                itemValue.setText(cacheType
                        + " (" + cache.getSize().getL10n() + ")");
            } else {
                itemValue.setText(cacheType);
            }
            detailsList.addView(itemLayout);

            // gc-code
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);

            itemName.setText(res.getString(R.string.cache_geocode));
            itemValue.setText(cache.getGeocode().toUpperCase());
            detailsList.addView(itemLayout);

            // cache state
            if (cache.isArchived() || cache.isDisabled() || cache.isMembers() || cache.isFound()) {
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
                if (cache.isMembers()) {
                    if (state.length() > 0) {
                        state.append(", ");
                    }
                    state.append(res.getString(R.string.cache_status_premium));
                }

                itemValue.setText(state.toString());
                detailsList.addView(itemLayout);

                state = null;
            }

            // distance
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);

            itemName.setText(res.getString(R.string.cache_distance));
            itemValue.setText("--");
            detailsList.addView(itemLayout);
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
                detailsList.addView(itemLayout);
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
                detailsList.addView(itemLayout);
            }

            // rating
            if (cache.getRating() != null && cache.getRating() > 0) {
                setRating(cache.getRating(), cache.getVotes());
            } else {
                if (Settings.isRatingWanted() && cache.supportsGCVote()) {
                    (new Thread() {

                        @Override
                        public void run() {
                            GCVoteRating rating = GCVote.getRating(cache.getGuid(), geocode);

                            Message msg = new Message();
                            Bundle bundle = new Bundle();

                            if (rating == null) {
                                return;
                            }

                            bundle.putFloat("rating", rating.getRating());
                            bundle.putInt("votes", rating.getVotes());
                            msg.setData(bundle);

                            ratingHandler.sendMessage(msg);
                        }
                    }).start();
                }
            }

            // more details
            if (!fromDetail) {
                ((LinearLayout) findViewById(R.id.more_details_box)).setVisibility(View.VISIBLE);

                Button buttonMore = (Button) findViewById(R.id.more_details);
                buttonMore.setOnClickListener(new OnClickListener() {

                    public void onClick(View arg0) {
                        Intent cachesIntent = new Intent(cgeopopup.this, CacheDetailActivity.class);
                        cachesIntent.putExtra("geocode", geocode.toUpperCase());
                        startActivity(cachesIntent);

                        finish();
                        return;
                    }
                });
            } else {
                ((LinearLayout) findViewById(R.id.more_details_box)).setVisibility(View.GONE);
            }

            if (!fromDetail) {
                ((LinearLayout) findViewById(R.id.offline_box)).setVisibility(View.VISIBLE);

                // offline use
                final TextView offlineText = (TextView) findViewById(R.id.offline_text);
                final Button offlineRefresh = (Button) findViewById(R.id.offline_refresh);
                final Button offlineStore = (Button) findViewById(R.id.offline_store);

                if (cache.getReason() > 0) {
                    Long diff = (System.currentTimeMillis() / (60 * 1000)) - (cache.getDetailedUpdate() / (60 * 1000)); // minutes

                    String ago = "";
                    if (diff < 15) {
                        ago = res.getString(R.string.cache_offline_time_mins_few);
                    } else if (diff < 50) {
                        ago = res.getString(R.string.cache_offline_time_about) + " " + diff + " " + res.getString(R.string.cache_offline_time_mins);
                    } else if (diff < 90) {
                        ago = res.getString(R.string.cache_offline_time_about) + " " + res.getString(R.string.cache_offline_time_hour);
                    } else if (diff < (48 * 60)) {
                        ago = res.getString(R.string.cache_offline_time_about) + " " + (diff / 60) + " " + res.getString(R.string.cache_offline_time_hours);
                    } else {
                        ago = res.getString(R.string.cache_offline_time_about) + " " + (diff / (24 * 60)) + " " + res.getString(R.string.cache_offline_time_days);
                    }

                    offlineText.setText(res.getString(R.string.cache_offline_stored) + "\n" + ago);

                    offlineRefresh.setVisibility(View.VISIBLE);
                    offlineRefresh.setEnabled(true);
                    offlineRefresh.setOnClickListener(new storeCache());

                    offlineStore.setText(res.getString(R.string.cache_offline_drop));
                    offlineStore.setEnabled(true);
                    offlineStore.setOnClickListener(new dropCache());
                } else {
                    offlineText.setText(res.getString(R.string.cache_offline_not_ready));

                    offlineRefresh.setVisibility(View.GONE);
                    offlineRefresh.setEnabled(false);
                    offlineRefresh.setOnTouchListener(null);
                    offlineRefresh.setOnClickListener(null);

                    offlineStore.setText(res.getString(R.string.cache_offline_store));
                    offlineStore.setEnabled(true);
                    offlineStore.setOnClickListener(new storeCache());
                }
            } else {
                ((LinearLayout) findViewById(R.id.offline_box)).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeopopup.init: " + e.toString());
        }

        if (geo != null) {
            geoUpdate.updateLocation(geo);
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

        init();
    }

    @Override
    public void onDestroy() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onDestroy();
    }

    @Override
    public void onStop() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onStop();
    }

    @Override
    public void onPause() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onPause();
    }

    private class update implements UpdateLocationCallback {

        @Override
        public void updateLocation(cgGeo geo) {
            if (geo == null) {
                return;
            }

            try {
                if (geo.coordsNow != null && cache != null && cache.getCoords() != null) {
                    cacheDistance.setText(cgBase.getHumanDistance(geo.coordsNow.distanceTo(cache.getCoords())));
                    cacheDistance.bringToFront();
                }
            } catch (Exception e) {
                Log.w(Settings.tag, "Failed to update location.");
            }
        }
    }

    private void navigateTo() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
        }

        cgeonavigate navigateActivity = new cgeonavigate();

        Intent navigateIntent = new Intent(this, navigateActivity.getClass());
        navigateIntent.putExtra("latitude", cache.getCoords().getLatitude());
        navigateIntent.putExtra("longitude", cache.getCoords().getLongitude());
        navigateIntent.putExtra("geocode", "");
        navigateIntent.putExtra("name", res.getString(R.string.search_some_destination));

        startActivity(navigateIntent);
    }

    private void cachesAround() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
        }

        cgeocaches.startActivityCachesAround(this, cache.getCoords());

        finish();
    }

    private class storeCache implements View.OnClickListener {

        public void onClick(View arg0) {
            if (dropDialog != null && dropDialog.isShowing()) {
                showToast("Still removing this cache.");
                return;
            }

            storeDialog = ProgressDialog.show(cgeopopup.this, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true);
            storeDialog.setCancelable(false);
            Thread thread = new storeCacheThread(storeCacheHandler);
            thread.start();
        }
    }

    private class storeCacheThread extends Thread {

        final private CancellableHandler handler;

        public storeCacheThread(final CancellableHandler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            cgBase.storeCache(app, cgeopopup.this, cache, null, 1, handler);
        }
    }

    private class dropCache implements View.OnClickListener {

        public void onClick(View arg0) {
            if (storeDialog != null && storeDialog.isShowing()) {
                showToast("Still saving this cache.");
                return;
            }

            dropDialog = ProgressDialog.show(cgeopopup.this, res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true);
            dropDialog.setCancelable(false);
            Thread thread = new dropCacheThread(dropCacheHandler);
            thread.start();
        }
    }

    private class dropCacheThread extends Thread {

        private Handler handler = null;

        public dropCacheThread(Handler handlerIn) {
            handler = handlerIn;
        }

        @Override
        public void run() {
            cgBase.dropCache(app, cache, handler);
        }
    }

    private void setRating(Float rating, Integer votes) {
        if (rating == null || rating <= 0) {
            return;
        }

        RelativeLayout itemLayout;
        TextView itemName;
        TextView itemValue;
        LinearLayout itemStars;
        LinearLayout detailsList = (LinearLayout) findViewById(R.id.details_list);

        itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_layout, null);
        itemName = (TextView) itemLayout.findViewById(R.id.name);
        itemValue = (TextView) itemLayout.findViewById(R.id.value);
        itemStars = (LinearLayout) itemLayout.findViewById(R.id.stars);

        itemName.setText(res.getString(R.string.cache_rating));
        itemValue.setText(String.format("%.1f", rating) + ' ' + res.getString(R.string.cache_rating_of) + " 5");
        itemStars.addView(cgBase.createStarRating(rating, 5, this), 1);

        if (votes != null) {
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
    public void goCompass(View view) {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));

            return;
        }

        cgeonavigate navigateActivity = new cgeonavigate();

        Intent navigateIntent = new Intent(cgeopopup.this, navigateActivity.getClass());
        navigateIntent.putExtra("latitude", cache.getCoords().getLatitude());
        navigateIntent.putExtra("longitude", cache.getCoords().getLongitude());
        navigateIntent.putExtra("geocode", cache.getGeocode().toUpperCase());
        navigateIntent.putExtra("name", cache.getName());

        startActivity(navigateIntent);

        finish();
    }

    @Override
    public void goManual(View view) {
        super.goManual(view);
        finish();
    }
}
