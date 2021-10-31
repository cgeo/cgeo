package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.INavigationSource;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.activity.TabbedViewPagerActivity;
import cgeo.geocaching.activity.TabbedViewPagerFragment;
import cgeo.geocaching.apps.cachelist.MapsMeCacheListApp;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.calendar.CalendarAdder;
import cgeo.geocaching.command.AbstractCommand;
import cgeo.geocaching.command.MoveToListAndRemoveFromOthersCommand;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.al.ALConnector;
import cgeo.geocaching.connector.capability.IFavoriteCapability;
import cgeo.geocaching.connector.capability.IIgnoreCapability;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.connector.capability.PersonalNoteCapability;
import cgeo.geocaching.connector.capability.PgcChallengeCheckerCapability;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.databinding.CachedetailDescriptionPageBinding;
import cgeo.geocaching.databinding.CachedetailDetailsPageBinding;
import cgeo.geocaching.databinding.CachedetailImagesPageBinding;
import cgeo.geocaching.databinding.CachedetailInventoryPageBinding;
import cgeo.geocaching.databinding.CachedetailWaypointsHeaderBinding;
import cgeo.geocaching.databinding.CachedetailWaypointsPageBinding;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.export.FieldNoteExport;
import cgeo.geocaching.export.GpxExport;
import cgeo.geocaching.export.PersonalNoteExport;
import cgeo.geocaching.gcvote.VoteDialog;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.CacheLogsViewCreator;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LoggingUI;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.WaypointParser;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.SmileyImage;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;
import cgeo.geocaching.ui.DecryptTextClickListener;
import cgeo.geocaching.ui.FastScrollListener;
import cgeo.geocaching.ui.ImagesList;
import cgeo.geocaching.ui.IndexOutOfBoundsAvoidingTextView;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.TrackableListAdapter;
import cgeo.geocaching.ui.UserClickListener;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.EditNoteDialog;
import cgeo.geocaching.ui.dialog.EditNoteDialog.EditNoteDialogListener;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.BranchDetectionHelper;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.CheckerUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.ColorUtils;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.SimpleDisposableHandler;
import cgeo.geocaching.utils.SimpleHandler;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;
import cgeo.geocaching.utils.functions.Action1;
import static cgeo.geocaching.apps.cache.WhereYouGoApp.getWhereIGoUrl;
import static cgeo.geocaching.apps.cache.WhereYouGoApp.isWhereYouGoInstalled;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Activity to handle all single-cache-stuff.
 *
 * e.g. details, description, logs, waypoints, inventory...
 */
public class CacheDetailActivity extends TabbedViewPagerActivity
        implements CacheMenuHandler.ActivityInterface, INavigationSource, AndroidBeam.ActivitySharingInterface, EditNoteDialogListener {

    private static final int MESSAGE_FAILED = -1;
    private static final int MESSAGE_SUCCEEDED = 1;

    private static final String EXTRA_FORCE_WAYPOINTSPAGE = "cgeo.geocaching.extra.cachedetail.forceWaypointsPage";

    /**
     * Minimal contrast ratio. If description:background contrast ratio is less than this value
     * for some string, foreground color will be removed and gray background will be used
     * in order to highlight the string
     *
     * @see <a href="https://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html">W3 Minimum Contrast</a>
     **/
    private static final float CONTRAST_THRESHOLD = 4.5f;

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

    private MenuItem menuItemToggleWaypointsFromNote = null;

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
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.tabbed_viewpager_activity_refreshable);

        // get parameters
        final Bundle extras = getIntent().getExtras();
        final Uri uri = AndroidBeam.getUri(getIntent());

        // try to get data from extras
        String name = null;
        String guid = null;
        boolean forceWaypointsPage = false;

        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            name = extras.getString(Intents.EXTRA_NAME);
            guid = extras.getString(Intents.EXTRA_GUID);
            forceWaypointsPage = extras.getBoolean(EXTRA_FORCE_WAYPOINTSPAGE);
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
            } else if (StringUtils.isNotBlank(geocode)) {
                title = geocode;
            }
            progress.show(this, title, res.getString(R.string.cache_dialog_loading_details), true, loadCacheHandler.disposeMessage());
        } catch (final RuntimeException ignored) {
            // nothing, we lost the window
        }

        locationUpdater = new CacheDetailsGeoDirHandler(this);

        final long pageToOpen = forceWaypointsPage ? Page.WAYPOINTS.id :
            savedInstanceState != null ?
                savedInstanceState.getLong(STATE_PAGE_INDEX, Page.DETAILS.id) :
                Settings.isOpenLastDetailsPage() ? Settings.getLastDetailsPage() : Page.DETAILS.id;

        createViewPager(pageToOpen, getOrderedPages(), currentPageId -> {
            if (Settings.isOpenLastDetailsPage()) {
                Settings.setLastDetailsPage((int) (long) currentPageId);
            }
            requireGeodata = currentPageId == Page.DETAILS.id;
            // resume location access
            PermissionHandler.executeIfLocationPermissionGranted(this, new RestartLocationPermissionGrantedCallback(PermissionRequestContext.CacheDetailActivity) {

                @Override
                public void executeAfter() {
                    startOrStopGeoDataListener(false);
                }
            });

            // dispose contextual actions on page change
            if (currentActionMode != null) {
                currentActionMode.finish();
            }
        }, true);
        requireGeodata = pageToOpen == Page.DETAILS.id;

        final String realGeocode = geocode;
        final String realGuid = guid;
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
            search = Geocache.searchByGeocode(realGeocode, StringUtils.isBlank(realGeocode) ? realGuid : null, false, loadCacheHandler);
            loadCacheHandler.sendMessage(Message.obtain());
        });

        // Load Generic Trackables
        if (StringUtils.isNotBlank(geocode)) {
            AndroidRxUtils.bindActivity(this,
                // Obtain the active connectors and load trackables in parallel.
                Observable.fromIterable(ConnectorFactory.getGenericTrackablesConnectors()).flatMap((Function<TrackableConnector, Observable<Trackable>>) trackableConnector -> {
                    processedBrands.add(trackableConnector.getBrand());
                    return Observable.defer(() -> Observable.fromIterable(trackableConnector.searchTrackables(geocode))).subscribeOn(AndroidRxUtils.networkScheduler);
                }).toList()).subscribe(trackables -> {
                // Todo: this is not really a good method, it may lead to duplicates ; ie: in OC connectors.
                // Store trackables.
                genericTrackables.addAll(trackables);
                if (!trackables.isEmpty()) {
                    // Update the UI if any trackables were found.
                    notifyDataSetChanged();
                }
            });
        }


        // If we have a newer Android device setup Android Beam for easy cache sharing
        AndroidBeam.enable(this, this);

        // get notified on cache changes (e.g.: waypoint creation from map)
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, new IntentFilter(Intents.INTENT_CACHE_CHANGED));
    }

    @Override
    @Nullable
    public String getAndroidBeamUri() {
        return cache != null ? cache.getUrl() : null;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_PAGE_INDEX, getCurrentPageId());
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
    public void onResume() {
        super.onResume();

        // resume location access
        PermissionHandler.executeIfLocationPermissionGranted(this, new RestartLocationPermissionGrantedCallback(PermissionRequestContext.CacheDetailActivity) {

            @Override
            public void executeAfter() {
                startOrStopGeoDataListener(true);
            }
        });

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
        SpeechService.stopService(this);
        if (cache != null) {
            cache.setChangeNotificationHandler(null);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();
        if (viewId == R.id.waypoint) {
            menu.setHeaderTitle(selectedWaypoint.getName() + " (" + res.getString(R.string.waypoint) + ")");
            getMenuInflater().inflate(R.menu.waypoint_options, menu);
            final boolean isOriginalWaypoint = selectedWaypoint.getWaypointType() == WaypointType.ORIGINAL;
            menu.findItem(R.id.menu_waypoint_reset_cache_coords).setVisible(isOriginalWaypoint);
            menu.findItem(R.id.menu_waypoint_edit).setVisible(!isOriginalWaypoint);
            menu.findItem(R.id.menu_waypoint_duplicate).setVisible(!isOriginalWaypoint);
            menu.findItem(R.id.menu_waypoint_delete).setVisible(!isOriginalWaypoint || selectedWaypoint.belongsToUserDefinedCache());
            final boolean hasCoords = selectedWaypoint.getCoords() != null;
            final MenuItem defaultNavigationMenu = menu.findItem(R.id.menu_waypoint_navigate_default);
            defaultNavigationMenu.setVisible(hasCoords);
            defaultNavigationMenu.setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());
            menu.findItem(R.id.menu_waypoint_navigate).setVisible(hasCoords);
            menu.findItem(R.id.menu_waypoint_caches_around).setVisible(hasCoords);
            menu.findItem(R.id.menu_waypoint_copy_coordinates).setVisible(hasCoords);
            final boolean canClearCoords = hasCoords && (selectedWaypoint.isUserDefined() || selectedWaypoint.isOriginalCoordsEmpty());
            menu.findItem(R.id.menu_waypoint_clear_coordinates).setVisible(canClearCoords);
            menu.findItem(R.id.menu_waypoint_toclipboard).setVisible(true);
        } else {
            if (imagesList != null) {
                imagesList.onCreateContextMenu(menu, view);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_waypoint_edit) {
            // waypoints
            if (selectedWaypoint != null) {
                ensureSaved();
                EditWaypointActivity.startActivityEditWaypoint(this, cache, selectedWaypoint.getId());
                refreshOnResume = true;
            }
        } else if (itemId == R.id.menu_waypoint_visited) {
            if (selectedWaypoint != null) {
                ensureSaved();
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(final Void... params) {
                        selectedWaypoint.setVisited(true);
                        DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                        return true;
                    }

                    @Override
                    protected void onPostExecute(final Boolean result) {
                        if (result) {
                            notifyDataSetChanged();
                        }
                    }
                }.execute();
            }
        } else if (itemId == R.id.menu_waypoint_copy_coordinates) {
            if (selectedWaypoint != null) {
                final Geopoint coordinates = selectedWaypoint.getCoords();
                if (coordinates != null) {
                    ClipboardUtils.copyToClipboard(
                        GeopointFormatter.reformatForClipboard(coordinates.toString()));
                    showToast(getString(R.string.clipboard_copy_ok));
                }
            }
        } else if (itemId == R.id.menu_waypoint_clear_coordinates) {
            if (selectedWaypoint != null) {
                ensureSaved();
                new ClearCoordinatesCommand(this, cache, selectedWaypoint).execute();
            }
        } else if (itemId == R.id.menu_waypoint_duplicate) {
            ensureSaved();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(final Void... params) {
                    if (cache.duplicateWaypoint(selectedWaypoint, true) != null) {
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
        } else if (itemId == R.id.menu_waypoint_toclipboard) {
            if (selectedWaypoint != null) {
                ensureSaved();
                ClipboardUtils.copyToClipboard(selectedWaypoint.reformatForClipboard());
                showToast(getString(R.string.clipboard_copy_ok));
            }
        } else if (itemId == R.id.menu_waypoint_delete) {
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
                        LocalBroadcastManager.getInstance(CacheDetailActivity.this).sendBroadcast(new Intent(Intents.INTENT_CACHE_CHANGED));
                    }
                }
            }.execute();
        } else if (itemId == R.id.menu_waypoint_navigate_default) {
            if (selectedWaypoint != null) {
                NavigationAppFactory.startDefaultNavigationApplication(1, this, selectedWaypoint);
            }
        } else if (itemId == R.id.menu_waypoint_navigate) {
            if (selectedWaypoint != null) {
                NavigationAppFactory.showNavigationMenu(this, null, selectedWaypoint, null);
            }
        } else if (itemId == R.id.menu_waypoint_caches_around) {
            if (selectedWaypoint != null) {
                final Geopoint coordinates = selectedWaypoint.getCoords();
                if (coordinates != null) {
                    CacheListActivity.startActivityCoordinates(this, coordinates, selectedWaypoint.getName());
                }
            }
        } else if (itemId == R.id.menu_waypoint_reset_cache_coords) {
            ensureSaved();
            if (ConnectorFactory.getConnector(cache).supportsOwnCoordinates()) {
                createResetCacheCoordinatesDialog(selectedWaypoint).show();
            } else {
                final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.cache), getString(R.string.waypoint_reset), true);
                final HandlerResetCoordinates handler = new HandlerResetCoordinates(this, progressDialog, false);
                resetCoords(cache, handler, selectedWaypoint, true, false, progressDialog);
            }
        } else if (itemId == R.id.menu_calendar) {
            CalendarAdder.addToCalendar(this, cache);
        } else if (imagesList == null || !imagesList.onContextItemSelected(item)) {
            return onOptionsItemSelected(item);
        }
        return true;
    }

    private abstract static class AbstractWaypointModificationCommand extends AbstractCommand {
        protected final Waypoint waypoint;
        protected final Geocache cache;

        protected AbstractWaypointModificationCommand(final CacheDetailActivity context, final Geocache cache, final Waypoint waypoint) {
            super(context);
            this.cache = cache;
            this.waypoint = waypoint;
        }

        @Override
        protected void onFinished() {
            ((CacheDetailActivity) getContext()).notifyDataSetChanged();
        }

        @Override
        protected void onFinishedUndo() {
            ((CacheDetailActivity) getContext()).notifyDataSetChanged();
        }
    }

    private static final class ClearCoordinatesCommand extends AbstractWaypointModificationCommand {

        private Geopoint coords;

        ClearCoordinatesCommand(final CacheDetailActivity context, final Geocache cache, final Waypoint waypoint) {
            super(context, cache, waypoint);
        }

        @Override
        protected void doCommand() {
            coords = waypoint.getCoords();
            waypoint.setCoords(null);
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
        }

        @Override
        protected void undoCommand() {
            waypoint.setCoords(coords);
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
        }

        @Override
        protected String getResultMessage() {
            return getContext().getString(R.string.info_waypoint_coordinates_cleared);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        CacheMenuHandler.addMenuItems(this, menu, cache);
        CacheMenuHandler.initDefaultNavigationMenuItem(menu, this);
        return true;
    }

    private void setMenuPreventWaypointsFromNote(final boolean preventWaypointsFromNote) {
        if (null != menuItemToggleWaypointsFromNote) {
            menuItemToggleWaypointsFromNote.setTitle(preventWaypointsFromNote ? R.string.cache_menu_allowWaypointExtraction : R.string.cache_menu_preventWaypointsFromNote);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final IConnector connector = null != cache ? ConnectorFactory.getConnector(cache) : null;
        final boolean isUDC = null != connector && connector.equals(InternalConnector.getInstance());

        CacheMenuHandler.onPrepareOptionsMenu(menu, cache, false);
        LoggingUI.onPrepareOptionsMenu(menu, cache);
        if (cache != null) {
            // top level menu items
            menu.findItem(R.id.menu_tts_toggle).setVisible(!cache.isGotoHistoryUDC());
            menu.findItem(R.id.menu_checker).setVisible(StringUtils.isNotEmpty(CheckerUtils.getCheckerUrl(cache)));
            if (connector instanceof PgcChallengeCheckerCapability) {
                menu.findItem(R.id.menu_challenge_checker).setVisible(((PgcChallengeCheckerCapability) connector).isChallengeCache(cache));
            }
            menu.findItem(R.id.menu_edit_fieldnote).setVisible(true);

            // submenu waypoints
            menu.findItem(R.id.menu_delete_userdefined_waypoints).setVisible(cache.isOffline() && cache.hasUserdefinedWaypoints());
            menu.findItem(R.id.menu_extract_waypoints).setVisible(!isUDC);
            menu.findItem(R.id.menu_clear_goto_history).setVisible(cache.isGotoHistoryUDC());
            menuItemToggleWaypointsFromNote = menu.findItem(R.id.menu_toggleWaypointsFromNote);
            setMenuPreventWaypointsFromNote(cache.isPreventWaypointsFromNote());
            menuItemToggleWaypointsFromNote.setVisible(!cache.isGotoHistoryUDC());
            menu.findItem(R.id.menu_waypoints).setVisible(true);

            // submenu share / export
            menu.findItem(R.id.menu_export).setVisible(true);

            // submenu advanced
            if (connector instanceof IVotingCapability) {
                final MenuItem menuItemGCVote = menu.findItem(R.id.menu_gcvote);
                menuItemGCVote.setVisible(((IVotingCapability) connector).supportsVoting(cache));
                menuItemGCVote.setEnabled(Settings.isRatingWanted() && Settings.isGCVoteLoginValid());
            }
            if (connector instanceof IIgnoreCapability) {
                menu.findItem(R.id.menu_ignore).setVisible(((IIgnoreCapability) connector).canIgnoreCache(cache));
            }
            menu.findItem(R.id.menu_set_cache_icon).setVisible(cache.isOffline());
            menu.findItem(R.id.menu_advanced).setVisible(cache.getCoords() != null);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (CacheMenuHandler.onMenuItemSelected(item, this, cache, this::notifyDataSetChanged, false)) {
            return true;
        }

        final int menuItem = item.getItemId();
        if (menuItem == R.id.menu_delete_userdefined_waypoints) {
            dropUserdefinedWaypoints();
        } else if (menuItem == R.id.menu_refresh) {
            refreshCache();
        } else if (menuItem == R.id.menu_gcvote) {
            showVoteDialog();
        } else if (menuItem == R.id.menu_checker) {
            ShareUtils.openUrl(this, CheckerUtils.getCheckerUrl(cache), true);
        } else if (menuItem == R.id.menu_challenge_checker) {
            ShareUtils.openUrl(this, "https://project-gc.com/Challenges/" + cache.getGeocode());
        } else if (menuItem == R.id.menu_ignore) {
            ignoreCache();
        } else if (menuItem == R.id.menu_extract_waypoints) {
            final String searchText = cache.getShortDescription() + ' ' + cache.getDescription();
            extractWaypoints(searchText, cache);
        } else if (menuItem == R.id.menu_toggleWaypointsFromNote) {
            cache.setPreventWaypointsFromNote(!cache.isPreventWaypointsFromNote());
            setMenuPreventWaypointsFromNote(cache.isPreventWaypointsFromNote());
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(final Void... params) {
                    DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                    return true;
                }

                @Override
                protected void onPostExecute(final Boolean result) {
                    if (result) {
                        notifyDataSetChanged();
                    }
                }
            }.execute();
        } else if (menuItem == R.id.menu_clear_goto_history) {
            SimpleDialog.of(this).setTitle(R.string.clear_goto_history_title).setMessage(R.string.clear_goto_history).confirm((dialog, which) -> AndroidRxUtils.andThenOnUi(Schedulers.io(), DataStore::clearGotoHistory, () -> {
                cache = DataStore.loadCache(InternalConnector.GEOCODE_HISTORY_CACHE, LoadFlags.LOAD_ALL_DB_ONLY);
                notifyDataSetChanged();
            }));
        } else if (menuItem == R.id.menu_export_gpx) {
            new GpxExport().export(Collections.singletonList(cache), this);
        } else if (menuItem == R.id.menu_export_fieldnotes) {
            new FieldNoteExport().export(Collections.singletonList(cache), this);
        } else if (menuItem == R.id.menu_export_persnotes) {
            new PersonalNoteExport().export(Collections.singletonList(cache), this);
        } else if (menuItem == R.id.menu_edit_fieldnote) {
            ensureSaved();
            editPersonalNote(cache, this);
        } else if (menuItem == R.id.menu_navigate) {
            NavigationAppFactory.onMenuItemSelected(item, this, cache);
        } else if (menuItem == R.id.menu_tts_toggle) {
            SpeechService.toggleService(this, cache.getCoords());
        } else if (menuItem == R.id.menu_set_cache_icon) {
            EmojiUtils.selectEmojiPopup(this, cache.getAssignedEmoji(), cache, this::setCacheIcon);
        } else if (LoggingUI.onMenuItemSelected(item, this, cache, null)) {
            refreshOnResume = true;
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void setCacheIcon(final int newCacheIcon) {
        cache.setAssignedEmoji(newCacheIcon);
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
        Toast.makeText(this, R.string.cache_icon_updated, Toast.LENGTH_SHORT).show();
        notifyDataSetChanged();
    }

    private void ignoreCache() {
        SimpleDialog.of(this).setTitle(R.string.ignore_confirm_title).setMessage(R.string.ignore_confirm_message).confirm((dialog, which) -> {
            AndroidRxUtils.networkScheduler.scheduleDirect(() -> ((IIgnoreCapability) ConnectorFactory.getConnector(cache)).addToIgnorelist(cache));
            // For consistency, remove also the local cache immediately from memory cache and database
            if (cache.isOffline()) {
                dropCache();
                DataStore.removeCache(cache.getGeocode(), EnumSet.of(RemoveFlag.DB));
            }
        });
    }

    private void showVoteDialog() {
        VoteDialog.show(this, cache, this::notifyDataSetChanged);
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

                    if (error == StatusCode.PREMIUM_ONLY) {
                        SimpleDialog.of(activity).setTitle(R.string.cache_status_premium).setMessage(R.string.err_detail_premium_log_found).setPositiveButton(TextParam.id(R.string.cache_menu_visit)).confirm((dialog, which) -> {
                            activity.startActivity(LogCacheActivity.getLogCacheIntent(activity, null, activity.geocode));
                            finishActivity();
                        }, (d, i) -> finishActivity());

                        dismissProgress();

                    } else {
                        activity.showToast(toastPrefix + error.getErrorString(res));
                        dismissProgress();
                        finishActivity();
                    }
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
        setIsContentRefreshable(cache.supportsRefresh());

        // reset imagesList so Images view page will be redrawn
        imagesList = null;
        setOrderedPages(getOrderedPages());
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

    public static void startActivity(final Context context, final String geocode, final boolean forceWaypointsPage) {
        final Intent detailIntent = new Intent(context, CacheDetailActivity.class);
        detailIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        detailIntent.putExtra(EXTRA_FORCE_WAYPOINTSPAGE, forceWaypointsPage);
        context.startActivity(detailIntent);
    }

    public static void startActivity(final Context context, final String geocode) {
        startActivity(context, geocode, false);
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
        IMAGES(R.string.cache_images),
        VARIABLES(R.string.cache_variables),
        ;

        private final int titleStringId;
        public final long id;

        Page(final int titleStringId) {
            this.titleStringId = titleStringId;
            this.id = ordinal();
        }

        static Page find(final long pageId) {
            for (Page page : Page.values()) {
                if (page.id == pageId) {
                    return page;
                }
            }
            return null;
        }
    }

    @Override
    public void pullToRefreshActionTrigger() {
        refreshCache();
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

    private void dropUserdefinedWaypoints() {
        if (null != cache && cache.hasUserdefinedWaypoints()) {
            String info = getString(R.string.cache_delete_userdefined_waypoints_confirm);
            if (!cache.isPreventWaypointsFromNote()) {
                info += "\n\n" + getString(R.string.cache_delete_userdefined_waypoints_note);
            }
            SimpleDialog.of(this).setTitle(R.string.cache_delete_userdefined_waypoints).setMessage(TextParam.text(info)).confirm((dialog, which) -> {
                for (Waypoint waypoint : new LinkedList<>(cache.getWaypoints())) {
                    if (waypoint.isUserDefined()) {
                        cache.deleteWaypoint(waypoint);
                    }
                }
                if (cache.addWaypointsFromNote()) {
                    Schedulers.io().scheduleDirect(() -> DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB)));
                }
                ActivityMixin.showShortToast(this, R.string.cache_delete_userdefined_waypoints_success);
                invalidateOptionsMenu();
                reinitializePage(Page.WAYPOINTS.id);
            });
        }
    }

    private void storeCache(final boolean fastStoreOnLastSelection) {
        if (progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        if (Settings.getChooseList() || cache.isOffline()) {
            // let user select list to store cache in
            new StoredList.UserInterface(this).promptForMultiListSelection(R.string.lists_title,
                    this::storeCacheInLists, true, cache.getLists(), fastStoreOnLastSelection);
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
                updateCacheLists(CacheDetailActivity.this.findViewById(R.id.offline_lists), cache, res);
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

    private static final class CheckboxHandler extends SimpleDisposableHandler {
        private final WeakReference<DetailsViewCreator> creatorRef;
        private final WeakReference<CacheDetailActivity> activityWeakReference;

        CheckboxHandler(final DetailsViewCreator creator, final CacheDetailActivity activity, final Progress progress) {
            super(activity, progress);

            creatorRef = new WeakReference<>(creator);
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleRegularMessage(final Message message) {
            final DetailsViewCreator creator = creatorRef.get();
            if (creator != null) {
                super.handleRegularMessage(message);
                creator.updateWatchlistBox(activityWeakReference.get());
                creator.updateFavPointBox();
            }
        }
    }

    /**
     * Creator for details-view.
     *
     * TODO: Extract inner class to own file for a better overview. Same might apply to all other view creators.
     */
    public static class DetailsViewCreator extends TabbedViewPagerFragment<CachedetailDetailsPageBinding> {
        private ImmutablePair<RelativeLayout, TextView> favoriteLine;
        private Geocache cache;

        @Override
        public CachedetailDetailsPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return CachedetailDetailsPageBinding.inflate(inflater, container, false);
        }

        @Override
        public long getPageId() {
            return Page.DETAILS.id;
        }

        @Override
        @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"}) // splitting up that method would not help improve readability
        public void setContent() {
            // retrieve activity and cache - if either of them is null, something's really wrong!
            final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
            if (activity == null) {
                return;
            }
            cache = activity.getCache();
            if (cache == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);

            // Reference to the details list and favorite line, so that the helper-method can access them without an additional argument
            final CacheDetailsCreator details = new CacheDetailsCreator(activity, binding.detailsList);

            // cache name (full name), may be editable
            final SpannableString span = TextUtils.coloredCacheText(cache, cache.getName());
            final TextView cachename = details.add(R.string.cache_name, span).right;
            activity.addContextMenu(cachename);
            if (cache.supportsNamechange()) {
                cachename.setOnClickListener(v -> {
                    Dialogs.input(activity, activity.getString(R.string.cache_name_set), cache.getName(), activity.getString(R.string.caches_sort_name), name -> {
                        cachename.setText(name);
                        cache.setName(name);
                        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
                        Toast.makeText(activity, R.string.cache_name_updated, Toast.LENGTH_SHORT).show();
                    });
                });
            }

            details.add(R.string.cache_type, cache.getType().getL10n());
            details.addSize(cache);
            activity.addContextMenu(details.add(R.string.cache_geocode, cache.getShortGeocode()).right);
            details.addCacheState(cache);

            activity.cacheDistanceView = details.addDistance(cache, activity.cacheDistanceView);

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
                ownerView.setOnClickListener(UserClickListener.forOwnerOf(cache));
            }

            // hidden or event date
            final TextView hiddenView = details.addHiddenDate(cache);
            if (hiddenView != null) {
                activity.addContextMenu(hiddenView);
                if (cache.isEventCache()) {
                    hiddenView.setOnClickListener(v -> CalendarUtils.openCalendar(activity, cache.getHiddenDate()));
                }
            }

            // cache location
            if (StringUtils.isNotBlank(cache.getLocation())) {
                details.add(R.string.cache_location, cache.getLocation());
            }

            // cache coordinates
            if (cache.getCoords() != null) {
                final TextView valueView = details.add(R.string.cache_coordinates, cache.getCoords().toString()).right;
                new CoordinatesFormatSwitcher().setView(valueView).setCoordinate(cache.getCoords());
                activity.addContextMenu(valueView);
            }

            // Latest logs
            details.addLatestLogs(cache);

            // cache attributes
            updateAttributesIcons(activity);
            updateAttributesText();
            binding.attributesBox.setVisibility(cache.getAttributes().isEmpty() ? View.GONE : View.VISIBLE);

            updateOfflineBox(binding.getRoot(), cache, activity.res, new RefreshCacheClickListener(), new DropCacheClickListener(),
                    new StoreCacheClickListener(), null, new MoveCacheClickListener(), new StoreCacheClickListener());

            // list
            updateCacheLists(binding.getRoot(), cache, activity.res);

            // watchlist

            binding.addToWatchlist.setOnClickListener(new AddToWatchlistClickListener());
            binding.removeFromWatchlist.setOnClickListener(new RemoveFromWatchlistClickListener());
            updateWatchlistBox(activity);

            // WhereYouGo, ChirpWolf and Adventure Lab
            updateWhereYouGoBox(activity);
            updateChirpWolfBox(activity);
            updateALCBox(activity);

            // favorite points
            binding.addToFavpoint.setOnClickListener(new FavoriteAddClickListener());
            binding.removeFromFavpoint.setOnClickListener(new FavoriteRemoveClickListener());
            updateFavPointBox();

            // data license
            final IConnector connector = ConnectorFactory.getConnector(cache);
            final String license = connector.getLicenseText(cache);
            if (StringUtils.isNotBlank(license)) {
                binding.licenseBox.setVisibility(View.VISIBLE);
                binding.license.setText(HtmlCompat.fromHtml(license, HtmlCompat.FROM_HTML_MODE_LEGACY), BufferType.SPANNABLE);
                binding.license.setClickable(true);
                binding.license.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            } else {
                binding.licenseBox.findViewById(R.id.license_box).setVisibility(View.GONE);
            }
        }

        private void updateAttributesIcons(final Activity activity) {
            final List<String> attributes = cache.getAttributes();
            if (!CacheAttribute.hasRecognizedAttributeIcon(attributes)) {
                binding.attributesGrid.setVisibility(View.GONE);
                return;
            }
            binding.attributesGrid.setAdapter(new AttributesGridAdapter(activity, cache));
            binding.attributesGrid.setVisibility(View.VISIBLE);
            binding.attributesGrid.setOnItemClickListener((parent, view, position, id) -> toggleAttributesView());
        }

        protected void toggleAttributesView() {
            binding.attributesText.setVisibility(binding.attributesText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            binding.attributesGrid.setVisibility(binding.attributesGrid.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }

        private void updateAttributesText() {
            final List<String> attributes = cache.getAttributes();
            if (attributes.isEmpty()) {
                binding.attributesText.setVisibility(View.GONE);
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
            binding.attributesText.setText(text);
            if (binding.attributesGrid.getVisibility() == View.VISIBLE) {
                binding.attributesText.setVisibility(View.GONE);
                binding.attributesText.setOnClickListener(v -> toggleAttributesView());
            } else {
                binding.attributesText.setVisibility(View.VISIBLE);
            }
        }

        private class StoreCacheClickListener implements View.OnClickListener, View.OnLongClickListener {
            @Override
            public void onClick(final View arg0) {
                final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
                if (activity != null) {
                    activity.storeCache(false);
                }
            }

            @Override
            public boolean onLongClick(final View v) {
                final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
                if (activity != null) {
                    activity.storeCache(true);
                }
                return true;
            }
        }

        private class MoveCacheClickListener implements OnLongClickListener {
            @Override
            public boolean onLongClick(final View v) {
                final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
                if (activity != null) {
                    activity.moveCache();
                }
                return true;
            }
        }

        private class DropCacheClickListener implements View.OnClickListener {
            @Override
            public void onClick(final View arg0) {
                final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
                if (activity != null) {
                    activity.dropCache();
                }
            }
        }

        private class RefreshCacheClickListener implements View.OnClickListener {
            @Override
            public void onClick(final View arg0) {
                final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
                if (activity != null) {
                    activity.refreshCache();
                }
            }
        }

        /**
         * Abstract Listener for add / remove buttons for watchlist
         */
        private abstract class AbstractPropertyListener implements View.OnClickListener {
            private final SimpleDisposableHandler handler;

            AbstractPropertyListener() {
                final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
                handler = new CheckboxHandler(DetailsViewCreator.this, activity, activity.progress);
            }

            public void doExecute(final int titleId, final int messageId, final Action1<SimpleDisposableHandler> action) {
                final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
                if (activity != null) {
                    if (activity.progress.isShowing()) {
                        activity.showToast(activity.res.getString(R.string.err_watchlist_still_managing));
                        return;
                    }
                    activity.progress.show(activity, activity.res.getString(titleId), activity.res.getString(messageId), true, null);
                }
                AndroidRxUtils.networkScheduler.scheduleDirect(() -> action.call(handler));
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
                        DetailsViewCreator.this::watchListAdd);
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
                        DetailsViewCreator.this::watchListRemove);
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
            final IFavoriteCapability connector = (IFavoriteCapability) ConnectorFactory.getConnector(cache);
            if (connector.addToFavorites(cache)) {
                handler.obtainMessage(MESSAGE_SUCCEEDED).sendToTarget();
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_favorite_failed);
            }
        }

        /** Remove this cache to the favorite list of the user */
        private void favoriteRemove(final SimpleDisposableHandler handler) {
            final IFavoriteCapability connector = (IFavoriteCapability) ConnectorFactory.getConnector(cache);
            if (connector.removeFromFavorites(cache)) {
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
                        DetailsViewCreator.this::favoriteAdd);
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
                        DetailsViewCreator.this::favoriteRemove);
            }
        }

        /**
         * Show/hide buttons, set text in watchlist box
         */
        private void updateWatchlistBox(final CacheDetailActivity activity) {
            final boolean supportsWatchList = cache.supportsWatchList();
            binding.watchlistBox.setVisibility(supportsWatchList ? View.VISIBLE : View.GONE);
            if (!supportsWatchList) {
                return;
            }

            final int watchListCount = cache.getWatchlistCount();

            if (cache.isOnWatchlist() || cache.isOwner()) {
                binding.addToWatchlist.setVisibility(View.GONE);
                binding.removeFromWatchlist.setVisibility(View.VISIBLE);
                if (watchListCount != -1) {
                    binding.watchlistText.setText(activity.res.getString(R.string.cache_watchlist_on_extra, watchListCount));
                } else {
                    binding.watchlistText.setText(R.string.cache_watchlist_on);
                }
            } else {
                binding.addToWatchlist.setVisibility(View.VISIBLE);
                binding.removeFromWatchlist.setVisibility(View.GONE);
                if (watchListCount != -1) {
                    binding.watchlistText.setText(activity.res.getString(R.string.cache_watchlist_not_on_extra, watchListCount));
                } else {
                    binding.watchlistText.setText(R.string.cache_watchlist_not_on);
                }
            }

            // the owner of a cache has it always on his watchlist. Adding causes an error
            if (cache.isOwner()) {
                binding.addToWatchlist.setEnabled(false);
                binding.addToWatchlist.setVisibility(View.GONE);
                binding.removeFromWatchlist.setEnabled(false);
                binding.removeFromWatchlist.setVisibility(View.GONE);
            }
        }

        /**
         * Show/hide buttons, set text in favorite line and box
         */
        private void updateFavPointBox() {
            // Favorite counts
            final int favCount = cache.getFavoritePoints();
            if (favCount >= 0 && !cache.isEventCache()) {
                favoriteLine.left.setVisibility(View.VISIBLE);

                final int findsCount = cache.getFindsCount();
                if (findsCount > 0) {
                    favoriteLine.right.setText(getString(R.string.favorite_count_percent, favCount, (float) (favCount * 100) / findsCount));
                } else {
                    favoriteLine.right.setText(getString(R.string.favorite_count, favCount));
                }
            } else {
                favoriteLine.left.setVisibility(View.GONE);
            }
            final boolean supportsFavoritePoints = cache.supportsFavoritePoints();
            binding.favpointBox.setVisibility(supportsFavoritePoints ? View.VISIBLE : View.GONE);
            if (!supportsFavoritePoints) {
                return;
            }

            if (cache.isFavorite()) {
                binding.addToFavpoint.setVisibility(View.GONE);
                binding.removeFromFavpoint.setVisibility(View.VISIBLE);
                binding.favpointText.setText(R.string.cache_favpoint_on);
            } else {
                binding.addToFavpoint.setVisibility(View.VISIBLE);
                binding.removeFromFavpoint.setVisibility(View.GONE);
                binding.favpointText.setText(R.string.cache_favpoint_not_on);
            }

            // Add/remove to Favorites is only possible if the cache has been found
            if (!cache.isFound()) {
                binding.addToFavpoint.setVisibility(View.GONE);
                binding.removeFromFavpoint.setVisibility(View.GONE);
            }
        }

        private void updateWhereYouGoBox(final CacheDetailActivity activity) {
            final boolean isEnabled = cache.getType() == CacheType.WHERIGO && StringUtils.isNotEmpty(getWhereIGoUrl(cache));
            binding.whereyougoBox.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
            binding.whereyougoText.setText(isWhereYouGoInstalled() ? R.string.cache_whereyougo_start : R.string.cache_whereyougo_install);
            if (isEnabled) {
                binding.sendToWhereyougo.setOnClickListener(v -> {
                    // re-check installation state, might have changed since creating the view
                    if (isWhereYouGoInstalled()) {
                        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getWhereIGoUrl(cache)));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } else {
                        ProcessUtils.openMarket(activity, getString(R.string.package_whereyougo));
                    }
                });
            }
        }

        private void updateChirpWolfBox(final CacheDetailActivity activity) {
            final Intent chirpWolf = ProcessUtils.getLaunchIntent(getString(R.string.package_chirpwolf));
            final String compare = CacheAttribute.WIRELESSBEACON.getValue(true);
            boolean isEnabled = false;
            for (String current : cache.getAttributes()) {
                if (StringUtils.equals(current, compare)) {
                    isEnabled = true;
                    break;
                }
            }
            binding.chirpBox.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
            binding.chirpText.setText(chirpWolf != null ? R.string.cache_chirpwolf_start : R.string.cache_chirpwolf_install);
            if (isEnabled) {
                binding.sendToChirp.setOnClickListener(v -> {
                    // re-check installation state, might have changed since creating the view
                    final Intent chirpWolf2 = ProcessUtils.getLaunchIntent(getString(R.string.package_chirpwolf));
                    if (chirpWolf2 != null) {
                        chirpWolf2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(chirpWolf2);
                    } else {
                        ProcessUtils.openMarket(activity, getString(R.string.package_chirpwolf));
                    }
                });
            }
        }

        private void updateALCBox(final CacheDetailActivity activity) {
            final boolean isEnabled = cache.getType() == CacheType.ADVLAB && StringUtils.isNotEmpty(cache.getUrl());
            final Intent alc = ProcessUtils.getLaunchIntent(getString(R.string.package_alc));
            binding.alcBox.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
            binding.alcText.setText(alc != null ? R.string.cache_alc_start : R.string.cache_alc_install);
            if (isEnabled) {
                binding.sendToAlc.setOnClickListener(v -> {
                    // re-check installation state, might have changed since creating the view
                    if (alc != null) {
                      final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cache.getUrl()));
                      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                      activity.startActivity(intent);
                    } else {
                        ProcessUtils.openMarket(activity, getString(R.string.package_alc));
                    }
                });
            }
        }
    }

    /**
     * Reflect the (contextual) action mode of the action bar.
     */
    protected ActionMode currentActionMode;

    public static class DescriptionViewCreator extends TabbedViewPagerFragment<CachedetailDescriptionPageBinding> {

        private int maxPersonalNotesChars = 0;
        private Geocache cache;

        @Override
        public CachedetailDescriptionPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return CachedetailDescriptionPageBinding.inflate(getLayoutInflater(), container, false);
        }

        @Override
        public long getPageId() {
            return Page.DESCRIPTION.id;
        }

        @Override
        @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"}) // splitting up that method would not help improve readability
        public void setContent() {
            // retrieve activity and cache - if either of them is null, something's really wrong!
            final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
            if (activity == null) {
                return;
            }
            cache = activity.getCache();
            if (cache == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);

            // reset description
            binding.description.setText("");

            // cache short description
            if (StringUtils.isNotBlank(cache.getShortDescription())) {
                loadDescription(activity, cache.getShortDescription(), false, binding.description, null);
            }

            // long description
            if (StringUtils.isNotBlank(cache.getDescription()) || cache.supportsDescriptionchange()) {
                loadLongDescription(activity, container);
            }

            // extra description
            final String geocode = cache.getGeocode();
            boolean hasExtraDescription = ALConnector.getInstance().canHandle(geocode); // could be generalized, but currently it's only AL
            if (hasExtraDescription) {
                final IConnector conn = ConnectorFactory.getConnector(geocode);
                if (conn != null) {
                    binding.extraDescriptionTitle.setText(conn.getName());
                    binding.extraDescription.setText(conn.getExtraDescription());
                } else {
                    hasExtraDescription = false;
                }
            }
            binding.extraDescriptionBox.setVisibility(hasExtraDescription ? View.VISIBLE : View.GONE);

            // cache personal note
            setPersonalNote(binding.personalnote, binding.personalnoteButtonSeparator, cache.getPersonalNote());
            binding.personalnote.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            activity.addContextMenu(binding.personalnote);
            TooltipCompat.setTooltipText(binding.editPersonalnote, getString(R.string.cache_personal_note_edit));
            binding.editPersonalnote.setOnClickListener(v -> {
                activity.ensureSaved();
                editPersonalNote(cache, activity);
            });
            binding.personalnote.setOnClickListener(v -> {
                activity.ensureSaved();
                editPersonalNote(cache, activity);
            });
            TooltipCompat.setTooltipText(binding.storewaypointsPersonalnote, getString(R.string.cache_personal_note_storewaypoints));
            binding.storewaypointsPersonalnote.setOnClickListener(v -> {
                activity.ensureSaved();
                activity.storeWaypointsInPersonalNote(cache, maxPersonalNotesChars);
            });
            TooltipCompat.setTooltipText(binding.deleteewaypointsPersonalnote, getString(R.string.cache_personal_note_removewaypoints));
            binding.deleteewaypointsPersonalnote.setOnClickListener(v -> {
                activity.ensureSaved();
                activity.removeWaypointsFromPersonalNote(cache);
            });
            final PersonalNoteCapability connector = ConnectorFactory.getConnectorAs(cache, PersonalNoteCapability.class);
            if (connector != null && connector.canAddPersonalNote(cache)) {
                maxPersonalNotesChars = connector.getPersonalNoteMaxChars();
                binding.uploadPersonalnote.setVisibility(View.VISIBLE);
                TooltipCompat.setTooltipText(binding.uploadPersonalnote, getString(R.string.cache_personal_note_upload));
                binding.uploadPersonalnote.setOnClickListener(v -> {
                    if (StringUtils.length(cache.getPersonalNote()) > maxPersonalNotesChars) {
                        warnPersonalNoteExceedsLimit(activity);
                    } else {
                        uploadPersonalNote(activity);
                    }
                });
            } else {
                binding.uploadPersonalnote.setVisibility(View.GONE);
            }

            // cache hint and spoiler images
            if (StringUtils.isNotBlank(cache.getHint()) || CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                binding.hintBox.setVisibility(View.VISIBLE);
            } else {
                binding.hintBox.setVisibility(View.GONE);
            }

            if (StringUtils.isNotBlank(cache.getHint())) {
                if (TextUtils.containsHtml(cache.getHint())) {
                    binding.hint.setText(HtmlCompat.fromHtml(cache.getHint(), HtmlCompat.FROM_HTML_MODE_LEGACY, new HtmlImage(cache.getGeocode(), false, false, false), null), TextView.BufferType.SPANNABLE);
                    binding.hint.setText(CryptUtils.rot13((Spannable) binding.hint.getText()));
                } else {
                    binding.hint.setText(CryptUtils.rot13(cache.getHint()));
                }
                binding.hint.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                binding.hint.setVisibility(View.VISIBLE);
                binding.hint.setClickable(true);
                binding.hint.setOnClickListener(new DecryptTextClickListener(binding.hint));
                binding.hintBox.setOnClickListener(new DecryptTextClickListener(binding.hint));
                binding.hintBox.setClickable(true);
                activity.addContextMenu(binding.hint);
            } else {
                binding.hint.setVisibility(View.GONE);
                binding.hint.setClickable(false);
                binding.hint.setOnClickListener(null);
                binding.hintBox.setClickable(false);
                binding.hintBox.setOnClickListener(null);
            }

            if (CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                binding.hintSpoilerlink.setVisibility(View.VISIBLE);
                binding.hintSpoilerlink.setClickable(true);
                binding.hintSpoilerlink.setOnClickListener(arg0 -> {
                    if (cache == null || CollectionUtils.isEmpty(cache.getSpoilers())) {
                        activity.showToast(getString(R.string.err_detail_no_spoiler));
                        return;
                    }
                    ImagesActivity.startActivity(activity, cache.getGeocode(), cache.getSpoilers());
                });

                // if there is only a listing background image without other additional pictures, change the text to better explain the content.
                if (cache.getSpoilers().size() == 1 && getString(R.string.cache_image_background).equals(cache.getSpoilers().get(0).title)) {
                    binding.hintSpoilerlink.setText(R.string.cache_image_background);
                } else {
                    binding.hintSpoilerlink.setText(R.string.cache_menu_spoilers);
                }
            } else {
                binding.hintSpoilerlink.setVisibility(View.GONE);
                binding.hintSpoilerlink.setClickable(true);
                binding.hintSpoilerlink.setOnClickListener(null);
            }
        }

        private void uploadPersonalNote(final CacheDetailActivity activity) {
            final SimpleDisposableHandler myHandler = new SimpleDisposableHandler(activity, activity.progress);

            final Message cancelMessage = myHandler.cancelMessage(getString(R.string.cache_personal_note_upload_cancelled));
            activity.progress.show(activity, getString(R.string.cache_personal_note_uploading), getString(R.string.cache_personal_note_uploading), true, cancelMessage);

            myHandler.add(AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
                final PersonalNoteCapability connector = (PersonalNoteCapability) ConnectorFactory.getConnector(cache);
                final boolean success = connector.uploadPersonalNote(cache);
                final Message msg = Message.obtain();
                final Bundle bundle = new Bundle();
                bundle.putString(SimpleDisposableHandler.MESSAGE_TEXT,
                        CgeoApplication.getInstance().getString(success ? R.string.cache_personal_note_upload_done : R.string.cache_personal_note_upload_error));
                msg.setData(bundle);
                myHandler.sendMessage(msg);
            }));
        }

        private void loadLongDescription(final CacheDetailActivity activity, final ViewGroup parentView) {
            binding.loading.setVisibility(View.VISIBLE);

            final String longDescription = cache.getDescription();
            loadDescription(activity, longDescription, true, binding.description, binding.loading);

            if (cache.supportsDescriptionchange()) {
                binding.description.setOnClickListener(v -> {
                    Dialogs.input(activity, activity.getString(R.string.cache_description_set), cache.getDescription(), "Description", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 5, 10, description -> {
                        binding.description.setText(description);
                        cache.setDescription(description);
                        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
                        Toast.makeText(activity, R.string.cache_description_updated, Toast.LENGTH_SHORT).show();
                    });
                });
            }
        }

        private void warnPersonalNoteExceedsLimit(final CacheDetailActivity activity) {
            SimpleDialog.of(activity).setTitle(R.string.cache_personal_note_limit).setMessage(R.string.cache_personal_note_truncation, maxPersonalNotesChars).confirm(
                    (dialog, which) -> {
                        dialog.dismiss();
                        uploadPersonalNote(activity);
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
        private void loadDescription(final CacheDetailActivity activity, final String descriptionString, final boolean isLongDescription, final IndexOutOfBoundsAvoidingTextView descriptionView, final View loadingIndicatorView) {
            try {
                final UnknownTagsHandler unknownTagsHandler = new UnknownTagsHandler();
                final Editable description = new SpannableStringBuilder(HtmlCompat.fromHtml(descriptionString, HtmlCompat.FROM_HTML_MODE_LEGACY, new HtmlImage(cache.getGeocode(), true, false, descriptionView, false), unknownTagsHandler));
                activity.addWarning(unknownTagsHandler, description);
                if (StringUtils.isNotBlank(description)) {
                    fixRelativeLinks(description);
                    fixTextColor(description, R.color.colorBackground);

                    // check if short description is contained in long description
                    boolean longDescriptionContainsShortDescription = false;
                    final String shortDescription = cache.getShortDescription();
                    if (StringUtils.isNotBlank(shortDescription)) {
                        final int index = StringUtils.indexOf(cache.getDescription(), shortDescription);
                        // allow up to 200 characters of HTML formatting
                        if (index >= 0 && index < 200) {
                            longDescriptionContainsShortDescription = true;
                        }
                    }

                    try {
                        if (descriptionView.getText().length() == 0 || (longDescriptionContainsShortDescription && isLongDescription)) {
                            descriptionView.setText(description, TextView.BufferType.SPANNABLE);
                        } else if (!longDescriptionContainsShortDescription) {
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
                    descriptionView.setVisibility(View.VISIBLE);
                    activity.addContextMenu(descriptionView);
                }
                if (loadingIndicatorView != null) {
                    loadingIndicatorView.setVisibility(View.GONE);
                }
            } catch (final RuntimeException ignored) {
                activity.showToast(getString(R.string.err_load_descr_failed));
            }
        }

        private void fixRelativeLinks(final Spannable spannable) {
            final String baseUrl = ConnectorFactory.getConnector(cache).getHostUrl() + "/";
            final URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
            for (final URLSpan span : spans) {
                final Uri uri = Uri.parse(span.getURL());
                if (uri.getScheme() == null && uri.getHost() == null) {
                    final int start = spannable.getSpanStart(span);
                    final int end = spannable.getSpanEnd(span);
                    final int flags = spannable.getSpanFlags(span);
                    final Uri absoluteUri = Uri.parse(baseUrl + uri.toString());
                    spannable.removeSpan(span);
                    spannable.setSpan(new URLSpan(absoluteUri.toString()), start, end, flags);
                }
            }
        }

    }

    // If description has an HTML construct which may be problematic to render, add a note at the end of the long description.
    // Technically, it may not be a table, but a pre, which has the same problems as a table, so the message is ok even though
    // sometimes technically incorrect.
    private void addWarning(final UnknownTagsHandler unknownTagsHandler, final Editable description) {
        if (unknownTagsHandler.isProblematicDetected()) {
            final int startPos = description.length();
            final IConnector connector = ConnectorFactory.getConnector(cache);
            if (StringUtils.isNotEmpty(cache.getUrl())) {
                final Spanned tableNote = HtmlCompat.fromHtml(res.getString(R.string.cache_description_table_note, "<a href=\"" + cache.getUrl() + "\">" + connector.getName() + "</a>"), HtmlCompat.FROM_HTML_MODE_LEGACY);
                description.append("\n\n").append(tableNote);
                description.setSpan(new StyleSpan(Typeface.ITALIC), startPos, description.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void fixTextColor(final Spannable spannable, final int backgroundColor) {
        final ForegroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);

        for (final ForegroundColorSpan span : spans) {
            if (ColorUtils.getContrastRatio(span.getForegroundColor(), backgroundColor) < CONTRAST_THRESHOLD) {
                final int start = spannable.getSpanStart(span);
                final int end = spannable.getSpanEnd(span);

                //  Assuming that backgroundColor can be either white or black,
                // this will set opposite background color (white for black and black for white)
                spannable.setSpan(new BackgroundColorSpan(backgroundColor ^ 0x00ffffff), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }


    protected void ensureSaved() {
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

    public static class WaypointsViewCreator extends TabbedViewPagerFragment<CachedetailWaypointsPageBinding> {
        private Geocache cache;

        private void setClipboardButtonVisibility(final Button createFromClipboard) {
            createFromClipboard.setVisibility(Waypoint.hasClipboardWaypoint() >= 0 ? View.VISIBLE : View.GONE);
        }

        private void addWaypointAndSort(final List<Waypoint> sortedWaypoints, final Waypoint newWaypoint) {
            sortedWaypoints.add(newWaypoint);
            Collections.sort(sortedWaypoints, cache.getWaypointComparator());
        }

        private List<Waypoint> createSortedWaypointList() {
            final List<Waypoint> sortedWaypoints2 = new ArrayList<>(cache.getWaypoints());
            final Iterator<Waypoint> waypointIterator = sortedWaypoints2.iterator();
            while (waypointIterator.hasNext()) {
                final Waypoint waypointInIterator = waypointIterator.next();
                if (waypointInIterator.isVisited() && Settings.getHideVisitedWaypoints()) {
                    waypointIterator.remove();
                }
            }
            return sortedWaypoints2;
        }

        @Override
        public CachedetailWaypointsPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return CachedetailWaypointsPageBinding.inflate(inflater, container, false);
        }

        @Override
        public long getPageId() {
            return Page.WAYPOINTS.id;
        }

        @Override
        public void setContent() {
            // retrieve activity and cache - if either if this is null, something is really wrong...
            final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
            if (activity == null) {
                return;
            }
            cache = activity.getCache();
            if (cache == null) {
                return;
            }

            final ListView v = binding.getRoot();
            v.setVisibility(View.VISIBLE);
            v.setClickable(true);

            // sort waypoints: PP, Sx, FI, OWN
            final List<Waypoint> sortedWaypoints = createSortedWaypointList();
            Collections.sort(sortedWaypoints, cache.getWaypointComparator());

            final ArrayAdapter<Waypoint> adapter = new ArrayAdapter<Waypoint>(activity, R.layout.waypoint_item, sortedWaypoints) {
                @Override
                public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
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
                    fillViewHolder(activity, rowView, holder, waypoint);
                    return rowView;
                }
            };
            v.setAdapter(adapter);
            v.setOnScrollListener(new FastScrollListener(v));

            if (v.getHeaderViewsCount() < 1) {
                final CachedetailWaypointsHeaderBinding headerBinding = CachedetailWaypointsHeaderBinding.inflate(getLayoutInflater(), v, false);
                v.addHeaderView(headerBinding.getRoot());

                headerBinding.addWaypoint.setOnClickListener(v2 -> {
                    activity.ensureSaved();
                    EditWaypointActivity.startActivityAddWaypoint(activity, cache);
                    activity.refreshOnResume = true;
                });

                headerBinding.addWaypointCurrentlocation.setOnClickListener(v2 -> {
                    activity.ensureSaved();
                    final Waypoint newWaypoint = new Waypoint(Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT), WaypointType.WAYPOINT, true);
                    newWaypoint.setCoords(Sensors.getInstance().currentGeo().getCoords());
                    newWaypoint.setGeocode(cache.getGeocode());
                    if (cache.addOrChangeWaypoint(newWaypoint, true)) {
                        addWaypointAndSort(sortedWaypoints, newWaypoint);
                        adapter.notifyDataSetChanged();
                        activity.reinitializePage(Page.WAYPOINTS.id);
                        ActivityMixin.showShortToast(activity, getString(R.string.waypoint_added));
                    }
                });

                headerBinding.hideVisitedWaypoints.setChecked(Settings.getHideVisitedWaypoints());
                headerBinding.hideVisitedWaypoints.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        Settings.setHideVisitedWaypoints(isChecked);

                        final List<Waypoint> sortedWaypoints2 = createSortedWaypointList();
                        Collections.sort(sortedWaypoints2, cache.getWaypointComparator());

                        adapter.clear();
                        adapter.addAll(sortedWaypoints2);
                        adapter.notifyDataSetChanged();
                        activity.reinitializePage(Page.WAYPOINTS.id);
                    }
                });


                // read waypoint from clipboard
                setClipboardButtonVisibility(headerBinding.addWaypointFromclipboard);
                headerBinding.addWaypointFromclipboard.setOnClickListener(v2 -> {
                    final Waypoint oldWaypoint = DataStore.loadWaypoint(Waypoint.hasClipboardWaypoint());
                    if (null != oldWaypoint) {
                        activity.ensureSaved();
                        final Waypoint newWaypoint = cache.duplicateWaypoint(oldWaypoint, cache.getGeocode().equals(oldWaypoint.getGeocode()));
                        if (null != newWaypoint) {
                            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                            addWaypointAndSort(sortedWaypoints, newWaypoint);
                            adapter.notifyDataSetChanged();
                            activity.reinitializePage(Page.WAYPOINTS.id);
                            if (oldWaypoint.isUserDefined()) {
                                SimpleDialog.of((Activity) v.getContext()).setTitle(R.string.cache_waypoints_add_fromclipboard).setMessage(R.string.cache_waypoints_remove_original_waypoint).confirm((dialog, which) -> {
                                    DataStore.deleteWaypoint(oldWaypoint.getId());
                                    ClipboardUtils.clearClipboard();
                                });
                            }
                        }
                    }
                });

                final ClipboardManager cliboardManager = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                cliboardManager.addPrimaryClipChangedListener(() -> setClipboardButtonVisibility(headerBinding.addWaypointFromclipboard));
            }
        }

        protected void fillViewHolder(final CacheDetailActivity activity, final View rowView, final WaypointViewHolder holder, final Waypoint wpt) {
            // coordinates
            final TextView coordinatesView = holder.binding.coordinates;
            final TextView calculatedCoordinatesView = holder.binding.calculatedCoordinates;
            final Geopoint coordinates = wpt.getCoords();
            final String calcStateJson = wpt.getCalcStateJson();

            // coordinates
            holder.setCoordinate(coordinates);
            activity.addContextMenu(coordinatesView);
            coordinatesView.setVisibility(null != coordinates ? View.VISIBLE : View.GONE);
            calculatedCoordinatesView.setVisibility(null != calcStateJson ? View.VISIBLE : View.GONE);

            // info
            final String waypointInfo = Formatter.formatWaypointInfo(wpt);
            final TextView infoView = holder.binding.info;
            if (StringUtils.isNotBlank(waypointInfo)) {
                infoView.setText(waypointInfo);
                infoView.setVisibility(View.VISIBLE);
            } else {
                infoView.setVisibility(View.GONE);
            }

            // title
            holder.binding.name.setText(StringUtils.isNotBlank(wpt.getName()) ? StringEscapeUtils.unescapeHtml4(wpt.getName()) : coordinates != null ? coordinates.toString() : getString(R.string.waypoint));
            holder.binding.textIcon.setImageDrawable(MapMarkerUtils.getWaypointMarker(activity.res, wpt, false).getDrawable());

            // visited
            /* @todo
            if (wpt.isVisited()) {
                final TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(R.attr.text_color_grey, typedValue, true);
                if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    // really should be just a color!
                    nameView.setTextColor(typedValue.data);
                }
            }
            */

            // note
            final TextView noteView = holder.binding.note;
            if (StringUtils.isNotBlank(wpt.getNote())) {
                noteView.setOnClickListener(new DecryptTextClickListener(noteView));
                noteView.setVisibility(View.VISIBLE);
                if (TextUtils.containsHtml(wpt.getNote())) {
                    noteView.setText(HtmlCompat.fromHtml(wpt.getNote(), HtmlCompat.FROM_HTML_MODE_LEGACY, new SmileyImage(cache.getGeocode(), noteView), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
                } else {
                    noteView.setText(wpt.getNote());
                }
            } else {
                noteView.setVisibility(View.GONE);
            }

            // user note
            final TextView userNoteView = holder.binding.userNote;
            if (StringUtils.isNotBlank(wpt.getUserNote()) && !StringUtils.equals(wpt.getNote(), wpt.getUserNote())) {
                userNoteView.setOnClickListener(new DecryptTextClickListener(userNoteView));
                userNoteView.setVisibility(View.VISIBLE);
                userNoteView.setText(wpt.getUserNote());
            } else {
                userNoteView.setVisibility(View.GONE);
            }

            final View wpNavView = holder.binding.wpDefaultNavigation;
            wpNavView.setOnClickListener(v -> NavigationAppFactory.startDefaultNavigationApplication(1, activity, wpt));
            wpNavView.setOnLongClickListener(v -> {
                NavigationAppFactory.startDefaultNavigationApplication(2, activity, wpt);
                return true;
            });

            activity.addContextMenu(rowView);
            rowView.setOnClickListener(v -> {
                activity.selectedWaypoint = wpt;
                activity.ensureSaved();
                EditWaypointActivity.startActivityEditWaypoint(activity, cache, wpt.getId());
                activity.refreshOnResume = true;
            });

            rowView.setOnLongClickListener(v -> {
                activity.selectedWaypoint = wpt;
                activity.openContextMenu(v);
                return true;
            });
        }
    }

    public static class InventoryViewCreator extends TabbedViewPagerFragment<CachedetailInventoryPageBinding> {

        @Override
        public CachedetailInventoryPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return CachedetailInventoryPageBinding.inflate(inflater, container, false);
        }

        @Override
        public long getPageId() {
            return Page.INVENTORY.id;
        }

        @Override
        public void setContent() {
            // retrieve activity and cache - if either if this is null, something is really wrong...
            final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
            if (activity == null) {
                return;
            }
            final Geocache cache = activity.getCache();
            if (cache == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);

            // TODO: fix layout, then switch back to Android-resource and delete copied one
            // this copy is modified to respect the text color
            RecyclerViewProvider.provideRecyclerView(activity, binding.getRoot(), true, true);
            cache.mergeInventory(activity.genericTrackables, activity.processedBrands);
            final TrackableListAdapter adapterTrackables = new TrackableListAdapter(cache.getInventory(), trackable -> TrackableActivity.startActivity(activity, trackable.getGuid(), trackable.getGeocode(), trackable.getName(), cache.getGeocode(), trackable.getBrand().getId()));
            binding.getRoot().setAdapter(adapterTrackables);
            cache.mergeInventory(activity.genericTrackables, activity.processedBrands);
        }
    }

    public static class ImagesViewCreator extends TabbedViewPagerFragment<CachedetailImagesPageBinding> {

        @Override
        public CachedetailImagesPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return CachedetailImagesPageBinding.inflate(inflater, container, false);
        }

        @Override
        public long getPageId() {
            return Page.IMAGES.id;
        }

        @Override
        public void setContent() {
            // retrieve activity and cache - if either if this is null, something is really wrong...
            final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
            if (activity == null) {
                return;
            }
            final Geocache cache = activity.getCache();
            if (cache == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);

            if (activity.imagesList == null) {
                activity.imagesList = new ImagesList(activity, cache.getGeocode(), cache);
                activity.createDisposables.add(activity.imagesList.loadImages(binding.getRoot(), cache.getNonStaticImages()));
            }
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
    public void onSupportActionModeStarted(@NonNull final ActionMode mode) {
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
    public void onSupportActionModeFinished(@NonNull final ActionMode mode) {
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
        view.setOnLongClickListener(v -> {
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

                private boolean prepareClipboardActionMode(final View view1, final ActionMode actionMode, final Menu menu) {
                    final int viewId = view1.getId();
                    if (viewId == R.id.value) { // coordinates, gc-code, name
                        clickedItemText = ((TextView) view1).getText();
                        final CharSequence itemTitle = ((TextView) ((View) view1.getParent()).findViewById(R.id.name)).getText();
                        if (itemTitle.equals(res.getText(R.string.cache_coordinates))) {
                            clickedItemText = GeopointFormatter.reformatForClipboard(clickedItemText);
                        }
                        buildDetailsContextMenu(actionMode, menu, itemTitle, true);
                    } else if (viewId == R.id.description) {
                        // combine short and long description
                        final String shortDesc = cache.getShortDescription();
                        if (StringUtils.isBlank(shortDesc)) {
                            clickedItemText = cache.getDescription();
                        } else {
                            clickedItemText = shortDesc + "\n\n" + cache.getDescription();
                        }
                        buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_description), false);
                    } else if (viewId == R.id.personalnote) {
                        clickedItemText = cache.getPersonalNote();
                        buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_personal_note), true);
                    } else if (viewId == R.id.hint) {
                        clickedItemText = cache.getHint();
                        buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_hint), false);
                    } else if (viewId == R.id.log) {
                        clickedItemText = ((TextView) view1).getText();
                        buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_logs), false);
                    } else if (viewId == R.id.date) { // event date
                        clickedItemText = Formatter.formatHiddenDate(cache);
                        buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_event), true);
                        menu.findItem(R.id.menu_calendar).setVisible(cache.canBeAddedToCalendar());
                    } else if (viewId == R.id.coordinates) {
                        clickedItemText = ((TextView) view1).getText();
                        clickedItemText = GeopointFormatter.reformatForClipboard(clickedItemText);
                        buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_coordinates), true);
                    } else {
                        return false;
                    }
                    return true;
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
                    // detail fields
                    if (menuItem.getItemId() == R.id.menu_calendar) {
                        CalendarAdder.addToCalendar(CacheDetailActivity.this, cache);
                        actionMode.finish();
                        return true;
                        // handle clipboard actions in base
                    }
                    return onClipboardItemSelected(actionMode, menuItem, clickedItemText, cache);
                }
            });
            return true;
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

        final AlertDialog.Builder builder = Dialogs.newBuilder(this);
        builder.setTitle(R.string.waypoint_reset_cache_coords);

        final String[] items = { res.getString(R.string.waypoint_localy_reset_cache_coords), res.getString(R.string.waypoint_reset_local_and_remote_cache_coords) };
        builder.setSingleChoiceItems(items, 0, (dialog, which) -> {
            dialog.dismiss();
            final ProgressDialog progressDialog = ProgressDialog.show(CacheDetailActivity.this, getString(R.string.cache), getString(R.string.waypoint_reset), true);
            final HandlerResetCoordinates handler = new HandlerResetCoordinates(CacheDetailActivity.this, progressDialog, which == 1);
            resetCoords(cache, handler, wpt, which == 0 || which == 1, which == 1, progressDialog);
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
                final CacheDetailActivity activity = getReference();
                if (activity != null) {
                    activity.notifyDataSetChanged();
                }
            }
        }
    }

    private void resetCoords(final Geocache cache, final Handler handler, final Waypoint wpt, final boolean local, final boolean remote, final ProgressDialog progress) {
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
            if (local) {
                runOnUiThread(() -> progress.setMessage(res.getString(R.string.waypoint_reset_cache_coords)));
                cache.setCoords(wpt.getCoords());
                cache.setUserModifiedCoords(false);
                cache.deleteWaypointForce(wpt);
                DataStore.saveUserModifiedCoords(cache);
                handler.sendEmptyMessage(HandlerResetCoordinates.LOCAL);
            }

            final IConnector con = ConnectorFactory.getConnector(cache);
            if (remote && con.supportsOwnCoordinates()) {
                runOnUiThread(() -> progress.setMessage(res.getString(R.string.waypoint_coordinates_being_reset_on_website)));

                final boolean result = con.deleteModifiedCoordinates(cache);

                runOnUiThread(() -> {
                    if (result) {
                        showToast(getString(R.string.waypoint_coordinates_has_been_reset_on_website));
                    } else {
                        showToast(getString(R.string.waypoint_coordinates_upload_error));
                    }
                    handler.sendEmptyMessage(HandlerResetCoordinates.ON_WEBSITE);
                    notifyDataSetChanged();
                });

            }
        });
    }

    @Override
    protected String getTitle(final long pageId) {
        // show number of waypoints directly in waypoint title
        if (pageId == Page.WAYPOINTS.id) {
            final int waypointCount = cache == null ? 0 : cache.getWaypoints().size();
            return String.format(getString(R.string.waypoints_tabtitle), waypointCount);
        } else if (pageId == Page.VARIABLES.id) {
            final int varCount = cache == null ? 0 : cache.getVariables().getVariableList().size();
            return this.getString(Page.VARIABLES.titleStringId) + " (" + varCount + ")";
        }
        return this.getString(Page.find(pageId).titleStringId);
    }

    protected long[] getOrderedPages() {
        final ArrayList<Long> pages = new ArrayList<>();
        if (!BranchDetectionHelper.isProductionBuild()) {
            pages.add(Page.VARIABLES.id);
        }
        pages.add(Page.WAYPOINTS.id);
        pages.add(Page.DETAILS.id);
        pages.add(Page.DESCRIPTION.id);
        // enforce showing the empty log book if new entries can be added
        if (cache != null) {
            if (cache.supportsLogging() || !cache.getLogs().isEmpty()) {
                pages.add(Page.LOGS.id);
            }
            if (CollectionUtils.isNotEmpty(cache.getFriendsLogs()) && Settings.isFriendLogsWanted()) {
                pages.add(Page.LOGSFRIENDS.id);
            }
            if (CollectionUtils.isNotEmpty(cache.getInventory()) || CollectionUtils.isNotEmpty(genericTrackables)) {
                pages.add(Page.INVENTORY.id);
            }
            if (CollectionUtils.isNotEmpty(cache.getNonStaticImages())) {
                pages.add(Page.IMAGES.id);
            }
        }
        final long[] result = new long[pages.size()];
        for (int i = 0; i < pages.size(); i++) {
            result[i] = pages.get(i);
        }
        return result;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected TabbedViewPagerFragment createNewFragment(final long pageId) {
        if (pageId == Page.DETAILS.id) {
            return new DetailsViewCreator();
        } else if (pageId == Page.DESCRIPTION.id) {
            return new DescriptionViewCreator();
        } else if (pageId == Page.LOGS.id) {
            return CacheLogsViewCreator.newInstance(true);
        } else if (pageId == Page.LOGSFRIENDS.id) {
            return CacheLogsViewCreator.newInstance(false);
        } else if (pageId == Page.WAYPOINTS.id) {
            return new WaypointsViewCreator();
        } else if (pageId == Page.INVENTORY.id) {
            return new InventoryViewCreator();
        } else if (pageId == Page.IMAGES.id) {
            return new ImagesViewCreator();
        } else if (pageId == Page.VARIABLES.id) {
            return new VariablesViewPageFragment();
        }
        throw new IllegalStateException(); // cannot happen as long as switch case is enum complete
    };

    @SuppressLint("SetTextI18n")
    static boolean setOfflineHintText(final OnClickListener showHintClickListener, final TextView offlineHintTextView, final String hint, final String personalNote) {
        if (null != showHintClickListener) {
            final boolean hintGiven = StringUtils.isNotEmpty(hint);
            final boolean personalNoteGiven = StringUtils.isNotEmpty(personalNote);
            if (hintGiven || personalNoteGiven) {
                offlineHintTextView.setText((hintGiven ? hint + (personalNoteGiven ? "\r\n" : "") : "") + (personalNoteGiven ? personalNote : ""));
                return true;
            }
        }
        return false;
    }

    static void updateOfflineBox(final View view, final Geocache cache, final Resources res,
            final OnClickListener refreshCacheClickListener,
            final OnClickListener dropCacheClickListener,
            final OnClickListener storeCacheClickListener,
            final OnClickListener showHintClickListener,
            final OnLongClickListener moveCacheListener,
            final OnLongClickListener storeCachePreselectedListener) {
        // offline use
        final TextView offlineText = view.findViewById(R.id.offline_text);
        final View offlineRefresh = view.findViewById(R.id.offline_refresh);
        final View offlineStore = view.findViewById(R.id.offline_store);
        final View offlineDrop = view.findViewById(R.id.offline_drop);
        final View offlineEdit = view.findViewById(R.id.offline_edit);

        // check if hint is available and set onClickListener and hint button visibility accordingly
        final boolean hintButtonEnabled = setOfflineHintText(showHintClickListener, view.findViewById(R.id.offline_hint_text), cache.getHint(), cache.getPersonalNote());
        final View offlineHint = view.findViewById(R.id.offline_hint);
        if (null != offlineHint) {
            if (hintButtonEnabled) {
                offlineHint.setVisibility(View.VISIBLE);
                offlineHint.setClickable(true);
                offlineHint.setOnClickListener(showHintClickListener);
            } else {
                offlineHint.setVisibility(View.GONE);
                offlineHint.setClickable(false);
                offlineHint.setOnClickListener(null);
            }
        }

        offlineStore.setClickable(true);
        offlineStore.setOnClickListener(storeCacheClickListener);
        offlineStore.setOnLongClickListener(storeCachePreselectedListener);

        offlineDrop.setClickable(true);
        offlineDrop.setOnClickListener(dropCacheClickListener);
        offlineDrop.setOnLongClickListener(null);

        offlineEdit.setOnClickListener(storeCacheClickListener);
        if (moveCacheListener != null) {
            offlineEdit.setOnLongClickListener(moveCacheListener);
        }

        offlineRefresh.setVisibility(cache.supportsRefresh() ? View.VISIBLE : View.GONE);
        offlineRefresh.setClickable(true);
        offlineRefresh.setOnClickListener(refreshCacheClickListener);

        if (cache.isOffline()) {
            offlineText.setText(Formatter.formatStoredAgo(cache.getDetailedUpdate()));

            offlineStore.setVisibility(View.GONE);
            offlineDrop.setVisibility(View.VISIBLE);
            offlineEdit.setVisibility(View.VISIBLE);
        } else {
            offlineText.setText(res.getString(R.string.cache_offline_not_ready));

            offlineStore.setVisibility(View.VISIBLE);
            offlineDrop.setVisibility(View.GONE);
            offlineEdit.setVisibility(View.GONE);
        }
    }

    static void updateCacheLists(final View view, final Geocache cache, final Resources res) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (final Integer listId : cache.getLists()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            appendClickableList(builder, view, listId);
        }
        builder.insert(0, res.getString(R.string.list_list_headline) + " ");
        final TextView offlineLists = view.findViewById(R.id.offline_lists);
        offlineLists.setText(builder);
        offlineLists.setMovementMethod(LinkMovementMethod.getInstance());
    }

    static void appendClickableList(final SpannableStringBuilder builder, final View view, final Integer listId) {
        final int start = builder.length();
        builder.append(DataStore.getList(listId).getTitle());
        builder.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull final View widget) {
                Settings.setLastDisplayedList(listId);
                CacheListActivity.startActivityOffline(view.getContext());
            }
        }, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> cache.store(listIds, storeCacheHandler));
    }

    public static void editPersonalNote(final Geocache cache, final CacheDetailActivity activity) {
        final FragmentManager fm = activity.getSupportFragmentManager();
        final EditNoteDialog dialog = EditNoteDialog.newInstance(cache.getPersonalNote(), cache.isPreventWaypointsFromNote());
        dialog.show(fm, "fragment_edit_note");
    }

    @Override
    public void onFinishEditNoteDialog(final String note, final boolean preventWaypointsFromNote) {
        setNewPersonalNote(note, preventWaypointsFromNote);

    }

    public void removeWaypointsFromPersonalNote(final Geocache cache) {
        final String note = cache.getPersonalNote() == null ? "" : cache.getPersonalNote();
        final String newNote = WaypointParser.removeParseableWaypointsFromText(note);
        if (newNote != null) {
            setNewPersonalNote(newNote);
        }
        showShortToast(note.equals(newNote) ? R.string.cache_personal_note_removewaypoints_nowaypoints : R.string.cache_personal_note_removedwaypoints);
    }

    public void storeWaypointsInPersonalNote(final Geocache cache, final int maxPersonalNotesChars) {
        final String note = cache.getPersonalNote() == null ? "" : cache.getPersonalNote();

        //only user modified waypoints
        final List<Waypoint> userModifiedWaypoints = new ArrayList<>();
        for (Waypoint w : cache.getWaypoints()) {
            if (w.isUserModified()) {
                userModifiedWaypoints.add(w);
            }
        }
        if (userModifiedWaypoints.isEmpty()) {
            showShortToast(getString(R.string.cache_personal_note_storewaypoints_nowaypoints));
            return;
        }

        //if given maxSize is obviously bogus, then make length unlimited
        final int maxSize = maxPersonalNotesChars == 0 ? -1 : maxPersonalNotesChars;

        final String newNote = WaypointParser.putParseableWaypointsInText(note, userModifiedWaypoints, maxSize);

        if (newNote != null) {
            setNewPersonalNote(newNote);
            final String newNoteNonShorted = WaypointParser.putParseableWaypointsInText(note, userModifiedWaypoints, -1);
            if (newNoteNonShorted.length() > newNote.length()) {
                showShortToast(getString(R.string.cache_personal_note_storewaypoints_success_limited, maxSize));
            } else {
                showShortToast(getString(R.string.cache_personal_note_storewaypoints_success));
            }
        } else {
            showShortToast(getString(R.string.cache_personal_note_storewaypoints_failed, maxSize));
        }

    }

    private void setNewPersonalNote(final String newNote) {
        setNewPersonalNote(newNote, cache.isPreventWaypointsFromNote());
    }

    /**
     * Internal method to set new personal note and update all corresponding entities (DB, dialogs etc)
     *
     * @param newNote                     new note to set
     * @param newPreventWaypointsFromNote new preventWaypointsFromNote flag to set
     */
    private void setNewPersonalNote(final String newNote, final boolean newPreventWaypointsFromNote) {
        cache.setPersonalNote(newNote);
        cache.setPreventWaypointsFromNote(newPreventWaypointsFromNote);
        if (cache.addWaypointsFromNote()) {
            reinitializePage(Page.WAYPOINTS.id);
            /* @todo mb Does above line work?
            final PageViewCreator wpViewCreator = getViewCreator(Page.WAYPOINTS);
            if (wpViewCreator != null) {
                wpViewCreator.notifyDataSetChanged();
            }
            */
        }
        setMenuPreventWaypointsFromNote(cache.isPreventWaypointsFromNote());

        final TextView personalNoteView = findViewById(R.id.personalnote);
        final View separator = findViewById(R.id.personalnote_button_separator);
        if (personalNoteView != null) {
            setPersonalNote(personalNoteView, separator, newNote);
        } else {
            reinitializePage(Page.DESCRIPTION.id);
        }

        Schedulers.io().scheduleDirect(() -> DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB)));
    }

    private static void setPersonalNote(final TextView personalNoteView, final View separator, final String personalNote) {
        personalNoteView.setText(personalNote, TextView.BufferType.SPANNABLE);
        if (StringUtils.isNotBlank(personalNote)) {
            personalNoteView.setVisibility(View.VISIBLE);
            separator.setVisibility(View.VISIBLE);
            Linkify.addLinks(personalNoteView, Linkify.MAP_ADDRESSES | Linkify.WEB_URLS);
        } else {
            personalNoteView.setVisibility(View.GONE);
            separator.setVisibility(View.GONE);
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
