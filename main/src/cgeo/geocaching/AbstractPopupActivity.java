package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteRating;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.CompassMiniView;
import cgeo.geocaching.ui.LoggingUI;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public abstract class AbstractPopupActivity extends AbstractActivity implements CacheMenuHandler.ActivityInterface {

    protected Geocache cache = null;
    protected String geocode = null;
    protected CacheDetailsCreator details;

    private TextView cacheDistance = null;
    private CompassMiniView directionView;
    private final int layout;

    private final GeoDirHandler geoUpdate = new GeoDirHandler() {

        private float azimuth = 0;

        @Override
        public void updateGeoData(final IGeoData geo) {
            try {
                if (geo.getCoords() != null && cache != null && cache.getCoords() != null) {
                    cacheDistance.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(cache.getCoords())));
                    cacheDistance.bringToFront();

                    if (directionView != null){
                        updateCompass(geo);
                    }
                }
                onUpdateGeoData(geo);
            } catch (final RuntimeException e) {
                Log.w("Failed to UpdateLocation location.");
            }
        }

        private void updateCompass(IGeoData geo) {
            directionView.updateCurrentCoords(geo.getCoords());
            if (!Settings.isUseCompass() || geo.getSpeed() > 5) { // use GPS when speed is higher than 18 km/h
                setActualHeading(geo.getBearing());
            }
        }


        @Override
        public void updateDirection(float direction) {
            if (!Settings.isLiveList() || directionView == null){
                return;
            }

            if (app.currentGeo().getSpeed() <= 5) { // use compass when speed is lower than 18 km/h) {
                final float northHeading = DirectionProvider.getDirectionNow(AbstractPopupActivity.this, direction);
                setActualHeading(northHeading);
            }
        }

        private void setActualHeading(final float direction) {
            if (Math.abs(AngleUtils.difference(azimuth, direction)) < 5) {
                return;
            }

            azimuth = direction;
            directionView.updateAzimuth(azimuth);
        }
    };

    /**
     * Callback to run when new location information is available.
     * This may be overridden by deriving classes. The default implementation does nothing.
     *
     * @param geo
     *            the new data
     */
    public void onUpdateGeoData(final IGeoData geo) {
    }

    protected AbstractPopupActivity(int layout) {
        this.layout = layout;
    }

    private void aquireGCVote() {
        if (!Settings.isRatingWanted()) {
            return;
        }
        if (!cache.supportsGCVote()) {
            return;
        }

        (new Thread("Load GCVote") {
            @Override
            public void run() {
                final GCVoteRating rating = GCVote.getRating(cache.getGuid(), geocode);

                if (rating == null) {
                    return;
                }
                cache.setRating(rating.getRating());
                cache.setVotes(rating.getVotes());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        details.addRating(cache);                    }
                });
            }
        }).start();
    }

    protected void init() {
        cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);

        if (cache == null) {
            showToast(res.getString(R.string.err_detail_cache_find));

            finish();
            return;
        }

        geocode = cache.getGeocode();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set theme
        this.setTheme(ActivityMixin.getDialogTheme());
        // set layout
        setContentView(layout);

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
        }

        if (StringUtils.isBlank(geocode)) {
            showToast(res.getString(R.string.err_detail_cache_find));

            finish();
            return;
        }

        final ImageView defaultNavigationImageView = (ImageView) findViewById(R.id.defaultNavigation);
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
        CacheMenuHandler.addMenuItems(this, menu, cache);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (CacheMenuHandler.onMenuItemSelected(item, this, cache)) {
            return true;
        }
        if (LoggingUI.onMenuItemSelected(item, this, cache)) {
            return true;
        }

        return true;
    }

    @Override
    public void onPause() {
        geoUpdate.stopGeoAndDir();
        super.onPause();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            CacheMenuHandler.onPrepareOptionsMenu(menu, cache);
            LoggingUI.onPrepareOptionsMenu(menu, cache);
        } catch (final RuntimeException e) {
            // nothing
        }

        return true;
    }

    protected abstract Geopoint getCoordinates();

    @Override
    public void onResume() {
        super.onResume();
        init();
        geoUpdate.startGeo();
        if (Settings.isLiveMap()) {
            geoUpdate.startDir();
        }
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

    protected abstract void startDefaultNavigation2();

    protected final void addCacheDetails() {
        assert cache != null;
        // cache type
        final String cacheType = cache.getType().getL10n();
        final String cacheSize = cache.getSize() != CacheSize.UNKNOWN ? " (" + cache.getSize().getL10n() + ")" : "";
        details.add(R.string.cache_type, cacheType + cacheSize);

        details.add(R.string.cache_geocode, cache.getGeocode());
        details.addCacheState(cache);

        details.addDistance(cache, cacheDistance);
        cacheDistance = details.getValueView();

        if (Settings.isLiveList()) {
            directionView = details.addDirection(cache, (RelativeLayout) cacheDistance.getParent());
        }

        details.addDifficulty(cache);
        details.addTerrain(cache);
        details.addEventDate(cache);

        // rating
        if (cache.getRating() > 0) {
            details.addRating(cache);
        } else {
            aquireGCVote();
        }

        // favorite count
        details.add(R.string.cache_favorite, cache.getFavoritePoints() + "×");

        // more details
        final Button buttonMore = (Button) findViewById(R.id.more_details);
        buttonMore.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                CacheDetailActivity.startActivity(AbstractPopupActivity.this, geocode);
                finish();
            }
        });
    }

    @Override
    public void cachesAround() {
        final Geopoint coords = getCoordinates();
        if (coords == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }
        CacheListActivity.startActivityCoordinates(this, coords);
        finish();
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public final void goDefaultNavigation(View view) {
        navigateTo();
        finish();
    }

}
