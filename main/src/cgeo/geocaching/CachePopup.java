package cgeo.geocaching;

import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CachePopup extends AbstractPopupActivity {
    private ProgressDialog storeDialog = null;
    private ProgressDialog dropDialog = null;
    private final CancellableHandler storeCacheHandler = new CancellableHandler() {

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
    private final Handler dropCacheHandler = new Handler() {

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
    protected void showNavigationMenu() {
        NavigationAppFactory.showNavigationMenu(this, cache, null, null);
    }

    @Override
    protected void init() {
        super.init();
        try {
            if (StringUtils.isNotBlank(cache.getName())) {
                setTitle(cache.getName());
            } else {
                setTitle(geocode.toUpperCase());
            }

            // actionbar icon
            ((TextView) findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(cache.getType().markerId), null, null, null);

            details = new CacheDetailsCreator(this, (LinearLayout) findViewById(R.id.details_list));

            addCacheDetails();

            ((LinearLayout) findViewById(R.id.offline_box)).setVisibility(View.VISIBLE);

            // offline use
            final TextView offlineText = (TextView) findViewById(R.id.offline_text);
            final Button offlineRefresh = (Button) findViewById(R.id.offline_refresh);
            final Button offlineStore = (Button) findViewById(R.id.offline_store);

            if (cache.getListId() > 0) {
                final long diff = (System.currentTimeMillis() / (60 * 1000)) - (cache.getDetailedUpdate() / (60 * 1000)); // minutes

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
                offlineRefresh.setOnClickListener(new StoreCache());

                offlineStore.setText(res.getString(R.string.cache_offline_drop));
                offlineStore.setEnabled(true);
                offlineStore.setOnClickListener(new DropCache());
            } else {
                offlineText.setText(res.getString(R.string.cache_offline_not_ready));

                offlineRefresh.setVisibility(View.GONE);
                offlineRefresh.setEnabled(false);
                offlineRefresh.setOnTouchListener(null);
                offlineRefresh.setOnClickListener(null);

                offlineStore.setText(res.getString(R.string.cache_offline_store));
                offlineStore.setEnabled(true);
                offlineStore.setOnClickListener(new StoreCache());
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

    private class StoreCache implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            if (dropDialog != null && dropDialog.isShowing()) {
                showToast("Still removing this cache.");
                return;
            }

            storeDialog = ProgressDialog.show(CachePopup.this, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true);
            storeDialog.setCancelable(false);
            final Thread thread = new StoreCacheThread(storeCacheHandler);
            thread.start();
        }
    }

    private class StoreCacheThread extends Thread {

        final private CancellableHandler handler;

        public StoreCacheThread(final CancellableHandler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            cache.store(handler);
            invalidateOptionsMenuCompatible();
        }
    }

    private class DropCache implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            if (storeDialog != null && storeDialog.isShowing()) {
                showToast("Still saving this cache.");
                return;
            }

            dropDialog = ProgressDialog.show(CachePopup.this, res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true);
            dropDialog.setCancelable(false);
            final Thread thread = new DropCacheThread(dropCacheHandler);
            thread.start();
        }
    }

    private class DropCacheThread extends Thread {
        final private Handler handler;

        public DropCacheThread(Handler handlerIn) {
            handler = handlerIn;
        }

        @Override
        public void run() {
            cache.drop(handler);
        }
    }

    @Override
    protected void navigateTo() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return;
        }

        NavigationAppFactory.startDefaultNavigationApplication(this, cache, null, null);
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
        NavigationAppFactory.startDefaultNavigationApplication2(this, cache, null, null);
        finish();
    }

    public static void startActivity(final Context context, final String geocode) {
        final Intent popupIntent = new Intent(context, CachePopup.class);
        popupIntent.putExtra(EXTRA_GEOCODE, geocode);
        context.startActivity(popupIntent);
    }

    @Override
    protected Geopoint getCoordinates() {
        if (cache == null) {
            return null;
        }
        return cache.getCoords();
    }
}
