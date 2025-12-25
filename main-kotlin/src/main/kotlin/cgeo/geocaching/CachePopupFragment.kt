// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractNavigationBarMapActivity
import cgeo.geocaching.activity.Progress
import cgeo.geocaching.apps.navi.NavigationAppFactory
import cgeo.geocaching.databinding.PopupBinding
import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.network.Network
import cgeo.geocaching.service.CacheDownloaderService
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.speech.SpeechService
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.CacheDetailsCreator
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.WeakReferenceHandler
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.CacheUtils
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.EmojiUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapMarkerUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.wherigo.WherigoActivity
import cgeo.geocaching.wherigo.WherigoUtils
import cgeo.geocaching.wherigo.WherigoViewUtils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

import java.lang.ref.WeakReference
import java.util.Collections
import java.util.List
import java.util.Set

import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils

class CachePopupFragment : AbstractDialogFragmentWithProximityNotification() {
    private val progress: Progress = Progress()
    private PopupBinding binding

    public static Fragment newInstance(final String geocode) {

        val args: Bundle = Bundle()
        args.putString(GEOCODE_ARG, geocode)

        val f: Fragment = CachePopupFragment()
        //f.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
        f.setArguments(args)

        return f
    }

    private static class StoreCacheHandler : DisposableHandler() {
        private final Int progressMessage
        private final WeakReference<CachePopupFragment> popupRef

        StoreCacheHandler(final CachePopupFragment popup, final Int progressMessage) {
            this.progressMessage = progressMessage
            popupRef = WeakReference<>(popup)
        }

        override         public Unit handleRegularMessage(final Message msg) {
            if (msg.what == UPDATE_LOAD_PROGRESS_DETAIL && msg.obj is String) {
                updateStatusMsg((String) msg.obj)
            } else {
                val popup: CachePopupFragment = popupRef.get()
                if (popup != null) {
                    popup.init()
                }
            }
        }

        private Unit updateStatusMsg(final String msg) {
            val popup: CachePopupFragment = popupRef.get()
            if (popup == null || !popup.isAdded()) {
                return
            }
            popup.progress.setMessage(popup.getString(progressMessage)
                    + "\n\n"
                    + msg)
        }
    }

    private static class DropCacheHandler : WeakReferenceHandler()<CachePopupFragment> {

        DropCacheHandler(final CachePopupFragment popup) {
            super(popup)
        }

        override         public Unit handleMessage(final Message msg) {
            val popup: CachePopupFragment = getReference()
            if (popup == null) {
                return
            }
            popup.progress.dismiss()
            ((AbstractNavigationBarMapActivity) popup.requireActivity()).sheetRemoveFragment()
        }
    }

    override     public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        binding = PopupBinding.inflate(getLayoutInflater(), container, false)
        return binding.getRoot()
    }

    override     protected Unit init() {
        super.init()
        try {
            if (null != proximityNotification) {
                proximityNotification.setReferencePoint(cache.getCoords())
                proximityNotification.setTextNotifications(getContext())
            }

            val toolbar: Toolbar = binding.toolbar.toolbar
            toolbar.setTitle(geocode)
            setToolbarBackgroundColor(toolbar, binding.swipeUpIndicator.swipeUpIndicator, cache.getType(), cache.isEnabled())

            toolbar.setLogo(MapMarkerUtils.getCacheMarker(getResources(), cache, CacheListType.MAP, Settings.getIconScaleEverywhere()).getDrawable())
            toolbar.setLongClickable(true)
            toolbar.setOnClickListener(v -> {
                if (cache.isOffline()) {
                    EmojiUtils.selectEmojiPopup(CachePopupFragment.this.requireContext(), cache.getAssignedEmoji(), cache, newCacheIcon -> {
                        cache.setAssignedEmoji(newCacheIcon)
                        toolbar.setLogo(MapMarkerUtils.getCacheMarker(getResources(), cache, CacheListType.MAP, Settings.getIconScaleEverywhere()).getDrawable())
                        DataStore.saveCache(cache, LoadFlags.SAVE_ALL)
                    })
                }
            })
            toolbar.setOnLongClickListener(v -> {
                ShareUtils.sharePlainText(v.getContext(), geocode)
                return true
            })
            onCreatePopupOptionsMenu(toolbar, this, cache)
            toolbar.setOnMenuItemClickListener(this::onPopupOptionsItemSelected)

            details = CacheDetailsCreator(requireActivity(), binding.detailsList)
            binding.title.setText(TextUtils.coloredCacheText(getActivity(), cache, StringUtils.defaultIfBlank(cache.getName(), "")))
            details.addShareAction(binding.title)

            addCacheDetails(false)

            binding.editPersonalnote.setVisibility(View.VISIBLE)
            binding.editPersonalnote.setOnClickListener(arg0 -> {
                CacheDetailActivity.startActivityForEditNote(getActivity(), geocode)
                ((AbstractNavigationBarMapActivity) requireActivity()).sheetRemoveFragment()
            })

            // Wherigo
            val wherigoGuis: List<String> = WherigoUtils.getWherigoGuids(cache)
            if (!wherigoGuis.isEmpty()) {
                binding.sendToWherigo.setVisibility(View.VISIBLE)
                binding.sendToWherigo.setOnClickListener(v -> WherigoViewUtils.executeForOneCartridge(requireActivity(), wherigoGuis, guid ->
                        WherigoActivity.startForGuid(requireActivity(), guid, cache.getGeocode(), true)))
            } else {
                binding.sendToWherigo.setVisibility(View.GONE)
            }

            // ALC
            if (CacheUtils.isLabAdventure(cache)) {
                binding.sendToAlc.setVisibility(View.VISIBLE)
                CacheUtils.setLabLink(requireActivity(), binding.sendToAlc, cache.getUrl())
            } else {
                binding.sendToAlc.setVisibility(View.GONE)
            }

            // offline use
            CacheDetailActivity.updateOfflineBox(binding.getRoot(), cache, res, RefreshCacheClickListener(), DropCacheClickListener(), StoreCacheClickListener(), ShowHintClickListener(binding), MoveCacheClickListener(), StoreCacheClickListener())

            CacheDetailActivity.updateCacheLists(binding.getRoot(), cache, res, null)

            updateStoreRefreshButtons(true)
            getLifecycle().addObserver(GeocacheChangedBroadcastReceiver(getContext()) {
                override                 protected Unit onReceive(final Context context, final String geocode) {
                    if (StringUtils == (geocode, CachePopupFragment.this.geocode)) {
                        init()
                    }
                }
            })

        } catch (final Exception e) {
            Log.e("CachePopupFragment.init", e)
        }

        // cache is loaded. remove progress-popup if any there
        progress.dismiss()
    }

    override     public Boolean onPopupOptionsItemSelected(final MenuItem item) {
        if (super.onPopupOptionsItemSelected(item)) {
            return true
        }

        val menuItem: Int = item.getItemId()

        if (menuItem == R.id.menu_tts_toggle) {
            SpeechService.toggleService(getActivity(), cache.getCoords())
        } else {
            return false
        }
        return true
    }

    override     public Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)

        init()
    }


    private Unit doStoreCacheOnLists(final Set<Integer> listIds) {
        if (cache.isOffline()) {
            // cache already offline, just add to another list
            DataStore.saveLists(Collections.singletonList(cache), listIds)
            CacheDetailActivity.updateOfflineBox(getView(), cache, res,
                    RefreshCacheClickListener(), DropCacheClickListener(),
                    StoreCacheClickListener(), ShowHintClickListener(binding), MoveCacheClickListener(), StoreCacheClickListener())
            CacheDetailActivity.updateCacheLists(getView(), cache, res, null)
        } else {
            val storeCacheHandler: StoreCacheHandler = StoreCacheHandler(CachePopupFragment.this, R.string.cache_dialog_offline_save_message)
            val activity: FragmentActivity = requireActivity()
            progress.show(activity, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true, storeCacheHandler.disposeMessage())
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> cache.store(listIds, storeCacheHandler), () -> {
                activity.invalidateOptionsMenu()
                val view: View = getView()
                if (view != null) {
                    CacheDetailActivity.updateOfflineBox(view, cache, res,
                            RefreshCacheClickListener(), DropCacheClickListener(),
                            StoreCacheClickListener(), ShowHintClickListener(binding), MoveCacheClickListener(), StoreCacheClickListener())
                    CacheDetailActivity.updateCacheLists(view, cache, res, null)
                }
            })
        }
    }


    private Unit updateStoreRefreshButtons(final Boolean enable) {
        val activity: Activity = getActivity()
        if (activity != null) {
            ViewUtils.setEnabled(getActivity().findViewById(R.id.offline_store), enable)
            ViewUtils.setEnabled(getActivity().findViewById(R.id.offline_refresh), enable)
        }
    }


    private class StoreCacheClickListener : View.OnClickListener, View.OnLongClickListener {
        override         public Unit onClick(final View v) {
            selectListsAndStore(false)
        }

        override         public Boolean onLongClick(final View v) {
            selectListsAndStore(true)
            return true
        }

        private Unit selectListsAndStore(final Boolean fastStoreOnLastSelection) {
            if (cache.isOffline()) {
                // just update list selection
                StoredList.UserInterface(requireActivity()).promptForMultiListSelection(R.string.lists_title,
                        CachePopupFragment.this::doStoreCacheOnLists, true, cache.getLists(), fastStoreOnLastSelection)
            } else {
                if (!Network.isConnected()) {
                    showToast(getString(R.string.err_server_general))
                    return
                }
                CacheDownloaderService.storeCache(getActivity(), cache, fastStoreOnLastSelection, () -> updateStoreRefreshButtons(false))
            }
        }
    }

    private class RefreshCacheClickListener : View.OnClickListener {
        override         public Unit onClick(final View arg0) {
            if (!Network.isConnected()) {
                showToast(getString(R.string.err_server_general))
                return
            }
            CacheDownloaderService.refreshCache(getActivity(), cache.getGeocode(), true, () -> updateStoreRefreshButtons(false))
        }
    }


    private class MoveCacheClickListener : View.OnLongClickListener {

        override         public Boolean onLongClick(final View v) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working))
                return false
            }

            StoredList.UserInterface(requireActivity()).promptForListSelection(R.string.cache_menu_move_list,
                    this::moveCacheToList, true, -1)

            return true
        }

        private Unit moveCacheToList(final Integer listId) {
            doStoreCacheOnLists(Collections.singleton(listId))
        }
    }


    private class DropCacheClickListener : View.OnClickListener {
        override         public Unit onClick(final View arg0) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working))
                return
            }

            val dropCacheHandler: DropCacheHandler = DropCacheHandler(CachePopupFragment.this)
            progress.show(getActivity(), res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true, null)
            cache.drop(dropCacheHandler)
        }
    }

    private static class ShowHintClickListener : View.OnClickListener {
        private final PopupBinding binding

        ShowHintClickListener(final PopupBinding binding) {
            this.binding = binding
        }

        override         public Unit onClick(final View view) {
            val offlineHintText: TextView = binding.offlineHintText
            val offlineHintSeparator: View = binding.offlineHintSeparator
            if (offlineHintText.getVisibility() == View.VISIBLE) {
                offlineHintText.setVisibility(View.GONE)
                offlineHintSeparator.setVisibility(View.GONE)
            } else {
                offlineHintText.setVisibility(View.VISIBLE)
                offlineHintSeparator.setVisibility(View.VISIBLE)
                CacheDetailsCreator.addShareAction(offlineHintText.getContext(), offlineHintText)
            }
        }
    }


    override     public Unit showNavigationMenu() {
        ViewUtils.setEnabled(requireView().findViewById(R.id.menu_navigate), false)
        NavigationAppFactory.showNavigationMenu(getActivity(), cache, null, null, true, true, R.id.menu_navigate)
    }

    /**
     * Tries to navigate to the {@link Geocache} of this activity.
     */
    override     public Unit startDefaultNavigation() {
        NavigationAppFactory.startDefaultNavigationApplication(1, getActivity(), cache)
    }

    /**
     * Tries to navigate to the {@link Geocache} of this activity.
     */
    override     public Unit startDefaultNavigation2() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no))
            return
        }
        NavigationAppFactory.startDefaultNavigationApplication(2, getActivity(), cache)
    }

    override     protected TargetInfo getTargetInfo() {
        if (cache == null) {
            return null
        }
        return TargetInfo(cache.getCoords(), cache.getGeocode())
    }

    override     public Unit onDestroyView() {
        super.onDestroyView()
        GeocacheChangedBroadcastReceiver.sendBroadcast(getContext(), geocode)
    }
}
