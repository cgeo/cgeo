package cgeo.geocaching.activity;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchActivity;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.databinding.ActivityNavigationbarBinding;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.QuickLaunchItem;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.Version;
import static cgeo.geocaching.settings.Settings.CUSTOMBNITEM_NEARBY;
import static cgeo.geocaching.settings.Settings.CUSTOMBNITEM_NONE;
import static cgeo.geocaching.settings.Settings.CUSTOMBNITEM_PLACEHOLDER;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.navigation.NavigationBarView;

public abstract class AbstractNavigationBarActivity extends AbstractActionBarActivity implements NavigationBarView.OnItemSelectedListener {
    private static final String STATE_BACKUPUTILS = "backuputils";

    public static final @IdRes
    int MENU_MAP = R.id.page_map;
    public static final @IdRes
    int MENU_LIST = R.id.page_list;
    public static final @IdRes
    int MENU_SEARCH = R.id.page_search;
    public static final @IdRes
    int MENU_CUSTOM = R.id.page_custom;
    public static final @IdRes
    int MENU_HOME = R.id.page_home;
    public static final @IdRes
    int MENU_HIDE_NAVIGATIONBAR = -1;

    private static Boolean loginSuccessful = null; // must be static so that the login state is stored while switching between activities

    private static final Object initializedMutex = new Object();
    private static boolean initialized = false;
    private static boolean restoreMessageShown = false;
    private BackupUtils backupUtils = null;

    private static final String BUNDLE_HIDENAVIGATIONBAR = "hideNavigationBar";
    private boolean hideNavigationBar = false;

    private ActivityNavigationbarBinding binding = null;

    private final ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
    private final Handler loginHandler = new Handler();

    private static final AtomicInteger LOGINS_IN_PROGRESS = new AtomicInteger(0);
    private static final AtomicInteger lowPrioNotificationCounter = ((CgeoApplication) CgeoApplication.getInstance()).getLowPrioNotificationCounter();
    private static final AtomicBoolean hasHighPrioNotification = ((CgeoApplication) CgeoApplication.getInstance()).getHasHighPrioNotification();

    @Override
    public void setContentView(final int layoutResID) {
        checkIntentHideNavigationBar();
        final View view = getLayoutInflater().inflate(layoutResID, null);
        setContentView(view);
    }

    @Override
    public void setContentView(final View contentView) {
        checkIntentHideNavigationBar();
        binding = ActivityNavigationbarBinding.inflate(getLayoutInflater());
        binding.activityContent.addView(contentView);
        super.setContentView(binding.getRoot());

        // --- other initialization --- //
        updateSelectedBottomNavItemId();
        // long click event listeners
        findViewById(MENU_LIST).setOnLongClickListener(view -> onListsLongClicked());
        findViewById(MENU_SEARCH).setOnLongClickListener(view -> onSearchLongClicked());
        // will be called if c:geo cannot log in
        startLoginIssueHandler();
    }

    @Nullable
    protected Handler getUpdateUserInfoHandler() {
        return null;
    }


    protected void onLoginIssue(final boolean issue) {
        synchronized (hasHighPrioNotification) {
            hasHighPrioNotification.set(issue);
        }
        updateHomeBadge(0);
    }

    private boolean onListsLongClicked() {
        new StoredList.UserInterface(this).promptForListSelection(R.string.list_title, selectedListId -> {
            if (selectedListId == PseudoList.HISTORY_LIST.id) {
                startActivity(CacheListActivity.getHistoryIntent(this));
            } else {
                Settings.setLastDisplayedList(selectedListId);
                startActivity(CacheListActivity.getActivityOfflineIntent(this));
            }
            ActivityMixin.overrideTransitionToFade(this);
        }, false, PseudoList.NEW_LIST.id);
        return true;
    }

    private boolean onSearchLongClicked() {
        final ArrayList<Geocache> lastCaches = new ArrayList<>(DataStore.getLastOpenedCaches());

        if (lastCaches.isEmpty()) {
            showToast(R.string.cache_recently_viewed_empty);
            return true;
        }

        final ListAdapter adapter = new ArrayAdapter<Geocache>(this, R.layout.cacheslist_item_select, lastCaches) {
            @Override
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                return GeoItemSelectorUtils.createGeocacheItemView(AbstractNavigationBarActivity.this, getItem(position),
                        GeoItemSelectorUtils.getOrCreateView(AbstractNavigationBarActivity.this, convertView, parent));
            }
        };
        Dialogs.newBuilder(this)
                .setTitle(R.string.cache_recently_viewed)
                .setAdapter(adapter, (dialog, which) -> CacheDetailActivity.startActivity(this, lastCaches.get(which).getGeocode()))
                .setPositiveButton(R.string.map_as_list, (d, w) -> {
                    CacheListActivity.startActivityLastViewed(this, new SearchResult(lastCaches));
                    ActivityMixin.overrideTransitionToFade(this);
                })
                .setNegativeButton(R.string.cache_clear_recently_viewed, (d, w) -> Settings.clearRecentlyViewedHistory())
                .show();
        return true;
    }

    @Override
    protected void onDestroy() {
        // remove callbacks before closing activity to avoid memory leaks
        loginHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    @Override
    public void onPause() {
        unregisterReceiver(connectivityChangeReceiver);
        super.onPause();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backupUtils = new BackupUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS));
    }

    @Override
    protected void onStart() {
        super.onStart();
        runInitAndMaintenance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSelectedBottomNavItemId();
        startLoginIssueHandler();
        setCustomBNitem();
        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        // avoid weird transitions
        ActivityMixin.overrideTransitionToFade(this);
    }

    @Override
    public void onBackPressed() {
        if (isTaskRoot() && !(this instanceof MainActivity)) {
            startActivity(new Intent(this, MainActivity.class));
        }
        super.onBackPressed();

        // avoid weird transitions
        ActivityMixin.overrideTransitionToFade(this);
    }

    public void updateSelectedBottomNavItemId() {
        // unregister listener before changing anything, as it would otherwise trigger the listener directly
        ((NavigationBarView) binding.activityNavigationBar).setOnItemSelectedListener(null);

        final int menuId = hideNavigationBar ? MENU_HIDE_NAVIGATIONBAR : getSelectedBottomItemId();

        if (menuId == MENU_HIDE_NAVIGATIONBAR) {
            binding.activityNavigationBar.setVisibility(View.GONE);
        } else {
            binding.activityNavigationBar.setVisibility(View.VISIBLE);
            ((NavigationBarView) binding.activityNavigationBar).setSelectedItemId(menuId);
        }

        // Don't show back button if bottom navigation is visible (although they can have a backstack as well)
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(menuId == MENU_HIDE_NAVIGATIONBAR);
        }

        // re-register the listener
        ((NavigationBarView) binding.activityNavigationBar).setOnItemSelectedListener(this);
    }

    private void setCustomBNitem() {
        final MenuItem menu = ((NavigationBarView) binding.activityNavigationBar).getMenu().findItem(MENU_CUSTOM);

        menu.setVisible(true);
        menu.setEnabled(true);
        final int item = Settings.getCustomBNitem();
        if (item == CUSTOMBNITEM_NEARBY) {
            menu.setIcon(R.drawable.ic_menu_nearby);
            menu.setTitle(R.string.caches_nearby_button);
        } else if (item == CUSTOMBNITEM_NONE) {
            menu.setVisible(false);
        } else if (item == CUSTOMBNITEM_PLACEHOLDER) {
            menu.setEnabled(false);
            menu.setIcon(R.drawable.ic_empty_placeholder);
            menu.setTitle("");
        } else {
            final QuickLaunchItem iitem = (QuickLaunchItem) QuickLaunchItem.getById(item, QuickLaunchItem.ITEMS);
            menu.setIcon(iitem.iconRes);
            menu.setTitle(iitem.getTitleResId());
        }
    }

    /**
     * @return the menu item id which should be selected
     */
    public abstract @IdRes
    int getSelectedBottomItemId();

    public void onNavigationItemReselected(final @NonNull MenuItem item) {
        // do nothing by default. Can be overridden by subclasses.
    }

    @Override
    public boolean onNavigationItemSelected(final @NonNull MenuItem item) {
        final int id = item.getItemId();

        if (id == getSelectedBottomItemId()) {
            onNavigationItemReselected(item);
            ActivityMixin.overrideTransitionToFade(this);
            return true;
        }
        return onNavigationItemSelectedDefaultBehaviour(item);
    }

    public static Intent getBottomNavigationIntent(final Activity fromActivity, final int id) {
        if (id == MENU_MAP) {
            return DefaultMap.getLiveMapIntent(fromActivity);
        } else if (id == MENU_LIST) {
            return CacheListActivity.getActivityOfflineIntent(fromActivity);
        } else if (id == MENU_SEARCH) {
            return new Intent(fromActivity, SearchActivity.class);
        } else if (id == MENU_CUSTOM) {
            return CacheListActivity.getNearestIntent(fromActivity);
        } else if (id == MENU_HOME) {
            return new Intent(fromActivity, MainActivity.class);
        } else {
            throw new IllegalStateException("unknown navigation item selected"); // should never happen
        }
    }

    private static void launchActivity(final Activity fromActivity, final int id) {
        if (id == MENU_CUSTOM) {
            final int item = Settings.getCustomBNitem();
            if (item != CUSTOMBNITEM_NEARBY) {
                QuickLaunchItem.launchQuickLaunchItem(fromActivity, item, false);
                return;
            }
        }
        final Intent launchIntent = getBottomNavigationIntent(fromActivity, id);
        fromActivity.startActivity(launchIntent);
    }

    public static void setIntentHideBottomNavigation(final Intent intent, final boolean hideBottomNavigation) {
        intent.putExtra(BUNDLE_HIDENAVIGATIONBAR, hideBottomNavigation);
    }

    protected void checkIntentHideNavigationBar() {
        checkIntentHideNavigationBar(false);
    }

    protected void checkIntentHideNavigationBar(final boolean defaultValue) {
        final Intent intent = getIntent();
        hideNavigationBar = (intent != null && intent.hasExtra(BUNDLE_HIDENAVIGATIONBAR)) ? intent.getBooleanExtra(BUNDLE_HIDENAVIGATIONBAR, defaultValue) : defaultValue;
    }

    public boolean onNavigationItemSelectedDefaultBehaviour(final @NonNull MenuItem item) {
        launchActivity(this, item.getItemId());

        // Clear activity stack if the user actively navigates via the bottom navigation
        clearBackStack();

        // avoid weird transitions
        ActivityMixin.overrideTransitionToFade(this);
        return true;
    }

    private final class ConnectivityChangeReceiver extends BroadcastReceiver {
        private boolean isConnected = Network.isConnected();

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final boolean wasConnected = isConnected;
            isConnected = Network.isConnected();
            if (isConnected && !wasConnected) {
                startLoginIssueHandler();
            }
        }
    }

    /**
     * check if at least one connector has been logged in successfully
     */
    public static boolean anyConnectorLoggedIn() {
        final ILogin[] activeConnectors = ConnectorFactory.getActiveLiveConnectors();
        for (final IConnector conn : activeConnectors) {
            if (((ILogin) conn).isLoggedIn()) {
                return true;
            }
        }
        return false;
    }

    /**
     * detect whether c:geo is unable to log in
     */
    public void startLoginIssueHandler() {
        if (loginSuccessful != null && !loginSuccessful) {
            loginSuccessful = anyConnectorLoggedIn();
        }
        if (loginSuccessful != null && !loginSuccessful) {
            onLoginIssue(true); // login still failing. Start loginIssueCallback
        }
        if (loginSuccessful != null && loginSuccessful) {
            onLoginIssue(false);
            return; // there was a successfully login
        }

        if (!Network.isConnected()) {
            onLoginIssue(true);
            return;
        }

        // We are probably not yet ready. Log in and wait a bit...
        startBackgroundLogin(getUpdateUserInfoHandler());
        loginHandler.postDelayed(() -> {
            loginSuccessful = anyConnectorLoggedIn();
            onLoginIssue(!loginSuccessful);
        }, 10000);
    }

    private void startBackgroundLogin(@Nullable final Handler updateUserInfoHandler) {

        final ILogin[] loginConns = ConnectorFactory.getActiveLiveConnectors();

        //ensure that login is not done while another login is still in progress
        synchronized (LOGINS_IN_PROGRESS) {
            if (LOGINS_IN_PROGRESS.get() > 0) {
                return;
            }
            LOGINS_IN_PROGRESS.set(loginConns.length);
        }

        final boolean mustLogin = ConnectorFactory.mustRelog();

        for (final ILogin conn : loginConns) {
            if (mustLogin || !conn.isLoggedIn()) {
                AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
                    if (mustLogin) {
                        // Properly log out from geocaching.com
                        conn.logout();
                    }
                    conn.login();

                    LOGINS_IN_PROGRESS.addAndGet(-1);

                    // the login state might have changed...
                    if (anyConnectorLoggedIn()) {
                        runOnUiThread(() -> onLoginIssue(false));
                    }
                    if (updateUserInfoHandler != null) {
                        updateUserInfoHandler.sendEmptyMessage(-1);
                    }
                });
            }
        }
    }

    public void updateHomeBadge(final int delta) {
        final int badgeColor;
        synchronized (hasHighPrioNotification) {
            badgeColor = hasHighPrioNotification.get() ? 0xffff0000 : 0xff0a67e2;
        }
        synchronized (lowPrioNotificationCounter) {
            lowPrioNotificationCounter.set(lowPrioNotificationCounter.get() + delta);
            final BadgeDrawable badge = ((NavigationBarView) binding.activityNavigationBar).getOrCreateBadge(MENU_HOME);
            badge.clearNumber();
            badge.setBackgroundColor(badgeColor);
            badge.setVisible(lowPrioNotificationCounter.get() > 0);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBundle(STATE_BACKUPUTILS, backupUtils.getState());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);  // call super to make lint happy
        backupUtils.onActivityResult(requestCode, resultCode, intent);
    }

    protected void runInitAndMaintenance() {
        synchronized (initializedMutex) {
            if (initialized) {
                return;
            }
            initialized = true;
        }
        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "AbstractBottomNavigationActivity.runInitAndMaintenance")) {

            //check database
            final String errorMsg = DataStore.initAndCheck();
            if (errorMsg != null) {
                DebugUtils.askUserToReportProblem(this, "Fatal DB error: " + errorMsg);
            }
            cLog.add("ds");

            Log.i("Starting " + getPackageName() + ' ' + Version.getVersionCode(this) + " a.k.a " + Version.getVersionName(this));

            final LocationDataProvider locationDataProvider = LocationDataProvider.getInstance();
            locationDataProvider.initialize();
            // Attempt to acquire an initial location before any real activity happens.
            locationDataProvider.geoDataObservable(true).subscribeOn(AndroidRxUtils.looperCallbacksScheduler).take(1).subscribe();
            cLog.add("ph");

            checkRestore();

            DataStore.cleanIfNeeded(this);

            LocalStorage.initGeocacheDataDir();
            if (LocalStorage.isRunningLowOnDiskSpace()) {
                SimpleDialog.of(this).setTitle(R.string.init_low_disk_space).setMessage(R.string.init_low_disk_space_message).show();
            }
            cLog.add("ls");

            confirmDebug();

            //do file migrations if necessary
            LocalStorage.migrateLocalStorage(this);
            cLog.add("mls");

            //sync map Theme folder
            RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder();
            cLog.add("rth");

            // automated backup check
            if (Settings.automaticBackupDue()) {
                new BackupUtils(this, null).backup(() -> Settings.setAutomaticBackupLastCheck(false), true);
            }
            cLog.add("ab");

            // check for finished, but unreceived downloads
            DownloaderUtils.checkPendingDownloads(this);
        }
    }

    private void checkRestore() {
        if (DataStore.isNewlyCreatedDatebase() && !restoreMessageShown && BackupUtils.hasBackup(BackupUtils.newestBackupFolder(false))) {
            restoreMessageShown = true;
            Dialogs.newBuilder(this)
                    .setTitle(res.getString(R.string.init_backup_restore))
                    .setMessage(res.getString(R.string.init_restore_confirm))
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.ok), (dialog, id) -> {
                        dialog.dismiss();
                        DataStore.resetNewlyCreatedDatabase();
                        backupUtils.restore(BackupUtils.newestBackupFolder(false));
                    })
                    .setNegativeButton(getString(android.R.string.cancel), (dialog, id) -> {
                        dialog.cancel();
                        DataStore.resetNewlyCreatedDatabase();
                    })
                    .create()
                    .show();
        }
    }

    private void confirmDebug() {
        if (Settings.isDebug() && !BuildConfig.DEBUG) {
            SimpleDialog.of(this).setTitle(R.string.init_confirm_debug).setMessage(R.string.list_confirm_debug_message).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm((dialog, whichButton) -> Settings.setDebug(false));
        }
    }



}
