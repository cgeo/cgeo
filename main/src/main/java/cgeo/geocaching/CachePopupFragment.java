package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractNavigationBarMapActivity;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.databinding.PopupBinding;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.service.CacheDownloaderService;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CacheUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.wherigo.WherigoActivity;
import cgeo.geocaching.wherigo.WherigoUtils;
import cgeo.geocaching.wherigo.WherigoViewUtils;

import android.app.Activity;
import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;

public class CachePopupFragment extends AbstractDialogFragmentWithProximityNotification {
    private final Progress progress = new Progress();
    private PopupBinding binding;

    public static Fragment newInstance(final String geocode) {

        final Bundle args = new Bundle();
        args.putString(GEOCODE_ARG, geocode);

        final Fragment f = new CachePopupFragment();
        //f.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
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
        public void handleMessage(@NonNull final Message msg) {
            final CachePopupFragment popup = getReference();
            if (popup == null) {
                return;
            }
            popup.progress.dismiss();
            ((AbstractNavigationBarMapActivity) popup.requireActivity()).sheetRemoveFragment();
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        binding = PopupBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
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
            toolbar.setLogo(MapMarkerUtils.getCacheMarker(getResources(), cache, CacheListType.MAP, Settings.getIconScaleEverywhere()).getDrawable());
            toolbar.setLongClickable(true);
            toolbar.setOnLongClickListener(v -> {
                if (cache.isOffline()) {
                    EmojiUtils.selectEmojiPopup(CachePopupFragment.this.requireContext(), cache.getAssignedEmoji(), cache, newCacheIcon -> {
                        cache.setAssignedEmoji(newCacheIcon);
                        toolbar.setLogo(MapMarkerUtils.getCacheMarker(getResources(), cache, CacheListType.MAP, Settings.getIconScaleEverywhere()).getDrawable());
                        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
                    });
                    return true;
                }
                return false;
            });
            onCreatePopupOptionsMenu(toolbar, this, cache);
            toolbar.setOnMenuItemClickListener(this::onPopupOptionsItemSelected);

            binding.title.setText(TextUtils.coloredCacheText(getActivity(), cache, StringUtils.defaultIfBlank(cache.getName(), "")));
            details = new CacheDetailsCreator(requireActivity(), binding.detailsList);

            addCacheDetails(false);

            binding.editPersonalnote.setVisibility(View.VISIBLE);
            binding.editPersonalnote.setOnClickListener(arg0 -> {
                CacheDetailActivity.startActivityForEditNote(getActivity(), geocode);
                ((AbstractNavigationBarMapActivity) requireActivity()).sheetRemoveFragment();
            });

            // Wherigo
            final List<String> wherigoGuis = WherigoUtils.getWherigoGuids(cache);
            if (!wherigoGuis.isEmpty()) {
                binding.sendToWherigo.setVisibility(View.VISIBLE);
                binding.sendToWherigo.setOnClickListener(v -> WherigoViewUtils.executeForOneCartridge(requireActivity(), wherigoGuis, guid ->
                        WherigoActivity.startForGuid(requireActivity(), guid, cache.getGeocode(), true)));
            } else {
                binding.sendToWherigo.setVisibility(View.GONE);
            }

            // ALC
            if (CacheUtils.isLabAdventure(cache)) {
                binding.sendToAlc.setVisibility(View.VISIBLE);
                CacheUtils.setLabLink(requireActivity(), binding.sendToAlc, cache.getUrl());
            } else {
                binding.sendToAlc.setVisibility(View.GONE);
            }

            // offline use
            CacheDetailActivity.updateOfflineBox(binding.getRoot(), cache, res, new RefreshCacheClickListener(), new DropCacheClickListener(), new StoreCacheClickListener(), new ShowHintClickListener(binding), new MoveCacheClickListener(), new StoreCacheClickListener());

            CacheDetailActivity.updateCacheLists(binding.getRoot(), cache, res, null);

            updateStoreRefreshButtons(true);
            getLifecycle().addObserver(new GeocacheChangedBroadcastReceiver(getContext()) {
                @Override
                protected void onReceive(final Context context, final String geocode) {
                    if (StringUtils.equals(geocode, CachePopupFragment.this.geocode)) {
                        init();
                    }
                }
            });

        } catch (final Exception e) {
            Log.e("CachePopupFragment.init", e);
        }

        // cache is loaded. remove progress-popup if any there
        progress.dismiss();
    }

    @Override
    public boolean onPopupOptionsItemSelected(@NonNull final MenuItem item) {
        if (super.onPopupOptionsItemSelected(item)) {
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


    private void doStoreCacheOnLists(final Set<Integer> listIds) {
        if (cache.isOffline()) {
            // cache already offline, just add to another list
            DataStore.saveLists(Collections.singletonList(cache), listIds);
            CacheDetailActivity.updateOfflineBox(getView(), cache, res,
                    new RefreshCacheClickListener(), new DropCacheClickListener(),
                    new StoreCacheClickListener(), new ShowHintClickListener(binding), new MoveCacheClickListener(), new StoreCacheClickListener());
            CacheDetailActivity.updateCacheLists(getView(), cache, res, null);
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
                            new StoreCacheClickListener(), new ShowHintClickListener(binding), new MoveCacheClickListener(), new StoreCacheClickListener());
                    CacheDetailActivity.updateCacheLists(view, cache, res, null);
                }
            });
        }
    }


    private void updateStoreRefreshButtons(final boolean enable) {
        final Activity activity = getActivity();
        if (activity != null) {
            ViewUtils.setEnabled(getActivity().findViewById(R.id.offline_store), enable);
            ViewUtils.setEnabled(getActivity().findViewById(R.id.offline_refresh), enable);
        }
    }


    private class StoreCacheClickListener implements View.OnClickListener, View.OnLongClickListener {
        @Override
        public void onClick(final View v) {
            selectListsAndStore(false);
        }

        @Override
        public boolean onLongClick(final View v) {
            selectListsAndStore(true);
            return true;
        }

        private void selectListsAndStore(final boolean fastStoreOnLastSelection) {
            if (cache.isOffline()) {
                // just update list selection
                new StoredList.UserInterface(requireActivity()).promptForMultiListSelection(R.string.lists_title,
                        CachePopupFragment.this::doStoreCacheOnLists, true, cache.getLists(), fastStoreOnLastSelection);
            } else {
                if (!Network.isConnected()) {
                    showToast(getString(R.string.err_server_general));
                    return;
                }
                CacheDownloaderService.storeCache(getActivity(), cache, fastStoreOnLastSelection, () -> updateStoreRefreshButtons(false));
            }
        }
    }

    private class RefreshCacheClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View arg0) {
            if (!Network.isConnected()) {
                showToast(getString(R.string.err_server_general));
                return;
            }
            CacheDownloaderService.refreshCache(getActivity(), cache.getGeocode(), true, () -> updateStoreRefreshButtons(false));
        }
    }


    private class MoveCacheClickListener implements View.OnLongClickListener {

        @Override
        public boolean onLongClick(final View v) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return false;
            }

            new StoredList.UserInterface(requireActivity()).promptForListSelection(R.string.cache_menu_move_list,
                    this::moveCacheToList, true, -1);

            return true;
        }

        private void moveCacheToList(final Integer listId) {
            doStoreCacheOnLists(Collections.singleton(listId));
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
        ViewUtils.setEnabled(requireView().findViewById(R.id.menu_navigate), false);
        NavigationAppFactory.showNavigationMenu(getActivity(), cache, null, null, true, true, R.id.menu_navigate);
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
    }

    @Override
    protected TargetInfo getTargetInfo() {
        if (cache == null) {
            return null;
        }
        return new TargetInfo(cache.getCoords(), cache.getGeocode());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        GeocacheChangedBroadcastReceiver.sendBroadcast(getContext(), geocode);
    }
}
