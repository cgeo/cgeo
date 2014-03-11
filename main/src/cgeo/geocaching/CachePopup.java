package cgeo.geocaching;

import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import rx.Scheduler.Inner;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CachePopup extends AbstractPopupActivity {
    private final Progress progress = new Progress();

    private class StoreCacheHandler extends CancellableHandler {
        private final int progressMessage;

        public StoreCacheHandler(final int progressMessage) {
            this.progressMessage = progressMessage;
        }

        @Override
        public void handleRegularMessage(Message msg) {
            if (UPDATE_LOAD_PROGRESS_DETAIL == msg.what && msg.obj instanceof String) {
                updateStatusMsg((String) msg.obj);
            } else {
                init();
            }
        }

        private void updateStatusMsg(final String msg) {
            progress.setMessage(res.getString(progressMessage)
                    + "\n\n"
                    + msg);
        }
    }

    private class DropCacheHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            CachePopup.this.finish();
        }
    }

    public CachePopup() {
        super(R.layout.popup);
    }

    @Override
    public void showNavigationMenu() {
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

            // offline use
            CacheDetailActivity.updateOfflineBox(findViewById(android.R.id.content), cache, res, new RefreshCacheClickListener(), new DropCacheClickListener(), new StoreCacheClickListener());

        } catch (Exception e) {
            Log.e("CachePopup.init", e);
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
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return;
            }

            if (Settings.getChooseList()) {
                // let user select list to store cache in
                new StoredList.UserInterface(CachePopup.this).promptForListSelection(R.string.list_title,
                        new Action1<Integer>() {
                            @Override
                            public void call(final Integer selectedListId) {
                                storeCache(selectedListId);
                            }
                        }, true, StoredList.TEMPORARY_LIST_ID);
            } else {
                storeCache(StoredList.TEMPORARY_LIST_ID);
            }
        }

        protected void storeCache(final int listId) {
            final StoreCacheHandler storeCacheHandler = new StoreCacheHandler(R.string.cache_dialog_offline_save_message);
            progress.show(CachePopup.this, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true, storeCacheHandler.cancelMessage());
            Schedulers.io().schedule(new Action1<Inner>() {
                @Override
                public void call(final Inner inner) {
                    cache.store(listId, storeCacheHandler);
                    invalidateOptionsMenuCompatible();
                }
            });
        }
    }

    private class RefreshCacheClickListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return;
            }

            if (!Network.isNetworkConnected(getApplicationContext())) {
                showToast(getString(R.string.err_server));
                return;
            }

            final StoreCacheHandler refreshCacheHandler = new StoreCacheHandler(R.string.cache_dialog_offline_save_message);
            progress.show(CachePopup.this, res.getString(R.string.cache_dialog_refresh_title), res.getString(R.string.cache_dialog_refresh_message), true, refreshCacheHandler.cancelMessage());
            cache.refresh(cache.getListId(), refreshCacheHandler, Schedulers.io());
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
            cache.drop(dropCacheHandler, Schedulers.io());
        }
    }

    @Override
    public void navigateTo() {
        NavigationAppFactory.startDefaultNavigationApplication(1, this, cache);
    }

    /**
     * Tries to navigate to the {@link Geocache} of this activity.
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
        popupIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
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
