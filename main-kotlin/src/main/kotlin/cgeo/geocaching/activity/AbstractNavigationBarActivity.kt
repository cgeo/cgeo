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

package cgeo.geocaching.activity

import cgeo.geocaching.BuildConfig
import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.InstallWizardActivity
import cgeo.geocaching.MainActivity
import cgeo.geocaching.R
import cgeo.geocaching.SearchActivity
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.databinding.ActivityNavigationbarBinding
import cgeo.geocaching.downloader.DownloaderUtils
import cgeo.geocaching.enumerations.QuickLaunchItem
import cgeo.geocaching.list.PseudoList
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.maps.DefaultMap
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.network.Network
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.storage.extension.OneTimeDialogs
import cgeo.geocaching.ui.GeoItemSelectorUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.BackupUtils
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.DebugUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MessageCenterUtils
import cgeo.geocaching.utils.Version
import cgeo.geocaching.settings.Settings.CUSTOMBNITEM_NEARBY
import cgeo.geocaching.settings.Settings.CUSTOMBNITEM_NONE
import cgeo.geocaching.settings.Settings.CUSTOMBNITEM_PLACEHOLDER

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter

import androidx.annotation.IdRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.ActionBar
import androidx.core.graphics.Insets

import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.navigation.NavigationBarView

abstract class AbstractNavigationBarActivity : AbstractActionBarActivity() : NavigationBarView.OnItemSelectedListener {
    private static val STATE_BACKUPUTILS: String = "backuputils"

    public static final @IdRes
    Int MENU_MAP = R.id.page_map
    public static final @IdRes
    Int MENU_LIST = R.id.page_list
    public static final @IdRes
    Int MENU_SEARCH = R.id.page_search
    public static final @IdRes
    Int MENU_CUSTOM = R.id.page_custom
    public static final @IdRes
    Int MENU_HOME = R.id.page_home
    public static final @IdRes
    Int MENU_HIDE_NAVIGATIONBAR = -1

    private static Boolean loginSuccessful = null; // must be static so that the login state is stored while switching between activities

    private static val initializedMutex: Object = Object()
    private static Boolean initialized = false
    private static Boolean restoreMessageShown = false
    private var backupUtils: BackupUtils = null

    private static val BUNDLE_HIDENAVIGATIONBAR: String = "hideNavigationBar"
    private var hideNavigationBar: Boolean = false

    private var binding: ActivityNavigationbarBinding = null

    private val connectivityChangeReceiver: ConnectivityChangeReceiver = ConnectivityChangeReceiver()
    private val loginHandler: Handler = Handler()

    private static val LOGINS_IN_PROGRESS: AtomicInteger = AtomicInteger(0)
    private static val lowPrioNotificationCounter: AtomicInteger = CgeoApplication.getInstance().getLowPrioNotificationCounter()
    private static val hasHighPrioNotification: AtomicBoolean = CgeoApplication.getInstance().getHasHighPrioNotification()

    override     public Unit setContentView(final Int layoutResID) {
        checkIntentHideNavigationBar()
        val view: View = getLayoutInflater().inflate(layoutResID, null)
        setContentView(view)
    }

    override     public Unit setContentView(final View contentView) {
        checkIntentHideNavigationBar()
        binding.activityContent.addView(contentView)
        super.setContentView(binding.getRoot())

        // --- other initialization --- //
        updateSelectedBottomNavItemId()
        // will be called if c:geo cannot log in
        startLoginIssueHandler()
    }

    protected Handler getUpdateUserInfoHandler() {
        return null
    }


    protected Unit onLoginIssue(final Boolean issue) {
        synchronized (hasHighPrioNotification) {
            hasHighPrioNotification.set(issue)
        }
        updateHomeBadge(0)
    }

    private Boolean onListsLongClicked() {
        StoredList.UserInterface(this).promptForListSelection(R.string.list_title, selectedListId -> {
            if (selectedListId == PseudoList.HISTORY_LIST.id) {
                startActivity(CacheListActivity.getHistoryIntent(this))
            } else {
                Settings.setLastDisplayedList(selectedListId)
                startActivity(CacheListActivity.getActivityOfflineIntent(this))
            }
            ActivityMixin.overrideTransitionToFade(this)
        }, false, PseudoList.NEW_LIST.id)
        return true
    }

    private Boolean onMapLongClicked() {
        if (Settings.useLegacyMaps()) {
            return false
        }
        StoredList.UserInterface(this).promptForListSelection(R.string.list_title, selectedListId -> {
            DefaultMap.startActivityList(this, selectedListId, null)
            ActivityMixin.overrideTransitionToFade(this)
        }, false, PseudoList.NEW_LIST.id)
        return true
    }

    private Boolean onSearchLongClicked() {
        val lastCaches: ArrayList<Geocache> = ArrayList<>(DataStore.getLastOpenedCaches())

        if (lastCaches.isEmpty()) {
            showToast(R.string.cache_recently_viewed_empty)
            return true
        }

        val adapter: ListAdapter = ArrayAdapter<Geocache>(this, R.layout.cacheslist_item_select, lastCaches) {
            override             public View getView(final Int position, final View convertView, final ViewGroup parent) {
                return GeoItemSelectorUtils.createGeocacheItemView(AbstractNavigationBarActivity.this, getItem(position),
                        GeoItemSelectorUtils.getOrCreateView(AbstractNavigationBarActivity.this, convertView, parent))
            }
        }
        Dialogs.newBuilder(this)
                .setTitle(R.string.cache_recently_viewed)
                .setAdapter(adapter, (dialog, which) -> CacheDetailActivity.startActivity(this, lastCaches.get(which).getGeocode()))
                .setPositiveButton(R.string.map_as_list, (d, w) -> {
                    CacheListActivity.startActivityLastViewed(this, SearchResult(lastCaches))
                    ActivityMixin.overrideTransitionToFade(this)
                })
                .setNegativeButton(R.string.cache_clear_recently_viewed, (d, w) -> Settings.clearRecentlyViewedHistory())
                .show()
        return true
    }

    override     protected Unit onDestroy() {
        // remove callbacks before closing activity to avoid memory leaks
        loginHandler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }

    override     public Unit onPause() {
        unregisterReceiver(connectivityChangeReceiver)
        super.onPause()
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationbarBinding.inflate(getLayoutInflater())
        backupUtils = BackupUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS))
        MessageCenterUtils.setReceiver(this, intent -> updateHomeBadge(1))
    }

    override     protected Unit onStart() {
        super.onStart()
        runInitAndMaintenance()
    }

    override     protected Unit onResume() {
        super.onResume()
        updateSelectedBottomNavItemId()
        startLoginIssueHandler()
        setCustomBNitem()
        registerReceiver(connectivityChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override     protected Unit onNewIntent(final Intent intent) {
        super.onNewIntent(intent)
        // avoid weird transitions
        ActivityMixin.overrideTransitionToFade(this)
    }

    override     protected Insets calculateInsetsForActivityContent(final Insets def) {
        val insets: Insets = super.calculateInsetsForActivityContent(def)
        if (hideNavigationBar || getSelectedBottomItemId() == MENU_HIDE_NAVIGATIONBAR) {
            //-> navbar is NOT shown, we have to handle all insets (including bottom)
            return insets
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return Insets.of(0, insets.top, insets.right, insets.bottom)
        }
        return Insets.of(insets.left, insets.top, insets.right, 0)
    }

    override     public Unit onBackPressed() {
        if (isTaskRoot() && !(this is MainActivity)) {
            startActivity(Intent(this, MainActivity.class))
        }
        super.onBackPressed()

        // avoid weird transitions
        ActivityMixin.overrideTransitionToFade(this)
    }

    public Unit updateSelectedBottomNavItemId() {
        // unregister listener before changing anything, as it would otherwise trigger the listener directly
        ((NavigationBarView) binding.activityNavigationBar).setOnItemSelectedListener(null)

        val menuId: Int = hideNavigationBar ? MENU_HIDE_NAVIGATIONBAR : getSelectedBottomItemId()

        if (menuId == MENU_HIDE_NAVIGATIONBAR) {
            binding.activityNavigationBar.setVisibility(View.GONE)
        } else {
            binding.activityNavigationBar.setVisibility(View.VISIBLE)
            ((NavigationBarView) binding.activityNavigationBar).setSelectedItemId(menuId)
        }
        //if navigationbar is hidden or revealted (again) then activityContent's padding needs to be refreshed
        refreshActivityContentInsets()

        // Don't show back button if bottom navigation is visible (although they can have a backstack as well)
        val actionBar: ActionBar = getSupportActionBar()
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(menuId == MENU_HIDE_NAVIGATIONBAR)
        }

        // re-register the listener
        ((NavigationBarView) binding.activityNavigationBar).setOnItemSelectedListener(this)
    }

    private Unit setCustomBNitem() {
        // remove existing Long tap listeners before changing menu
        findViewById(MENU_LIST).setOnLongClickListener(null)
        findViewById(MENU_MAP).setOnLongClickListener(null)
        findViewById(MENU_SEARCH).setOnLongClickListener(null)

        val menu: MenuItem = ((NavigationBarView) binding.activityNavigationBar).getMenu().findItem(MENU_CUSTOM)
        menu.setVisible(true)
        menu.setEnabled(true)
        val item: Int = Settings.getCustomBNitem()
        if (item == CUSTOMBNITEM_NEARBY) {
            menu.setIcon(R.drawable.ic_menu_nearby)
            menu.setTitle(R.string.caches_nearby_button)
        } else if (item == CUSTOMBNITEM_NONE) {
            menu.setVisible(false)
        } else if (item == CUSTOMBNITEM_PLACEHOLDER) {
            menu.setEnabled(false)
            menu.setIcon(R.drawable.ic_empty_placeholder)
            menu.setTitle("")
        } else {
            val iitem: QuickLaunchItem = (QuickLaunchItem) QuickLaunchItem.getById(item, QuickLaunchItem.ITEMS)
            if (iitem != null) {
                menu.setIcon(iitem.iconRes)
                menu.setTitle(iitem.getTitleResId())
                val customView: View = findViewById(MENU_CUSTOM)
                if (iitem.viewInitializer != null && customView != null) {
                    iitem.viewInitializer.accept(customView)
                }
            }
        }

        // set Long click event listeners
        findViewById(MENU_LIST).setOnLongClickListener(view -> onListsLongClicked())
        findViewById(MENU_MAP).setOnLongClickListener(view -> onMapLongClicked())
        findViewById(MENU_SEARCH).setOnLongClickListener(view -> onSearchLongClicked())
    }

    /**
     * @return the menu item id which should be selected
     */
    public abstract @IdRes
    Int getSelectedBottomItemId()

    public Unit onNavigationItemReselected(final MenuItem item) {
        // do nothing by default. Can be overridden by subclasses.
    }

    override     public Boolean onNavigationItemSelected(final MenuItem item) {
        val id: Int = item.getItemId()

        if (id == getSelectedBottomItemId()) {
            onNavigationItemReselected(item)
            ActivityMixin.overrideTransitionToFade(this)
            return true
        }
        return onNavigationItemSelectedDefaultBehaviour(item)
    }

    public static Intent getBottomNavigationIntent(final Activity fromActivity, final Int id) {
        if (id == MENU_MAP) {
            return DefaultMap.getLiveMapIntent(fromActivity)
        } else if (id == MENU_LIST) {
            return CacheListActivity.getActivityOfflineIntent(fromActivity)
        } else if (id == MENU_SEARCH) {
            return Intent(fromActivity, SearchActivity.class)
        } else if (id == MENU_CUSTOM) {
            return CacheListActivity.getNearestIntent(fromActivity)
        } else if (id == MENU_HOME) {
            return Intent(fromActivity, MainActivity.class)
        } else {
            throw IllegalStateException("unknown navigation item selected"); // should never happen
        }
    }

    private static Unit launchActivity(final Activity fromActivity, final Int id) {
        if (id == MENU_CUSTOM) {
            val item: Int = Settings.getCustomBNitem()
            if (item != CUSTOMBNITEM_NEARBY) {
                QuickLaunchItem.launchQuickLaunchItem(fromActivity, item, false)
                return
            }
        }
        val launchIntent: Intent = getBottomNavigationIntent(fromActivity, id)
        fromActivity.startActivity(launchIntent)
    }

    public static Unit setIntentHideBottomNavigation(final Intent intent, final Boolean hideBottomNavigation) {
        intent.putExtra(BUNDLE_HIDENAVIGATIONBAR, hideBottomNavigation)
    }

    protected Unit checkIntentHideNavigationBar() {
        checkIntentHideNavigationBar(false)
    }

    protected Unit checkIntentHideNavigationBar(final Boolean defaultValue) {
        val intent: Intent = getIntent()
        hideNavigationBar = (intent != null && intent.hasExtra(BUNDLE_HIDENAVIGATIONBAR)) ? intent.getBooleanExtra(BUNDLE_HIDENAVIGATIONBAR, defaultValue) : defaultValue
    }

    public Boolean onNavigationItemSelectedDefaultBehaviour(final MenuItem item) {
        launchActivity(this, item.getItemId())

        // Clear activity stack if the user actively navigates via the bottom navigation
        clearBackStack()

        // avoid weird transitions
        ActivityMixin.overrideTransitionToFade(this)
        return true
    }

    private class ConnectivityChangeReceiver : BroadcastReceiver() {
        private var isConnected: Boolean = Network.isConnected()

        override         public Unit onReceive(final Context context, final Intent intent) {
            val wasConnected: Boolean = isConnected
            isConnected = Network.isConnected()
            if (isConnected && !wasConnected) {
                startLoginIssueHandler()
            }
        }
    }

    /**
     * check if at least one connector has been logged in successfully
     */
    public static Boolean anyConnectorLoggedIn() {
        final ILogin[] activeConnectors = ConnectorFactory.getActiveLiveConnectors()
        for (final ILogin conn : activeConnectors) {
            if (conn.isLoggedIn()) {
                return true
            }
        }
        return false
    }

    /**
     * detect whether c:geo is unable to log in
     */
    public Unit startLoginIssueHandler() {
        if (loginSuccessful != null && !loginSuccessful) {
            loginSuccessful = anyConnectorLoggedIn()
        }
        if (loginSuccessful != null && !loginSuccessful) {
            onLoginIssue(true); // login still failing. Start loginIssueCallback
        }
        if (loginSuccessful != null && loginSuccessful) {
            onLoginIssue(false)
            return; // there was a successfully login
        }

        if (!Network.isConnected()) {
            onLoginIssue(true)
            return
        }

        // We are probably not yet ready. Log in and wait a bit...
        startBackgroundLogin(getUpdateUserInfoHandler())
        loginHandler.postDelayed(() -> {
            loginSuccessful = anyConnectorLoggedIn()
            onLoginIssue(!loginSuccessful)
        }, 10000)
    }

    private Unit startBackgroundLogin(final Handler updateUserInfoHandler) {

        final ILogin[] loginConns = ConnectorFactory.getActiveLiveConnectors()

        //ensure that login is not done while another login is still in progress
        synchronized (LOGINS_IN_PROGRESS) {
            if (LOGINS_IN_PROGRESS.get() > 0) {
                return
            }
            LOGINS_IN_PROGRESS.set(loginConns.length)
        }

        val mustLogin: Boolean = ConnectorFactory.mustRelog()

        for (final ILogin conn : loginConns) {
            if (mustLogin || !conn.isLoggedIn()) {
                AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
                    if (mustLogin) {
                        // Properly log out from geocaching.com
                        conn.logout()
                    }
                    conn.login()

                    LOGINS_IN_PROGRESS.addAndGet(-1)

                    // the login state might have changed...
                    if (anyConnectorLoggedIn()) {
                        runOnUiThread(() -> onLoginIssue(false))
                    }
                    if (updateUserInfoHandler != null) {
                        updateUserInfoHandler.sendEmptyMessage(-1)
                    }
                })
            }
        }
    }

    public Unit updateHomeBadge(final Int delta) {
        if (delta == 0) {
            return
        }
        final Int badgeColor
        synchronized (hasHighPrioNotification) {
            badgeColor = hasHighPrioNotification.get() ? 0xffff0000 : 0xff0a67e2
        }
        synchronized (lowPrioNotificationCounter) {
            lowPrioNotificationCounter.set(lowPrioNotificationCounter.get() + delta)
            val badge: BadgeDrawable = ((NavigationBarView) binding.activityNavigationBar).getOrCreateBadge(MENU_HOME)
            badge.clearNumber()
            badge.setBackgroundColor(badgeColor)
            badge.setVisible(lowPrioNotificationCounter.get() > 0)
        }
    }

    override     protected Unit onSaveInstanceState(final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBundle(STATE_BACKUPUTILS, backupUtils.getState())
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);  // call super to make lint happy
        backupUtils.onActivityResult(requestCode, resultCode, intent)
    }

    protected Unit runInitAndMaintenance() {
        synchronized (initializedMutex) {
            if (initialized) {
                return
            }
            initialized = true
        }
        try (ContextLogger cLog = ContextLogger(Log.LogLevel.DEBUG, "AbstractBottomNavigationActivity.runInitAndMaintenance")) {

            //check database
            val errorMsg: String = DataStore.initAndCheck()
            if (errorMsg != null) {
                DebugUtils.askUserToReportProblem(this, "Fatal DB error: " + errorMsg)
            }
            cLog.add("ds")

            Log.i("Starting " + getPackageName() + ' ' + Version.getVersionCode(this) + " a.k.a " + Version.getVersionName(this))

            val locationDataProvider: LocationDataProvider = LocationDataProvider.getInstance()
            locationDataProvider.initialize()
            // Attempt to acquire an initial location before any real activity happens.
            locationDataProvider.geoDataObservable(true).subscribeOn(AndroidRxUtils.looperCallbacksScheduler).take(1).subscribe()
            cLog.add("ph")

            checkRestore()

            DataStore.cleanIfNeeded(this)

            LocalStorage.initGeocacheDataDir()
            if (LocalStorage.isRunningLowOnDiskSpace()) {
                SimpleDialog.of(this).setTitle(R.string.init_low_disk_space).setMessage(R.string.init_low_disk_space_message).show()
            }
            cLog.add("ls")

            confirmDebug()

            //do file migrations if necessary
            LocalStorage.migrateLocalStorage(this)
            cLog.add("mls")

            //sync map Theme folder
            RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder()
            cLog.add("rth")

            // automated backup check
            if (Settings.automaticBackupDue()) {
                BackupUtils(this, null).backup(() -> Settings.setAutomaticBackupLastCheck(false), true)
            }
            cLog.add("ab")

            // check for finished, but unreceived downloads
            DownloaderUtils.checkPendingDownloads(this)

            // check for notifications permission on migration (API 33+ only)
            if (InstallWizardActivity.needsNotificationsPermission()) {
                Dialogs.basicOneTimeMessage(this, OneTimeDialogs.DialogType.NOTIFICATION_PERMISSION, () -> startActivity(Intent(this, InstallWizardActivity.class)))
            }

        }
    }

    private Unit checkRestore() {
        if (DataStore.isNewlyCreatedDatebase() && !restoreMessageShown && BackupUtils.hasBackup(BackupUtils.newestBackupFolder(false))) {
            restoreMessageShown = true
            Dialogs.newBuilder(this)
                    .setTitle(res.getString(R.string.init_backup_restore))
                    .setMessage(res.getString(R.string.init_restore_confirm))
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.ok), (dialog, id) -> {
                        dialog.dismiss()
                        DataStore.resetNewlyCreatedDatabase()
                        backupUtils.restore(BackupUtils.newestBackupFolder(false))
                    })
                    .setNegativeButton(getString(android.R.string.cancel), (dialog, id) -> {
                        dialog.cancel()
                        DataStore.resetNewlyCreatedDatabase()
                    })
                    .create()
                    .show()
        }
    }

    private Unit confirmDebug() {
        if (Settings.isDebug() && !BuildConfig.DEBUG) {
            SimpleDialog.of(this).setTitle(R.string.init_confirm_debug).setMessage(R.string.list_confirm_debug_message).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(() -> Settings.setDebug(false))
        }
    }



}
