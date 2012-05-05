package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.connector.gc.GCMap;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteRating;
import cgeo.geocaching.geopoint.HumanDistance;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Collections;

public abstract class AbstractPopupActivity extends AbstractActivity {

    private static final int MENU_CACHES_AROUND = 5;
    private static final int MENU_NAVIGATION = 3;
    private static final int MENU_DEFAULT_NAVIGATION = 2;
    private static final int MENU_SHOW_IN_BROWSER = 7;
    protected static final String EXTRA_GEOCODE = "geocode";

    protected class UpdateLocation extends GeoObserver {

        @Override
        protected void updateLocation(final IGeoData geo) {
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

    protected LayoutInflater inflater = null;
    protected cgCache cache = null;
    protected TextView cacheDistance = null;
    private GeoObserver geoUpdate = new UpdateLocation();

    protected String geocode = null;

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
    private int layout;

    public AbstractPopupActivity(String helpTopic, int layout) {
        super(helpTopic);
        this.layout = layout;
    }

    protected void aquireGCVote() {
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

    protected RelativeLayout createCacheState() {
        RelativeLayout itemLayout;
        TextView itemName;
        TextView itemValue;
        itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
        itemName = (TextView) itemLayout.findViewById(R.id.name);
        itemValue = (TextView) itemLayout.findViewById(R.id.value);

        itemName.setText(res.getString(R.string.cache_status));

        final StringBuilder state = new StringBuilder();
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
        return itemLayout;
    }

    protected RelativeLayout createDifficulty() {
        RelativeLayout itemLayout;
        TextView itemName;
        TextView itemValue;
        LinearLayout itemStars;
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
        return itemLayout;
    }

    protected RelativeLayout createTerrain() {
        RelativeLayout itemLayout;
        TextView itemName;
        TextView itemValue;
        LinearLayout itemStars;
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
        return itemLayout;
    }

    @Override
    public void goManual(View view) {
        super.goManual(view);
        finish();
    }

    protected void init() {
        app.setAction(geocode);

        cache = app.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);

        if (cache == null) {
            showToast(res.getString(R.string.err_detail_cache_find));

            finish();
            return;
        }

        if (CacheType.UNKNOWN == cache.getType()) {
            SearchResult search = GCMap.searchByGeocodes(Collections.singleton(geocode));
            cache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_ONLY);
        }
        inflater = getLayoutInflater();
        geocode = cache.getGeocode().toUpperCase();

    }

    protected abstract void cachesAround();

    protected abstract void logOffline(int menuItem);

    protected abstract void logVisit();

    protected abstract void navigateTo();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set layout
        setTheme(R.style.transparent);
        setContentView(layout);
        setTitle(res.getString(R.string.detail));

        // get parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(EXTRA_GEOCODE);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_DEFAULT_NAVIGATION, 0, NavigationAppFactory.getDefaultNavigationApplication(this).getName()).setIcon(R.drawable.ic_menu_compass); // default navigation tool
        menu.add(0, MENU_NAVIGATION, 0, res.getString(R.string.cache_menu_navigate)).setIcon(R.drawable.ic_menu_mapmode);
        addVisitMenu(menu, cache);
        menu.add(0, MENU_CACHES_AROUND, 0, res.getString(R.string.cache_menu_around)).setIcon(R.drawable.ic_menu_rotate); // caches around
        menu.add(0, MENU_SHOW_IN_BROWSER, 0, res.getString(R.string.cache_menu_browser)).setIcon(R.drawable.ic_menu_info_details); // browser

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int menuItem = item.getItemId();

        switch (menuItem) {
            case MENU_DEFAULT_NAVIGATION:
                navigateTo();
                break;
            case MENU_NAVIGATION:
                showNavigationMenu();
                break;
            case MENU_CACHES_AROUND:
                cachesAround();
                break;
            case MENU_LOG_VISIT:
                logVisit();
                break;
            case MENU_SHOW_IN_BROWSER:
                showInBrowser();
                break;
            default:
                logOffline(menuItem);
        }

        return true;
    }

    @Override
    public void onPause() {
        app.deleteGeoObserver(geoUpdate);
        super.onPause();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            if (cache != null && cache.getCoords() != null) {
                menu.findItem(MENU_DEFAULT_NAVIGATION).setVisible(true);
                menu.findItem(MENU_NAVIGATION).setVisible(true);
                menu.findItem(MENU_CACHES_AROUND).setVisible(true);
            } else {
                menu.findItem(MENU_DEFAULT_NAVIGATION).setVisible(false);
                menu.findItem(MENU_NAVIGATION).setVisible(false);
                menu.findItem(MENU_CACHES_AROUND).setVisible(false);
            }

            menu.findItem(MENU_LOG_VISIT).setEnabled(Settings.isLogin());
        } catch (Exception e) {
            // nothing
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
        app.addGeoObserver(geoUpdate);
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

    protected void setRating(float rating, int votes) {
        if (rating <= 0) {
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
        itemStars.addView(createStarRating(rating, 5, this), 1);

        if (votes > 0) {
            final TextView itemAddition = (TextView) itemLayout.findViewById(R.id.addition);
            itemAddition.setText("(" + votes + ")");
            itemAddition.setVisibility(View.VISIBLE);
        }
        detailsList.addView(itemLayout);
    }

    protected abstract void showInBrowser();

    protected abstract void showNavigationMenu();

    protected abstract void startDefaultNavigation2();

}
