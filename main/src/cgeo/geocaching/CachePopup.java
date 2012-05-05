package cgeo.geocaching;

import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CachePopup extends AbstractPopupActivity {
    private ProgressDialog storeDialog = null;
    private ProgressDialog dropDialog = null;
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

                Log.e("cgeopopup.storeCacheHandler: " + e.toString());
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

                Log.e("cgeopopup.dropCacheHandler: " + e.toString());
            }

            if (dropDialog != null) {
                dropDialog.dismiss();
            }
            init();
        }
    };

    public CachePopup() {
        super("c:geo-cache-info", R.layout.popup);
    }

    @Override
    protected void logOffline(final int menuItem) {
        cache.logOffline(this, LogType.getById(menuItem - MENU_LOG_VISIT_OFFLINE));
    }

    @Override
    protected void showInBrowser() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.getGeocode())));
    }

    @Override
    protected void logVisit() {
        cache.logVisit(this);
        finish();
    }

    @Override
    protected void showNavigationMenu() {
        NavigationAppFactory.showNavigationMenu(app.currentGeo(), this, cache, null, null);
    }

    @Override
    protected void init() {
        super.init();
        try {
            RelativeLayout itemLayout;
            TextView itemName;
            TextView itemValue;

            if (StringUtils.isNotBlank(cache.getName())) {
                setTitle(cache.getName());
            } else {
                setTitle(geocode.toUpperCase());
            }

            LinearLayout detailsList = (LinearLayout) findViewById(R.id.details_list);
            detailsList.removeAllViews();

            // actionbar icon
            ((TextView) findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(cache.getType().markerId), null, null, null);

            // cache type
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);

            itemName.setText(res.getString(R.string.cache_type));

            String cacheType = cache.getType().getL10n();
            String cacheSize = cache.getSize() != CacheSize.UNKNOWN ? " (" + cache.getSize().getL10n() + ")" : "";
            itemValue.setText(cacheType + cacheSize);
            detailsList.addView(itemLayout);

            // gc-code
            itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
            itemName = (TextView) itemLayout.findViewById(R.id.name);
            itemValue = (TextView) itemLayout.findViewById(R.id.value);

            itemName.setText(res.getString(R.string.cache_geocode));
            itemValue.setText(cache.getGeocode().toUpperCase());
            detailsList.addView(itemLayout);

            // cache state
            if (cache.isArchived() || cache.isDisabled() || cache.isPremiumMembersOnly() || cache.isFound()) {
                itemLayout = createCacheState();
                detailsList.addView(itemLayout);
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
                itemLayout = createDifficulty();
                detailsList.addView(itemLayout);
            }

            // terrain
            if (cache.getTerrain() > 0f) {
                itemLayout = createTerrain();
                detailsList.addView(itemLayout);
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
                    Intent cachesIntent = new Intent(CachePopup.this, CacheDetailActivity.class);
                    cachesIntent.putExtra(EXTRA_GEOCODE, geocode.toUpperCase());
                    startActivity(cachesIntent);

                    finish();
                }
            });

            ((LinearLayout) findViewById(R.id.offline_box)).setVisibility(View.VISIBLE);

            // offline use
            final TextView offlineText = (TextView) findViewById(R.id.offline_text);
            final Button offlineRefresh = (Button) findViewById(R.id.offline_refresh);
            final Button offlineStore = (Button) findViewById(R.id.offline_store);

            if (cache.getListId() > 0) {
                long diff = (System.currentTimeMillis() / (60 * 1000)) - (cache.getDetailedUpdate() / (60 * 1000)); // minutes

                String ago;
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
    protected void navigateTo() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        NavigationAppFactory.startDefaultNavigationApplication(app.currentGeo(), this, cache, null, null);
    }

    @Override
    protected void cachesAround() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        cgeocaches.startActivityCoordinates(this, cache.getCoords());

        finish();
    }

    private class storeCache implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            if (dropDialog != null && dropDialog.isShowing()) {
                showToast("Still removing this cache.");
                return;
            }

            storeDialog = ProgressDialog.show(CachePopup.this, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true);
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
            cache.store(handler);
            invalidateOptionsMenuCompatible();
        }
    }

    private class dropCache implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            if (storeDialog != null && storeDialog.isShowing()) {
                showToast("Still saving this cache.");
                return;
            }

            dropDialog = ProgressDialog.show(CachePopup.this, res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true);
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
            cache.drop(handler);
        }
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
    @Override
    protected void startDefaultNavigation2() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));
            return;
        }
        NavigationAppFactory.startDefaultNavigationApplication2(app.currentGeo(), this, cache, null, null);
        finish();
    }

    public static void startActivity(final Context context, final String geocode) {
        final Intent popupIntent = new Intent(context, CachePopup.class);
        popupIntent.putExtra(EXTRA_GEOCODE, geocode);
        context.startActivity(popupIntent);
    }
}
