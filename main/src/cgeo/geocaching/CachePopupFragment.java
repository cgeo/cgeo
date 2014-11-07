package cgeo.geocaching;

import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import org.apache.commons.lang3.StringUtils;

import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CachePopupFragment extends AbstractDialogFragment {
    private final Progress progress = new Progress();

    public static DialogFragment newInstance(String geocode) {

        Bundle args = new Bundle();
        args.putString(GEOCODE_ARG,geocode);

        DialogFragment f = new CachePopupFragment();
        f.setStyle(DialogFragment.STYLE_NO_TITLE,0);
        f.setArguments(args);

        return f;
    }

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
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.popup, container, false);
        initCustomActionBar(v);
        return v;
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

            ((TextView) getView().findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(cache.getType().markerId), null, null, null);

            details = new CacheDetailsCreator(getActivity(), (LinearLayout) getView().findViewById(R.id.details_list));

            addCacheDetails();

            // offline use
            CacheDetailActivity.updateOfflineBox(getView(), cache, res, new RefreshCacheClickListener(), new DropCacheClickListener(), new StoreCacheClickListener());

        } catch (Exception e) {
            Log.e("CachePopupFragment.init", e);
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
                new StoredList.UserInterface(getActivity()).promptForListSelection(R.string.list_title,
                        new Action1<Integer>() {
                            @Override
                            public void call(final Integer selectedListId) {
                                storeCache(selectedListId);
                            }
                        }, true, StoredList.TEMPORARY_LIST.id);
            } else {
                storeCache(StoredList.TEMPORARY_LIST.id);
            }
        }

        protected void storeCache(final int listId) {
            final StoreCacheHandler storeCacheHandler = new StoreCacheHandler(R.string.cache_dialog_offline_save_message);
            progress.show(getActivity(), res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true, storeCacheHandler.cancelMessage());
            Schedulers.io().createWorker().schedule(new Action0() {
                @Override
                public void call() {
                    cache.store(listId, storeCacheHandler);
                    getActivity().supportInvalidateOptionsMenu();
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

            if (!Network.isNetworkConnected()) {
                showToast(getString(R.string.err_server));
                return;
            }

            final StoreCacheHandler refreshCacheHandler = new StoreCacheHandler(R.string.cache_dialog_offline_save_message);
            progress.show(getActivity(), res.getString(R.string.cache_dialog_refresh_title), res.getString(R.string.cache_dialog_refresh_message), true, refreshCacheHandler.cancelMessage());
            cache.refresh(refreshCacheHandler, RxUtils.networkScheduler);
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
            progress.show(getActivity(), res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true, null);
            cache.drop(dropCacheHandler);
        }
    }


    @Override
    public void navigateTo() {
        NavigationAppFactory.startDefaultNavigationApplication(1, getActivity(), cache);
    }

    @Override
    public void showNavigationMenu() {
        NavigationAppFactory.showNavigationMenu(getActivity(), cache, null, null, true, true);
    }


    /**
     * Tries to navigate to the {@link cgeo.geocaching.Geocache} of this activity.
     */
    @Override
    protected void startDefaultNavigation2() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));
            return;
        }
        NavigationAppFactory.startDefaultNavigationApplication(2, getActivity(), cache);
        getActivity().finish();
    }

    @Override
    protected Geopoint getCoordinates() {
        if (cache == null) {
            return null;
        }
        return cache.getCoords();
    }


}
