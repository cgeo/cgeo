package cgeo.geocaching;

import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.databinding.PopupBinding;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class CachePopupFragment extends AbstractDialogFragmentWithProximityNotification {
    private final Progress progress = new Progress();
    private PopupBinding binding;

    public static DialogFragment newInstance(final String geocode) {

        final Bundle args = new Bundle();
        args.putString(GEOCODE_ARG, geocode);

        final DialogFragment f = new CachePopupFragment();
        f.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        f.setArguments(args);

        return f;
    }

    private static class StoreCacheHandler extends DisposableHandler {
        private final int progressMessage;
        private final WeakReference<CachePopupFragment> popupRef;

        StoreCacheHandler(final CachePopupFragment popup, final int progressMessage) {
            this.progressMessage = progressMessage;
            popupRef = new WeakReference<>(popup);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            if (msg.what == UPDATE_LOAD_PROGRESS_DETAIL && msg.obj instanceof String) {
                updateStatusMsg((String) msg.obj);
            } else {
                final CachePopupFragment popup = popupRef.get();
                if (popup != null) {
                    popup.init();
                }
            }
        }

        private void updateStatusMsg(final String msg) {
            final CachePopupFragment popup = popupRef.get();
            if (popup == null || !popup.isAdded()) {
                return;
            }
            popup.progress.setMessage(popup.getString(progressMessage)
                    + "\n\n"
                    + msg);
        }
    }

    private static class DropCacheHandler extends WeakReferenceHandler<CachePopupFragment> {

        DropCacheHandler(final CachePopupFragment popup) {
            super(popup);
        }

        @Override
        public void handleMessage(final Message msg) {
            final CachePopupFragment popup = getReference();
            if (popup == null) {
                return;
            }
            popup.getActivity().finish();
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        binding = PopupBinding.inflate(getLayoutInflater(), container, false);
        final View v = binding.getRoot();
        initCustomActionBar(v);
        return v;
    }

    @Override
    protected void init() {
        super.init();
        try {
            if (null != proximityNotification) {
                proximityNotification.setReferencePoint(cache.getCoords());
                proximityNotification.setTextNotifications(getContext());
            }

            final Toolbar toolbar = binding.toolbar.toolbar;
            toolbar.setTitle(geocode);
            toolbar.setLogo(MapMarkerUtils.getCacheMarker(getResources(), cache, CacheListType.MAP).getDrawable());
            toolbar.setLongClickable(true);
            toolbar.setOnLongClickListener(v -> {
                if (cache.isOffline()) {
                    EmojiUtils.selectEmojiPopup(CachePopupFragment.this.requireContext(), cache.getAssignedEmoji(), cache, newCacheIcon -> {
                        cache.setAssignedEmoji(newCacheIcon);
                        toolbar.setLogo(MapMarkerUtils.getCacheMarker(getResources(), cache, CacheListType.MAP).getDrawable());
                        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
                    });
                    return true;
                }
                return false;
            });

            binding.title.setText(TextUtils.coloredCacheText(getActivity(), cache, cache.getName()));
            details = new CacheDetailsCreator(getActivity(), binding.detailsList);

            addCacheDetails(false);

            // offline use
            CacheDetailActivity.updateOfflineBox(binding.getRoot(), cache, res, new RefreshCacheClickListener(), new DropCacheClickListener(), new StoreCacheClickListener(), new ShowHintClickListener(binding), null, new StoreCacheClickListener());

            CacheDetailActivity.updateCacheLists(binding.getRoot(), cache, res);

        } catch (final Exception e) {
            Log.e("CachePopupFragment.init", e);
        }

        // cache is loaded. remove progress-popup if any there
        progress.dismiss();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        final int menuItem = item.getItemId();

        if (menuItem == R.id.menu_tts_toggle) {
            SpeechService.toggleService(getActivity(), cache.getCoords());
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    private class StoreCacheClickListener implements View.OnClickListener, View.OnLongClickListener {
        @Override
        public void onClick(final View arg0) {
            selectListsAndStore(false);
        }

        @Override
        public boolean onLongClick(final View v) {
            selectListsAndStore(true);
            return true;
        }

        private void selectListsAndStore(final boolean fastStoreOnLastSelection) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return;
            }

            if (Settings.getChooseList() || cache.isOffline()) {
                // let user select list to store cache in
                new StoredList.UserInterface(getActivity()).promptForMultiListSelection(R.string.lists_title,
                        this::storeCacheOnLists, true, cache.getLists(), fastStoreOnLastSelection);
            } else {
                storeCacheOnLists(Collections.singleton(StoredList.STANDARD_LIST_ID));
            }
        }

        private void storeCacheOnLists(final Set<Integer> listIds) {
            if (cache.isOffline()) {
                // cache already offline, just add to another list
                DataStore.saveLists(Collections.singletonList(cache), listIds);
                CacheDetailActivity.updateOfflineBox(getView(), cache, res,
                        new RefreshCacheClickListener(), new DropCacheClickListener(),
                        new StoreCacheClickListener(), new ShowHintClickListener(binding), null, new StoreCacheClickListener());
                CacheDetailActivity.updateCacheLists(getView(), cache, res);
            } else {
                final StoreCacheHandler storeCacheHandler = new StoreCacheHandler(CachePopupFragment.this, R.string.cache_dialog_offline_save_message);
                final FragmentActivity activity = requireActivity();
                progress.show(activity, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true, storeCacheHandler.disposeMessage());
                AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> cache.store(listIds, storeCacheHandler), () -> {
                    activity.invalidateOptionsMenu();
                    final View view = getView();
                    if (view != null) {
                        CacheDetailActivity.updateOfflineBox(view, cache, res,
                                new RefreshCacheClickListener(), new DropCacheClickListener(),
                                new StoreCacheClickListener(), new ShowHintClickListener(binding), null, new StoreCacheClickListener());
                        CacheDetailActivity.updateCacheLists(view, cache, res);
                    }
                });
            }
        }
    }

    private class RefreshCacheClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View arg0) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return;
            }

            if (!Network.isConnected()) {
                showToast(getString(R.string.err_server));
                return;
            }

            final StoreCacheHandler refreshCacheHandler = new StoreCacheHandler(CachePopupFragment.this, R.string.cache_dialog_offline_save_message);
            progress.show(getActivity(), res.getString(R.string.cache_dialog_refresh_title), res.getString(R.string.cache_dialog_refresh_message), true, refreshCacheHandler.disposeMessage());
            cache.refresh(refreshCacheHandler, AndroidRxUtils.networkScheduler);
        }
    }

    private class DropCacheClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View arg0) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return;
            }

            final DropCacheHandler dropCacheHandler = new DropCacheHandler(CachePopupFragment.this);
            progress.show(getActivity(), res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true, null);
            cache.drop(dropCacheHandler);
        }
    }

    private static class ShowHintClickListener implements View.OnClickListener {
        private final PopupBinding binding;

        ShowHintClickListener(final PopupBinding binding) {
            this.binding = binding;
        }

        @Override
        public void onClick(final View view) {
            final TextView offlineHintText = binding.offlineHintText;
            final View offlineHintSeparator = binding.offlineHintSeparator;
            if (offlineHintText.getVisibility() == View.VISIBLE) {
                offlineHintText.setVisibility(View.GONE);
                offlineHintSeparator.setVisibility(View.GONE);
            } else {
                offlineHintText.setVisibility(View.VISIBLE);
                offlineHintSeparator.setVisibility(View.VISIBLE);
            }
        }
    }


    @Override
    public void showNavigationMenu() {
        NavigationAppFactory.showNavigationMenu(getActivity(), cache, null, null, true, true);
    }

    /**
     * Tries to navigate to the {@link Geocache} of this activity.
     */
    @Override
    public void startDefaultNavigation() {
        NavigationAppFactory.startDefaultNavigationApplication(1, getActivity(), cache);
    }

    /**
     * Tries to navigate to the {@link Geocache} of this activity.
     */
    @Override
    public void startDefaultNavigation2() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));
            return;
        }
        NavigationAppFactory.startDefaultNavigationApplication(2, getActivity(), cache);
        getActivity().finish();
    }

    @Override
    protected TargetInfo getTargetInfo() {
        if (cache == null) {
            return null;
        }
        return new TargetInfo(cache.getCoords(), cache.getGeocode());
    }


}
