package cgeo.geocaching;

import cgeo.calendar.CalendarAddon;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.AbstractViewPagerActivity;
import cgeo.geocaching.activity.INavigationSource;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.cachelist.MapsMeCacheListApp;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.apps.navi.NavigationSelectionActionProvider;
import cgeo.geocaching.command.MoveToListAndRemoveFromOthersCommand;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.IgnoreCapability;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
import cgeo.geocaching.connector.capability.PgcChallengeCheckerCapability;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.export.FieldNoteExport;
import cgeo.geocaching.export.GpxExport;
import cgeo.geocaching.export.PersonalNoteExport;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteDialog;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.CacheLogsViewCreator;
import cgeo.geocaching.log.LoggingUI;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.SmileyImage;
import cgeo.geocaching.playservices.AppInvite;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.staticmaps.StaticMapsProvider;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;
import cgeo.geocaching.ui.DecryptTextClickListener;
import cgeo.geocaching.ui.EditNoteDialog;
import cgeo.geocaching.ui.EditNoteDialog.EditNoteDialogListener;
import cgeo.geocaching.ui.ImagesList;
import cgeo.geocaching.ui.IndexOutOfBoundsAvoidingTextView;
import cgeo.geocaching.ui.NavigationActionProvider;
import cgeo.geocaching.ui.OwnerActionsClickListener;
import cgeo.geocaching.ui.TrackableListAdapter;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CheckerUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.SimpleDisposableHandler;
import cgeo.geocaching.utils.SimpleHandler;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;
import cgeo.geocaching.utils.functions.Action1;

import android.R.color;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Activity to handle all single-cache-stuff.
 *
 * e.g. details, description, logs, waypoints, inventory...
 */
public class CacheDetailActivity extends AbstractViewPagerActivity<CacheDetailActivity.Page>
        implements CacheMenuHandler.ActivityInterface, INavigationSource, AndroidBeam.ActivitySharingInterface, EditNoteDialogListener {

    private static final int MESSAGE_FAILED = -1;
    private static final int MESSAGE_SUCCEEDED = 1;

    private static final Pattern[] DARK_COLOR_PATTERNS = {
            Pattern.compile("((?<!bg)color)=\"#" + "(0[0-9]){3}" + "\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("((?<!bg)color)=\"" + "black" + "\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("((?<!bg)color)=\"#" + "000080" + "\"", Pattern.CASE_INSENSITIVE) };
    private static final Pattern[] LIGHT_COLOR_PATTERNS = {
            Pattern.compile("((?<!bg)color)=\"#" + "([F][6-9A-F]){3}" + "\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("((?<!bg)color)=\"" + "white" + "\"", Pattern.CASE_INSENSITIVE) };
    public static final String STATE_PAGE_INDEX = "cgeo.geocaching.pageIndex";

    // Store Geocode here, as 'cache' is loaded Async.
    private String geocode;
    private Geocache cache;
    @NonNull
    private final List<Trackable> genericTrackables = new ArrayList<>();
    private final Progress progress = new Progress();

    private SearchResult search;

    private GeoDirHandler locationUpdater;

    private CharSequence clickedItemText = null;

    /**
     * If another activity is called and can modify the data of this activity, we refresh it on resume.
     */
    private boolean refreshOnResume = false;

    // some views that must be available from everywhere // TODO: Reference can block GC?
    private TextView cacheDistanceView;

    protected ImagesList imagesList;
    private final CompositeDisposable createDisposables = new CompositeDisposable();
    /**
     * waypoint selected in context menu. This variable will be gone when the waypoint context menu is a fragment.
     */
    private Waypoint selectedWaypoint;

    private boolean requireGeodata;
    private final CompositeDisposable geoDataDisposable = new CompositeDisposable();

    private final EnumSet<TrackableBrand> processedBrands = EnumSet.noneOf(TrackableBrand.class);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.cachedetail_activity);

        // get parameters
        final Bundle extras = getIntent().getExtras();
        final Uri uri = AndroidBeam.getUri(getIntent());

        // try to get data from extras
        String name = null;
        String guid = null;

        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            name = extras.getString(Intents.EXTRA_NAME);
            guid = extras.getString(Intents.EXTRA_GUID);
        }

        // When clicking a cache in MapsWithMe, we get back a PendingIntent
        if (StringUtils.isEmpty(geocode)) {
            geocode = MapsMeCacheListApp.getCacheFromMapsWithMe(this, getIntent());
        }

        if (geocode == null && uri != null) {
            geocode = ConnectorFactory.getGeocodeFromURL(uri.toString());
        }

        // try to get data from URI
        if (geocode == null && guid == null && uri != null) {
            final String uriHost = uri.getHost().toLowerCase(Locale.US);
            final String uriPath = uri.getPath().toLowerCase(Locale.US);
            final String uriQuery = uri.getQuery();

            if (uriQuery != null) {
                Log.i("Opening URI: " + uriHost + uriPath + "?" + uriQuery);
            } else {
                Log.i("Opening URI: " + uriHost + uriPath);
            }

            if (uriHost.contains("geocaching.com")) {
                if (StringUtils.startsWith(uriPath, "/geocache/gc")) {
                    geocode = StringUtils.substringBefore(uriPath.substring(10), "_").toUpperCase(Locale.US);
                } else {
                    geocode = uri.getQueryParameter("wp");
                    guid = uri.getQueryParameter("guid");

                    if (StringUtils.isNotBlank(geocode)) {
                        geocode = geocode.toUpperCase(Locale.US);
                        guid = null;
                    } else if (StringUtils.isNotBlank(guid)) {
                        geocode = null;
                        guid = guid.toLowerCase(Locale.US);
                    } else {
                        showToast(res.getString(R.string.err_detail_open));
                        finish();
                        return;
                    }
                }
            }
        }

        // no given data
        if (geocode == null && guid == null) {
            showToast(res.getString(R.string.err_detail_cache));
            finish();
            return;
        }

        // If we open this cache from a search, let's properly initialize the title bar, even if we don't have cache details
        setCacheTitleBar(geocode, name, null);

        final LoadCacheHandler loadCacheHandler = new LoadCacheHandler(this, progress);

        try {
            String title = res.getString(R.string.cache);
            if (StringUtils.isNotBlank(name)) {
                title = name;
            } else if (geocode != null && StringUtils.isNotBlank(geocode)) { // can't be null, but the compiler doesn't understand StringUtils.isNotBlank()
                title = geocode;
            }
            progress.show(this, title, res.getString(R.string.cache_dialog_loading_details), true, loadCacheHandler.disposeMessage());
        } catch (final RuntimeException ignored) {
            // nothing, we lost the window
        }

        final int pageToOpen = savedInstanceState != null ?
                savedInstanceState.getInt(STATE_PAGE_INDEX, 0) :
                Settings.isOpenLastDetailsPage() ? Settings.getLastDetailsPage() : 1;
        createViewPager(pageToOpen, new OnPageSelectedListener() {

            @Override
            public void onPageSelected(final int position) {
                if (Settings.isOpenLastDetailsPage()) {
                    Settings.setLastDetailsPage(position);
                }
                // lazy loading of cache images
                if (getPage(position) == Page.IMAGES) {
                    loadCacheImages();
                }
                requireGeodata = getPage(position) == Page.DETAILS;
                startOrStopGeoDataListener(false);

                // dispose contextual actions on page change
                if (currentActionMode != null) {
                    currentActionMode.finish();
                }
            }
        });
        requireGeodata = pageToOpen == 1;

        final String realGeocode = geocode;
        final String realGuid = guid;
        AndroidRxUtils.networkScheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                search = Geocache.searchByGeocode(realGeocode, StringUtils.isBlank(realGeocode) ? realGuid : null, false, loadCacheHandler);
                loadCacheHandler.sendMessage(Message.obtain());
            }
        });

        // Load Generic Trackables
        if (StringUtils.isNotBlank(geocode)) {
            AndroidRxUtils.bindActivity(this,
            // Obtain the active connectors and load trackables in parallel.
                    Observable.fromIterable(ConnectorFactory.getGenericTrackablesConnectors()).flatMap(new Function<TrackableConnector, Observable<Trackable>>() {
                @Override
                public Observable<Trackable> apply(final TrackableConnector trackableConnector) {
                    processedBrands.add(trackableConnector.getBrand());
                    return Observable.defer(new Callable<Observable<Trackable>>() {
                        @Override
                        public Observable<Trackable> call() {
                            return Observable.fromIterable(trackableConnector.searchTrackables(geocode));
                        }
                    }).subscribeOn(AndroidRxUtils.networkScheduler);
                }
            }).toList()).subscribe(new Consumer<List<Trackable>>() {
                @Override
                public void accept(final List<Trackable> trackables) {
                    // Todo: this is not really a good method, it may lead to duplicates ; ie: in OC connectors.
                    // Store trackables.
                    genericTrackables.addAll(trackables);
                    if (!trackables.isEmpty()) {
                        // Update the UI if any trackables were found.
                        notifyDataSetChanged();
                    }
                }
            });
        }

        locationUpdater = new CacheDetailsGeoDirHandler(this);

        // If we have a newer Android device setup Android Beam for easy cache sharing
        AndroidBeam.enable(this, this);
    }

    @Override
    @Nullable
    public String getAndroidBeamUri() {
        return cache != null ? cache.getCgeoUrl() : null;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE_INDEX, getCurrentItem());
    }

    private void startOrStopGeoDataListener(final boolean initial) {
        final boolean start;
        if (Settings.useLowPowerMode()) {
            geoDataDisposable.clear();
            start = requireGeodata;
        } else {
            start = initial;
        }
        if (start) {
            geoDataDisposable.add(locationUpdater.start(GeoDirHandler.UPDATE_GEODATA));
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, new IntentFilter(Intents.INTENT_CACHE_CHANGED));
    }

    @Override
    public void onResume() {
        super.onResume();
        startOrStopGeoDataListener(true);

        if (refreshOnResume) {
            notifyDataSetChanged();
            refreshOnResume = false;
        }
    }

    @Override
    public void onPause() {
        geoDataDisposable.clear();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        createDisposables.clear();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        if (cache != null) {
            cache.setChangeNotificationHandler(null);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        super.onStop();
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();
        switch (viewId) {
            case R.id.waypoint:
                menu.setHeaderTitle(selectedWaypoint.getName() + " (" + res.getString(R.string.waypoint) + ")");
                getMenuInflater().inflate(R.menu.waypoint_options, menu);
                final boolean isOriginalWaypoint = selectedWaypoint.getWaypointType() == WaypointType.ORIGINAL;
                menu.findItem(R.id.menu_waypoint_reset_cache_coords).setVisible(isOriginalWaypoint);
                menu.findItem(R.id.menu_waypoint_edit).setVisible(!isOriginalWaypoint);
                menu.findItem(R.id.menu_waypoint_duplicate).setVisible(!isOriginalWaypoint);
                menu.findItem(R.id.menu_waypoint_delete).setVisible(!isOriginalWaypoint);
                final boolean hasCoords = selectedWaypoint.getCoords() != null;
                final MenuItem defaultNavigationMenu = menu.findItem(R.id.menu_waypoint_navigate_default);
                defaultNavigationMenu.setVisible(hasCoords);
                defaultNavigationMenu.setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());
                menu.findItem(R.id.menu_waypoint_navigate).setVisible(hasCoords);
                menu.findItem(R.id.menu_waypoint_caches_around).setVisible(hasCoords);
                menu.findItem(R.id.menu_waypoint_copy_coordinates).setVisible(hasCoords);
                break;
            default:
                if (imagesList != null) {
                    imagesList.onCreateContextMenu(menu, view);
                }
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            // waypoints
            case R.id.menu_waypoint_edit:
                if (selectedWaypoint != null) {
                    ensureSaved();
                    EditWaypointActivity.startActivityEditWaypoint(this, cache, selectedWaypoint.getId());
                    refreshOnResume = true;
                }
                return true;
            case R.id.menu_waypoint_copy_coordinates:
                if (selectedWaypoint != null) {
                    final Geopoint coordinates = selectedWaypoint.getCoords();
                    if (coordinates != null) {
                        ClipboardUtils.copyToClipboard(
                                GeopointFormatter.reformatForClipboard(coordinates.toString()));
                        showToast(getString(R.string.clipboard_copy_ok));
                    }
                }
                return true;
            case R.id.menu_waypoint_duplicate:
                ensureSaved();
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(final Void... params) {
                        if (cache.duplicateWaypoint(selectedWaypoint)) {
                            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                            return true;
                        }
                        return false;
                    }

                    @Override
                    protected void onPostExecute(final Boolean result) {
                        if (result) {
                            notifyDataSetChanged();
                        }
                    }
                }.execute();
                return true;
            case R.id.menu_waypoint_delete:
                ensureSaved();
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(final Void... params) {
                        if (cache.deleteWaypoint(selectedWaypoint)) {
                            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                            return true;
                        }
                        return false;
                    }

                    @Override
                    protected void onPostExecute(final Boolean result) {
                        if (result) {
                            notifyDataSetChanged();
                        }
                    }
                }.execute();
                return true;
            case R.id.menu_waypoint_navigate_default:
                if (selectedWaypoint != null) {
                    NavigationAppFactory.startDefaultNavigationApplication(1, this, selectedWaypoint);
                }
                return true;
            case R.id.menu_waypoint_navigate:
                if (selectedWaypoint != null) {
                    NavigationAppFactory.showNavigationMenu(this, null, selectedWaypoint, null);
                }
                return true;
            case R.id.menu_waypoint_caches_around:
                if (selectedWaypoint != null) {
                    final Geopoint coordinates = selectedWaypoint.getCoords();
                    if (coordinates != null) {
                        CacheListActivity.startActivityCoordinates(this, coordinates, selectedWaypoint.getName());
                    }
                }
                return true;
            case R.id.menu_waypoint_reset_cache_coords:
                ensureSaved();
                if (ConnectorFactory.getConnector(cache).supportsOwnCoordinates()) {
                    createResetCacheCoordinatesDialog(selectedWaypoint).show();
                } else {
                    final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.cache), getString(R.string.waypoint_reset), true);
                    final HandlerResetCoordinates handler = new HandlerResetCoordinates(this, progressDialog, false);
                    resetCoords(cache, handler, selectedWaypoint, true, false, progressDialog);
                }
                return true;
            case R.id.menu_calendar:
                CalendarAddon.addToCalendarWithIntent(this, cache);
                return true;
            default:
                break;
        }
        if (imagesList != null && imagesList.onContextItemSelected(item)) {
            return true;
        }
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        CacheMenuHandler.addMenuItems(this, menu, cache);
        final MenuItem menuItem = menu.findItem(R.id.menu_default_navigation);
        final NavigationActionProvider navAction = (NavigationActionProvider) MenuItemCompat.getActionProvider(menuItem);
        if (navAction != null) {
            navAction.setNavigationSource(this);
        }
        NavigationSelectionActionProvider.initialize(menu.findItem(R.id.menu_navigate), cache);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        CacheMenuHandler.onPrepareOptionsMenu(menu, cache);
        LoggingUI.onPrepareOptionsMenu(menu, cache);
        menu.findItem(R.id.menu_edit_fieldnote).setVisible(true);
        menu.findItem(R.id.menu_store_in_list).setVisible(cache != null);
        menu.findItem(R.id.menu_delete).setVisible(cache != null && cache.isOffline());
        menu.findItem(R.id.menu_refresh).setVisible(cache != null && cache.supportsRefresh());
        menu.findItem(R.id.menu_gcvote).setVisible(cache != null && GCVote.isVotingPossible(cache));
        menu.findItem(R.id.menu_checker).setVisible(cache != null && StringUtils.isNotEmpty(CheckerUtils.getCheckerUrl(cache)));
        menu.findItem(R.id.menu_app_invite).setVisible(cache != null && AppInvite.isAvailable());
        menu.findItem(R.id.menu_extract_waypoints).setVisible(cache != null);
        menu.findItem(R.id.menu_export).setVisible(cache != null);
        if (cache != null) {
            final IConnector connector = ConnectorFactory.getConnector(cache);
            if (connector instanceof IgnoreCapability) {
                menu.findItem(R.id.menu_ignore).setVisible(((IgnoreCapability) connector).canIgnoreCache(cache));
            }
            if (connector instanceof PgcChallengeCheckerCapability) {
                menu.findItem(R.id.menu_challenge_checker).setVisible(((PgcChallengeCheckerCapability) connector).isChallengeCache(cache));
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (CacheMenuHandler.onMenuItemSelected(item, this, cache)) {
            return true;
        }

        final int menuItem = item.getItemId();

        switch (menuItem) {
            case R.id.menu_delete:
                dropCache();
                return true;
            case R.id.menu_store_in_list:
                storeCache(false);
                return true;
            case R.id.menu_refresh:
                refreshCache();
                return true;
            case R.id.menu_gcvote:
                showVoteDialog();
                return true;
            case R.id.menu_checker:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(CheckerUtils.getCheckerUrl(cache))));
                return true;
            case R.id.menu_challenge_checker:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://project-gc.com/Challenges/" + cache.getGeocode())));
                return true;
            case R.id.menu_ignore:
                ignoreCache();
                return true;
            case R.id.menu_extract_waypoints:
                extractWaypoints(cache.getDescription(), cache);
                return true;
            case R.id.menu_export_gpx:
                new GpxExport().export(Collections.singletonList(cache), this);
                return true;
            case R.id.menu_export_fieldnotes:
                new FieldNoteExport().export(Collections.singletonList(cache), this);
                return true;
            case R.id.menu_export_persnotes:
                new PersonalNoteExport().export(Collections.singletonList(cache), this);
                return true;
            case R.id.menu_edit_fieldnote:
                ensureSaved();
                editPersonalNote(cache, this);
                return true;
            case R.id.menu_navigate:
                if (NavigationAppFactory.onMenuItemSelected(item, this, cache)) {
                    return true;
                }
                break;
            case R.id.menu_app_invite:
                if (AppInvite.isAvailable()) {
                    AppInvite.send(this, cache);
                }
                return true;
            default:
                if (LoggingUI.onMenuItemSelected(item, this, cache, null)) {
                    refreshOnResume = true;
                    return true;
                }
        }

        return super.onOptionsItemSelected(item);
    }

    private void ignoreCache() {
        Dialogs.confirm(this, R.string.ignore_confirm_title, R.string.ignore_confirm_message, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                AndroidRxUtils.networkScheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        ((IgnoreCapability) ConnectorFactory.getConnector(cache)).ignoreCache(cache);
                    }
                });
                // For consistency, remove also the local cache immediately from memory cache and database
                if (cache.isOffline()) {
                    dropCache();
                    DataStore.removeCache(cache.getGeocode(), EnumSet.of(RemoveFlag.DB));
                }
            }
        });
    }

    private void showVoteDialog() {
        GCVoteDialog.show(this, cache, new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    private static final class CacheDetailsGeoDirHandler extends GeoDirHandler {
        private final WeakReference<CacheDetailActivity> activityRef;

        CacheDetailsGeoDirHandler(final CacheDetailActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void updateGeoData(final GeoData geo) {
            final CacheDetailActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            if (activity.cacheDistanceView == null) {
                return;
            }

            if (activity.cache != null && activity.cache.getCoords() != null) {
                activity.cacheDistanceView.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(activity.cache.getCoords())));
                activity.cacheDistanceView.bringToFront();
            }
        }
    }

    private static final class LoadCacheHandler extends SimpleDisposableHandler {

        LoadCacheHandler(final CacheDetailActivity activity, final Progress progress) {
            super(activity, progress);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            if (msg.what == UPDATE_LOAD_PROGRESS_DETAIL && msg.obj instanceof String) {
                updateStatusMsg((String) msg.obj);
            } else {
                final CacheDetailActivity activity = (CacheDetailActivity) activityRef.get();
                if (activity == null) {
                    return;
                }
                if (activity.search == null) {
                    showToast(R.string.err_dwld_details_failed);
                    dismissProgress();
                    finishActivity();
                    return;
                }

                if (activity.search.getError() != StatusCode.NO_ERROR) {
                    // Cache not found is not a download error
                    final StatusCode error = activity.search.getError();
                    final Resources res = activity.getResources();
                    final String toastPrefix = error != StatusCode.CACHE_NOT_FOUND ? res.getString(R.string.err_dwld_details_failed) + " " : "";
                    activity.showToast(toastPrefix + error.getErrorString(res));
                    dismissProgress();
                    finishActivity();
                    return;
                }

                updateStatusMsg(activity.getString(R.string.cache_dialog_loading_details_status_render));

                // Data loaded, we're ready to show it!
                activity.notifyDataSetChanged();
            }
        }

        private void updateStatusMsg(final String msg) {
            final CacheDetailActivity activity = (CacheDetailActivity) activityRef.get();
            if (activity == null) {
                return;
            }
            setProgressMessage(activity.getString(R.string.cache_dialog_loading_details)
                    + "\n\n"
                    + msg);
        }

        @Override
        public void handleDispose() {
            finishActivity();
        }

    }

    private void notifyDataSetChanged() {
        // This might get called asynchronous when the activity is shut down
        if (isFinishing()) {
            return;
        }

        if (search == null) {
            return;
        }

        cache = search.getFirstCacheFromResult(LoadFlags.LOAD_ALL_DB_ONLY);

        if (cache == null) {
            progress.dismiss();
            showToast(res.getString(R.string.err_detail_cache_find_some));
            finish();
            return;
        }

        // allow cache to notify CacheDetailActivity when it changes so it can be reloaded
        cache.setChangeNotificationHandler(new ChangeNotificationHandler(this, progress));

        setCacheTitleBar(cache);

        // reset imagesList so Images view page will be redrawn
        imagesList = null;
        reinitializeViewPager();

        // rendering done! remove progress popup if any there
        invalidateOptionsMenuCompatible();
        progress.dismiss();

        Settings.addCacheToHistory(cache.getGeocode());
    }

    /**
     * Receives update notifications from asynchronous processes
     */
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            notifyDataSetChanged();
        }
    };

    /**
     * Tries to navigate to the {@link Geocache} of this activity using the default navigation tool.
     */
    @Override
    public void startDefaultNavigation() {
        NavigationAppFactory.startDefaultNavigationApplication(1, this, cache);
    }

    /**
     * Tries to navigate to the {@link Geocache} of this activity using the second default navigation tool.
     */
    @Override
    public void startDefaultNavigation2() {
        NavigationAppFactory.startDefaultNavigationApplication(2, this, cache);
    }

    /**
     * Wrapper for the referenced method in the xml-layout.
     */
    public void goDefaultNavigation(@SuppressWarnings("unused") final View view) {
        startDefaultNavigation();
    }

    /**
     * referenced from XML view
     */
    public void showNavigationMenu(@SuppressWarnings("unused") final View view) {
        NavigationAppFactory.showNavigationMenu(this, cache, null, null, true, true);
    }

    private void loadCacheImages() {
        if (imagesList != null) {
            return;
        }
        final PageViewCreator creator = getViewCreator(Page.IMAGES);
        if (creator == null) {
            return;
        }
        final View imageView = creator.getView(null);
        if (imageView == null) {
            return;
        }
        imagesList = new ImagesList(this, cache.getGeocode(), cache);
        createDisposables.add(imagesList.loadImages(imageView, cache.getNonStaticImages()));
    }

    public static void startActivity(final Context context, final String geocode) {
        final Intent detailIntent = new Intent(context, CacheDetailActivity.class);
        detailIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        context.startActivity(detailIntent);
    }

    /**
     * Enum of all possible pages with methods to get the view and a title.
     */
    public enum Page {
        DETAILS(R.string.detail),
        DESCRIPTION(R.string.cache_description),
        LOGS(R.string.cache_logs),
        LOGSFRIENDS(R.string.cache_logs_friends_and_own),
        WAYPOINTS(R.string.cache_waypoints),
        INVENTORY(R.string.cache_inventory),
        IMAGES(R.string.cache_images);

        private final int titleStringId;

        Page(final int titleStringId) {
            this.titleStringId = titleStringId;
        }
    }

    private void refreshCache() {
        if (progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        if (!Network.isConnected()) {
            showToast(getString(R.string.err_server));
            return;
        }

        final RefreshCacheHandler refreshCacheHandler = new RefreshCacheHandler(this, progress);

        progress.show(this, res.getString(R.string.cache_dialog_refresh_title), res.getString(R.string.cache_dialog_refresh_message), true, refreshCacheHandler.disposeMessage());

        cache.refresh(refreshCacheHandler, AndroidRxUtils.refreshScheduler);
    }

    private void dropCache() {
        if (progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        progress.show(this, res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true, null);
        cache.drop(new ChangeNotificationHandler(this, progress));
    }

    private void storeCache(final boolean fastStoreOnLastSelection) {
        if (progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        if (Settings.getChooseList() || cache.isOffline()) {
            // let user select list to store cache in
            new StoredList.UserInterface(this).promptForMultiListSelection(R.string.lists_title,
                    new Action1<Set<Integer>>() {
                        @Override
                        public void call(final Set<Integer> selectedListIds) {
                            storeCacheInLists(selectedListIds);
                        }
                    }, true, cache.getLists(), fastStoreOnLastSelection);
        } else {
            storeCacheInLists(Collections.singleton(StoredList.STANDARD_LIST_ID));
        }
    }

    private void moveCache() {
        if (progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        new MoveToListAndRemoveFromOthersCommand(CacheDetailActivity.this, cache) {

            @Override
            protected void onFinished() {
                updateCacheLists(ButterKnife.findById(CacheDetailActivity.this, R.id.offline_lists), cache, res);
            }
        }.execute();
    }

    private void storeCacheInLists(final Set<Integer> selectedListIds) {
        if (cache.isOffline()) {
            // cache already offline, just add to another list
            DataStore.saveLists(Collections.singletonList(cache), selectedListIds);
            new StoreCacheHandler(CacheDetailActivity.this, progress).sendEmptyMessage(DisposableHandler.DONE);
        } else {
            storeCache(selectedListIds);
        }
    }

    /**
     * Creator for details-view.
     */
    public class DetailsViewCreator extends AbstractCachingPageViewCreator<ScrollView> {
        // Reference to the details list and favorite line, so that the helper-method can access them without an additional argument
        private LinearLayout detailsList;
        private ImmutablePair<RelativeLayout, TextView> favoriteLine;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            if (cache == null) {
                // something is really wrong
                return null;
            }

            view = (ScrollView) getLayoutInflater().inflate(R.layout.cachedetail_details_page, parentView, false);

            // Start loading preview map
            AndroidRxUtils.bindActivity(CacheDetailActivity.this, previewMap).subscribeOn(AndroidRxUtils.networkScheduler)
                    .subscribe(new Consumer<BitmapDrawable>() {
                        @Override
                        public void accept(final BitmapDrawable image) {
                            final Bitmap bitmap = image.getBitmap();
                            if (bitmap != null && bitmap.getWidth() > 10) {
                                final ImageView imageView = ButterKnife.findById(view, R.id.map_preview);
                                imageView.setImageDrawable(image);
                                view.findViewById(R.id.map_preview_box).setVisibility(View.VISIBLE);
                            }
                        }
                    });

            detailsList = ButterKnife.findById(view, R.id.details_list);
            final CacheDetailsCreator details = new CacheDetailsCreator(CacheDetailActivity.this, detailsList);

            // cache name (full name)
            final SpannableString span = TextUtils.coloredCacheText(cache, cache.getName());

            addContextMenu(details.add(R.string.cache_name, span).right);
            details.add(R.string.cache_type, cache.getType().getL10n());
            details.addSize(cache);
            addContextMenu(details.add(R.string.cache_geocode, cache.getGeocode()).right);
            details.addCacheState(cache);

            cacheDistanceView = details.addDistance(cache, cacheDistanceView);

            details.addDifficulty(cache);
            details.addTerrain(cache);
            details.addRating(cache);

            // favorite count
            favoriteLine = details.add(R.string.cache_favorite, "");

            // own rating
            if (cache.getMyVote() > 0) {
                details.addStars(R.string.cache_own_rating, cache.getMyVote());
            }

            // cache author
            if (StringUtils.isNotBlank(cache.getOwnerDisplayName()) || StringUtils.isNotBlank(cache.getOwnerUserId())) {
                final TextView ownerView = details.add(R.string.cache_owner, "").right;
                if (StringUtils.isNotBlank(cache.getOwnerDisplayName())) {
                    ownerView.setText(cache.getOwnerDisplayName(), TextView.BufferType.SPANNABLE);
                } else { // OwnerReal guaranteed to be not blank based on above
                    ownerView.setText(cache.getOwnerUserId(), TextView.BufferType.SPANNABLE);
                }
                ownerView.setOnClickListener(new OwnerActionsClickListener(cache));
            }

            // hidden or event date
            final TextView hiddenView = details.addHiddenDate(cache);
            if (hiddenView != null) {
                addContextMenu(hiddenView);
            }

            // cache location
            if (StringUtils.isNotBlank(cache.getLocation())) {
                details.add(R.string.cache_location, cache.getLocation());
            }

            // cache coordinates
            if (cache.getCoords() != null) {
                final TextView valueView = details.add(R.string.cache_coordinates, cache.getCoords().toString()).right;
                valueView.setOnClickListener(new CoordinatesFormatSwitcher(cache.getCoords()));
                addContextMenu(valueView);
            }

            // cache attributes
            updateAttributesIcons();
            updateAttributesText();
            ButterKnife.findById(view, R.id.attributes_box).setVisibility(cache.getAttributes().isEmpty() ? View.GONE : View.VISIBLE);

            updateOfflineBox(view, cache, res, new RefreshCacheClickListener(), new DropCacheClickListener(),
                    new StoreCacheClickListener(), new MoveCacheClickListener(), new StoreCacheClickListener());

            // list
            updateCacheLists(view, cache, res);

            // watchlist
            final ImageButton buttonWatchlistAdd = ButterKnife.findById(view, R.id.add_to_watchlist);
            final ImageButton buttonWatchlistRemove = ButterKnife.findById(view, R.id.remove_from_watchlist);
            buttonWatchlistAdd.setOnClickListener(new AddToWatchlistClickListener());
            buttonWatchlistRemove.setOnClickListener(new RemoveFromWatchlistClickListener());
            updateWatchlistBox();

            // favorite points
            final ImageButton buttonFavPointAdd = ButterKnife.findById(view, R.id.add_to_favpoint);
            final ImageButton buttonFavPointRemove = ButterKnife.findById(view, R.id.remove_from_favpoint);
            buttonFavPointAdd.setOnClickListener(new FavoriteAddClickListener());
            buttonFavPointRemove.setOnClickListener(new FavoriteRemoveClickListener());
            updateFavPointBox();

            // data license
            final IConnector connector = ConnectorFactory.getConnector(cache);
            final String license = connector.getLicenseText(cache);
            if (StringUtils.isNotBlank(license)) {
                view.findViewById(R.id.license_box).setVisibility(View.VISIBLE);
                final TextView licenseView = ButterKnife.findById(view, R.id.license);
                licenseView.setText(Html.fromHtml(license), BufferType.SPANNABLE);
                licenseView.setClickable(true);
                licenseView.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            } else {
                view.findViewById(R.id.license_box).setVisibility(View.GONE);
            }

            return view;
        }

        private void updateAttributesIcons() {
            final GridView gridView = ButterKnife.findById(view, R.id.attributes_grid);
            final List<String> attributes = cache.getAttributes();
            if (!CacheAttribute.hasRecognizedAttributeIcon(attributes)) {
                gridView.setVisibility(View.GONE);
                return;
            }
            gridView.setAdapter(new AttributesGridAdapter(CacheDetailActivity.this, cache));
            gridView.setVisibility(View.VISIBLE);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                    toggleAttributesView();
                }
            });
        }

        protected void toggleAttributesView() {
            final View textView = ButterKnife.findById(view, R.id.attributes_text);
            textView.setVisibility(textView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            final View gridView = ButterKnife.findById(view, R.id.attributes_grid);
            gridView.setVisibility(gridView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }

        private void updateAttributesText() {
            final TextView attribView = ButterKnife.findById(view, R.id.attributes_text);
            final List<String> attributes = cache.getAttributes();
            if (attributes.isEmpty()) {
                attribView.setVisibility(View.GONE);
                return;
            }
            final StringBuilder text = new StringBuilder();
            for (final String attributeName : attributes) {
                final boolean enabled = CacheAttribute.isEnabled(attributeName);
                // search for a translation of the attribute
                final CacheAttribute attrib = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attributeName));
                String attributeNameL10n = attributeName;
                if (attrib != null) {
                    attributeNameL10n = attrib.getL10n(enabled);
                }
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(attributeNameL10n);
            }
            attribView.setText(text);
            if (ButterKnife.findById(view, R.id.attributes_grid).getVisibility() == View.VISIBLE) {
                attribView.setVisibility(View.GONE);
                attribView.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        toggleAttributesView();
                    }
                });
            } else {
                attribView.setVisibility(View.VISIBLE);
            }
        }

        private class StoreCacheClickListener implements View.OnClickListener, View.OnLongClickListener {
            @Override
            public void onClick(final View arg0) {
                storeCache(false);
            }

            @Override
            public boolean onLongClick(final View v) {
                storeCache(true);
                return true;
            }
        }

        private class MoveCacheClickListener implements OnLongClickListener {
            @Override
            public boolean onLongClick(final View v) {
                moveCache();
                return true;
            }
        }

        private class DropCacheClickListener implements View.OnClickListener {
            @Override
            public void onClick(final View arg0) {
                dropCache();
            }
        }

        private class RefreshCacheClickListener implements View.OnClickListener {
            @Override
            public void onClick(final View arg0) {
                refreshCache();
            }
        }

        /**
         * Abstract Listener for add / remove buttons for watchlist
         */
        private abstract class AbstractPropertyListener implements View.OnClickListener {

            private final SimpleDisposableHandler handler = new SimpleDisposableHandler(CacheDetailActivity.this, progress) {
                @Override
                public void handleRegularMessage(final Message message) {
                    super.handleRegularMessage(message);
                    updateWatchlistBox();
                    updateFavPointBox();
                }
            };

            public void doExecute(final int titleId, final int messageId, final Action1<SimpleDisposableHandler> action) {
                if (progress.isShowing()) {
                    showToast(res.getString(R.string.err_watchlist_still_managing));
                    return;
                }
                progress.show(CacheDetailActivity.this, res.getString(titleId), res.getString(messageId), true, null);
                AndroidRxUtils.networkScheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        action.call(handler);
                    }
                });
            }
        }

        /**
         * Listener for "add to watchlist" button
         */
        private class AddToWatchlistClickListener extends AbstractPropertyListener {
            @Override
            public void onClick(final View arg0) {
                doExecute(R.string.cache_dialog_watchlist_add_title,
                        R.string.cache_dialog_watchlist_add_message,
                        new Action1<SimpleDisposableHandler>() {
                            @Override
                            public void call(final SimpleDisposableHandler simpleCancellableHandler) {
                                watchListAdd(simpleCancellableHandler);
                            }
                        });
            }
        }

        /**
         * Listener for "remove from watchlist" button
         */
        private class RemoveFromWatchlistClickListener extends AbstractPropertyListener {
            @Override
            public void onClick(final View arg0) {
                doExecute(R.string.cache_dialog_watchlist_remove_title,
                        R.string.cache_dialog_watchlist_remove_message,
                        new Action1<SimpleDisposableHandler>() {
                            @Override
                            public void call(final SimpleDisposableHandler simpleCancellableHandler) {
                                watchListRemove(simpleCancellableHandler);
                            }
                        });
            }
        }

        /** Add this cache to the watchlist of the user */
        private void watchListAdd(final SimpleDisposableHandler handler) {
            final WatchListCapability connector = (WatchListCapability) ConnectorFactory.getConnector(cache);
            if (connector.addToWatchlist(cache)) {
                handler.obtainMessage(MESSAGE_SUCCEEDED).sendToTarget();
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_watchlist_failed);
            }
        }

        /** Remove this cache from the watchlist of the user */
        private void watchListRemove(final SimpleDisposableHandler handler) {
            final WatchListCapability connector = (WatchListCapability) ConnectorFactory.getConnector(cache);
            if (connector.removeFromWatchlist(cache)) {
                handler.obtainMessage(MESSAGE_SUCCEEDED).sendToTarget();
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_watchlist_failed);
            }
        }

        /** Add this cache to the favorite list of the user */
        private void favoriteAdd(final SimpleDisposableHandler handler) {
            if (GCConnector.addToFavorites(cache)) {
                handler.obtainMessage(MESSAGE_SUCCEEDED).sendToTarget();
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_favorite_failed);
            }
        }

        /** Remove this cache to the favorite list of the user */
        private void favoriteRemove(final SimpleDisposableHandler handler) {
            if (GCConnector.removeFromFavorites(cache)) {
                handler.obtainMessage(MESSAGE_SUCCEEDED).sendToTarget();
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_favorite_failed);
            }
        }

        /**
         * Listener for "add to favorites" button
         */
        private class FavoriteAddClickListener extends AbstractPropertyListener {
            @Override
            public void onClick(final View arg0) {
                doExecute(R.string.cache_dialog_favorite_add_title,
                        R.string.cache_dialog_favorite_add_message,
                        new Action1<SimpleDisposableHandler>() {
                            @Override
                            public void call(final SimpleDisposableHandler simpleCancellableHandler) {
                                favoriteAdd(simpleCancellableHandler);
                            }
                        });
            }
        }

        /**
         * Listener for "remove from favorites" button
         */
        private class FavoriteRemoveClickListener extends AbstractPropertyListener {
            @Override
            public void onClick(final View arg0) {
                doExecute(R.string.cache_dialog_favorite_remove_title,
                        R.string.cache_dialog_favorite_remove_message,
                        new Action1<SimpleDisposableHandler>() {
                            @Override
                            public void call(final SimpleDisposableHandler simpleCancellableHandler) {
                                favoriteRemove(simpleCancellableHandler);
                            }
                        });
            }
        }

        /**
         * Show/hide buttons, set text in watchlist box
         */
        private void updateWatchlistBox() {
            final LinearLayout layout = ButterKnife.findById(view, R.id.watchlist_box);
            final boolean supportsWatchList = cache.supportsWatchList();
            layout.setVisibility(supportsWatchList ? View.VISIBLE : View.GONE);
            if (!supportsWatchList) {
                return;
            }
            final ImageButton buttonAdd = ButterKnife.findById(view, R.id.add_to_watchlist);
            final ImageButton buttonRemove = ButterKnife.findById(view, R.id.remove_from_watchlist);
            final TextView text = ButterKnife.findById(view, R.id.watchlist_text);

            final int watchListCount = cache.getWatchlistCount();

            if (cache.isOnWatchlist() || cache.isOwner()) {
                buttonAdd.setVisibility(View.GONE);
                buttonRemove.setVisibility(View.VISIBLE);
                if (watchListCount != -1) {
                    text.setText(res.getString(R.string.cache_watchlist_on_extra, watchListCount));
                } else {
                    text.setText(R.string.cache_watchlist_on);
                }
            } else {
                buttonAdd.setVisibility(View.VISIBLE);
                buttonRemove.setVisibility(View.GONE);
                if (watchListCount != -1) {
                    text.setText(res.getString(R.string.cache_watchlist_not_on_extra, watchListCount));
                } else {
                    text.setText(R.string.cache_watchlist_not_on);
                }
            }

            // the owner of a cache has it always on his watchlist. Adding causes an error
            if (cache.isOwner()) {
                buttonAdd.setEnabled(false);
                buttonAdd.setVisibility(View.GONE);
                buttonRemove.setEnabled(false);
                buttonRemove.setVisibility(View.GONE);
            }
        }

        /**
         * Show/hide buttons, set text in favorite line and box
         */
        private void updateFavPointBox() {
            // Favorite counts
            if (cache.getFavoritePoints() > 0) {
                favoriteLine.left.setVisibility(View.VISIBLE);
                favoriteLine.right.setText(cache.getFavoritePoints() + "×");
            } else {
                favoriteLine.left.setVisibility(View.GONE);
            }

            final LinearLayout layout = ButterKnife.findById(view, R.id.favpoint_box);
            final boolean supportsFavoritePoints = cache.supportsFavoritePoints();
            layout.setVisibility(supportsFavoritePoints ? View.VISIBLE : View.GONE);
            if (!supportsFavoritePoints || cache.isOwner() || !Settings.isGCPremiumMember()) {
                return;
            }
            final ImageButton buttonAdd = ButterKnife.findById(view, R.id.add_to_favpoint);
            final ImageButton buttonRemove = ButterKnife.findById(view, R.id.remove_from_favpoint);
            final TextView text = ButterKnife.findById(view, R.id.favpoint_text);

            if (cache.isFavorite()) {
                buttonAdd.setVisibility(View.GONE);
                buttonRemove.setVisibility(View.VISIBLE);
                text.setText(R.string.cache_favpoint_on);
            } else {
                buttonAdd.setVisibility(View.VISIBLE);
                buttonRemove.setVisibility(View.GONE);
                text.setText(R.string.cache_favpoint_not_on);
            }

            // Add/remove to Favorites is only possible if the cache has been found
            if (!cache.isFound()) {
                buttonAdd.setEnabled(false);
                buttonAdd.setVisibility(View.GONE);
                buttonRemove.setEnabled(false);
                buttonRemove.setVisibility(View.GONE);
            }
        }

    }

    private final Maybe<BitmapDrawable> previewMap = Maybe.create(new MaybeOnSubscribe<BitmapDrawable>() {
        @Override
        public void subscribe(final MaybeEmitter<BitmapDrawable> emitter) throws Exception {
            try {
                // persistent preview from storage
                Bitmap image = StaticMapsProvider.getPreviewMap(cache);

                if (image == null && Settings.isStoreOfflineMaps() && cache.getCoords() != null) {
                    StaticMapsProvider.storeCachePreviewMap(cache).blockingAwait();
                    image = StaticMapsProvider.getPreviewMap(cache);
                }

                if (image != null) {
                    emitter.onSuccess(ImageUtils.scaleBitmapToFitDisplay(image));
                } else {
                    emitter.onComplete();
                }
            } catch (final Exception e) {
                Log.w("CacheDetailActivity.previewMap", e);
                emitter.onError(e);
            }
        }

    });

    /**
     * Reflect the (contextual) action mode of the action bar.
     */
    protected ActionMode currentActionMode;

    protected class DescriptionViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @BindView(R.id.personalnote) protected TextView personalNoteView;
        @BindView(R.id.description) protected IndexOutOfBoundsAvoidingTextView descView;
        @BindView(R.id.loading) protected View loadingView;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            if (cache == null) {
                // something is really wrong
                return null;
            }

            view = (ScrollView) getLayoutInflater().inflate(R.layout.cachedetail_description_page, parentView, false);
            ButterKnife.bind(this, view);

            // cache short description
            if (StringUtils.isNotBlank(cache.getShortDescription())) {
                loadDescription(cache.getShortDescription(), descView, null);
            }

            // long description
            if (StringUtils.isNotBlank(cache.getDescription())) {
                loadLongDescription();
            }

            // cache personal note
            setPersonalNote(personalNoteView, cache.getPersonalNote());
            personalNoteView.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            addContextMenu(personalNoteView);
            final Button personalNoteEdit = ButterKnife.findById(view, R.id.edit_personalnote);
            personalNoteEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    ensureSaved();
                    editPersonalNote(cache, CacheDetailActivity.this);
                }
            });
            final Button personalNoteUpload = ButterKnife.findById(view, R.id.upload_personalnote);
            final PersonalNoteCapability connector = ConnectorFactory.getConnectorAs(cache, PersonalNoteCapability.class);
            if (connector != null && connector.canAddPersonalNote(cache)) {
                personalNoteUpload.setVisibility(View.VISIBLE);
                personalNoteUpload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if (StringUtils.length(cache.getPersonalNote()) > GCConstants.PERSONAL_NOTE_MAX_CHARS) {
                            warnPersonalNoteExceedsLimit();
                        } else {
                            uploadPersonalNote();
                        }
                    }
                });
            } else {
                personalNoteUpload.setVisibility(View.GONE);
            }

            // cache hint and spoiler images
            final View hintBoxView = view.findViewById(R.id.hint_box);
            if (StringUtils.isNotBlank(cache.getHint()) || CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                hintBoxView.setVisibility(View.VISIBLE);
            } else {
                hintBoxView.setVisibility(View.GONE);
            }

            final TextView hintView = ButterKnife.findById(view, R.id.hint);
            if (StringUtils.isNotBlank(cache.getHint())) {
                if (TextUtils.containsHtml(cache.getHint())) {
                    hintView.setText(Html.fromHtml(cache.getHint(), new HtmlImage(cache.getGeocode(), false, false, false), null), TextView.BufferType.SPANNABLE);
                    hintView.setText(CryptUtils.rot13((Spannable) hintView.getText()));
                } else {
                    hintView.setText(CryptUtils.rot13(cache.getHint()));
                }
                hintView.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                hintView.setVisibility(View.VISIBLE);
                hintView.setClickable(true);
                hintView.setOnClickListener(new DecryptTextClickListener(hintView));
                hintBoxView.setOnClickListener(new DecryptTextClickListener(hintView));
                hintBoxView.setClickable(true);
                addContextMenu(hintView);
            } else {
                hintView.setVisibility(View.GONE);
                hintView.setClickable(false);
                hintView.setOnClickListener(null);
                hintBoxView.setClickable(false);
                hintBoxView.setOnClickListener(null);
            }

            final TextView spoilerlinkView = ButterKnife.findById(view, R.id.hint_spoilerlink);
            if (CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                spoilerlinkView.setVisibility(View.VISIBLE);
                spoilerlinkView.setClickable(true);
                spoilerlinkView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View arg0) {
                        if (cache == null || CollectionUtils.isEmpty(cache.getSpoilers())) {
                            showToast(res.getString(R.string.err_detail_no_spoiler));
                            return;
                        }

                        ImagesActivity.startActivity(CacheDetailActivity.this, cache.getGeocode(), cache.getSpoilers());
                    }
                });
            } else {
                spoilerlinkView.setVisibility(View.GONE);
                spoilerlinkView.setClickable(true);
                spoilerlinkView.setOnClickListener(null);
            }

            return view;
        }

        private void uploadPersonalNote() {
            final SimpleDisposableHandler myHandler = new SimpleDisposableHandler(CacheDetailActivity.this, progress);

            final Message cancelMessage = myHandler.cancelMessage(res.getString(R.string.cache_personal_note_upload_cancelled));
            progress.show(CacheDetailActivity.this, res.getString(R.string.cache_personal_note_uploading), res.getString(R.string.cache_personal_note_uploading), true, cancelMessage);

            myHandler.add(AndroidRxUtils.networkScheduler.scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    final PersonalNoteCapability connector = (PersonalNoteCapability) ConnectorFactory.getConnector(cache);
                    final boolean success = connector.uploadPersonalNote(cache);
                    final Message msg = Message.obtain();
                    final Bundle bundle = new Bundle();
                    bundle.putString(SimpleDisposableHandler.MESSAGE_TEXT,
                            CgeoApplication.getInstance().getString(success ? R.string.cache_personal_note_upload_done : R.string.cache_personal_note_upload_error));
                    msg.setData(bundle);
                    myHandler.sendMessage(msg);
                }
            }));
        }

        private void loadLongDescription() {
            loadingView.setVisibility(View.VISIBLE);

            final String longDescription = cache.getDescription();
            loadDescription(longDescription, descView, loadingView);
        }

        private void warnPersonalNoteExceedsLimit() {
            Dialogs.confirm(CacheDetailActivity.this, R.string.cache_personal_note_limit, getString(R.string.cache_personal_note_truncation, GCConstants.PERSONAL_NOTE_MAX_CHARS),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            dialog.dismiss();
                            uploadPersonalNote();
                        }

                    });
        }

        /**
         * Load the description in the background.
         *
         * @param descriptionString
         *            the HTML description as retrieved from the connector
         * @param descriptionView
         *            the view to fill
         * @param loadingIndicatorView
         *            the loading indicator view, will be hidden when completed
         */
        private void loadDescription(final String descriptionString, final IndexOutOfBoundsAvoidingTextView descriptionView, final View loadingIndicatorView) {
            try {
                final UnknownTagsHandler unknownTagsHandler = new UnknownTagsHandler();
                final Spanned description = Html.fromHtml(descriptionString, new HtmlImage(cache.getGeocode(), true, false, descriptionView, false), unknownTagsHandler);
                addWarning(unknownTagsHandler, description);
                if (StringUtils.isNotBlank(descriptionString)) {
                    try {
                        if (descriptionView.getText().length() == 0) {
                            descriptionView.setText(description, TextView.BufferType.SPANNABLE);
                        } else {
                            descriptionView.append("\n");
                            descriptionView.append(description);
                        }
                    } catch (final Exception e) {
                        // On 4.1, there is sometimes a crash on measuring the layout: https://code.google.com/p/android/issues/detail?id=35412
                        Log.e("Android bug setting text: ", e);
                        // remove the formatting by converting to a simple string
                        descriptionView.append(description.toString());
                    }
                    descriptionView.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                    fixTextColor(descriptionString, descriptionView);
                    descriptionView.setVisibility(View.VISIBLE);
                    addContextMenu(descriptionView);
                    potentiallyHideShortDescription();
                }
                if (loadingIndicatorView != null) {
                    loadingIndicatorView.setVisibility(View.GONE);
                }
            } catch (final RuntimeException ignored) {
                showToast(res.getString(R.string.err_load_descr_failed));
            }
        }

    }

    // If description has an HTML construct which may be problematic to render, add a note at the end of the long description.
    // Technically, it may not be a table, but a pre, which has the same problems as a table, so the message is ok even though
    // sometimes technically incorrect.
    private void addWarning(final UnknownTagsHandler unknownTagsHandler, final Spanned description) {
        if (unknownTagsHandler.isProblematicDetected()) {
            final int startPos = description.length();
            final IConnector connector = ConnectorFactory.getConnector(cache);
            if (StringUtils.isNotEmpty(cache.getUrl())) {
                final Spanned tableNote = Html.fromHtml(res.getString(R.string.cache_description_table_note, "<a href=\"" + cache.getUrl() + "\">" + connector.getName() + "</a>"));
                ((Editable) description).append("\n\n").append(tableNote);
                ((Editable) description).setSpan(new StyleSpan(Typeface.ITALIC), startPos, description.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void fixTextColor(final String descriptionString, final IndexOutOfBoundsAvoidingTextView descriptionView) {
        final int backcolor;
        if (Settings.isLightSkin()) {
            backcolor = color.white;

            for (final Pattern pattern : LIGHT_COLOR_PATTERNS) {
                final MatcherWrapper matcher = new MatcherWrapper(pattern, descriptionString);
                if (matcher.find()) {
                    descriptionView.setBackgroundResource(color.darker_gray);
                    return;
                }
            }
        } else {
            backcolor = color.black;

            for (final Pattern pattern : DARK_COLOR_PATTERNS) {
                final MatcherWrapper matcher = new MatcherWrapper(pattern, descriptionString);
                if (matcher.find()) {
                    descriptionView.setBackgroundResource(color.darker_gray);
                    return;
                }
            }
        }
        descriptionView.setBackgroundResource(backcolor);
    }

    /**
     * Hide the short description, if it is contained somewhere at the start of the long description.
     */
    public void potentiallyHideShortDescription() {
        final View shortView = ButterKnife.findById(this, R.id.description);
        if (shortView == null) {
            return;
        }
        if (shortView.getVisibility() == View.GONE) {
            return;
        }
        final String shortDescription = cache.getShortDescription();
        if (StringUtils.isNotBlank(shortDescription)) {
            final int index = StringUtils.indexOf(cache.getDescription(), shortDescription);
            // allow up to 200 characters of HTML formatting
            if (index >= 0 && index < 200) {
                shortView.setVisibility(View.GONE);
            }
        }
    }

    private void ensureSaved() {
        if (!cache.isOffline()) {
            showToast(getString(R.string.info_cache_saved));
            cache.getLists().add(StoredList.STANDARD_LIST_ID);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
                    return null;
                }
            }.execute();
            notifyDataSetChanged();
        }
    }

    private class WaypointsViewCreator extends AbstractCachingPageViewCreator<ListView> {
        private final int visitedInset = (int) (6.6f * CgeoApplication.getInstance().getResources().getDisplayMetrics().density + 0.5f);

        @Override
        public ListView getDispatchedView(final ViewGroup parentView) {
            if (cache == null) {
                // something is really wrong
                return null;
            }

            // sort waypoints: PP, Sx, FI, OWN
            final List<Waypoint> sortedWaypoints = new ArrayList<>(cache.getWaypoints());
            Collections.sort(sortedWaypoints, Waypoint.WAYPOINT_COMPARATOR);

            view = (ListView) getLayoutInflater().inflate(R.layout.cachedetail_waypoints_page, parentView, false);
            view.setClickable(true);
            final View addWaypointButton = getLayoutInflater().inflate(R.layout.cachedetail_waypoints_footer, view, false);
            view.addFooterView(addWaypointButton);
            addWaypointButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View v) {
                    ensureSaved();
                    EditWaypointActivity.startActivityAddWaypoint(CacheDetailActivity.this, cache);
                    refreshOnResume = true;
                }
            });

            view.setAdapter(new ArrayAdapter<Waypoint>(CacheDetailActivity.this, R.layout.waypoint_item, sortedWaypoints) {
                @Override
                public View getView(final int position, final View convertView, final ViewGroup parent) {
                    View rowView = convertView;
                    if (rowView == null) {
                        rowView = getLayoutInflater().inflate(R.layout.waypoint_item, parent, false);
                        rowView.setClickable(true);
                        rowView.setLongClickable(true);
                        registerForContextMenu(rowView);
                    }
                    WaypointViewHolder holder = (WaypointViewHolder) rowView.getTag();
                    if (holder == null) {
                        holder = new WaypointViewHolder(rowView);
                    }

                    final Waypoint waypoint = getItem(position);
                    fillViewHolder(rowView, holder, waypoint);
                    return rowView;
                }
            });

            return view;
        }

        protected void fillViewHolder(final View rowView, final WaypointViewHolder holder, final Waypoint wpt) {
            // coordinates
            final TextView coordinatesView = holder.coordinatesView;
            final Geopoint coordinates = wpt.getCoords();
            if (coordinates != null) {
                coordinatesView.setOnClickListener(new CoordinatesFormatSwitcher(coordinates));
                coordinatesView.setText(coordinates.toString());
                coordinatesView.setVisibility(View.VISIBLE);
            } else {
                coordinatesView.setVisibility(View.GONE);
            }

            // info
            final String waypointInfo = Formatter.formatWaypointInfo(wpt);
            final TextView infoView = holder.infoView;
            if (StringUtils.isNotBlank(waypointInfo)) {
                infoView.setText(waypointInfo);
                infoView.setVisibility(View.VISIBLE);
            } else {
                infoView.setVisibility(View.GONE);
            }

            // title
            final TextView nameView = holder.nameView;
            if (StringUtils.isNotBlank(wpt.getName())) {
                nameView.setText(StringEscapeUtils.unescapeHtml4(wpt.getName()));
            } else if (coordinates != null) {
                nameView.setText(coordinates.toString());
            } else {
                nameView.setText(res.getString(R.string.waypoint));
            }
            setWaypointIcon(nameView, wpt);

            // visited
            if (wpt.isVisited()) {
                final TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(R.attr.text_color_grey, typedValue, true);
                if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    // really should be just a color!
                    nameView.setTextColor(typedValue.data);
                }
            }

            // note
            final TextView noteView = holder.noteView;
            if (StringUtils.isNotBlank(wpt.getNote())) {
                noteView.setOnClickListener(new DecryptTextClickListener(noteView));
                noteView.setVisibility(View.VISIBLE);
                if (TextUtils.containsHtml(wpt.getNote())) {
                    noteView.setText(Html.fromHtml(wpt.getNote(), new SmileyImage(cache.getGeocode(), noteView), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
                } else {
                    noteView.setText(wpt.getNote());
                }
            } else {
                noteView.setVisibility(View.GONE);
            }

            // user note
            final TextView userNoteView = holder.userNoteView;
            if (StringUtils.isNotBlank(wpt.getUserNote()) && !StringUtils.equals(wpt.getNote(), wpt.getUserNote())) {
                userNoteView.setOnClickListener(new DecryptTextClickListener(userNoteView));
                userNoteView.setVisibility(View.VISIBLE);
                if (TextUtils.containsHtml(wpt.getUserNote())) {
                    userNoteView.setText(Html.fromHtml(wpt.getUserNote(), new SmileyImage(cache.getGeocode(), userNoteView), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
                } else {
                    userNoteView.setText(wpt.getUserNote());
                }
            } else {
                userNoteView.setVisibility(View.GONE);
            }

            final View wpNavView = holder.wpNavView;
            wpNavView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    NavigationAppFactory.startDefaultNavigationApplication(1, CacheDetailActivity.this, wpt);
                }
            });
            wpNavView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    NavigationAppFactory.startDefaultNavigationApplication(2, CacheDetailActivity.this, wpt);
                    return true;
                }
            });

            addContextMenu(rowView);
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    selectedWaypoint = wpt;
                    ensureSaved();
                    EditWaypointActivity.startActivityEditWaypoint(CacheDetailActivity.this, cache, wpt.getId());
                    refreshOnResume = true;
                }
            });

            rowView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    selectedWaypoint = wpt;
                    openContextMenu(v);
                    return true;
                }
            });
        }

        private void setWaypointIcon(final TextView nameView, final Waypoint wpt) {
            final WaypointType waypointType = wpt.getWaypointType();
            final Drawable icon;
            if (wpt.isVisited()) {
                final LayerDrawable ld = new LayerDrawable(new Drawable[] {
                        Compatibility.getDrawable(res, waypointType.markerId),
                        Compatibility.getDrawable(res, R.drawable.tick) });
                ld.setLayerInset(0, 0, 0, visitedInset, visitedInset);
                ld.setLayerInset(1, visitedInset, visitedInset, 0, 0);
                icon = ld;
            } else {
                icon = Compatibility.getDrawable(res, waypointType.markerId);
            }
            nameView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
    }

    private class InventoryViewCreator extends AbstractCachingPageViewCreator<RecyclerView> {

        @Override
        public RecyclerView getDispatchedView(final ViewGroup parentView) {
            if (cache == null) {
                // something is really wrong
                return null;
            }


            view = (RecyclerView) getLayoutInflater().inflate(R.layout.cachedetail_inventory_page, parentView, false);
            final RecyclerView recyclerView = ButterKnife.findById(view, R.id.list);

            // TODO: fix layout, then switch back to Android-resource and delete copied one
            // this copy is modified to respect the text color
            RecyclerViewProvider.provideRecyclerView(CacheDetailActivity.this, recyclerView, true, true);

            final TrackableListAdapter adapterTrackables = new TrackableListAdapter(cache.getInventory(), new TrackableListAdapter.TrackableClickListener() {

                @Override
                public void onTrackableClicked(final Trackable trackable) {
                    TrackableActivity.startActivity(CacheDetailActivity.this, trackable.getGuid(), trackable.getGeocode(), trackable.getName(), cache.getGeocode(), trackable.getBrand().getId());
                }
            });
            recyclerView.setAdapter(adapterTrackables);
            cache.mergeInventory(genericTrackables, processedBrands);

            return view;
        }
    }

    private class ImagesViewCreator extends AbstractCachingPageViewCreator<View> {

        @Override
        public View getDispatchedView(final ViewGroup parentView) {
            if (cache == null) {
                return null; // something is really wrong
            }

            view = getLayoutInflater().inflate(R.layout.cachedetail_images_page, parentView, false);
            if (imagesList == null && isCurrentPage(Page.IMAGES)) {
                loadCacheImages();
            }
            return view;
        }
    }

    public static void startActivity(final Context context, final String geocode, final String cacheName) {
        final Intent cachesIntent = new Intent(context, CacheDetailActivity.class);
        cachesIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        cachesIntent.putExtra(Intents.EXTRA_NAME, cacheName);
        context.startActivity(cachesIntent);
    }

    private ActionMode mActionMode = null;
    private boolean mSelectionModeActive = false;
    private IndexOutOfBoundsAvoidingTextView selectedTextView;

    private class TextMenuItemClickListener implements MenuItem.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(final MenuItem menuItem) {
            final int startSelection = selectedTextView.getSelectionStart();
            final int endSelection = selectedTextView.getSelectionEnd();
            clickedItemText = selectedTextView.getText().subSequence(startSelection, endSelection);
            return onClipboardItemSelected(mActionMode, menuItem, clickedItemText, cache);
        }
    }

    @Override
    public void onSupportActionModeStarted(final ActionMode mode) {
        if (mSelectionModeActive && selectedTextView != null) {
            mSelectionModeActive = false;
            mActionMode = mode;
            final Menu menu = mode.getMenu();
            mode.getMenuInflater().inflate(R.menu.details_context, menu);
            menu.findItem(R.id.menu_copy).setVisible(false);
            menu.findItem(R.id.menu_cache_share_field).setOnMenuItemClickListener(new TextMenuItemClickListener());
            menu.findItem(R.id.menu_translate_to_sys_lang).setOnMenuItemClickListener(new TextMenuItemClickListener());
            menu.findItem(R.id.menu_translate_to_english).setOnMenuItemClickListener(new TextMenuItemClickListener());
            final MenuItem extWpts = menu.findItem(R.id.menu_extract_waypoints);
            extWpts.setVisible(true);
            extWpts.setOnMenuItemClickListener(new TextMenuItemClickListener());
            buildDetailsContextMenu(mode, menu, res.getString(R.string.cache_description), false);
            selectedTextView.setWindowFocusWait(true);
        }
        super.onSupportActionModeStarted(mode);
    }

    @Override
    public void onSupportActionModeFinished(final ActionMode mode) {
        mActionMode = null;
        if (selectedTextView != null) {
            selectedTextView.setWindowFocusWait(false);
        }
        if (!mSelectionModeActive) {
            selectedTextView = null;
        }
        super.onSupportActionModeFinished(mode);
    }

    @Override
    public void addContextMenu(final View view) {
        view.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                if (view.getId() == R.id.description || view.getId() == R.id.hint) {
                    selectedTextView = (IndexOutOfBoundsAvoidingTextView) view;
                    mSelectionModeActive = true;
                    return false;
                }
                currentActionMode = startSupportActionMode(new ActionMode.Callback() {

                    @Override
                    public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
                        return prepareClipboardActionMode(view, actionMode, menu);
                    }

                    private boolean prepareClipboardActionMode(final View view, final ActionMode actionMode, final Menu menu) {
                        switch (view.getId()) {
                            case R.id.value: // coordinates, gc-code, name
                                clickedItemText = ((TextView) view).getText();
                                final CharSequence itemTitle = ((TextView) ((View) view.getParent()).findViewById(R.id.name)).getText();
                                if (itemTitle.equals(res.getText(R.string.cache_coordinates))) {
                                    clickedItemText = GeopointFormatter.reformatForClipboard(clickedItemText);
                                }
                                buildDetailsContextMenu(actionMode, menu, itemTitle, true);
                                return true;
                            case R.id.description:
                                // combine short and long description
                                final String shortDesc = cache.getShortDescription();
                                if (StringUtils.isBlank(shortDesc)) {
                                    clickedItemText = cache.getDescription();
                                } else {
                                    clickedItemText = shortDesc + "\n\n" + cache.getDescription();
                                }
                                buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_description), false);
                                return true;
                            case R.id.personalnote:
                                clickedItemText = cache.getPersonalNote();
                                buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_personal_note), true);
                                return true;
                            case R.id.hint:
                                clickedItemText = cache.getHint();
                                buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_hint), false);
                                return true;
                            case R.id.log:
                                clickedItemText = ((TextView) view).getText();
                                buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_logs), false);
                                return true;
                            case R.id.date: // event date
                                clickedItemText = Formatter.formatHiddenDate(cache);
                                buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_event), true);
                                menu.findItem(R.id.menu_calendar).setVisible(cache.canBeAddedToCalendar());
                                return true;
                        }
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(final ActionMode actionMode) {
                        currentActionMode = null;
                    }

                    @Override
                    public boolean onCreateActionMode(final ActionMode actionMode, final Menu menu) {
                        actionMode.getMenuInflater().inflate(R.menu.details_context, menu);
                        prepareClipboardActionMode(view, actionMode, menu);
                        // Return true so that the action mode is shown
                        return true;
                    }

                    @Override
                    public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            // detail fields
                            case R.id.menu_calendar:
                                CalendarAddon.addToCalendarWithIntent(CacheDetailActivity.this, cache);
                                actionMode.finish();
                                return true;
                            // handle clipboard actions in base
                            default:
                                return onClipboardItemSelected(actionMode, menuItem, clickedItemText, cache);
                        }
                    }
                });
                return true;
            }
        });
    }

    public static void startActivityGuid(final Context context, final String guid, final String cacheName) {
        final Intent cacheIntent = new Intent(context, CacheDetailActivity.class);
        cacheIntent.putExtra(Intents.EXTRA_GUID, guid);
        cacheIntent.putExtra(Intents.EXTRA_NAME, cacheName);
        context.startActivity(cacheIntent);
    }

    /**
     * A dialog to allow the user to select reseting coordinates local/remote/both.
     */
    private AlertDialog createResetCacheCoordinatesDialog(final Waypoint wpt) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.waypoint_reset_cache_coords);

        final String[] items = { res.getString(R.string.waypoint_localy_reset_cache_coords), res.getString(R.string.waypoint_reset_local_and_remote_cache_coords) };
        builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
                final ProgressDialog progressDialog = ProgressDialog.show(CacheDetailActivity.this, getString(R.string.cache), getString(R.string.waypoint_reset), true);
                final HandlerResetCoordinates handler = new HandlerResetCoordinates(CacheDetailActivity.this, progressDialog, which == 1);
                resetCoords(cache, handler, wpt, which == 0 || which == 1, which == 1, progressDialog);
            }
        });
        return builder.create();
    }

    private static class HandlerResetCoordinates extends WeakReferenceHandler<CacheDetailActivity> {
        public static final int LOCAL = 0;
        public static final int ON_WEBSITE = 1;

        private boolean remoteFinished = false;
        private boolean localFinished = false;
        private final ProgressDialog progressDialog;
        private final boolean resetRemote;

        protected HandlerResetCoordinates(final CacheDetailActivity activity, final ProgressDialog progressDialog, final boolean resetRemote) {
            super(activity);
            this.progressDialog = progressDialog;
            this.resetRemote = resetRemote;
        }

        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == LOCAL) {
                localFinished = true;
            } else {
                remoteFinished = true;
            }

            if (localFinished && (remoteFinished || !resetRemote)) {
                progressDialog.dismiss();
                final CacheDetailActivity activity = getActivity();
                if (activity != null) {
                    activity.notifyDataSetChanged();
                }
            }
        }

    }

    private void resetCoords(final Geocache cache, final Handler handler, final Waypoint wpt, final boolean local, final boolean remote, final ProgressDialog progress) {
        AndroidRxUtils.networkScheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                if (local) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage(res.getString(R.string.waypoint_reset_cache_coords));
                        }
                    });
                    cache.setCoords(wpt.getCoords());
                    cache.setUserModifiedCoords(false);
                    cache.deleteWaypointForce(wpt);
                    DataStore.saveChangedCache(cache);
                    handler.sendEmptyMessage(HandlerResetCoordinates.LOCAL);
                }

                final IConnector con = ConnectorFactory.getConnector(cache);
                if (remote && con.supportsOwnCoordinates()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage(res.getString(R.string.waypoint_coordinates_being_reset_on_website));
                        }
                    });

                    final boolean result = con.deleteModifiedCoordinates(cache);

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (result) {
                                showToast(getString(R.string.waypoint_coordinates_has_been_reset_on_website));
                            } else {
                                showToast(getString(R.string.waypoint_coordinates_upload_error));
                            }
                            handler.sendEmptyMessage(HandlerResetCoordinates.ON_WEBSITE);
                            notifyDataSetChanged();
                        }

                    });

                }
            }
        });
    }

    @Override
    protected String getTitle(final Page page) {
        // show number of waypoints directly in waypoint title
        if (page == Page.WAYPOINTS) {
            final int waypointCount = cache.getWaypoints().size();
            return res.getQuantityString(R.plurals.waypoints, waypointCount, waypointCount);
        }
        return res.getString(page.titleStringId);
    }

    @Override
    protected Pair<List<? extends Page>, Integer> getOrderedPages() {
        final ArrayList<Page> pages = new ArrayList<>();
        pages.add(Page.WAYPOINTS);
        pages.add(Page.DETAILS);
        final int detailsIndex = pages.size() - 1;
        pages.add(Page.DESCRIPTION);
        // enforce showing the empty log book if new entries can be added
        if (cache.supportsLogging() || !cache.getLogs().isEmpty()) {
            pages.add(Page.LOGS);
        }
        if (CollectionUtils.isNotEmpty(cache.getFriendsLogs())) {
            pages.add(Page.LOGSFRIENDS);
        }
        if (CollectionUtils.isNotEmpty(cache.getInventory()) || CollectionUtils.isNotEmpty(genericTrackables)) {
            pages.add(Page.INVENTORY);
        }
        if (CollectionUtils.isNotEmpty(cache.getNonStaticImages())) {
            pages.add(Page.IMAGES);
        }
        return new ImmutablePair<List<? extends Page>, Integer>(pages, detailsIndex);
    }

    @Override
    protected AbstractViewPagerActivity.PageViewCreator createViewCreator(final Page page) {
        switch (page) {
            case DETAILS:
                return new DetailsViewCreator();

            case DESCRIPTION:
                return new DescriptionViewCreator();

            case LOGS:
                return new CacheLogsViewCreator(this, true);

            case LOGSFRIENDS:
                return new CacheLogsViewCreator(this, false);

            case WAYPOINTS:
                return new WaypointsViewCreator();

            case INVENTORY:
                return new InventoryViewCreator();

            case IMAGES:
                return new ImagesViewCreator();

        }
        throw new IllegalStateException(); // cannot happen as long as switch case is enum complete
    }

    static void updateOfflineBox(final View view, final Geocache cache, final Resources res,
            final OnClickListener refreshCacheClickListener,
            final OnClickListener dropCacheClickListener,
            final OnClickListener storeCacheClickListener,
            final OnLongClickListener moveCacheListener,
            final OnLongClickListener storeCachePreselectedListener) {
        // offline use
        final TextView offlineText = ButterKnife.findById(view, R.id.offline_text);
        final ImageButton offlineRefresh = ButterKnife.findById(view, R.id.offline_refresh);
        final ImageButton offlineStoreDrop = ButterKnife.findById(view, R.id.offline_store_drop);
        final ImageButton offlineEdit = ButterKnife.findById(view, R.id.offline_edit);

        offlineStoreDrop.setClickable(true);
        offlineStoreDrop.setOnClickListener(storeCacheClickListener);
        offlineStoreDrop.setOnLongClickListener(storeCachePreselectedListener);

        if (moveCacheListener != null) {
            offlineEdit.setOnLongClickListener(moveCacheListener);
        }

        offlineRefresh.setVisibility(cache.supportsRefresh() ? View.VISIBLE : View.GONE);
        offlineRefresh.setClickable(true);
        offlineRefresh.setOnClickListener(refreshCacheClickListener);

        if (cache.isOffline()) {
            final long diff = (System.currentTimeMillis() / (60 * 1000)) - (cache.getUpdated() / (60 * 1000)); // minutes

            final String ago;
            if (diff < 15) {
                ago = res.getString(R.string.cache_offline_time_mins_few);
            } else if (diff < 50) {
                ago = res.getQuantityString(R.plurals.cache_offline_about_time_mins, (int) diff, (int) diff);
            } else if (diff < (48 * 60)) {
                ago = res.getQuantityString(R.plurals.cache_offline_about_time_hours, (int) (diff / 60), (int) (diff / 60));
            } else {
                ago = res.getQuantityString(R.plurals.cache_offline_about_time_days, (int) (diff / (24 * 60)), (int) (diff / (24 * 60)));
            }

            offlineText.setText(res.getString(R.string.cache_offline_stored) + "\n" + ago);

            offlineStoreDrop.setOnClickListener(dropCacheClickListener);
            offlineStoreDrop.setOnLongClickListener(null);
            offlineStoreDrop.setClickable(true);
            offlineStoreDrop.setImageResource(R.drawable.ic_menu_delete);

            offlineEdit.setVisibility(View.VISIBLE);
            offlineEdit.setOnClickListener(storeCacheClickListener);
        } else {
            offlineText.setText(res.getString(R.string.cache_offline_not_ready));
            offlineStoreDrop.setImageResource(R.drawable.ic_menu_save);

            offlineEdit.setVisibility(View.GONE);
        }

    }

    static void updateCacheLists(final View view, final Geocache cache, final Resources res) {
        final Set<String> listNames = new HashSet<>();
        for (final Integer listId : cache.getLists()) {
            final StoredList list = DataStore.getList(listId);
            listNames.add(list.getTitle());
        }
        final TextView offlineLists = ButterKnife.findById(view, R.id.offline_lists);
        offlineLists.setText(res.getString(R.string.list_list_headline) + " " + StringUtils.join(listNames.toArray(), ", "));
    }

    public Geocache getCache() {
        return cache;
    }

    private static class StoreCacheHandler extends SimpleDisposableHandler {

        StoreCacheHandler(final CacheDetailActivity activity, final Progress progress) {
            super(activity, progress);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            if (msg.what == UPDATE_LOAD_PROGRESS_DETAIL && msg.obj instanceof String) {
                updateStatusMsg(R.string.cache_dialog_offline_save_message, (String) msg.obj);
            } else {
                notifyDataSetChanged(activityRef);
            }
        }
    }

    private static final class RefreshCacheHandler extends SimpleDisposableHandler {

        RefreshCacheHandler(final CacheDetailActivity activity, final Progress progress) {
            super(activity, progress);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            if (msg.what == UPDATE_LOAD_PROGRESS_DETAIL && msg.obj instanceof String) {
                updateStatusMsg(R.string.cache_dialog_refresh_message, (String) msg.obj);
            } else {
                notifyDataSetChanged(activityRef);
            }
        }
    }

    private static final class ChangeNotificationHandler extends SimpleHandler {

        ChangeNotificationHandler(final CacheDetailActivity activity, final Progress progress) {
            super(activity, progress);
        }

        @Override
        public void handleMessage(final Message msg) {
            notifyDataSetChanged(activityRef);
        }
    }

    private static void notifyDataSetChanged(final WeakReference<AbstractActivity> activityRef) {
        final CacheDetailActivity activity = (CacheDetailActivity) activityRef.get();
        if (activity != null) {
            activity.notifyDataSetChanged();
        }
    }

    protected void storeCache(final Set<Integer> listIds) {
        final StoreCacheHandler storeCacheHandler = new StoreCacheHandler(CacheDetailActivity.this, progress);
        progress.show(this, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true, storeCacheHandler.disposeMessage());
        AndroidRxUtils.networkScheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                cache.store(listIds, storeCacheHandler);
            }
        });
    }

    public static void editPersonalNote(final Geocache cache, final CacheDetailActivity activity) {
        final FragmentManager fm = activity.getSupportFragmentManager();
        final EditNoteDialog dialog = EditNoteDialog.newInstance(cache.getPersonalNote());
        dialog.show(fm, "fragment_edit_note");
    }

    @Override
    public void onFinishEditNoteDialog(final String note) {
        cache.setPersonalNote(note);
        if (cache.addWaypointsFromNote()) {
            getViewCreator(Page.WAYPOINTS).notifyDataSetChanged();
        }

        final TextView personalNoteView = ButterKnife.findById(this, R.id.personalnote);
        if (personalNoteView != null) {
            setPersonalNote(personalNoteView, note);
        } else {
            reinitializeViewPager();
        }

        Schedulers.io().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
            }
        });
    }

    private static void setPersonalNote(final TextView personalNoteView, final String personalNote) {
        personalNoteView.setText(personalNote, TextView.BufferType.SPANNABLE);
        if (StringUtils.isNotBlank(personalNote)) {
            personalNoteView.setVisibility(View.VISIBLE);
            Linkify.addLinks(personalNoteView, Linkify.ALL);
        } else {
            personalNoteView.setVisibility(View.GONE);
        }
    }

    @Override
    public void navigateTo() {
        startDefaultNavigation();
    }

    @Override
    public void showNavigationMenu() {
        NavigationAppFactory.showNavigationMenu(this, cache, null, null);
    }

    @Override
    public void cachesAround() {
        CacheListActivity.startActivityCoordinates(this, cache.getCoords(), cache.getName());
    }

    public void setNeedsRefresh() {
        refreshOnResume = true;
    }
}
