package cgeo.geocaching.activity;

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
import cgeo.geocaching.databinding.ActivityBottomNavigationBinding;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
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


public abstract class AbstractBottomNavigationActivity extends AbstractActionBarActivity implements NavigationBarView.OnItemSelectedListener {
    public static final @IdRes
    int MENU_MAP = R.id.page_map;
    public static final @IdRes
    int MENU_LIST = R.id.page_list;
    public static final @IdRes
    int MENU_SEARCH = R.id.page_search;
    public static final @IdRes
    int MENU_NEARBY = R.id.page_nearby;
    public static final @IdRes
    int MENU_HOME = R.id.page_home;
    public static final @IdRes
    int MENU_HIDE_BOTTOM_NAVIGATION = -1;

    private static Boolean loginSuccessful = null; // must be static so that the login state is stored while switching between activities


    private ActivityBottomNavigationBinding binding = null;

    private final ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
    private final Handler loginHandler = new Handler();

    private static final AtomicInteger LOGINS_IN_PROGRESS = new AtomicInteger(0);
    private static final AtomicInteger lowPrioNotificationCounter = ((CgeoApplication) CgeoApplication.getInstance()).getLowPrioNotificationCounter();
    private static final AtomicBoolean hasHighPrioNotification = ((CgeoApplication) CgeoApplication.getInstance()).getHasHighPrioNotification();

    @Override
    public void setContentView(final int layoutResID) {
        final View view = getLayoutInflater().inflate(layoutResID, null);
        setContentView(view);
    }

    @Override
    public void setContentView(final View contentView) {
        binding = ActivityBottomNavigationBinding.inflate(getLayoutInflater());
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
                return GeoItemSelectorUtils.createGeocacheItemView(AbstractBottomNavigationActivity.this, getItem(position),
                        GeoItemSelectorUtils.getOrCreateView(AbstractBottomNavigationActivity.this, convertView, parent));
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
    protected void onResume() {
        super.onResume();
        updateSelectedBottomNavItemId();
        startLoginIssueHandler();
        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        // avoid weired transitions
        ActivityMixin.overrideTransitionToFade(this);
    }

    @Override
    public void onBackPressed() {
        if (isTaskRoot() && !(this instanceof MainActivity)) {
            startActivity(new Intent(this, MainActivity.class));
        }
        super.onBackPressed();

        // avoid weired transitions
        ActivityMixin.overrideTransitionToFade(this);
    }

    public void updateSelectedBottomNavItemId() {
        // unregister listener before changing anything, as it would otherwise trigger the listener directly
        ((NavigationBarView) binding.activityBottomNavigation).setOnItemSelectedListener(null);

        final int menuId = getSelectedBottomItemId();

        if (menuId == MENU_HIDE_BOTTOM_NAVIGATION) {
            binding.activityBottomNavigation.setVisibility(View.GONE);
        } else {
            binding.activityBottomNavigation.setVisibility(View.VISIBLE);
            ((NavigationBarView) binding.activityBottomNavigation).setSelectedItemId(menuId);
        }

        // Don't show back button if bottom navigation is visible (although they can have a backstack as well)
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(menuId == MENU_HIDE_BOTTOM_NAVIGATION);
        }

        // re-register the listener
        ((NavigationBarView) binding.activityBottomNavigation).setOnItemSelectedListener(this);
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

    public boolean onNavigationItemSelectedDefaultBehaviour(final @NonNull MenuItem item) {
        final int id = item.getItemId();
        final Intent launchIntent;

        if (id == MENU_MAP) {
            launchIntent = DefaultMap.getLiveMapIntent(this);
        } else if (id == MENU_LIST) {
            launchIntent = CacheListActivity.getActivityOfflineIntent(this);
        } else if (id == MENU_SEARCH) {
            launchIntent = new Intent(this, SearchActivity.class);
        } else if (id == MENU_NEARBY) {
            launchIntent = CacheListActivity.getNearestIntent(this);
        } else if (id == MENU_HOME) {
            launchIntent = new Intent(this, MainActivity.class);
        } else {
            throw new IllegalStateException("unknown navigation item selected"); // should never happen
        }

        startActivity(launchIntent);

        // Clear activity stack if the user actively navigates via the bottom navigation
        clearBackStack();

        // avoid weird transitions
        ActivityMixin.overrideTransitionToFade(this);
        return true;
    }

    /** prerequisite for toggleActionBar without moving content around; call it before setContentView() */
    public void setStableLayout() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    public boolean toggleActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return false;
        }
        if (actionBar.isShowing()) {
            actionBar.hide();
            return false;
        } else {
            actionBar.show();
            return true;
        }
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
     *
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
            final BadgeDrawable badge = ((NavigationBarView) binding.activityBottomNavigation).getOrCreateBadge(MENU_HOME);
            badge.clearNumber();
            badge.setBackgroundColor(badgeColor);
            badge.setVisible(lowPrioNotificationCounter.get() > 0);
        }
    }
}
