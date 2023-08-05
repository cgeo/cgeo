package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.IAvatar;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.gc.BookmarkListActivity;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.connector.gc.PocketQueryListActivity;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.databinding.MainActivityBinding;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.downloader.PendingDownloadsActivity;
import cgeo.geocaching.enumerations.QuickLaunchItem;
import cgeo.geocaching.helper.UsefulAppsActivity;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.permission.PermissionAction;
import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.search.GeocacheSuggestionsAdapter;
import cgeo.geocaching.search.SearchUtils;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.GnssStatusProvider;
import cgeo.geocaching.sensors.GnssStatusProvider.Status;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.FoundNumCounter;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.ui.AvatarUtils;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.unifiedmap.UnifiedMapActivity;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.DisplayUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.util.Pair;
import androidx.core.view.MenuCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class MainActivity extends AbstractNavigationBarActivity {

    private MainActivityBinding binding;

    /**
     * view of the action bar search
     */
    private SearchView searchView;
    private MenuItem searchItem;

    private boolean initialized = false;

    private final UpdateLocation locationUpdater = new UpdateLocation();
    private final Handler updateUserInfoHandler = new UpdateUserInfoHandler(this);
    /**
     * initialization with an empty subscription
     */
    private final CompositeDisposable resumeDisposables = new CompositeDisposable();

    private final PermissionAction<Void> askLocationPermissionAction = PermissionAction.register(this, PermissionContext.LOCATION, b -> binding.locationStatus.updatePermissions());
    private final PermissionAction<Void> askShowWallpaperPermissionAction = PermissionAction.register(this, PermissionContext.SHOW_WALLPAPER, b -> setWallpaper());

    private Long lastMCTime = 0L;

    private static final class UpdateUserInfoHandler extends WeakReferenceHandler<MainActivity> {

        UpdateUserInfoHandler(final MainActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            try (ContextLogger ignore = new ContextLogger(Log.LogLevel.DEBUG, "MainActivity.UpdateUserInfoHandler.handleMessage")) {
                final MainActivity activity = getReference();
                if (activity != null) {
                    // Get active connectors with login status
                    final ILogin[] loginConns = ConnectorFactory.getActiveLiveConnectors();

                    // Update UI
                    activity.binding.connectorstatusArea.setAdapter(new ArrayAdapter<ILogin>(activity, R.layout.main_activity_connectorstatus, loginConns) {
                        @Override
                        public View getView(final int position, final View convertView, @NonNull final android.view.ViewGroup parent) {
                            // do NOT use convertView, as it gets filled asynchronously, which may lead to the wrong view being filled
                            final View view = activity.getLayoutInflater().inflate(R.layout.main_activity_connectorstatus, parent, false);
                            final ILogin connector = getItem(position);
                            fillView(view, connector);
                            return view;

                        }

                        private void fillView(final View connectorInfo, final ILogin conn) {

                            final TextView connectorStatus = connectorInfo.findViewById(R.id.item_status);
                            final StringBuilder connInfo = new StringBuilder(conn.getNameAbbreviated()).append(Formatter.SEPARATOR).append(conn.getLoginStatusString());
                            if (conn instanceof GCConnector && !StringUtils.equalsAny(conn.getLoginStatusString(), activity.getString(R.string.init_login_popup_working), activity.getString(R.string.init_login_popup_ok))) {
                                final Pair<String, Long> lastError = Settings.getLastLoginErrorGC();
                                if (lastError != null && StringUtils.isNotBlank(lastError.first)) {
                                    connInfo.append(" (").append(lastError.first).append(")");
                                }
                            }
                            connectorStatus.setText(connInfo);
                            connectorStatus.setOnClickListener(v -> SettingsActivity.openForScreen(R.string.preference_screen_services, activity));

                            AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler,
                                    () -> {
                                        final StringBuilder userFoundCount = new StringBuilder();

                                        final int count = FoundNumCounter.getAndUpdateFoundNum(conn);
                                        if (count >= 0) {
                                            userFoundCount.append(activity.getResources().getQuantityString(R.plurals.user_finds, count, count));

                                            if (Settings.isDisplayOfflineLogsHomescreen()) {
                                                final int offlinefounds = DataStore.getFoundsOffline(conn);
                                                if (offlinefounds > 0) {
                                                    userFoundCount.append(" + ").append(activity.getResources().getQuantityString(R.plurals.user_finds_offline, offlinefounds, offlinefounds));
                                                }
                                            }
                                        }
                                        final String userNameText = FoundNumCounter.getNotBlankUserName(conn);
                                        return new Pair<>(userFoundCount, userNameText);
                                    },
                                    p -> {
                                        if (conn instanceof GCConnector) {
                                            connectorStatus.setText(connInfo.append(Formatter.SEPARATOR).append(CgeoApplication.getInstance().getString(Settings.isGCPremiumMember() ? R.string.gc_premium : R.string.gc_basic)));
                                        }
                                        final TextView userName = connectorInfo.findViewById(R.id.item_title);
                                        final TextView userFounds = connectorInfo.findViewById(R.id.item_info);
                                        userName.setText(p.second);
                                        final String userFoundCount = p.first.toString();
                                        if (userFoundCount.isEmpty()) {
                                            userFounds.setVisibility(View.GONE);
                                        } else {
                                            userFounds.setVisibility(View.VISIBLE);
                                            userFounds.setText(userFoundCount);
                                            userFounds.setOnClickListener(v -> {
                                                activity.startActivity(CacheListActivity.getHistoryIntent(activity));
                                                ActivityMixin.overrideTransitionToFade(activity);
                                            });
                                        }
                                    });

                            final ImageView userAvatar = connectorInfo.findViewById(R.id.item_icon);

                            if (conn instanceof IAvatar) {
                                // already reserve space, so that other content does not jump as soon as avatar is loaded
                                userAvatar.setVisibility(View.INVISIBLE);

                                AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler,
                                        () -> AvatarUtils.getAvatar((IAvatar) conn),
                                        img -> {
                                            userAvatar.setVisibility(View.VISIBLE);
                                            if (img == null) {
                                                userAvatar.setImageResource(R.drawable.avartar_placeholder);
                                            } else {
                                                userAvatar.setImageDrawable(img);
                                            }
                                        });
                            } else {
                                userAvatar.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }
        }
    }

    private class UpdateLocation extends GeoDirHandler {

        @Override
        @SuppressLint("SetTextI18n")
        public void updateGeoData(final GeoData geo) {
            binding.locationStatus.updateGeoData(geo);
        }
    }

    private final Consumer<GnssStatusProvider.Status> satellitesHandler = new Consumer<Status>() {
        @Override
        public void accept(final Status gnssStatus) {
            binding.locationStatus.updateSatelliteStatus(gnssStatus);
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "MainActivity.onCreate")) {
            // don't call the super implementation with the layout argument, as that would set the wrong theme
            super.onCreate(savedInstanceState);

            binding = MainActivityBinding.inflate(getLayoutInflater());

            // adding the bottom navigation component is handled by {@link AbstractBottomNavigationActivity#setContentView}
            setContentView(binding.getRoot());

            setTitle(R.string.app_name);

            cLog.add("setview");

            setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // type to search

            init();
            cLog.add("init");

            binding.infoNotloggedin.setOnClickListener(v ->
                    SimpleDialog.of(this).setTitle(R.string.warn_notloggedin_title).setMessage(R.string.warn_notloggedin_long).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm((dialog, which) -> SettingsActivity.openForScreen(R.string.preference_screen_services, this)));

            // automated update check
            DownloaderUtils.checkForRoutingTileUpdates(this);
            cLog.add("rtu");

            DownloaderUtils.checkForMapUpdates(this);
            cLog.add("mu");

            binding.locationStatus.setPermissionRequestCallback(() -> {
                this.askLocationPermissionAction.launch(null);
            });

            if (Settings.isWallpaper()) {
                askShowWallpaperPermissionAction.launch();
            }

            configureMessageCenterPolling();
        }

        if (Log.isEnabled(Log.LogLevel.DEBUG)) {
            binding.getRoot().post(() -> Log.d("Post after MainActivity.onCreate"));
        }

    }

    private void configureMessageCenterPolling() {
        final Observable<JsonNode> pollingObservable = Observable.interval(10, 300, TimeUnit.SECONDS)
                .flatMap(tick -> {
                    if (Settings.getBoolean(R.string.pref_pollMessageCenter, false)) {
                        final JsonNode temp = getMessageCenterStatus();
                        return temp != null ? Observable.just(temp) : Observable.empty();
                    } else {
                        return Observable.empty();
                    }
                });

        resumeDisposables.add(pollingObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe(data -> {
                    @SuppressLint("SimpleDateFormat")
                    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    final long time = Objects.requireNonNull(df.parse(data.at("/lastConversationActivityDateUtc").textValue())).getTime();
                    final int count = Integer.parseInt(data.at("/unreadConversationCount").toString());
                    if (time != lastMCTime) {
                        lastMCTime = time;
                        if (count > 0) {
                            Looper.prepare();
                            new Handler(Looper.getMainLooper()).post(() -> { // needs to be done on UI thread
                                updateHomeBadge(1);
                                displayActionItem(R.id.mcupdate, res.getQuantityString(R.plurals.mcupdate, count, count), (actionRequested) -> {
                                    updateHomeBadge(-1);
                                    if (actionRequested) {
                                        ShareUtils.openUrl(this, GCConstants.URL_MESSAGECENTER);
                                    }
                                });
                            });
                        }
                    }
                }, throwable -> {
                    Log.e("Error occurred while polling message center: " + throwable.getMessage());
                }));
    }


    @Nullable
    private static JsonNode getMessageCenterStatus() {
        if (!GCConnector.getInstance().isLoggedIn()) {
            return null;
        }
        final Response response = Network.getRequest("https://www.geocaching.com/api/communication-service/participant/" + GCLogin.getInstance().getPublicGuid() + "/summary/", new Parameters()).blockingGet();
        if (!response.isSuccessful()) {
            return null;
        }
        final String jsonString = Network.getResponseData(response);
        try {
            return JsonUtils.reader.readTree(jsonString);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void prepareQuickLaunchItems() {
        final int dimSize = DisplayUtils.getPxFromDp(getResources(), 48.0f, 1.0f);
        final int dimMargin = DisplayUtils.getPxFromDp(getResources(), 7.0f, 1.0f);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dimSize, dimSize);
        lp.setMargins(dimMargin, 0, dimMargin, 0);

        final List<Integer> quicklaunchitems = Settings.getInfoItems(R.string.pref_quicklaunchitems, 1);
        binding.quicklaunchitems.removeAllViews();
        binding.quicklaunchitems.setVisibility(View.GONE);
        for (int i : quicklaunchitems) {
            final QuickLaunchItem item = (QuickLaunchItem) QuickLaunchItem.getById(i, QuickLaunchItem.ITEMS);
            if (item != null && (!item.gcPremiumOnly || Settings.isGCPremiumMember())) {
                addButton(item.iconRes, lp, () -> QuickLaunchItem.launchQuickLaunchItem(this, item.getId()), getString(item.getTitleResId()));
            }
        }

        // temporarily add button for unified map, if enabled in settings
        if (Settings.showUnifiedMap()) {
            addButton(R.drawable.sc_icon_map, lp, () -> startActivity(new Intent(this, UnifiedMapActivity.class)), "Start unified map");
        }
    }

    private void addButton(@DrawableRes final int iconRes, final LinearLayout.LayoutParams lp, final Runnable action, final String tooltip) {
        final MaterialButton b = new MaterialButton(this, null, R.attr.quickLaunchButtonStyle);
        b.setIconResource(iconRes);
        b.setLayoutParams(lp);
        b.setVisibility(View.VISIBLE);
        b.setOnClickListener(view -> action.run());
        TooltipCompat.setTooltipText(b, tooltip);
        binding.quicklaunchitems.addView(b);
        binding.quicklaunchitems.setVisibility(View.VISIBLE);
    }

    private void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        updateCacheCounter();
        prepareQuickLaunchItems();
        checkPendingDownloads();
        binding.locationStatus.setShowAddress(Settings.isShowAddress());
    }

    /** prompts user if there's at least one blocked or failed download */
    private void checkPendingDownloads() {
        if (Settings.pendingDownloadsNeedCheck()) {
            final ArrayList<PendingDownload.PendingDownloadDescriptor> pendingDownloads = PendingDownload.getAllPendingDownloads();
            if (pendingDownloads.size() == 0) {
                return;
            }

            final DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            for (PendingDownload.PendingDownloadDescriptor download : pendingDownloads) {
                final DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(download.id);
                try (Cursor c = downloadManager.query(query)) {
                    if (c.isAfterLast()) {
                        if (download.date < 1665433698000L /* Oct 10th, 2022 */) {
                            // entry is pretty old and no longer available in system's download manager, so do some housekeeping in our own database
                            PendingDownload.remove(download.id);
                            Log.w("removed stale download no longer recognized by download manager: id=" + download.id + ", fn=" + download.filename + ", date=" + Formatter.formatDate(download.date));
                        }
                    } else {
                        while (c.moveToNext()) {
                            final int colStatus = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            if (colStatus >= 0) {
                                final int status = c.getInt(colStatus);
                                if (status != DownloadManager.STATUS_RUNNING && status != DownloadManager.STATUS_SUCCESSFUL) {
                                    SimpleDialog.of(this).setTitle(R.string.downloader_pending_downloads).setMessage(R.string.downloader_pending_info).confirm((dialog, which) -> startActivity(new Intent(this, PendingDownloadsActivity.class)));
                                    Settings.setPendingDownloadsLastCheck(false);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        init();
    }

    @Override
    public void onResume() {
        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "MainActivity.onResume")) {

            super.onResume();

            resumeDisposables.add(locationUpdater.start(GeoDirHandler.UPDATE_GEODATA | GeoDirHandler.LOW_POWER));
            resumeDisposables.add(LocationDataProvider.getInstance().gpsStatusObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(satellitesHandler));

            updateUserInfoHandler.sendEmptyMessage(-1);
            cLog.add("perm");

            setWallpaper();

            init();
        }

        if (Log.isEnabled(Log.LogLevel.DEBUG)) {
            binding.getRoot().post(() -> Log.d("Post after MainActivity.onResume"));
        }
    }

    private void setWallpaper() {
        final Drawable wallpaper =  (Settings.isWallpaper() && PermissionContext.SHOW_WALLPAPER.hasAllPermissions()) ?
                WallpaperManager.getInstance(this).getDrawable() : null;
            ((ImageView) findViewById(R.id.background)).setImageDrawable(wallpaper);
    }

    @Override
    public void onDestroy() {
        initialized = false;

        super.onDestroy();
    }

    @Override
    public void onStop() {
        initialized = false;
        super.onStop();
    }

    @Override
    public void onPause() {
        initialized = false;
        resumeDisposables.clear();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        try (ContextLogger ignore = new ContextLogger(Log.LogLevel.DEBUG, "MainActivity.onCreateOptionsMenu")) {

            getMenuInflater().inflate(R.menu.main_activity_options, menu);
            MenuCompat.setGroupDividerEnabled(menu, true);
            final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchItem = menu.findItem(R.id.menu_gosearch);
            searchView = (SearchView) searchItem.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setSuggestionsAdapter(new GeocacheSuggestionsAdapter(this));

            // initialize menu items
            menu.findItem(R.id.menu_wizard).setVisible(!InstallWizardActivity.isConfigurationOk());
            menu.findItem(R.id.menu_update_routingdata).setEnabled(Settings.useInternalRouting());

            final boolean isPremiumActive = Settings.isGCConnectorActive() && Settings.isGCPremiumMember();
            menu.findItem(R.id.menu_pocket_queries).setVisible(isPremiumActive);
            menu.findItem(R.id.menu_bookmarklists).setVisible(isPremiumActive);

            SearchUtils.hideKeyboardOnSearchClick(searchView, searchItem);
            SearchUtils.hideActionIconsWhenSearchIsActive(this, menu, searchItem);
            SearchUtils.handleDropDownVisibility(this, searchView, searchItem);
        }

        if (Log.isEnabled(Log.LogLevel.DEBUG)) {
            binding.getRoot().post(() -> Log.d("Post after MainActivity.onCreateOptionsMenu"));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.menu_about) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.menu_report_problem) {
            DebugUtils.askUserToReportProblem(this, null);
        } else if (id == R.id.menu_helpers) {
            startActivity(new Intent(this, UsefulAppsActivity.class));
        } else if (id == R.id.menu_wizard) {
            final Intent wizard = new Intent(this, InstallWizardActivity.class);
            wizard.putExtra(InstallWizardActivity.BUNDLE_MODE, InstallWizardActivity.needsFolderMigration() ? InstallWizardActivity.WizardMode.WIZARDMODE_MIGRATION.id : InstallWizardActivity.WizardMode.WIZARDMODE_RETURNING.id);
            startActivity(wizard);
        } else if (id == R.id.menu_settings) {
            startActivityForResult(new Intent(this, SettingsActivity.class), Intents.SETTINGS_ACTIVITY_REQUEST_CODE);
        } else if (id == R.id.menu_backup) {
            SettingsActivity.openForScreen(R.string.preference_screen_backup, this);
        } else if (id == R.id.menu_paste_search) {
            startActivity(new Intent(this, SearchActivity.class).setAction(SearchActivity.ACTION_CLIPBOARD).putExtra(SearchManager.QUERY, ClipboardUtils.getText()));
        } else if (id == R.id.menu_history) {
            startActivity(CacheListActivity.getHistoryIntent(this));
            ActivityMixin.overrideTransitionToFade(this);
        } else if (id == R.id.menu_goto) {
            InternalConnector.assertHistoryCacheExists(this);
            CacheDetailActivity.startActivity(this, InternalConnector.GEOCODE_HISTORY_CACHE, true);
        } else if (id == R.id.menu_pocket_queries) {
            if (Settings.isGCPremiumMember()) {
                startActivity(new Intent(this, PocketQueryListActivity.class));
            }
        } else if (id == R.id.menu_bookmarklists) {
            if (Settings.isGCPremiumMember()) {
                startActivity(new Intent(this, BookmarkListActivity.class));
            }
        } else if (id == R.id.menu_update_routingdata) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES, R.string.updates_check, DownloaderUtils::returnFromTileUpdateCheck);
        } else if (id == R.id.menu_update_mapdata) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(this, Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED, R.string.updates_check, DownloaderUtils::returnFromMapUpdateCheck);
        } else if (id == R.id.menu_pending_downloads) {
            startActivity(new Intent(this, PendingDownloadsActivity.class));
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void updateCacheCounter() {
        AndroidRxUtils.bindActivity(this, DataStore.getAllCachesCountObservable()).subscribe(countOfflineCaches -> {
            final TextView counter = findViewById(R.id.offline_counter);
            counter.setVisibility(countOfflineCaches > 0 ? View.VISIBLE : View.GONE);
            if (countOfflineCaches > 0) {
                counter.setText(getResources().getQuantityString(R.plurals.caches_stored_offline, countOfflineCaches, countOfflineCaches));
            }
        }, throwable -> Log.e("Unable to add cache count", throwable));
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);  // call super to make lint happy
        if (requestCode == Intents.SETTINGS_ACTIVITY_REQUEST_CODE) {
            if (resultCode == SettingsActivity.RESTART_NEEDED) {
                ProcessUtils.restartApplication(this);
            }
        } else {
            final IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanResult != null) {
                final String scan = scanResult.getContents();
                if (StringUtils.isBlank(scan)) {
                    return;
                }
                SearchActivity.startActivityScan(scan, this);
            } else if (requestCode == Intents.SEARCH_REQUEST_CODE) {
                // SearchActivity activity returned without making a search
                if (resultCode == RESULT_CANCELED) {
                    String query = intent.getStringExtra(SearchManager.QUERY);
                    if (query == null) {
                        query = "";
                    }
                    SimpleDialog.of(this).setMessage(TextParam.text(res.getString(R.string.unknown_scan) + "\n\n" + query)).show();
                }
            }
        }
    }

    @Nullable
    @Override
    protected Handler getUpdateUserInfoHandler() {
        return updateUserInfoHandler;
    }

    /**
     * if no connector can log in, set visibility of warning message accordingly
     */
    @Override
    protected void onLoginIssue(final boolean issue) {
        if (issue) {
            binding.infoNotloggedinIcon.attributeImage.setImageResource(R.drawable.attribute_wirelessbeacon);
            binding.infoNotloggedinIcon.attributeStrikethru.setVisibility(View.VISIBLE);
            binding.infoNotloggedin.setVisibility(View.VISIBLE);
        } else {
            binding.infoNotloggedin.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        // back may exit the app instead of closing the search action bar
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
            searchItem.collapseActionView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public int getSelectedBottomItemId() {
        return MENU_HOME;
    }

    @Override
    public void updateSelectedBottomNavItemId() {
        super.updateSelectedBottomNavItemId();

        // Always show c:geo logo for this activity
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.cgeo_actionbar_squircle);
            actionBar.setHomeActionContentDescription(R.string.about);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * display action notifications, e. g. update or backup reminders
     * action callback accepts true, if action is to be performed / false if to be postponed
     */
    public void displayActionItem(final int layout, final @StringRes int info, final Action1<Boolean> action) {
        displayActionItem(layout, getString(info), action);
    }

    public void displayActionItem(final int layout, final String info, final Action1<Boolean> action) {
        final TextView l = findViewById(layout);
        if (l != null) {
            l.setVisibility(View.VISIBLE);
            updateHomeBadge(1);
            l.setText(info);
            l.setOnClickListener(v -> {
                action.call(true);
                l.setVisibility(View.GONE);
                updateHomeBadge(-1);
            });
            l.setOnLongClickListener(v -> {
                action.call(false);
                l.setVisibility(View.GONE);
                updateHomeBadge(-1);
                return true;
            });
        }
    }

}
