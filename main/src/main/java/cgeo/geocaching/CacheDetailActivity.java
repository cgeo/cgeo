package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.INavigationSource;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.activity.TabbedViewPagerActivity;
import cgeo.geocaching.activity.TabbedViewPagerFragment;
import cgeo.geocaching.apps.cache.WhereYouGoApp;
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
import cgeo.geocaching.contacts.ContactsHelper;
import cgeo.geocaching.contacts.IContactCardProvider;
import cgeo.geocaching.databinding.CachedetailDescriptionPageBinding;
import cgeo.geocaching.databinding.CachedetailDetailsPageBinding;
import cgeo.geocaching.databinding.CachedetailImagegalleryPageBinding;
import cgeo.geocaching.databinding.CachedetailInventoryPageBinding;
import cgeo.geocaching.databinding.CachedetailWaypointsHeaderBinding;
import cgeo.geocaching.databinding.CachedetailWaypointsPageBinding;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheAttributeCategory;
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
import cgeo.geocaching.models.CacheArtefactParser;
import cgeo.geocaching.models.CalculatedCoordinate;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.permission.PermissionAction;
import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;
import cgeo.geocaching.ui.DecryptTextClickListener;
import cgeo.geocaching.ui.FastScrollListener;
import cgeo.geocaching.ui.ImageGalleryView;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ToggleItemType;
import cgeo.geocaching.ui.TrackableListAdapter;
import cgeo.geocaching.ui.UserClickListener;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.EditNoteDialog;
import cgeo.geocaching.ui.dialog.EditNoteDialog.EditNoteDialogListener;
import cgeo.geocaching.ui.dialog.NewCoordinateInputDialog;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CacheUtils;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.CheckerUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.MenuUtils;
import cgeo.geocaching.utils.OfflineTranslateUtils;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.ProgressBarDisposableHandler;
import cgeo.geocaching.utils.ProgressButtonDisposableHandler;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.SimpleDisposableHandler;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.html.HtmlStyle;
import cgeo.geocaching.utils.html.HtmlUtils;
import cgeo.geocaching.utils.html.UnknownTagsHandler;
import cgeo.geocaching.wherigo.WherigoActivity;
import cgeo.geocaching.wherigo.WherigoUtils;
import cgeo.geocaching.wherigo.WherigoViewUtils;
import static cgeo.geocaching.apps.cache.WhereYouGoApp.isWhereYouGoInstalled;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Pair;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.FragmentManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.android.material.button.MaterialButton;
import com.google.mlkit.nl.translate.Translator;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Activity to handle all single-cache-stuff.
 * <br>
 * e.g. details, description, logs, waypoints, inventory, variables...
 */
public class CacheDetailActivity extends TabbedViewPagerActivity
        implements IContactCardProvider, CacheMenuHandler.ActivityInterface, INavigationSource, EditNoteDialogListener {

    private static final int MESSAGE_FAILED = -1;
    private static final int MESSAGE_SUCCEEDED = 1;

    private static final String EXTRA_FORCE_WAYPOINTSPAGE = "cgeo.geocaching.extra.cachedetail.forceWaypointsPage";
    private static final String EXTRA_EDIT_PERSONALNOTE = "cgeo.geocaching.extra.cachedetail.editPersonalNote";

    public static final String STATE_PAGE_INDEX = "cgeo.geocaching.pageIndex";
    public static final String STATE_IMAGE_GALLERY = "cgeo.geocaching.imageGallery";
    public static final String STATE_DESCRIPTION_STYLE = "cgeo.geocaching.descriptionStyle";
    public static final String STATE_SPEECHSERVICE_RUNNING = "cgeo.geocaching.speechServiceRunning";
    public static final String STATE_TRANSLATION_LANGUAGE_SOURCE = "cgeo.geocaching.translation.languageSource";


    // Store Geocode here, as 'cache' is loaded Async.
    private String geocode;
    private Geocache cache;
    private boolean restartSpeechService = false;
    @NonNull
    private final List<Trackable> genericTrackables = new ArrayList<>();
    private final Progress progress = new Progress();

    private SearchResult search;

    private GeoDirHandler locationUpdater;

    private MenuItem menuItemToggleWaypointsFromNote = null;

    /**
     * If another activity is called and can modify the data of this activity, we refresh it on resume.
     */
    private boolean refreshOnResume = false;

    // some views that must be available from everywhere // TODO: Reference can block GC?
    private TextView cacheDistanceView;

    private ImageGalleryView imageGallery;
    private int imageGalleryPos = -1;

    private int imageGalleryResultRequestCode = -1;
    private int imageGalleryResultResultCode = -1;
    private Intent imageGalleryData;
    private Bundle imageGalleryState = null;

    private final CompositeDisposable createDisposables = new CompositeDisposable();
    /**
     * waypoint selected in context menu. This variable will be gone when the waypoint context menu is a fragment.
     */
    private Waypoint selectedWaypoint;

    private boolean requireGeodata;
    private final CompositeDisposable geoDataDisposable = new CompositeDisposable();
    private boolean lastActionWasEditNote = false;

    private final EnumSet<TrackableBrand> processedBrands = EnumSet.noneOf(TrackableBrand.class);

    private final PermissionAction<String> openContactCardAction = PermissionAction.register(this, PermissionContext.SEARCH_USER_IN_CONTACTS, user -> new ContactsHelper(this).openContactCard(user));
    private boolean activityIsStartedForEditNote = false;

    private HtmlStyle descriptionStyle = HtmlStyle.DEFAULT;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.tabbed_viewpager_activity_refreshable);

        // get parameters
        final Bundle extras = getIntent().getExtras();
        final Uri uri = getIntent().getData();

        // try to get data from extras
        String name = null;
        String guid = null;
        boolean forceWaypointsPage = false;
        boolean forceEditPersonalNote = false;

        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            name = extras.getString(Intents.EXTRA_NAME);
            guid = extras.getString(Intents.EXTRA_GUID);
            forceWaypointsPage = extras.getBoolean(EXTRA_FORCE_WAYPOINTSPAGE);
            forceEditPersonalNote = extras.getBoolean(EXTRA_EDIT_PERSONALNOTE);
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

        if (forceEditPersonalNote) {
            progress.setOnDismissListener((dialog) -> {
                activityIsStartedForEditNote = true;
                editPersonalNote(cache, this);
            });
        }

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
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_DESCRIPTION_STYLE)) {
                this.descriptionStyle = CommonUtils.intToEnum(HtmlStyle.class, savedInstanceState.getInt(STATE_DESCRIPTION_STYLE), this.descriptionStyle);
            }
            restartSpeechService = (savedInstanceState.getInt(STATE_SPEECHSERVICE_RUNNING, 0) > 0);

            if (savedInstanceState.containsKey(STATE_TRANSLATION_LANGUAGE_SOURCE)) {
                final OfflineTranslateUtils.Language newLanguage = new OfflineTranslateUtils.Language(savedInstanceState.getString(STATE_TRANSLATION_LANGUAGE_SOURCE));
                if (newLanguage.isValid()) {
                    translationStatus.setSourceLanguage(newLanguage);
                    translationStatus.setNeedsRetranslation();
                }
            }
        }

        imageGalleryState = savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_IMAGE_GALLERY);

        createViewPager(pageToOpen, getOrderedPages(), currentPageId -> {
            if (Settings.isOpenLastDetailsPage()) {
                Settings.setLastDetailsPage((int) (long) currentPageId);
            }
            requireGeodata = currentPageId == Page.DETAILS.id;
            // resume location access
            startOrStopGeoDataListener(false);
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


        // get notified on async cache changes (e.g.: waypoint creation from map or background refresh)
        getLifecycle().addObserver(new GeocacheChangedBroadcastReceiver(this, true) {
            @Override
            protected void onReceive(final Context context, final String geocode) {
                if (cache != null && cache.getGeocode().equals(geocode)) {
                    notifyDataSetChanged();
                }
            }
        });

        // "go to" functionality deprecation notice
        if (StringUtils.equals(geocode, InternalConnector.GEOCODE_HISTORY_CACHE)) {
            Dialogs.basicOneTimeMessage(this, OneTimeDialogs.DialogType.GOTO_DEPRECATION_NOTICE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_PAGE_INDEX, getCurrentPageId());
        outState.putBundle(STATE_IMAGE_GALLERY, imageGallery == null ? null : imageGallery.getState());
        outState.putInt(STATE_DESCRIPTION_STYLE, CommonUtils.enumToInt(this.descriptionStyle));
        outState.putInt(STATE_SPEECHSERVICE_RUNNING, SpeechService.isRunning() ? 1 : 0);

        outState.putString(STATE_TRANSLATION_LANGUAGE_SOURCE, this.translationStatus.isTranslated() ? this.translationStatus.getSourceLanguage().getCode() : "");
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
        if (cache != null) {
            cache.setChangeNotificationHandler(null);
        }
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
            menu.findItem(R.id.menu_waypoint_geofence).setVisible(selectedWaypoint.canChangeGeofence());
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
            menu.findItem(R.id.menu_waypoint_open_geochecker).setVisible(CheckerUtils.getCheckerUrl(cache) != null);
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
                setNeedsRefresh();
            }
        } else if (itemId == R.id.menu_waypoint_geofence) {
            if (selectedWaypoint != null) {
                ensureSaved();
                SimpleDialog.of(this)
                        .setTitle(TextParam.id(R.string.waypoint_geofence))
                        .setMessage(TextParam.id(R.string.waypoint_geofence_summary))
                        .input(
                                new SimpleDialog.InputOptions().setInitialValue(String.valueOf(selectedWaypoint.getGeofence())).setInputType(InputType.TYPE_CLASS_NUMBER),
                                result -> {
                                    float newValue = 0;
                                    try {
                                        newValue = Float.parseFloat(result);
                                    } catch (NumberFormatException ignore) {
                                        // ignore
                                    }
                                    selectedWaypoint.setGeofence(newValue);
                                    saveAndNotify();
                                }
                );
                refreshOnResume = true;
            }
        } else if (itemId == R.id.menu_waypoint_visited) {
            if (selectedWaypoint != null) {
                ensureSaved();
                AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> {
                    selectedWaypoint.setVisited(true);
                    saveAndNotify();
                }, this::notifyDataSetChanged);
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
            AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> {
                if (cache.duplicateWaypoint(selectedWaypoint, true) != null) {
                    saveAndNotify();
                    return true;
                }
                return false;
            }, result -> {
                if (result) {
                    notifyDataSetChanged();
                }
            });
        } else if (itemId == R.id.menu_waypoint_toclipboard) {
            if (selectedWaypoint != null) {
                ensureSaved();
                ClipboardUtils.copyToClipboard(selectedWaypoint.reformatForClipboard());
                showToast(getString(R.string.clipboard_copy_ok));
            }
        } else if (itemId == R.id.menu_waypoint_open_geochecker) {
            if (selectedWaypoint != null) {
                ensureSaved();
                final Pair<String, CheckerUtils.GeoChecker> geocheckerData = CheckerUtils.getCheckerData(cache, selectedWaypoint.getCoords());
                if (geocheckerData != null) {
                    if (!geocheckerData.second.allowsCoordinate()) {
                        final Geopoint coordinates = selectedWaypoint.getCoords();
                        if (coordinates != null) {
                            ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(coordinates.toString()));
                        }
                    }
                    ShareUtils.openUrl(this, geocheckerData.first, true);
                }
            }
        } else if (itemId == R.id.menu_waypoint_delete) {
            ensureSaved();
            final int waypointPos = WaypointsViewCreator.indexOfWaypoint(cache, selectedWaypoint);
            AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, () -> {
                if (cache.deleteWaypoint(selectedWaypoint)) {
                    saveAndNotify();
                    return true;
                }
                return false;
            }, result -> {
                if (result) {
                    if (waypointPos > 0) {
                        selectedWaypoint = WaypointsViewCreator.createSortedWaypointList(cache).get(waypointPos - 1); // set new list position to avoid jumping to the top
                    }
                    notifyDataSetChanged();
                    GeocacheChangedBroadcastReceiver.sendBroadcast(CacheDetailActivity.this, cache.getGeocode());
                }
            });
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
                final HandlerResetCoordinates handler = new HandlerResetCoordinates(this, false);
                handler.showProgress();
                resetCoords(cache, handler, selectedWaypoint, true, false);
            }
        } else if (itemId == R.id.menu_calendar) {
            CalendarAdder.addToCalendar(this, cache);
        }
        return true;
    }

    @Override
    public void showContactCard(final String userName) {
        openContactCardAction.launch(userName);
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
            CacheDetailActivity.saveAndNotify(getContext(), cache);
        }

        @Override
        protected void undoCommand() {
            waypoint.setCoords(coords);
            CacheDetailActivity.saveAndNotify(getContext(), cache);
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
        ToggleItemType.WAYPOINTS_FROM_NOTE.toggleMenuItem(menuItemToggleWaypointsFromNote, preventWaypointsFromNote);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final IConnector connector = null != cache ? ConnectorFactory.getConnector(cache) : null;
        final boolean isUDC = null != connector && connector.equals(InternalConnector.getInstance());

        CacheMenuHandler.onPrepareOptionsMenu(menu, cache, false);
        LoggingUI.onPrepareOptionsMenu(menu, cache);
        MenuUtils.setVisible(menu.findItem(R.id.menu_set_coordinates), isUDC);

        if (cache != null) {
            // top level menu items

            final MenuItem ttsMenuItem = menu.findItem(R.id.menu_tts_toggle);
            ttsMenuItem.setVisible(!cache.isGotoHistoryUDC());
            ToggleItemType.TOGGLE_SPEECH.toggleMenuItem(ttsMenuItem, SpeechService.isRunning());

            if (connector instanceof PgcChallengeCheckerCapability) {
                menu.findItem(R.id.menu_challenge_checker).setVisible(((PgcChallengeCheckerCapability) connector).isChallengeCache(cache));
            }
            menu.findItem(R.id.menu_edit_fieldnote).setVisible(true);

            // submenu waypoints
            menu.findItem(R.id.menu_delete_userdefined_waypoints).setVisible(cache.isOffline() && cache.hasUserdefinedWaypoints());
            menu.findItem(R.id.menu_delete_generated_waypoints).setVisible(cache.isOffline() && cache.hasGeneratedWaypoints());
            menu.findItem(R.id.menu_extract_waypoints).setVisible(!isUDC);
            menu.findItem(R.id.menu_scan_calculated_waypoints).setVisible(!isUDC);
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

        MenuUtils.enableIconsInOverflowMenu(menu);
        MenuUtils.tintToolbarAndOverflowIcons(menu);

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
        } else if (menuItem == R.id.menu_delete_generated_waypoints) {
            dropGeneratedWaypoints();
        } else if (menuItem == R.id.menu_refresh) {
            refreshCache();
        } else if (menuItem == R.id.menu_gcvote) {
            showVoteDialog();
        } else if (menuItem == R.id.menu_challenge_checker) {
            ShareUtils.openUrl(this, "https://project-gc.com/Challenges/" + cache.getGeocode());
        } else if (menuItem == R.id.menu_ignore) {
            ignoreCache();
        } else if (menuItem == R.id.menu_set_coordinates) {
            setCoordinates(this);
        } else if (menuItem == R.id.menu_extract_waypoints) {
            final String searchText = cache.getShortDescription() + ' ' + cache.getDescription();
            extractWaypoints(searchText, cache);
        } else if (menuItem == R.id.menu_scan_calculated_waypoints) {
            scanForCalculatedWaypoints(cache);
        } else if (menuItem == R.id.menu_toggleWaypointsFromNote) {
            cache.setPreventWaypointsFromNote(!cache.isPreventWaypointsFromNote());
            setMenuPreventWaypointsFromNote(cache.isPreventWaypointsFromNote());
            AndroidRxUtils.andThenOnUi(AndroidRxUtils.computationScheduler, this::saveAndNotify, this::notifyDataSetChanged);
        } else if (menuItem == R.id.menu_clear_goto_history) {
            SimpleDialog.of(this).setTitle(R.string.clear_goto_history_title).setMessage(R.string.clear_goto_history).confirm(() -> AndroidRxUtils.andThenOnUi(Schedulers.io(), DataStore::clearGotoHistory, () -> {
                cache = DataStore.loadCache(InternalConnector.GEOCODE_HISTORY_CACHE, LoadFlags.LOAD_ALL_DB_ONLY);
                notifyDataSetChanged();
            }));
        } else if (menuItem == R.id.menu_add_to_route) {
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.appendToIndividualRoute(cache.getWaypoints()), () -> ActivityMixin.showShortToast(this, R.string.waypoints_appended_to_route));
        } else if (menuItem == R.id.menu_export_gpx) {
            new GpxExport().export(Collections.singletonList(cache), this, cache.getName());
        } else if (menuItem == R.id.menu_export_fieldnotes) {
            new FieldNoteExport().export(Collections.singletonList(cache), this);
        } else if (menuItem == R.id.menu_export_persnotes) {
            new PersonalNoteExport().export(Collections.singletonList(cache), this);
        } else if (menuItem == R.id.menu_edit_fieldnote) {
            editPersonalNote(cache, this);
        } else if (menuItem == R.id.menu_navigate) {
            NavigationAppFactory.onMenuItemSelected(item, this, cache);
        } else if (menuItem == R.id.menu_tts_toggle) {
            SpeechService.toggleService(this, cache.getCoords());
            ToggleItemType.TOGGLE_SPEECH.toggleMenuItem(item, SpeechService.isRunning());
        } else if (menuItem == R.id.menu_set_cache_icon) {
            EmojiUtils.selectEmojiPopup(this, cache.getAssignedEmoji(), cache, this::setCacheIcon);
        } else if (menuItem == R.id.menu_change_description_style) {
            changeDescriptionStyle();
        } else if (LoggingUI.onMenuItemSelected(item, this, cache, null)) {
            setNeedsRefresh();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private static void openGeochecker(final Activity activity, final Geocache cache) {
        ShareUtils.openUrl(activity, CheckerUtils.getCheckerUrl(cache), true);
    }

    private void setCacheIcon(final int newCacheIcon) {
        cache.setAssignedEmoji(newCacheIcon);
        saveAndNotify(LoadFlags.SAVE_ALL);
        ViewUtils.showShortToast(this, R.string.cache_icon_updated);
        notifyDataSetChanged();
    }

    private void ignoreCache() {
        SimpleDialog.of(this).setTitle(R.string.ignore_confirm_title).setMessage(R.string.ignore_confirm_message).confirm(() -> {
            AndroidRxUtils.networkScheduler.scheduleDirect(() -> ((IIgnoreCapability) ConnectorFactory.getConnector(cache)).addToIgnorelist(cache));
            // For consistency, remove also the local cache immediately from memory cache and database
            if (cache.isOffline()) {
                dropCache();
                DataStore.removeCache(cache.getGeocode(), EnumSet.of(RemoveFlag.DB));
            }
        });
    }

    private void setCoordinates(Activity activity) {
        ensureSaved();
        NewCoordinateInputDialog.show(activity,this::onCoordinatesUpdated, cache.getCoords());
    }

    public void onCoordinatesUpdated(final Geopoint input) {
        cache.setCoords(input);
        if (cache.isOffline()) {
            storeCache(cache.getLists());
        } else {
            storeCache(false);
        }
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
                        SimpleDialog.of(activity).setTitle(R.string.cache_status_premium).setMessage(R.string.err_detail_premium_log_found).setPositiveButton(TextParam.id(R.string.cache_menu_visit)).confirm(() -> {
                            LogCacheActivity.startForCreate(activity, activity.geocode);
                            finishActivity();
                        }, this::finishActivity);

                        dismissProgress();

                    } else {
                        activity.showToast(toastPrefix + error.getErrorString());
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
        cache.setChangeNotificationHandler(new ChangeNotificationHandler(this));

        setCacheTitleBar(cache);
        setIsContentRefreshable(cache.supportsRefresh());

        // reset imagesList so Images view page will be redrawn
        imageGallery = null;
        setOrderedPages(getOrderedPages());
        reinitializeViewPager();

        // rendering done! remove progress popup if any there
        invalidateOptionsMenuCompatible();
        progress.dismiss();

        Settings.addCacheToHistory(cache.getGeocode());

        if (CacheDetailActivity.this.restartSpeechService) {
            SpeechService.toggleService(this, cache.getCoords());
            CacheDetailActivity.this.restartSpeechService = false;
        }
    }

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
        NavigationAppFactory.showNavigationMenu(this, cache, null, null, true, true, 0);
    }

    public static void startActivity(final Context context, final String geocode, final boolean forceWaypointsPage) {
        final Intent detailIntent = new Intent(context, CacheDetailActivity.class);
        detailIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        detailIntent.putExtra(EXTRA_FORCE_WAYPOINTSPAGE, forceWaypointsPage);
        context.startActivity(detailIntent);
    }

    public static void startActivityForEditNote(final Context context, final String geocode) {
        final Intent detailIntent = new Intent(context, CacheDetailActivity.class);
        detailIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        detailIntent.putExtra(EXTRA_EDIT_PERSONALNOTE, true);
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
        IMAGEGALLERY(R.string.cache_images),
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
        if (ProgressBarDisposableHandler.isInProgress(this) || progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        if (!Network.isConnected()) {
            showToast(getString(R.string.err_server_general));
            return;
        }

        final RefreshCacheHandler refreshCacheHandler = new RefreshCacheHandler(this);
        refreshCacheHandler.showProgress();

        cache.refresh(refreshCacheHandler, AndroidRxUtils.refreshScheduler);
    }

    private void dropCache() {
        if (ProgressBarDisposableHandler.isInProgress(this) || progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }
        final ChangeNotificationHandler handler = new ChangeNotificationHandler(this);
        handler.showProgress();
        cache.drop(handler);
    }

    private void dropUserdefinedWaypoints() {
        if (null != cache && cache.hasUserdefinedWaypoints()) {
            String info = getString(R.string.cache_delete_userdefined_waypoints_confirm);
            if (!cache.isPreventWaypointsFromNote()) {
                info += "\n\n" + getString(R.string.cache_delete_userdefined_waypoints_note);
            }
            SimpleDialog.of(this).setTitle(R.string.cache_delete_userdefined_waypoints).setMessage(TextParam.text(info)).confirm(() -> {
                for (Waypoint waypoint : new LinkedList<>(cache.getWaypoints())) {
                    if (waypoint.isUserDefined()) {
                        cache.deleteWaypoint(waypoint);
                    }
                }
                if (cache.addCacheArtefactsFromNotes()) {
                    Schedulers.io().scheduleDirect(this::saveAndNotify);
                }
                ActivityMixin.showShortToast(this, R.string.cache_delete_userdefined_waypoints_success);
                invalidateOptionsMenu();
                reinitializePage(Page.WAYPOINTS.id);
            });
        }
    }

    private void changeDescriptionStyle() {
        final SimpleDialog.ItemSelectModel<HtmlStyle> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(Arrays.asList(HtmlStyle.values()))
            .setDisplayMapper((l) -> TextParam.text(l.getTitle()))
            .setSelectedItems(Collections.singleton(descriptionStyle))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO);
        SimpleDialog.of(this).setTitle(R.string.cache_menu_change_description_style)
                .selectSingle(model, ds -> {
                    descriptionStyle = ds;
                    reinitializePage(Page.DESCRIPTION.id);
                });

    }

    private void dropGeneratedWaypoints() {
        if (null != cache && cache.hasGeneratedWaypoints()) {
            final String info = getString(R.string.cache_delete_generated_waypoints_confirm);
            SimpleDialog.of(this).setTitle(R.string.cache_delete_generated_waypoints).setMessage(TextParam.text(info)).confirm(() -> {
                for (Waypoint waypoint : new LinkedList<>(cache.getWaypoints())) {
                    if (waypoint.getWaypointType() == WaypointType.GENERATED) {
                        cache.deleteWaypoint(waypoint);
                    }
                }
                ActivityMixin.showShortToast(this, R.string.cache_delete_generated_waypoints_success);
                invalidateOptionsMenu();
                reinitializePage(Page.WAYPOINTS.id);
            });
        }
    }

    private void storeCache(final boolean fastStoreOnLastSelection) {
        if (ProgressBarDisposableHandler.isInProgress(this) || progress.isShowing()) {
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
        if (ProgressBarDisposableHandler.isInProgress(this) || progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        new MoveToListAndRemoveFromOthersCommand(CacheDetailActivity.this, cache) {

            @Override
            protected void onFinished() {
                updateCacheLists(CacheDetailActivity.this.findViewById(R.id.offline_lists), cache, res, null);
            }
        }.execute();
    }

    private void storeCacheInLists(final Set<Integer> selectedListIds) {
        if (cache.isOffline()) {
            // cache already offline, just add to another list
            DataStore.saveLists(Collections.singletonList(cache), selectedListIds);
            new StoreCacheHandler(CacheDetailActivity.this).sendEmptyMessage(DisposableHandler.DONE);
        } else {
            storeCache(selectedListIds);
        }
    }

    private static final class CheckboxHandler extends ProgressButtonDisposableHandler {
        private final WeakReference<DetailsViewCreator> creatorRef;
        private final WeakReference<CacheDetailActivity> activityWeakReference;

        CheckboxHandler(final DetailsViewCreator creator, final CacheDetailActivity activity) {
            super(activity);
            creatorRef = new WeakReference<>(creator);
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleRegularMessage(final Message message) {
            final DetailsViewCreator creator = creatorRef.get();
            if (creator != null) {
                super.handleRegularMessage(message);
                creator.updateWatchlistBox(activityWeakReference.get());
                creator.updateFavPointBox(activityWeakReference.get());
            }
        }
    }

    /**
     * Creator for details-view.
     * <br>
     * TODO: Extract inner class to own file for a better overview. Same might apply to all other view creators.
     */
    public static class DetailsViewCreator extends TabbedViewPagerFragment<CachedetailDetailsPageBinding> {
        private CacheDetailsCreator.NameValueLine favoriteLine;
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
        // splitting up that method would not help improve readability
        @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
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
            // Not using colored cache names at this place to have at least one place without any formatting to support visually impaired users
            final TextView cachename = details.add(R.string.cache_name, cache.getName()).valueView;
            activity.addShareAction(cachename);
            if (cache.supportsNamechange()) {
                cachename.setOnClickListener(v -> Dialogs.input(activity, activity.getString(R.string.cache_name_set), cache.getName(), activity.getString(R.string.caches_sort_name), name -> {
                    cachename.setText(name);
                    cache.setName(name);
                    CacheDetailActivity.saveAndNotify(getContext(), cache, LoadFlags.SAVE_ALL);
                    ViewUtils.showShortToast(activity, R.string.cache_name_updated);
                }));
            }

            details.add(R.string.cache_type, cache.getType().getL10n());
            if (cache.getType() == CacheType.ADVLAB) {
                details.addAlcMode(cache);
            }
            details.addSize(cache);
            activity.addShareAction(details.add(R.string.cache_geocode, cache.getShortGeocode()).valueView);
            details.addCacheState(cache);

            activity.cacheDistanceView = details.addDistance(cache, activity.cacheDistanceView);

            details.addDifficulty(cache);
            details.addTerrain(cache);
            details.addRating(cache);

            // favorite count
            favoriteLine = details.add(R.string.cache_favorite, "");

            details.addBetterCacher(cache);

            // own rating
            if (cache.getMyVote() > 0) {
                details.addStars(R.string.cache_own_rating, cache.getMyVote());
            }

            // cache author
            if (StringUtils.isNotBlank(cache.getOwnerDisplayName()) || StringUtils.isNotBlank(cache.getOwnerUserId())) {
                final TextView ownerView = details.add(R.string.cache_owner, "").valueView;
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
                activity.addShareAction(hiddenView);
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
                final TextView valueView = details.add(R.string.cache_coordinates, cache.getCoords().toString()).valueView;
                new CoordinatesFormatSwitcher().setView(valueView).setCoordinate(cache.getCoords());
                activity.addShareAction(valueView, s -> GeopointFormatter.reformatForClipboard(s).toString());
            }

            // Latest logs
            details.addLatestLogs(cache);

            // cache attributes
            updateAttributes(activity);
            binding.attributesBox.setVisibility(cache.getAttributes().isEmpty() ? View.GONE : View.VISIBLE);

            updateOfflineBox(binding.getRoot(), cache, activity.res, new RefreshCacheClickListener(), new DropCacheClickListener(),
                    new StoreCacheClickListener(), null, new MoveCacheClickListener(), new StoreCacheClickListener());

            // list
            updateCacheLists(binding.getRoot(), cache, activity.res, activity);

            // watchlist

            binding.addToWatchlist.setOnClickListener(new AddToWatchlistClickListener());
            binding.removeFromWatchlist.setOnClickListener(new RemoveFromWatchlistClickListener());
            updateWatchlistBox(activity);

            // internal WIG player, WhereYouGo, ChirpWolf, Adventure Lab
            updateWherigoBox(activity);
            updateWhereYouGoBox(activity);
            updateChirpWolfBox(activity);
            updateALCBox(activity);

            // favorite points
            binding.addToFavpoint.setOnClickListener(new FavoriteAddClickListener());
            binding.removeFromFavpoint.setOnClickListener(new FavoriteRemoveClickListener());
            updateFavPointBox(activity);

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

        private void updateAttributes(final Activity activity) {
            final List<String> attributes = cache.getAttributes();
            if (!CacheAttribute.hasRecognizedAttributeIcon(attributes)) {
                binding.attributesGrid.setVisibility(View.GONE);
                return;
            }
            // traverse by category and attribute order
            final ArrayList<String> orderedAttributeNames = new ArrayList<>();
            final StringBuilder attributesText = new StringBuilder();
            CacheAttributeCategory lastCategory = null;
            for (CacheAttributeCategory category : CacheAttributeCategory.getOrderedCategoryList()) {
                for (CacheAttribute attr : CacheAttribute.getByCategory(category)) {
                    for (Boolean enabled : Arrays.asList(false, true, null)) {
                        final String key = attr.getValue(enabled);
                        if (attributes.contains(key)) {
                            if (lastCategory != category) {
                                if (lastCategory != null) {
                                    attributesText.append("<br /><br />");
                                }
                                attributesText.append("<b><u>").append(category.getName(activity)).append("</u></b><br />");
                                lastCategory = category;
                            } else {
                                attributesText.append("<br />");
                            }
                            orderedAttributeNames.add(key);
                            attributesText.append(attr.getL10n(enabled == null || enabled));
                        }
                    }
                }
            }

            binding.attributesGrid.setAdapter(new AttributesGridAdapter(activity, orderedAttributeNames, this::toggleAttributesView));
            binding.attributesGrid.setVisibility(View.VISIBLE);

            binding.attributesText.setText(HtmlCompat.fromHtml(attributesText.toString(), 0));
            binding.attributesText.setVisibility(View.GONE);
            binding.attributesText.setOnClickListener(v -> toggleAttributesView());
        }

        protected void toggleAttributesView() {
            binding.attributesText.setVisibility(binding.attributesText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            binding.attributesGrid.setVisibility(binding.attributesGrid.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
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
            private final ProgressButtonDisposableHandler handler;
            protected MaterialButton button;

            AbstractPropertyListener() {
                final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
                handler = new CheckboxHandler(DetailsViewCreator.this, activity);
            }

            public void doExecute(final Action1<ProgressButtonDisposableHandler> action) {
                final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
                if (activity != null) {
                    if (ProgressBarDisposableHandler.isInProgress(activity) || activity.progress.isShowing()) {
                        activity.showToast(activity.res.getString(R.string.err_detail_still_working));
                        return;
                    }
                    handler.showProgress(button);
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
                button = (MaterialButton) arg0;
                doExecute(DetailsViewCreator.this::watchListAdd);
            }
        }

        /**
         * Listener for "remove from watchlist" button
         */
        private class RemoveFromWatchlistClickListener extends AbstractPropertyListener {
            @Override
            public void onClick(final View arg0) {
                button = (MaterialButton) arg0;
                doExecute(DetailsViewCreator.this::watchListRemove);
            }
        }

        /**
         * Add this cache to the watchlist of the user
         */
        private void watchListAdd(final ProgressButtonDisposableHandler handler) {
            final WatchListCapability connector = (WatchListCapability) ConnectorFactory.getConnector(cache);
            if (connector.addToWatchlist(cache)) {
                handler.sendTextMessage(MESSAGE_SUCCEEDED, R.string.cachedetails_progress_watch);
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_watchlist_failed);
            }
        }

        /**
         * Remove this cache from the watchlist of the user
         */
        private void watchListRemove(final ProgressButtonDisposableHandler handler) {
            final WatchListCapability connector = (WatchListCapability) ConnectorFactory.getConnector(cache);
            if (connector.removeFromWatchlist(cache)) {
                handler.sendTextMessage(MESSAGE_SUCCEEDED, R.string.cachedetails_progress_unwatch);
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_watchlist_failed);
            }
        }

        /**
         * Add this cache to the favorite list of the user
         */
        private void favoriteAdd(final ProgressButtonDisposableHandler handler) {
            final IFavoriteCapability connector = (IFavoriteCapability) ConnectorFactory.getConnector(cache);
            if (connector.addToFavorites(cache)) {
                handler.sendTextMessage(MESSAGE_SUCCEEDED, R.string.cachedetails_progress_favorite);
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_favorite_failed);
            }
        }

        /**
         * Remove this cache to the favorite list of the user
         */
        private void favoriteRemove(final ProgressButtonDisposableHandler handler) {
            final IFavoriteCapability connector = (IFavoriteCapability) ConnectorFactory.getConnector(cache);
            if (connector.removeFromFavorites(cache)) {
                handler.sendTextMessage(MESSAGE_SUCCEEDED, R.string.cachedetails_progress_unfavorite);
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
                button = (MaterialButton) arg0;
                doExecute(DetailsViewCreator.this::favoriteAdd);
            }
        }

        /**
         * Listener for "remove from favorites" button
         */
        private class FavoriteRemoveClickListener extends AbstractPropertyListener {
            @Override
            public void onClick(final View arg0) {
                button = (MaterialButton) arg0;
                doExecute(DetailsViewCreator.this::favoriteRemove);
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
        private void updateFavPointBox(final CacheDetailActivity activity) {
            // Favorite counts
            final int favCount = cache.getFavoritePoints();
            if (favCount >= 0 && !cache.isEventCache()) {
                favoriteLine.layout.setVisibility(View.VISIBLE);

                final int findsCount = cache.getFindsCount();
                if (findsCount > 0) {
                    favoriteLine.valueView.setText(activity.res.getString(R.string.favorite_count_percent, favCount, Math.min((float) (favCount * 100) / findsCount, 100.0f)));
                } else {
                    favoriteLine.valueView.setText(activity.res.getString(R.string.favorite_count, favCount));
                }
            } else {
                favoriteLine.layout.setVisibility(View.GONE);
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
            final List<String> wherigoGuis = WherigoUtils.getWherigoGuids(cache);
            binding.whereyougoBox.setVisibility(!wherigoGuis.isEmpty() ? View.VISIBLE : View.GONE);
            binding.whereyougoText.setText(isWhereYouGoInstalled() ? R.string.cache_whereyougo_start : R.string.cache_whereyougo_install);
            binding.sendToWhereyougo.setOnClickListener(v -> WherigoViewUtils.executeForOneCartridge(activity, wherigoGuis, guid ->
                WhereYouGoApp.openWherigo(activity, guid)));
        }

        private void updateWherigoBox(final CacheDetailActivity activity) {
            final List<String> wherigoGuis = WherigoUtils.getWherigoGuids(cache);
            binding.wherigoBox.setVisibility(!wherigoGuis.isEmpty() ? View.VISIBLE : View.GONE);
            binding.wherigoText.setText(wherigoGuis.isEmpty() || Settings.hasGCCredentials() ? R.string.cache_wherigo_start : R.string.cache_wherigo_credentials);
            binding.playInCgeo.setOnClickListener(v -> {
                    if (Settings.hasGCCredentials()) {
                        WherigoViewUtils.executeForOneCartridge(activity, wherigoGuis, guid ->
                                WherigoActivity.startForGuid(activity, guid, cache.getGeocode(), true));
                    } else {
                        SettingsActivity.openForScreen(R.string.preference_screen_gc, activity);
                    }
            });
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
            final boolean isLabListing = CacheUtils.isLabAdventure(cache);
            final boolean isEnabled = isLabListing || (cache.getType() == CacheType.MYSTERY && CacheUtils.findAdvLabUrl(cache) != null);
            binding.alcBox.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
            binding.alcText.setText(CacheUtils.isLabPlayerInstalled(activity) ? (isLabListing ? R.string.cache_alc_start : R.string.cache_alc_related_start) : R.string.cache_alc_install);
            if (isEnabled) {
                CacheUtils.setLabLink(activity, binding.sendToAlc, isLabListing ? cache.getUrl() : CacheUtils.findAdvLabUrl(cache));
            }
        }
    }

    public static class DescriptionViewCreator extends TabbedViewPagerFragment<CachedetailDescriptionPageBinding> {

        private static final int DESCRIPTION_MAX_SAFE_LENGTH = 50000;

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
        // splitting up that method would not help improve readability
        @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
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

            // load description
            reloadDescription(this.getActivity(), cache, true, 0, activity.descriptionStyle, null, null, null);

            //check for geochecker
            final String checkerUrl = CheckerUtils.getCheckerUrl(cache);
            binding.descriptionChecker.setVisibility(checkerUrl == null ? View.GONE : View.VISIBLE);
            if (checkerUrl != null) {
                binding.descriptionChecker.setOnClickListener(v -> openGeochecker(activity, cache));
                binding.descriptionCheckerButton.setOnClickListener(v -> openGeochecker(activity, cache));
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
            activity.addShareAction(binding.personalnote);
            TooltipCompat.setTooltipText(binding.editPersonalnote, getString(R.string.cache_personal_note_edit));
            binding.editPersonalnote.setOnClickListener(v -> editPersonalNote(cache, activity));
            binding.personalnote.setOnClickListener(v -> editPersonalNote(cache, activity));
            TooltipCompat.setTooltipText(binding.storewaypointsPersonalnote, getString(R.string.cache_personal_note_storewaypoints));
            binding.storewaypointsPersonalnote.setOnClickListener(v -> {
                activity.ensureSaved();
                activity.storeWaypointsInPersonalNote(cache);
            });
            TooltipCompat.setTooltipText(binding.deleteewaypointsPersonalnote, getString(R.string.cache_personal_note_removewaypoints));
            binding.deleteewaypointsPersonalnote.setOnClickListener(v -> {
                activity.ensureSaved();
                activity.removeWaypointsFromPersonalNote(cache);
            });
            binding.personalnoteVarsOutOfSync.setOnClickListener(v -> handleVariableOutOfSync());
            activity.adjustPersonalNoteVarsOutOfSyncButton(binding.personalnoteVarsOutOfSync);
            final PersonalNoteCapability connector = ConnectorFactory.getConnectorAs(cache, PersonalNoteCapability.class);
            if (connector != null && connector.canAddPersonalNote(cache)) {
                binding.uploadPersonalnote.setVisibility(View.VISIBLE);
                TooltipCompat.setTooltipText(binding.uploadPersonalnote, getString(R.string.cache_personal_note_upload));
                binding.uploadPersonalnote.setOnClickListener(v -> activity.checkAndUploadPersonalNote(connector));
            } else {
                binding.uploadPersonalnote.setVisibility(View.GONE);
            }

            final List<Image> spoilerImages = cache.getFilteredSpoilers();
            final boolean hasSpoilerImages = CollectionUtils.isNotEmpty(spoilerImages);
            // cache hint and spoiler images
            if (StringUtils.isNotBlank(cache.getHint()) || hasSpoilerImages) {
                binding.hintBox.setVisibility(View.VISIBLE);
            } else {
                binding.hintBox.setVisibility(View.GONE);
            }

            if (StringUtils.isNotBlank(cache.getHint())) {
                if (TextUtils.containsHtml(cache.getHint())) {
                    binding.hint.setText(HtmlCompat.fromHtml(cache.getHint(), HtmlCompat.FROM_HTML_MODE_LEGACY, new HtmlImage(cache.getGeocode(), false, false, false), null), TextView.BufferType.SPANNABLE);
                } else {
                    binding.hint.setText(cache.getHint());
                }
                binding.hint.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                binding.hint.setVisibility(View.VISIBLE);
                if (Settings.getHintAsRot13()) {
                    binding.hint.setText(CryptUtils.rot13((Spannable) binding.hint.getText()));
                }
                final DecryptTextClickListener decryptListener = new DecryptTextClickListener(binding.hint);
                binding.hint.setOnClickListener(decryptListener);
                binding.hint.setClickable(true);
                binding.hintBox.setOnClickListener(decryptListener);
                binding.hintBox.setClickable(true);
            } else {
                binding.hint.setVisibility(View.GONE);
                binding.hint.setClickable(false);
                binding.hint.setOnClickListener(null);
                binding.hintBox.setClickable(false);
                binding.hintBox.setOnClickListener(null);
            }


            if (hasSpoilerImages) {
                binding.hintSpoilerlink.setVisibility(View.VISIBLE);
                binding.hintSpoilerlink.setClickable(true);
                binding.hintSpoilerlink.setOnClickListener(arg0 -> {
                    final List<Image> sis = cache.getFilteredSpoilers();
                    if (cache == null || sis.isEmpty()) {
                        activity.showToast(getString(R.string.err_detail_no_spoiler));
                        return;
                    }
                    ImageGalleryActivity.startActivity(activity, cache.getGeocode(), sis);
                });

                // if there is only a listing background image without other additional pictures, change the text to better explain the content.
                if (spoilerImages.size() == 1 && getString(R.string.cache_image_background).equals(spoilerImages.get(0).title)) {
                    binding.hintSpoilerlink.setText(R.string.cache_image_background);
                } else {
                    binding.hintSpoilerlink.setText(R.string.cache_menu_spoilers);
                }
            } else {
                binding.hintSpoilerlink.setVisibility(View.GONE);
                binding.hintSpoilerlink.setClickable(true);
                binding.hintSpoilerlink.setOnClickListener(null);
            }

            //register for changes of variableslist -> state of variable sync may change
            cache.getVariables().addChangeListener(this, s -> activity.adjustPersonalNoteVarsOutOfSyncButton(binding.personalnoteVarsOutOfSync));

            final OfflineTranslateUtils.Status currentTranslationStatus = activity.translationStatus;
            if (currentTranslationStatus.checkRetranslation()) {
                currentTranslationStatus.setNotTranslated();
                translateListing();
            }
        }

        private void translateListing() {
            final CacheDetailActivity cda = (CacheDetailActivity) getActivity();
            final OfflineTranslateUtils.Language sourceLng = cda.translationStatus.getSourceLanguage();

            if (cda.translationStatus.isTranslated()) {
                cda.translationStatus.setNotTranslated();
                reloadDescription(cda, cache, true, 0, cda.descriptionStyle, null, null, null);
                if (TextUtils.containsHtml(cache.getHint())) {
                    binding.hint.setText(HtmlCompat.fromHtml(cache.getHint(), HtmlCompat.FROM_HTML_MODE_LEGACY, new HtmlImage(cache.getGeocode(), false, false, false), null), TextView.BufferType.SPANNABLE);
                } else {
                    binding.hint.setText(cache.getHint());
                }
                binding.descriptionTranslateNote.setText(String.format(getString(R.string.translator_language_detected), sourceLng));
                return;
            }

            cda.translationStatus.startTranslation(2, cda, cda.findViewById(R.id.description_translate_button));

            OfflineTranslateUtils.getTranslator(cda, sourceLng,
                unsupportedLng -> {
                    cda.translationStatus.abortTranslation();
                    binding.descriptionTranslateNote.setText(getResources().getString(R.string.translator_language_unsupported, sourceLng));
                }, modelDownloading -> binding.descriptionTranslateNote.setText(R.string.translator_model_download_notification),
    translator -> {
                    if (null == translator) {
                        binding.descriptionTranslateNote.setText(R.string.translator_translation_initerror);
                        return;
                    }

                    final Consumer<Exception> errorConsumer = error -> {
                        binding.descriptionTranslateNote.setText(getResources().getText(R.string.translator_translation_error, error.getMessage()));
                        binding.descriptionTranslateButton.setEnabled(false);
                    };
                    reloadDescription(cda, cache, true, 0, cda.descriptionStyle, translator, cda.translationStatus, errorConsumer);
        /*
                    OfflineTranslateUtils.translateParagraph(translator, cda.translationStatus, binding.description.getText().toString(), translatedText -> {
                        displayDescription(getActivity(), cache, translatedText, binding.description);
                        binding.descriptionTranslateNote.setText(String.format(getString(R.string.translator_translation_success), sourceLng));
                    }, errorConsumer);
        */
                    OfflineTranslateUtils.translateParagraph(translator, cda.translationStatus, binding.hint.getText().toString(), binding.hint::setText, errorConsumer);
                });
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (cache != null && cache.getVariables() != null) {
                cache.getVariables().removeChangeListener(this);
            }
        }


        private void handleVariableOutOfSync() {
            final Map<String, Pair<String, String>> diff = cache.getVariableDifferencesFromUserNotes();
            if (diff.isEmpty()) {
                return;
            }
            final List<ImmutableTriple<String, String, String>> items = CollectionStream.of(diff.entrySet())
                    .map(e -> new ImmutableTriple<>(e.getKey(), e.getValue().first, e.getValue().second)).toList();
            TextUtils.sortListLocaleAware(items, i -> i.left);

            final SimpleDialog.ItemSelectModel<ImmutableTriple<String, String, String>> model = new SimpleDialog.ItemSelectModel<>();
            model.setItems(items).setDisplayMapper((i) -> TextParam.id(R.string.cache_personal_note_vars_out_of_sync_line, i.left, i.middle, i.right));
            SimpleDialog.of(getActivity()).setTitle(TextParam.id(R.string.cache_personal_note_vars_out_of_sync_dialog_title))
                    .setMessage(TextParam.id(R.string.cache_personal_note_vars_out_of_sync_title))
                    .selectMultiple(model, sel -> {
                           final Map<String, String> toChange = new HashMap<>();
                           for (ImmutableTriple<String, String, String> e : sel) {
                               toChange.put(e.left, e.middle);
                           }
                           cache.changeVariables(toChange);
                        ((CacheDetailActivity) getActivity()).adjustPersonalNoteVarsOutOfSyncButton(binding.personalnoteVarsOutOfSync);
                    });
        }

        /** re-renders the caches Listing (=description) in background and fills in the result. Includes handling of too long listings */
        private void reloadDescription(final Activity activity, final Geocache cache, final boolean restrictLength, final int initialScroll, final HtmlStyle descriptionStyle,
            final Translator translator, final OfflineTranslateUtils.Status status, final Consumer<Exception> errorConsumer) {
            binding.descriptionRenderFully.setVisibility(View.GONE);
            binding.description.setText(TextUtils.setSpan(activity.getString(translator != null ? R.string.cache_description_translating_and_rendering : R.string.cache_description_rendering), new StyleSpan(Typeface.ITALIC)));
            binding.description.setVisibility(View.VISIBLE);
            AndroidRxUtils.computationScheduler.scheduleDirect(() ->
                    createDescriptionContent(activity, cache, restrictLength, binding.description, descriptionStyle, translator, status, p -> {
                        displayDescription(activity, cache, p.first, binding.description);
                        if (translator != null) {
                            // @todo: Box does not come up again after successful translation
                            binding.descriptionTranslateNote.setText(String.format(getString(R.string.translator_translation_success), status.getSourceLanguage()));
                            binding.descriptionTranslate.setVisibility(View.VISIBLE);
                        }

                        OfflineTranslateUtils.initializeListingTranslatorInTabbedViewPagerActivity((CacheDetailActivity) getActivity(), binding.descriptionTranslate, binding.description.getText().toString(), this::translateListing);

                        // we need to use post, so that the textview is layouted before scrolling gets called
                        if (((CacheDetailActivity) activity).lastActionWasEditNote) {
                            ((CacheDetailActivity) activity).scrollToBottom();
                        } else if (initialScroll != 0) {
                            binding.detailScroll.post(() -> binding.detailScroll.setScrollY(initialScroll));
                        }
                        if (p.second) {
                            binding.descriptionRenderFully.setVisibility(View.VISIBLE);
                            binding.descriptionRenderFully.setOnClickListener(v -> reloadDescription(activity, cache, false, binding.detailScroll.getScrollY(), descriptionStyle, translator, status, errorConsumer));
                        } else {
                            ((CacheDetailActivity) activity).lastActionWasEditNote = false;
                        }
                    }, errorConsumer)
            );
        }

        /** displays given description into listing textview */
        @MainThread
        private static void displayDescription(final Activity activity, final Geocache cache, final CharSequence renderedDescription, final TextView descriptionView) {
            try {
                descriptionView.setText(renderedDescription, TextView.BufferType.SPANNABLE);
                descriptionView.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                if (cache.supportsDescriptionchange()) {
                    descriptionView.setOnClickListener(v ->
                            Dialogs.input(activity, activity.getString(R.string.cache_description_set), cache.getDescription(), "Description", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 5, 10, description -> {
                                descriptionView.setText(description);
                                cache.setDescription(description);
                                saveAndNotify(activity, cache, LoadFlags.SAVE_ALL);
                                ViewUtils.showShortToast(activity, R.string.cache_description_updated);
                            }));
                } else {
                    descriptionView.setOnClickListener(null);
                }
            } catch (final RuntimeException ignored) {
                ActivityMixin.showToast(activity, R.string.err_load_descr_failed);
            }
        }

        /** CALL IN BACKGROUND ONLY! Renders cache description into an Editable. */
        // splitting up that method would not help improve readability
        @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
        @WorkerThread
        private static void createDescriptionContent(final Activity activity, final Geocache cache, final boolean restrictLength, final TextView descriptionView, final HtmlStyle descriptionStyle,
            final Translator translator, final OfflineTranslateUtils.Status status, final Consumer<Pair<CharSequence, Boolean>> successConsumer, final Consumer<Exception> errorConsumer) {
            //combine short and long description to the final description to render
            String descriptionText = cache.getDescription();
            final String shortDescriptionText = cache.getShortDescription();
            if (StringUtils.isNotBlank(shortDescriptionText)) {
                final int index = StringUtils.indexOf(descriptionText, shortDescriptionText);
                // allow up to 200 characters of HTML formatting
                if (index < 0 || index > 200) {
                    descriptionText = shortDescriptionText + "\n" + descriptionText;
                }
            }

            //check for too-long-listing
            final int descriptionFullLength = descriptionText.length();
            final boolean textTooLong = descriptionFullLength > DESCRIPTION_MAX_SAFE_LENGTH;
            if (textTooLong && restrictLength) {
                descriptionText = descriptionText.substring(0, DESCRIPTION_MAX_SAFE_LENGTH);
            }

            // translate, if requested
            if (translator == null) {
                successConsumer.accept(createDescriptionContentHelper(activity, descriptionText, textTooLong, descriptionFullLength, cache, restrictLength, descriptionView, descriptionStyle));
            } else {
                OfflineTranslateUtils.translateParagraph(translator, status, descriptionText, translatedText -> successConsumer.accept(createDescriptionContentHelper(activity, translatedText, textTooLong, descriptionFullLength, cache, restrictLength, descriptionView, descriptionStyle)), errorConsumer);
            }
        }

        @WorkerThread
        private static Pair<CharSequence, Boolean> createDescriptionContentHelper(final Activity activity, final String descriptionText, final boolean textTooLong, final int descriptionFullLength, final Geocache cache, final boolean restrictLength, final TextView descriptionView, final HtmlStyle descriptionStyle) {
            try {
                //Format to HTML. This takes time on long listings or those with e.g. many images...
                final HtmlImage imageGetter = new HtmlImage(cache.getGeocode(), true, false, descriptionView, false);
                final Pair<Spannable, Boolean> renderedHtml = descriptionStyle.render(activity, descriptionText, imageGetter::getDrawable);
                final SpannableStringBuilder description = new SpannableStringBuilder(renderedHtml.first);
                final boolean renderError = renderedHtml.second;

                if (StringUtils.isNotBlank(description)) {
                    handleImageClick(activity, cache, description);
                    WherigoViewUtils.htmlReplaceWherigoClickAction(activity, cache.getGeocode(), description);
                    //display various fixes
                    HtmlUtils.fixRelativeLinks(description, ConnectorFactory.getConnector(cache).getHostUrl() + "/");
                    fixOldGeocheckerLink(activity, cache, description);
                }

                // If description has an HTML construct which may be problematic to render, add a note at the end of the long description.
                // Technically, it may not be a table, but a pre, which has the same problems as a table, so the message is ok even though
                // sometimes technically incorrect.
                if (renderError) {
                    final IConnector connector = ConnectorFactory.getConnector(cache);
                    if (StringUtils.isNotEmpty(cache.getUrl())) {
                        final Spanned tableNote = HtmlCompat.fromHtml(activity.getString(R.string.cache_description_table_note, "<a href=\"" + cache.getUrl() + "\">" + connector.getName() + "</a>"), HtmlCompat.FROM_HTML_MODE_LEGACY);
                        description.append("\n\n").append(TextUtils.setSpan(tableNote, new StyleSpan(Typeface.ITALIC)));
                    }
                }

                //add warning on too-long-listing
                if (textTooLong && restrictLength) {
                    description.append("\n\n")
                            .append(TextParam.id(R.string.cache_description_render_restricted_warning, descriptionFullLength, DESCRIPTION_MAX_SAFE_LENGTH * 100 / descriptionFullLength).getText(null), new StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                return new Pair<>(description, textTooLong && restrictLength);
            } catch (final RuntimeException re) {
                Log.e("Problems parsing cache description", re);
                return new Pair<>(activity.getString(R.string.err_load_descr_failed) + ": " + re.getMessage(), false);
            }
        }

        private static void handleImageClick(final Activity activity, final Geocache cache, final Spannable spannable) {
            HtmlUtils.addImageClick(spannable, span -> {
                final String imageUrl = span.getSource();
                final List<Image> listingImages = cache.getNonStaticImages();
                CollectionUtils.filter(listingImages, i -> i.category == Image.ImageCategory.LISTING);

                final int pos = IterableUtils.indexOf(listingImages, i -> ImageUtils.imageUrlForSpoilerCompare(imageUrl).equals(ImageUtils.imageUrlForSpoilerCompare(i.getUrl())));
                ImageViewActivity.openImageView(activity, cache.getGeocode(), listingImages, pos, null);
            });
        }

        /**
         * Old GCParser logic inserted an HTML link to their geochecker (when applicable)
         * directly into the description text.
         * When running on Android 12 this direct URL might be redirected to c:geo under
         * specific system settings, leading to a loop (see #12889), so let's reassign
         * this link to the proper openGeochecker method.
         *
         * @param activity calling activity
         */
        private static void fixOldGeocheckerLink(final Activity activity, final Geocache cache, final Spannable spannable) {
            final String gcLinkInfo = activity.getString(R.string.link_gc_checker);
            HtmlUtils.replaceUrlClickAction(spannable, gcLinkInfo, span -> openGeochecker(activity, cache));
        }
    }

    protected void saveAndNotify() {
        saveAndNotify(CacheDetailActivity.this, cache);
    }

    protected void saveAndNotify(final Set<SaveFlag> flags) {
        saveAndNotify(CacheDetailActivity.this, cache, flags);
    }

    protected static void saveAndNotify(final Context context, final Geocache cache) {
        saveAndNotify(context, cache, EnumSet.of(SaveFlag.DB));
    }

    protected static void saveAndNotify(final Context context, final Geocache cache, final Set<SaveFlag> flags) {
        DataStore.saveCache(cache, flags);
        GeocacheChangedBroadcastReceiver.sendBroadcast(context, cache.getGeocode());
    }

    protected void ensureSaved() {
        if (!cache.isOffline()) {
            showToast(getString(R.string.info_cache_saved));
            cache.getLists().add(StoredList.STANDARD_LIST_ID);
            AndroidRxUtils.computationScheduler.scheduleDirect(() -> saveAndNotify(CacheDetailActivity.this, cache, LoadFlags.SAVE_ALL));
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

        private static List<Waypoint> createSortedWaypointList(final Geocache cache) {
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

        private static int indexOfWaypoint(final Geocache cache, final Waypoint waypoint) {
            final List<Waypoint> sortedWaypoints = createSortedWaypointList(cache);
            Collections.sort(sortedWaypoints, cache.getWaypointComparator());
            return IterableUtils.indexOf(sortedWaypoints, wp -> wp.getId() == waypoint.getId());
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
            final List<Waypoint> sortedWaypoints = createSortedWaypointList(cache);
            Collections.sort(sortedWaypoints, cache.getWaypointComparator());

            final ArrayAdapter<Waypoint> adapter = new ArrayAdapter<Waypoint>(activity, R.layout.waypoint_item, sortedWaypoints) {
                @NonNull
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

            if (activity.selectedWaypoint != null) {
                final int index = indexOfWaypoint(cache, activity.selectedWaypoint);
                if (index >= 0) {
                    v.setSelection(index);
                }
            }

            //register for changes of variableslist -> calculated waypoints may have changed
            cache.getVariables().addChangeListener(this, s -> activity.runOnUiThread(adapter::notifyDataSetChanged));

            if (v.getHeaderViewsCount() < 1) {
                final CachedetailWaypointsHeaderBinding headerBinding = CachedetailWaypointsHeaderBinding.inflate(getLayoutInflater(), v, false);
                v.addHeaderView(headerBinding.getRoot());

                headerBinding.addWaypoint.setOnClickListener(v2 -> {
                    activity.ensureSaved();
                    EditWaypointActivity.startActivityAddWaypoint(activity, cache);
                    if (sortedWaypoints != null && !sortedWaypoints.isEmpty()) {
                        activity.selectedWaypoint = sortedWaypoints.get(sortedWaypoints.size() - 1); // move to bottom where new waypoint will be created
                    }
                    activity.setNeedsRefresh();
                });

                headerBinding.addWaypointCurrentlocation.setOnClickListener(v2 -> {
                    activity.ensureSaved();
                    final Waypoint newWaypoint = new Waypoint(Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT), WaypointType.WAYPOINT, true);
                    newWaypoint.setCoords(LocationDataProvider.getInstance().currentGeo().getCoords());
                    newWaypoint.setGeocode(cache.getGeocode());
                    if (cache.addOrChangeWaypoint(newWaypoint, true)) {
                        addWaypointAndSort(sortedWaypoints, newWaypoint);
                        adapter.notifyDataSetChanged();
                        activity.reinitializePage(Page.WAYPOINTS.id);
                        ActivityMixin.showShortToast(activity, getString(R.string.waypoint_added));
                    }
                });

                headerBinding.hideVisitedWaypoints.setChecked(Settings.getHideVisitedWaypoints());
                headerBinding.hideVisitedWaypoints.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    Settings.setHideVisitedWaypoints(isChecked);

                    final List<Waypoint> sortedWaypoints2 = createSortedWaypointList(cache);
                    Collections.sort(sortedWaypoints2, cache.getWaypointComparator());

                    adapter.clear();
                    adapter.addAll(sortedWaypoints2);
                    adapter.notifyDataSetChanged();
                    activity.reinitializePage(Page.WAYPOINTS.id);
                });


                // read waypoint from clipboard
                setClipboardButtonVisibility(headerBinding.addWaypointFromclipboard);
                headerBinding.addWaypointFromclipboard.setOnClickListener(v2 -> {
                    final Waypoint oldWaypoint = DataStore.loadWaypoint(Waypoint.hasClipboardWaypoint());
                    if (null != oldWaypoint) {
                        activity.ensureSaved();
                        final Waypoint newWaypoint = cache.duplicateWaypoint(oldWaypoint, cache.getGeocode().equals(oldWaypoint.getGeocode()));
                        if (null != newWaypoint) {
                            CacheDetailActivity.saveAndNotify(getContext(), cache);
                            addWaypointAndSort(sortedWaypoints, newWaypoint);
                            adapter.notifyDataSetChanged();
                            activity.reinitializePage(Page.WAYPOINTS.id);
                            if (oldWaypoint.isUserDefined()) {
                                SimpleDialog.of((Activity) v.getContext()).setTitle(R.string.cache_waypoints_add_fromclipboard).setMessage(R.string.cache_waypoints_remove_original_waypoint).confirm(() -> {
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

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (cache != null && cache.getVariables() != null) {
                cache.getVariables().removeChangeListener(this);
            }
        }

        @SuppressLint("SetTextI18n")
        protected void fillViewHolder(final CacheDetailActivity activity, final View rowView, final WaypointViewHolder holder, final Waypoint wpt) {
            // coordinates
            final TextView coordinatesView = holder.binding.coordinates;
            final TextView calculatedCoordinatesView = holder.binding.calculatedCoordinateInfo;
            final Geopoint coordinates = wpt.getCoords();
            final String calcStateJson = wpt.getCalcStateConfig();

            // coordinates
            holder.setCoordinate(coordinates);
            activity.addShareAction(coordinatesView, s -> GeopointFormatter.reformatForClipboard(s).toString());
            coordinatesView.setVisibility(null != coordinates ? View.VISIBLE : View.GONE);
            calculatedCoordinatesView.setVisibility(null != calcStateJson ? View.VISIBLE : View.GONE);
            final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig(calcStateJson);
            calculatedCoordinatesView.setText("(x):" + cc.getLatitudePattern() + " | " + cc.getLongitudePattern());
            holder.binding.calculatedCoordinatesIcon.setVisibility(wpt.isCalculated() ? View.VISIBLE : View.GONE);

            holder.binding.projectionIcon.setVisibility(wpt.hasProjection() ? View.VISIBLE : View.GONE);
            holder.binding.projectionIcon.setImageResource(wpt.getProjectionType().markerId);
            holder.binding.projectionInfo.setVisibility(wpt.hasProjection() ? View.VISIBLE : View.GONE);
            holder.binding.projectionInfo.setText("(↦):" + wpt.getProjectionType().getUserDisplayableInfo(wpt.getProjectionFormula1(), wpt.getProjectionFormula2(), wpt.getProjectionDistanceUnit()));

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
            holder.binding.textIcon.setImageDrawable(MapMarkerUtils.getWaypointMarker(activity.res, wpt, false, Settings.getIconScaleEverywhere()).getDrawable());

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
                    noteView.setText(HtmlCompat.fromHtml(wpt.getNote(), HtmlCompat.FROM_HTML_MODE_LEGACY, new HtmlImage(cache.getGeocode(), true, false, noteView, false), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
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

            rowView.setOnClickListener(v -> {
                activity.selectedWaypoint = wpt;
                activity.ensureSaved();
                EditWaypointActivity.startActivityEditWaypoint(activity, cache, wpt.getId());
                activity.setNeedsRefresh();
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

    public static class ImageGalleryCreator extends TabbedViewPagerFragment<CachedetailImagegalleryPageBinding> {

        @Override
        public CachedetailImagegalleryPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return CachedetailImagegalleryPageBinding.inflate(inflater, container, false);
        }


        @Override
        public long getPageId() {
            return Page.IMAGEGALLERY.id;
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

            if (activity.imageGallery == null) {
                final ImageGalleryView imageGallery = binding.getRoot().findViewById(R.id.image_gallery);
                ImageUtils.initializeImageGallery(imageGallery, cache.getGeocode(), cache.getNonStaticImages(), true);
                activity.imageGallery = imageGallery;
                activity.imageGallery.initializeToPosition(activity.imageGalleryPos);
                reinitializeTitle();
                activity.imageGallery.setImageCountChangeCallback((ig, c) -> reinitializeTitle());
                if (activity.imageGalleryState != null) {
                    imageGallery.setState(activity.imageGalleryState);
                }
                if (activity.imageGalleryResultRequestCode >= 0) {
                    activity.imageGallery.onActivityResult(activity.imageGalleryResultRequestCode, activity.imageGalleryResultResultCode, activity.imageGalleryData);
                }
                activity.imageGalleryState = null;
                activity.imageGalleryResultRequestCode = -1;
                activity.imageGalleryResultResultCode = 0;
                activity.imageGalleryData = null;
            }
        }
    }

    public static void startActivity(final Context context, final String geocode, final String cacheName) {
        final Intent cachesIntent = new Intent(context, CacheDetailActivity.class);
        cachesIntent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        cachesIntent.putExtra(Intents.EXTRA_NAME, cacheName);
        context.startActivity(cachesIntent);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (imageGallery != null) {
            imageGallery.onActivityResult(requestCode, resultCode, data);
        } else {
            imageGalleryResultRequestCode = requestCode;
            imageGalleryResultResultCode = resultCode;
            imageGalleryData = data;
        }
    }

    @Override
    public void onActivityReenter(final int resultCode, final Intent data) {
        super.onActivityReenter(resultCode, data);
        this.imageGalleryPos = ImageGalleryView.onActivityReenter(this, this.imageGallery, data);
    }

    public void addShareAction(final TextView view) {
        addShareAction(view, s -> s);
    }

    public void addShareAction(final TextView view, final androidx.arch.core.util.Function<String, String> formatter) {
        view.setOnLongClickListener(v -> {
            ShareUtils.sharePlainText(this, formatter.apply(view.getText().toString()));
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

        final String[] items = {res.getString(R.string.waypoint_localy_reset_cache_coords), res.getString(R.string.waypoint_reset_local_and_remote_cache_coords)};
        builder.setSingleChoiceItems(items, 0, (dialog, which) -> {
            dialog.dismiss();
            final HandlerResetCoordinates handler = new HandlerResetCoordinates(CacheDetailActivity.this, which == 1);
            handler.showProgress();
            resetCoords(cache, handler, wpt, which == 0 || which == 1, which == 1);
        });
        return builder.create();
    }

    private static class HandlerResetCoordinates extends ProgressBarDisposableHandler {
        public static final int LOCAL = 0;
        public static final int ON_WEBSITE = 1;
        private boolean remoteFinished = false;
        private boolean localFinished = false;
        private final boolean resetRemote;

        protected HandlerResetCoordinates(final CacheDetailActivity activity, final boolean resetRemote) {
            super(activity);
            this.resetRemote = resetRemote;
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            if (msg.what == LOCAL) {
                localFinished = true;
            } else {
                remoteFinished = true;
            }

            if (localFinished && (remoteFinished || !resetRemote)) {
                final CacheDetailActivity cacheDetailActivity = (CacheDetailActivity) activityRef.get();
                if (cacheDetailActivity != null) {
                    cacheDetailActivity.notifyDataSetChanged();
                }
                dismissProgress();
            }
        }
    }

    private void resetCoords(final Geocache cache, final Handler handler, final Waypoint wpt, final boolean local, final boolean remote) {
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
            if (local) {
                cache.resetUserModifiedCoords(wpt);
                handler.sendEmptyMessage(HandlerResetCoordinates.LOCAL);
            }

            final IConnector con = ConnectorFactory.getConnector(cache);
            if (remote && con.supportsOwnCoordinates()) {
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
            final int varCount = cache == null ? 0 : cache.getVariables().size();
            return this.getString(Page.VARIABLES.titleStringId) + " (" + varCount + ")";
        } else if (pageId == Page.IMAGEGALLERY.id) {
            String title = this.getString(Page.IMAGEGALLERY.titleStringId);
            if (this.imageGallery != null) {
                title += " (" + this.imageGallery.getImageCount() + ")";
            }
            return  title;
        }  else if (pageId == Page.LOGSFRIENDS.id) {
            final int logCount = cache == null ? 0 : cache.getFriendsLogs().size();
            return this.getString(Page.LOGSFRIENDS.titleStringId) + " (" + logCount + ")";
        }
        return this.getString(Page.find(pageId).titleStringId);
    }

    protected long[] getOrderedPages() {
        final ArrayList<Long> pages = new ArrayList<>();
        pages.add(Page.VARIABLES.id);
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
            pages.add(Page.IMAGEGALLERY.id);
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
        } else if (pageId == Page.IMAGEGALLERY.id) {
            return new ImageGalleryCreator();
        } else if (pageId == Page.VARIABLES.id) {
            return new VariablesViewPageFragment();
        }
        throw new IllegalStateException(); // cannot happen as long as switch case is enum complete
    }

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
        if (view == null) {
            return; // fragment already destroyed?
        }

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

    static void updateCacheLists(final View view, final Geocache cache, final Resources res, @Nullable final CacheDetailActivity cacheDetailActivity) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (final Integer listId : cache.getLists()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            appendClickableList(builder, view, listId, cacheDetailActivity);
        }
        builder.insert(0, res.getString(R.string.list_list_headline) + " ");
        final TextView offlineLists = view.findViewById(R.id.offline_lists);
        offlineLists.setText(builder);
        offlineLists.setMovementMethod(LinkMovementMethod.getInstance());
    }

    static void appendClickableList(final SpannableStringBuilder builder, final View view, final Integer listId, @Nullable final CacheDetailActivity cacheDetailActivity) {
        final int start = builder.length();
        builder.append(DataStore.getList(listId).getTitle());
        builder.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull final View widget) {
                Settings.setLastDisplayedList(listId);
                if (cacheDetailActivity != null) {
                    cacheDetailActivity.setNeedsRefresh();
                }
                CacheListActivity.startActivityOffline(view.getContext());
            }
        }, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public Geocache getCache() {
        return cache;
    }

    private static class StoreCacheHandler extends ProgressButtonDisposableHandler {

        StoreCacheHandler(final CacheDetailActivity activity) {
            super(activity);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            if (msg.what != UPDATE_LOAD_PROGRESS_DETAIL) {
                super.handleRegularMessage(msg);
                notifyDataSetChanged(activityRef);
            }
        }
    }

    private static final class RefreshCacheHandler extends ProgressBarDisposableHandler {

        RefreshCacheHandler(final CacheDetailActivity activity) {
            super(activity);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            final CacheDetailActivity cacheDetailActivity = (CacheDetailActivity) activityRef.get();
            if (msg.what == UPDATE_SHOW_STATUS_TOAST && msg.obj instanceof String) {
                showToast((String) msg.obj);
            } else if (msg.what != UPDATE_LOAD_PROGRESS_DETAIL) {
                dismissProgress(cacheDetailActivity.getString(R.string.cachedetails_progress_refresh, cacheDetailActivity.geocode));
                cacheDetailActivity.translationStatus.setNotTranslated();
                notifyDataSetChanged(activityRef);
            }
        }
    }

    private static final class ChangeNotificationHandler extends ProgressBarDisposableHandler {

        ChangeNotificationHandler(final CacheDetailActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            notifyDataSetChanged(activityRef);
            dismissProgress();
        }
    }

    private static void notifyDataSetChanged(final WeakReference<AbstractActivity> activityRef) {
        final CacheDetailActivity activity = (CacheDetailActivity) activityRef.get();
        if (activity != null) {
            activity.notifyDataSetChanged();
        }
    }

    protected void storeCache(final Set<Integer> listIds) {
        final StoreCacheHandler storeCacheHandler = new StoreCacheHandler(CacheDetailActivity.this);
        storeCacheHandler.showProgress(findViewById(R.id.offline_store));
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> cache.store(listIds, storeCacheHandler));
    }

    public static void editPersonalNote(final Geocache cache, final CacheDetailActivity activity) {
        if (cache == null) {
            return;
        }
        if (cache.getVariables().wasModified()) {
            // make sure variables state gets updated before saving cache
            cache.getVariables().saveState();
        }
        activity.ensureSaved();
        final FragmentManager fm = activity.getSupportFragmentManager();
        final PersonalNoteCapability connector = ConnectorFactory.getConnectorAs(cache, PersonalNoteCapability.class);
        final EditNoteDialog dialog = EditNoteDialog.newInstance(cache.getPersonalNote(), cache.isPreventWaypointsFromNote(), (connector != null && connector.canAddPersonalNote(cache)));
        dialog.show(fm, "fragment_edit_note");
    }

    @Override
    public void onFinishEditNoteDialog(final String note, final boolean preventWaypointsFromNote, final boolean uploadNote) {
        setNewPersonalNote(note, preventWaypointsFromNote);
        if (uploadNote) {
            checkAndUploadPersonalNote(ConnectorFactory.getConnectorAs(cache, PersonalNoteCapability.class));
        }
        scrollToBottom();
        lastActionWasEditNote = true;
    }

    @Override
    public void onDismissEditNoteDialog() {
        if (activityIsStartedForEditNote) {
            finish();
        }
    }

    private void uploadPersonalNote() {
        final ProgressButtonDisposableHandler myHandler = new ProgressButtonDisposableHandler(this);
        myHandler.showProgress(findViewById(R.id.upload_personalnote));

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

    private void checkAndUploadPersonalNote(final PersonalNoteCapability connector) {
        final int personalNoteLength = StringUtils.length(cache.getPersonalNote());
        if (personalNoteLength > connector.getPersonalNoteMaxChars()) {
            final int reduceLength = personalNoteLength - connector.getPersonalNoteMaxChars();
            SimpleDialog.of(this).setTitle(R.string.cache_personal_note_limit).setMessage(R.string.cache_personal_note_truncated_by, reduceLength, connector.getPersonalNoteMaxChars(), connector.getName()).confirm(
                    this::uploadPersonalNote);
        } else {
            uploadPersonalNote();
        }
    }

    public void removeWaypointsFromPersonalNote(final Geocache cache) {
        final String note = cache.getPersonalNote() == null ? "" : cache.getPersonalNote();
        final String newNote = CacheArtefactParser.removeParseableWaypointsFromText(note);
        if (newNote != null) {
            setNewPersonalNote(newNote);
        }
        showShortToast(note.equals(newNote) ? R.string.cache_personal_note_removewaypoints_nowaypoints : R.string.cache_personal_note_removedwaypoints);
    }

    public void storeWaypointsInPersonalNote(final Geocache cache) {
        final String note = cache.getPersonalNote() == null ? "" : cache.getPersonalNote();

        //only user modified waypoints
        final List<Waypoint> userModifiedWaypoints = new ArrayList<>();
        for (Waypoint w : cache.getWaypoints()) {
            if (w.isUserModified()) {
                userModifiedWaypoints.add(w);
            }
        }
        if (userModifiedWaypoints.isEmpty() && cache.getVariables().isEmpty()) {
            showShortToast(getString(R.string.cache_personal_note_storewaypoints_nowaypoints));
            return;
        }

        final String newNote = CacheArtefactParser.putParseableWaypointsInText(note, userModifiedWaypoints, cache.getVariables());
        setNewPersonalNote(newNote);
        showShortToast(R.string.cache_personal_note_storewaypoints_success);
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
        // trim note to avoid unnecessary uploads for whitespace only changes
        final String trimmedNote = StringUtils.trim(newNote);

        final String oldAllUserNotes = cache.getAllUserNotes();

        cache.setPersonalNote(trimmedNote);
        cache.setPreventWaypointsFromNote(newPreventWaypointsFromNote);

        if (cache.addCacheArtefactsFromNotes(oldAllUserNotes)) {
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
            setPersonalNote(personalNoteView, separator, trimmedNote);
        } else {
            reinitializePage(Page.DESCRIPTION.id);
        }
        adjustPersonalNoteVarsOutOfSyncButton();

        Schedulers.io().scheduleDirect(this::saveAndNotify);
    }

    private void adjustPersonalNoteVarsOutOfSyncButton() {
        adjustPersonalNoteVarsOutOfSyncButton(findViewById(R.id.personalnote_vars_out_of_sync));
    }

    private void adjustPersonalNoteVarsOutOfSyncButton(@Nullable final TextView personalNotOutOfSync) {
        if (personalNotOutOfSync == null) {
            return;
        }
        final Map<String, Pair<String, String>> varsOutOfSync = cache.getVariableDifferencesFromUserNotes();
        personalNotOutOfSync.setVisibility(varsOutOfSync.isEmpty() ? View.GONE : View.VISIBLE);
        personalNotOutOfSync.setText(LocalizationUtils.getString(R.string.cache_personal_note_vars_out_of_sync, varsOutOfSync.size()));
    }

    private static void setPersonalNote(final TextView personalNoteView, final View separator, final String personalNote) {
        personalNoteView.setText(personalNote, TextView.BufferType.SPANNABLE);
        if (StringUtils.isNotBlank(personalNote)) {
            personalNoteView.setVisibility(View.VISIBLE);
            separator.setVisibility(View.VISIBLE);
            ViewUtils.safeAddLinks(personalNoteView, Linkify.MAP_ADDRESSES | Linkify.WEB_URLS);
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
