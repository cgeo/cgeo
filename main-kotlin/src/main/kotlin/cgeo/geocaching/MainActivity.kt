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

import cgeo.geocaching.activity.AbstractNavigationBarActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.capability.IAvatar
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.connector.gc.BookmarkListActivity
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCConstants
import cgeo.geocaching.connector.gc.PocketQueryListActivity
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.databinding.MainActivityBinding
import cgeo.geocaching.downloader.DownloaderUtils
import cgeo.geocaching.downloader.PendingDownloadsActivity
import cgeo.geocaching.enumerations.QuickLaunchItem
import cgeo.geocaching.helper.UsefulAppsActivity
import cgeo.geocaching.models.Download
import cgeo.geocaching.network.Network
import cgeo.geocaching.permission.PermissionAction
import cgeo.geocaching.permission.PermissionContext
import cgeo.geocaching.search.GeocacheSuggestionsAdapter
import cgeo.geocaching.search.SearchUtils
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.sensors.GnssStatusProvider
import cgeo.geocaching.sensors.GnssStatusProvider.Status
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.extension.FoundNumCounter
import cgeo.geocaching.storage.extension.PendingDownload
import cgeo.geocaching.ui.AvatarUtils
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.WeakReferenceHandler
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.ClipboardUtils
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.DebugUtils
import cgeo.geocaching.utils.DisplayUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MessageCenterUtils
import cgeo.geocaching.utils.ProcessUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.utils.offlinetranslate.TranslatorUtils
import cgeo.geocaching.wherigo.WherigoActivity
import cgeo.geocaching.Intents.EXTRA_MESSAGE_CENTER_COUNTER

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.util.Pair
import androidx.core.view.MenuCompat

import java.util.ArrayList
import java.util.List

import com.google.android.material.button.MaterialButton
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import org.apache.commons.lang3.StringUtils

class MainActivity : AbstractNavigationBarActivity() {

    private MainActivityBinding binding

    /**
     * view of the action bar search
     */
    private SearchView searchView
    private MenuItem searchItem

    private var initialized: Boolean = false

    private val locationUpdater: UpdateLocation = UpdateLocation()
    private val updateUserInfoHandler: Handler = UpdateUserInfoHandler(this)
    /**
     * initialization with an empty subscription
     */
    private val resumeDisposables: CompositeDisposable = CompositeDisposable()

    private val askLocationPermissionAction: PermissionAction<Void> = PermissionAction.register(this, PermissionContext.LOCATION, b -> binding.locationStatus.updatePermissions())

    private static class UpdateUserInfoHandler : WeakReferenceHandler()<MainActivity> {

        UpdateUserInfoHandler(final MainActivity activity) {
            super(activity)
        }

        override         public Unit handleMessage(final Message msg) {
            try (ContextLogger ignore = ContextLogger(Log.LogLevel.DEBUG, "MainActivity.UpdateUserInfoHandler.handleMessage")) {
                val activity: MainActivity = getReference()
                if (activity != null) {
                    // Get active connectors with login status
                    final ILogin[] loginConns = ConnectorFactory.getActiveLiveConnectors()

                    // Update UI
                    activity.binding.connectorstatusArea.setAdapter(ArrayAdapter<ILogin>(activity, R.layout.main_activity_connectorstatus, loginConns) {
                        override                         public View getView(final Int position, final View convertView, final android.view.ViewGroup parent) {
                            // do NOT use convertView, as it gets filled asynchronously, which may lead to the wrong view being filled
                            val view: View = activity.getLayoutInflater().inflate(R.layout.main_activity_connectorstatus, parent, false)
                            val connector: ILogin = getItem(position)
                            fillView(view, connector)
                            return view
                        }

                        private Unit fillView(final View connectorInfo, final ILogin conn) {

                            val connectorStatus: TextView = connectorInfo.findViewById(R.id.item_status)
                            val isLoggingIn: Boolean = StringUtils == (conn.getLoginStatusString(), activity.getString(R.string.init_login_popup_working))
                            val isLoggingOk: Boolean = StringUtils == (conn.getLoginStatusString(), activity.getString(R.string.init_login_popup_ok))
                            val connInfo: StringBuilder = StringBuilder(conn.getNameAbbreviated()).append(Formatter.SEPARATOR).append(conn.getLoginStatusString())
                            if (conn is GCConnector && Network.isConnected() && !isLoggingIn && !isLoggingOk) {
                                val lastError: Pair<String, Long> = Settings.getLastLoginErrorGC()
                                if (lastError != null && StringUtils.isNotBlank(lastError.first) && lastError.second > Settings.getLastLoginSuccessGC()) {
                                    connInfo.append(" (").append(lastError.first).append(")")
                                }
                            }
                            connectorStatus.setText(connInfo)
                            final View.OnClickListener connectorConfig = v -> SettingsActivity.openForScreen(conn.getServiceSpecificPreferenceScreenKey(), activity)
                            connectorStatus.setOnClickListener(connectorConfig)

                            val manualLogin: Button = connectorInfo.findViewById(R.id.manual_login)
                            manualLogin.setVisibility(connInfo.toString().contains(activity.getString(R.string.err_auth_gc_captcha)) ? View.VISIBLE : View.GONE)
                            manualLogin.setOnClickListener(b -> conn.performManualLogin(activity, () -> {
                                if (!activity.isDestroyed() && !activity.isFinishing()) {
                                    activity.updateUserInfoHandler.sendEmptyMessage(-1)
                                    activity.onLoginIssue(!anyConnectorLoggedIn())
                                }
                            }))

                            AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler,
                                    () -> {
                                        val userFoundCount: StringBuilder = StringBuilder()

                                        val count: Int = FoundNumCounter.getAndUpdateFoundNum(conn)
                                        if (count >= 0) {
                                            userFoundCount.append(activity.getResources().getQuantityString(R.plurals.user_finds, count, count))

                                            if (Settings.isDisplayOfflineLogsHomescreen()) {
                                                val offlinefounds: Int = DataStore.getFoundsOffline(conn)
                                                if (offlinefounds > 0) {
                                                    userFoundCount.append(" + ").append(activity.getResources().getQuantityString(R.plurals.user_finds_offline, offlinefounds, offlinefounds))
                                                }
                                            }
                                        }
                                        val userNameText: String = FoundNumCounter.getNotBlankUserName(conn)
                                        if (conn is GCConnector && StringUtils.contains(Settings.getUserName(), '@') && StringUtils.isNotBlank(userNameText)) {
                                            // auto-fix email address used as login instead of username for GC connector (#16397)
                                            Settings.setGCUserName(userNameText)
                                            Log.d("Auto-fixed GC login settings from email to username")
                                        }
                                        return Pair<>(userFoundCount, userNameText)
                                    },
                                    p -> {
                                        if (conn is GCConnector) {
                                            connectorStatus.setText(connInfo.append(Formatter.SEPARATOR).append(CgeoApplication.getInstance().getString(Settings.isGCPremiumMember() ? R.string.gc_premium : R.string.gc_basic)))
                                        }
                                        val userName: TextView = connectorInfo.findViewById(R.id.item_title)
                                        val userFounds: TextView = connectorInfo.findViewById(R.id.item_info)
                                        userName.setText(p.second)
                                        userName.setOnClickListener(v -> {
                                            ShareUtils.openUrl(getContext(), conn.geMyAccountUrl())
                                        })
                                        val userFoundCount: String = p.first.toString()
                                        if (userFoundCount.isEmpty()) {
                                            userFounds.setVisibility(View.GONE)
                                        } else {
                                            userFounds.setVisibility(View.VISIBLE)
                                            userFounds.setText(userFoundCount)
                                            userFounds.setOnClickListener(v -> {
                                                activity.startActivity(CacheListActivity.getHistoryIntent(activity))
                                                ActivityMixin.overrideTransitionToFade(activity)
                                            })
                                            userFounds.setOnLongClickListener(v -> {
                                                getContext().startActivity(CacheListActivity.getHistoryIntent(activity, conn))
                                                ActivityMixin.overrideTransitionToFade(activity)
                                                return true
                                            })
                                        }
                                    })

                            val userAvatar: ImageView = connectorInfo.findViewById(R.id.item_icon)

                            if (conn is IAvatar) {
                                // already reserve space, so that other content does not jump as soon as avatar is loaded
                                userAvatar.setVisibility(View.INVISIBLE)
                                userAvatar.setOnClickListener(null)

                                AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler,
                                        () -> AvatarUtils.getAvatar((IAvatar) conn),
                                        img -> {
                                            userAvatar.setVisibility(View.VISIBLE)
                                            if (img == null) {
                                                userAvatar.setImageResource(R.drawable.avartar_placeholder)
                                            } else {
                                                userAvatar.setImageDrawable(img)
                                            }
                                            userAvatar.setOnClickListener(connectorConfig)
                                        })
                            } else {
                                userAvatar.setVisibility(View.GONE)
                            }
                        }
                    })
                }
            }
        }
    }

    private class UpdateLocation : GeoDirHandler() {

        override         @SuppressLint("SetTextI18n")
        public Unit updateGeoData(final GeoData geo) {
            binding.locationStatus.updateGeoData(geo)
        }
    }

    private val satellitesHandler: Consumer<GnssStatusProvider.Status> = Consumer<Status>() {
        override         public Unit accept(final Status gnssStatus) {
            binding.locationStatus.updateSatelliteStatus(gnssStatus)
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        try (ContextLogger cLog = ContextLogger(Log.LogLevel.DEBUG, "MainActivity.onCreate")) {
            // don't call the super implementation with the layout argument, as that would set the wrong theme
            setTheme(Settings.isWallpaper() ? R.style.cgeo_withWallpaper : R.style.cgeo)
            super.onCreate(savedInstanceState)

            binding = MainActivityBinding.inflate(getLayoutInflater())

            // adding the bottom navigation component is handled by {@link AbstractBottomNavigationActivity#setContentView}
            setContentView(binding.getRoot())

            setTitle(R.string.app_name)

            cLog.add("setview")

            setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // type to search

            init()
            cLog.add("init")

            binding.infoNotloggedin.setOnClickListener(v ->
                    SimpleDialog.of(this).setTitle(R.string.warn_notloggedin_title).setMessage(R.string.warn_notloggedin_long).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(() -> SettingsActivity.openForScreen(R.string.preference_screen_services, this)))

            // automated update check
            DownloaderUtils.checkForRoutingTileUpdates(this)
            cLog.add("rtu")

            DownloaderUtils.checkForMapUpdates(this)
            cLog.add("mu")

            // location permission currently granted?
            if (!PermissionContext.LOCATION.getNotGrantedPermissions().isEmpty()) {
                displayActionItem(R.id.missingLocationPermission, R.string.location_no_permission, false, doAsk -> {
                    if (doAsk) {
                        this.askLocationPermissionAction.launch(null)
                    }
                })
            }
            binding.locationStatus.setPermissionRequestCallback(() -> this.askLocationPermissionAction.launch(null))

            configureMessageCenterPolling()

            LegacyFilterConfig.checkAndMigrate()
        }

        if (Log.isEnabled(Log.LogLevel.DEBUG)) {
            binding.getRoot().post(() -> Log.d("Post after MainActivity.onCreate"))
        }

    }

    private Unit configureMessageCenterPolling() {
        val that: Activity = this
        MessageCenterUtils.setReceiver(this, intent -> {
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }
            val count: Int = intent.getIntExtra(EXTRA_MESSAGE_CENTER_COUNTER, 0)
            Handler(Looper.getMainLooper()).post(() -> { // needs to be done on UI thread
                displayActionItem(R.id.mcupdate, res.getQuantityString(R.plurals.mcupdate, count, count), true, (actionRequested) -> {
                    updateHomeBadge(-1)
                    if (actionRequested) {
                        ShareUtils.openUrl(that, GCConstants.URL_MESSAGECENTER)
                    }
                })
            })
        })
    }

    private Unit prepareQuickLaunchItems() {
        val dimSize: Int = DisplayUtils.getPxFromDp(getResources(), 48.0f, 1.0f)
        val dimMargin: Int = DisplayUtils.getPxFromDp(getResources(), 7.0f, 1.0f)
        final LinearLayout.LayoutParams lp = LinearLayout.LayoutParams(dimSize, dimSize)
        lp.setMargins(dimMargin, 0, dimMargin, 0)

        val quicklaunchitems: List<Integer> = Settings.getInfoItems(R.string.pref_quicklaunchitems, 1)
        binding.quicklaunchitems.removeAllViews()
        binding.quicklaunchitems.setVisibility(View.GONE)
        for (Int i : quicklaunchitems) {
            val item: QuickLaunchItem = (QuickLaunchItem) QuickLaunchItem.getById(i, QuickLaunchItem.ITEMS)
            if (QuickLaunchItem.conditionsFulfilled(item)) {
                addButton(item.iconRes, lp, () -> QuickLaunchItem.launchQuickLaunchItem(this, item.getId(), true), getString(item.getTitleResId()), item.viewInitializer)
            }
        }
    }

    private Unit addButton(@DrawableRes final Int iconRes, final LinearLayout.LayoutParams lp, final Runnable action, final String tooltip, final java.util.function.Consumer<View> viewInitializer) {
        val b: MaterialButton = MaterialButton(this, null, R.attr.quickLaunchButtonStyle)
        b.setIconResource(iconRes)
        b.setLayoutParams(lp)
        b.setVisibility(View.VISIBLE)
        b.setOnClickListener(view -> action.run())
        if (viewInitializer != null) {
            viewInitializer.accept(b)
        }
        TooltipCompat.setTooltipText(b, tooltip)
        binding.quicklaunchitems.addView(b)
        binding.quicklaunchitems.setVisibility(View.VISIBLE)
    }

    private Unit init() {
        if (initialized) {
            return
        }

        initialized = true

        updateCacheCounter()
        prepareQuickLaunchItems()
        checkPendingDownloads()
        binding.locationStatus.setShowAddress(Settings.isShowAddress())
    }

    /** prompts user if there's at least one blocked or failed download */
    private Unit checkPendingDownloads() {
        if (Settings.pendingDownloadsNeedCheck()) {
            val pendingDownloads: ArrayList<PendingDownload.PendingDownloadDescriptor> = PendingDownload.getAllPendingDownloads()
            if (pendingDownloads.isEmpty()) {
                return
            }

            val downloadManager: DownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)
            for (PendingDownload.PendingDownloadDescriptor download : pendingDownloads) {
                final DownloadManager.Query query = DownloadManager.Query()
                query.setFilterById(download.id)
                try (Cursor c = downloadManager.query(query)) {
                    if (c.isAfterLast()) {
                        if (download.date < 1665433698000L /* Oct 10th, 2022 */) {
                            // entry is pretty old and no longer available in system's download manager, so do some housekeeping in our own database
                            PendingDownload.remove(download.id)
                            Log.w("removed stale download no longer recognized by download manager: id=" + download.id + ", fn=" + download.filename + ", date=" + Formatter.formatDate(download.date))
                        }
                    } else {
                        while (c.moveToNext()) {
                            val colStatus: Int = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (colStatus >= 0) {
                                val status: Int = c.getInt(colStatus)
                                if (status != DownloadManager.STATUS_RUNNING && status != DownloadManager.STATUS_SUCCESSFUL) {
                                    SimpleDialog.of(this).setTitle(R.string.downloader_pending_downloads).setMessage(R.string.downloader_pending_info).confirm(() -> startActivity(Intent(this, PendingDownloadsActivity.class)))
                                    Settings.setPendingDownloadsLastCheck(false)
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override     public Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)
        init()
    }

    override     public Unit onResume() {
        try (ContextLogger cLog = ContextLogger(Log.LogLevel.DEBUG, "MainActivity.onResume")) {

            super.onResume()

            resumeDisposables.add(locationUpdater.start(GeoDirHandler.UPDATE_GEODATA | GeoDirHandler.LOW_POWER))
            resumeDisposables.add(LocationDataProvider.getInstance().gpsStatusObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(satellitesHandler))

            updateUserInfoHandler.sendEmptyMessage(-1)
            cLog.add("perm")

            init()
        }

        if (Log.isEnabled(Log.LogLevel.DEBUG)) {
            binding.getRoot().post(() -> Log.d("Post after MainActivity.onResume"))
        }
    }

    override     public Unit onDestroy() {
        initialized = false

        super.onDestroy()
    }

    override     public Unit onStop() {
        initialized = false
        super.onStop()
    }

    override     public Unit onPause() {
        initialized = false
        resumeDisposables.clear()

        super.onPause()
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        try (ContextLogger ignore = ContextLogger(Log.LogLevel.DEBUG, "MainActivity.onCreateOptionsMenu")) {

            getMenuInflater().inflate(R.menu.main_activity_options, menu)
            MenuCompat.setGroupDividerEnabled(menu, true)
            val searchManager: SearchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE)
            searchItem = menu.findItem(R.id.menu_gosearch)
            searchView = (SearchView) searchItem.getActionView()
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()))
            searchView.setSuggestionsAdapter(GeocacheSuggestionsAdapter(this))
            SearchUtils.setSearchViewColor(searchView)

            // initialize menu items
            menu.findItem(R.id.menu_wizard).setVisible(!InstallWizardActivity.isConfigurationOk())
            menu.findItem(R.id.menu_update_routingdata).setEnabled(Settings.useInternalRouting())

            val isPremiumActive: Boolean = Settings.isGCConnectorActive() && Settings.isGCPremiumMember()
            menu.findItem(R.id.menu_pocket_queries).setVisible(isPremiumActive)
            menu.findItem(R.id.menu_bookmarklists).setVisible(isPremiumActive)

            SearchUtils.hideKeyboardOnSearchClick(searchView, searchItem)
            SearchUtils.hideActionIconsWhenSearchIsActive(this, menu, searchItem)
            SearchUtils.handleDropDownVisibility(this, searchView, searchItem)
        }

        if (Log.isEnabled(Log.LogLevel.DEBUG)) {
            binding.getRoot().post(() -> Log.d("Post after MainActivity.onCreateOptionsMenu"))
        }

        return true
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val id: Int = item.getItemId()
        if (id == android.R.id.home) {
            startActivity(Intent(this, AboutActivity.class))
        } else if (id == R.id.menu_about) {
            startActivity(Intent(this, AboutActivity.class))
        } else if (id == R.id.menu_report_problem) {
            DebugUtils.askUserToReportProblem(this, null)
        } else if (id == R.id.menu_helpers) {
            startActivity(Intent(this, UsefulAppsActivity.class))
        } else if (id == R.id.menu_wherigo) {
            startActivity(Intent(this, WherigoActivity.class))
        } else if (id == R.id.menu_wizard) {
            val wizard: Intent = Intent(this, InstallWizardActivity.class)
            wizard.putExtra(InstallWizardActivity.BUNDLE_MODE, InstallWizardActivity.needsFolderMigration() ? InstallWizardActivity.WizardMode.WIZARDMODE_MIGRATION.id : InstallWizardActivity.WizardMode.WIZARDMODE_RETURNING.id)
            startActivity(wizard)
        } else if (id == R.id.menu_settings) {
            startActivityForResult(Intent(this, SettingsActivity.class), Intents.SETTINGS_ACTIVITY_REQUEST_CODE)
        } else if (id == R.id.menu_backup) {
            SettingsActivity.openForScreen(R.string.preference_screen_backup, this, true)
        } else if (id == R.id.menu_paste_search) {
            startActivity(Intent(this, SearchActivity.class).setAction(SearchActivity.ACTION_CLIPBOARD).putExtra(SearchManager.QUERY, ClipboardUtils.getText()))
        } else if (id == R.id.menu_history) {
            val intent: Intent = CacheListActivity.getHistoryIntent(this)
            AbstractNavigationBarActivity.setIntentHideBottomNavigation(intent, true)
            startActivity(intent)
            ActivityMixin.overrideTransitionToFade(this)
        } else if (id == R.id.menu_goto) {
            InternalConnector.assertHistoryCacheExists(this)
            CacheDetailActivity.startActivity(this, InternalConnector.GEOCODE_HISTORY_CACHE, true)
        } else if (id == R.id.menu_pocket_queries) {
            if (Settings.isGCPremiumMember()) {
                startActivity(Intent(this, PocketQueryListActivity.class))
            }
        } else if (id == R.id.menu_bookmarklists) {
            if (Settings.isGCPremiumMember()) {
                startActivity(Intent(this, BookmarkListActivity.class))
            }
        } else if (id == R.id.menu_update_routingdata) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES, R.string.updates_check, DownloaderUtils::returnFromTileUpdateCheck)
        } else if (id == R.id.menu_update_mapdata) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED, R.string.updates_check, DownloaderUtils::returnFromMapUpdateCheck)
        } else if (id == R.id.menu_download_language) {
            TranslatorUtils.downloadLanguageModels(this)
        } else if (id == R.id.menu_delete_offline_data) {
            DownloaderUtils.deleteOfflineData(this)
        } else if (id == R.id.menu_pending_downloads) {
            startActivity(Intent(this, PendingDownloadsActivity.class))
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    public Unit updateCacheCounter() {
        AndroidRxUtils.bindActivity(this, DataStore.getAllCachesCountObservable()).subscribe(countOfflineCaches -> {
            val counter: TextView = findViewById(R.id.offline_counter)
            counter.setVisibility(countOfflineCaches > 0 ? View.VISIBLE : View.GONE)
            if (countOfflineCaches > 0) {
                counter.setText(getResources().getQuantityString(R.plurals.caches_stored_offline, countOfflineCaches, countOfflineCaches))
            }
        }, throwable -> Log.e("Unable to add cache count", throwable))
    }

    override     public Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);  // call super to make lint happy
        if (requestCode == Intents.SETTINGS_ACTIVITY_REQUEST_CODE) {
            if (resultCode == SettingsActivity.RESTART_NEEDED) {
                ProcessUtils.restartApplication(this)
            }
        } else if (requestCode == Intents.SEARCH_REQUEST_CODE) {
            // SearchActivity activity returned without making a search
            if (resultCode == RESULT_CANCELED) {
                String query = intent.getStringExtra(SearchManager.QUERY)
                if (query == null) {
                    query = ""
                }
                SimpleDialog.of(this).setMessage(TextParam.text(res.getString(R.string.unknown_scan) + "\n\n" + query)).show()
            }
        }
    }

    override     protected Handler getUpdateUserInfoHandler() {
        return updateUserInfoHandler
    }

    /**
     * if no connector can log in, set visibility of warning message accordingly
     */
    override     protected Unit onLoginIssue(final Boolean issue) {
        if (issue) {
            binding.infoNotloggedinIcon.attributeImage.setImageResource(R.drawable.attribute_wirelessbeacon)
            binding.infoNotloggedinIcon.attributeStrikethru.setVisibility(View.VISIBLE)
            binding.infoNotloggedin.setVisibility(View.VISIBLE)
        } else {
            binding.infoNotloggedin.setVisibility(View.GONE)
        }
    }

    override     public Unit onBackPressed() {
        // back may exit the app instead of closing the search action bar
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true)
            searchItem.collapseActionView()
        } else {
            super.onBackPressed()
        }
    }

    override     public Int getSelectedBottomItemId() {
        return MENU_HOME
    }

    override     public Unit updateSelectedBottomNavItemId() {
        super.updateSelectedBottomNavItemId()

        // Always show c:geo logo for this activity
        val actionBar: ActionBar = getSupportActionBar()
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_launcher_rounded_noborder)
            actionBar.setHomeActionContentDescription(R.string.about)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * display action notifications, e. g. update or backup reminders
     * action callback accepts true, if action is to be performed / false if to be postponed
     */
    public Unit displayActionItem(final Int layout, final @StringRes Int info, final Boolean withBadge, final Action1<Boolean> action) {
        displayActionItem(layout, getString(info), withBadge, action)
    }

    public Unit displayActionItem(final Int layout, final String info, final Boolean withBadge, final Action1<Boolean> action) {
        val delta: Int = withBadge ? 1 : 0
        val l: TextView = findViewById(layout)
        if (l != null) {
            l.setVisibility(View.VISIBLE)
            updateHomeBadge(delta)
            l.setText(info)
            l.setOnClickListener(v -> {
                action.call(true)
                l.setVisibility(View.GONE)
                updateHomeBadge(-delta)
            })
            l.setOnLongClickListener(v -> {
                action.call(false)
                l.setVisibility(View.GONE)
                updateHomeBadge(-delta)
                return true
            })
        }
    }

}
