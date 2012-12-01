package cgeo.geocaching;

import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.downloadservice.CacheDownloadService;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

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
    private final Progress progress = new Progress();

    private class DropCacheHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            init();
        }
    }

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
                setTitle(geocode);
            }

            // actionbar icon
            ((TextView) findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(cache.getType().markerId), null, null, null);

            details = new CacheDetailsCreator(this, (LinearLayout) findViewById(R.id.details_list));

            addCacheDetails();

            findViewById(R.id.offline_box).setVisibility(View.VISIBLE);

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
                offlineRefresh.setOnClickListener(new RefreshCacheClickListener());

                offlineStore.setText(res.getString(R.string.cache_offline_drop));
                offlineStore.setEnabled(true);
                offlineStore.setOnClickListener(new DropCacheClickListener());
            } else {
                offlineText.setText(res.getString(R.string.cache_offline_not_ready));

                offlineRefresh.setVisibility(View.GONE);
                offlineRefresh.setEnabled(false);
                offlineRefresh.setOnTouchListener(null);
                offlineRefresh.setOnClickListener(null);

                offlineStore.setText(res.getString(R.string.cache_offline_store));
                offlineStore.setEnabled(true);
                offlineStore.setOnClickListener(new StoreCacheClickListener());
            }
        } catch (Exception e) {
            Log.e("cgeopopup.init: " + e.toString());
        }

        // cache is loaded. remove progress-popup if any there
        progress.dismiss();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    private class StoreCacheClickListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            Intent serviceIntent = new Intent(getApplicationContext(), CacheDownloadService.class);
            serviceIntent.putExtra(CacheDownloadService.EXTRA_GEOCODE, cache.getGeocode());
            startService(serviceIntent);
            finish();
        }
    }

    private class RefreshCacheClickListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            Intent serviceIntent = new Intent(getApplicationContext(), CacheDownloadService.class);
            serviceIntent.putExtra(CacheDownloadService.EXTRA_GEOCODE, cache.getGeocode());
            serviceIntent.putExtra(CacheDownloadService.EXTRA_REFRESH, true);
            serviceIntent.putExtra(CacheDownloadService.EXTRA_LIST_ID, cache.getListId());
            startService(serviceIntent);
            finish();
        }
    }

    private class DropCacheClickListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return;
            }
            final DropCacheHandler dropCacheHandler = new DropCacheHandler();
            progress.show(CachePopup.this, res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true, null);
            new DropCacheThread(dropCacheHandler).start();
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
        NavigationAppFactory.startDefaultNavigationApplication(1, this, cache);
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
        NavigationAppFactory.startDefaultNavigationApplication(2, this, cache);
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
