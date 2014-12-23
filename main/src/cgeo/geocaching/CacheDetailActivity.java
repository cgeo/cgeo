package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.calendar.CalendarAddon;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.AbstractActivity.ActivitySharingInterface;
import cgeo.geocaching.activity.AbstractViewPagerActivity;
import cgeo.geocaching.activity.INavigationSource;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cache.navi.NavigationSelectionActionProvider;
import cgeo.geocaching.apps.cachelist.MapsWithMeCacheListApp;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.ui.CacheDetailsCreator;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;
import cgeo.geocaching.ui.DecryptTextClickListener;
import cgeo.geocaching.ui.EditNoteDialog;
import cgeo.geocaching.ui.EditNoteDialog.EditNoteDialogListener;
import cgeo.geocaching.ui.ImagesList;
import cgeo.geocaching.ui.IndexOutOfBoundsAvoidingTextView;
import cgeo.geocaching.ui.LoggingUI;
import cgeo.geocaching.ui.NavigationActionProvider;
import cgeo.geocaching.ui.OwnerActionsClickListener;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.logs.CacheLogsViewCreator;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.RxUtils;
import cgeo.geocaching.utils.SimpleCancellableHandler;
import cgeo.geocaching.utils.SimpleHandler;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.R.color;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Activity to handle all single-cache-stuff.
 *
 * e.g. details, description, logs, waypoints, inventory...
 */
public class CacheDetailActivity extends AbstractViewPagerActivity<CacheDetailActivity.Page>
        implements CacheMenuHandler.ActivityInterface, INavigationSource, ActivitySharingInterface, EditNoteDialogListener {

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

    private Geocache cache;
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
    private CompositeSubscription createSubscriptions;
    /**
     * waypoint selected in context menu. This variable will be gone when the waypoint context menu is a fragment.
     */
    private Waypoint selectedWaypoint;

    private boolean requireGeodata;
    private Subscription geoDataSubscription = Subscriptions.empty();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.cachedetail_activity);

        createSubscriptions = new CompositeSubscription();

        // get parameters
        final Bundle extras = getIntent().getExtras();
        final Uri uri = getIntent().getData();

        // try to get data from extras
        String name = null;
        String geocode = null;
        String guid = null;
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            name = extras.getString(Intents.EXTRA_NAME);
            guid = extras.getString(Intents.EXTRA_GUID);
        }

        // When clicking a cache in MapsWithMe, we get back a PendingIntent
        if (StringUtils.isEmpty(geocode)) {
            geocode = MapsWithMeCacheListApp.getCacheFromMapsWithMe(this, getIntent());
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
            } else if (uriHost.contains("coord.info")) {
                if (StringUtils.startsWith(uriPath, "/gc")) {
                    geocode = uriPath.substring(1).toUpperCase(Locale.US);
                } else {
                    showToast(res.getString(R.string.err_detail_open));
                    finish();
                    return;
                }
            } else if (uriHost.contains("opencaching.de") || uriHost.contains("opencaching.fr")) {
                if (StringUtils.startsWith(uriPath, "/oc")) {
                    geocode = uriPath.substring(1).toUpperCase(Locale.US);
                } else {
                    geocode = uri.getQueryParameter("wp");
                    if (StringUtils.isNotBlank(geocode)) {
                        geocode = geocode.toUpperCase(Locale.US);
                    } else {
                        showToast(res.getString(R.string.err_detail_open));
                        finish();
                        return;
                    }
                }
            } else {
                showToast(res.getString(R.string.err_detail_open));
                finish();
                return;
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
            } else if (null != geocode && StringUtils.isNotBlank(geocode)) { // can't be null, but the compiler doesn't understand StringUtils.isNotBlank()
                title = geocode;
            }
            progress.show(this, title, res.getString(R.string.cache_dialog_loading_details), true, loadCacheHandler.cancelMessage());
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
                startOrStopGeoDataListener();
            }
        });
        requireGeodata = pageToOpen == 1;

        final String realGeocode = geocode;
        final String realGuid = guid;
        RxUtils.networkScheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                search = Geocache.searchByGeocode(realGeocode, StringUtils.isBlank(realGeocode) ? realGuid : null, 0, false, loadCacheHandler);
                loadCacheHandler.sendMessage(Message.obtain());
            }
        });

        locationUpdater = new CacheDetailsGeoDirHandler(this);

        // If we have a newer Android device setup Android Beam for easy cache sharing
        initializeAndroidBeam(this);
    }

    @Override
    public String getAndroidBeamUri() {
        return cache != null ? cache.getCgeoUrl() : null;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE_INDEX, getCurrentItem());
    }

    private void startOrStopGeoDataListener() {
        geoDataSubscription.unsubscribe();
        if (requireGeodata) {
            geoDataSubscription = locationUpdater.start(GeoDirHandler.UPDATE_GEODATA);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startOrStopGeoDataListener();

        if (refreshOnResume) {
            notifyDataSetChanged();
            refreshOnResume = false;
        }
    }

    @Override
    public void onPause() {
        geoDataSubscription.unsubscribe();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        createSubscriptions.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        if (cache != null) {
            cache.setChangeNotificationHandler(null);
        }
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
                final boolean isOriginalWaypoint = selectedWaypoint.getWaypointType().equals(WaypointType.ORIGINAL);
                menu.findItem(R.id.menu_waypoint_reset_cache_coords).setVisible(isOriginalWaypoint);
                menu.findItem(R.id.menu_waypoint_edit).setVisible(!isOriginalWaypoint);
                menu.findItem(R.id.menu_waypoint_duplicate).setVisible(!isOriginalWaypoint);
                final boolean userDefined = selectedWaypoint.isUserDefined() && !selectedWaypoint.getWaypointType().equals(WaypointType.ORIGINAL);
                menu.findItem(R.id.menu_waypoint_delete).setVisible(userDefined);
                final boolean hasCoords = selectedWaypoint.getCoords() != null;
                final MenuItem defaultNavigationMenu = menu.findItem(R.id.menu_waypoint_navigate_default);
                defaultNavigationMenu.setVisible(hasCoords);
                defaultNavigationMenu.setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());
                menu.findItem(R.id.menu_waypoint_navigate).setVisible(hasCoords);
                menu.findItem(R.id.menu_waypoint_caches_around).setVisible(hasCoords);
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
                    CacheListActivity.startActivityCoordinates(this, selectedWaypoint.getCoords());
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
        MenuItem menuItem = menu.findItem(R.id.menu_default_navigation);
        final NavigationActionProvider navAction = (NavigationActionProvider) MenuItemCompat.getActionProvider(menuItem);
        if (navAction != null) {
            navAction.setNavigationSource(this);
        }
        menuItem = menu.findItem(R.id.menu_navigate);
        NavigationSelectionActionProvider.initialize(menuItem, cache);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        CacheMenuHandler.onPrepareOptionsMenu(menu, cache);
        LoggingUI.onPrepareOptionsMenu(menu, cache);
        menu.findItem(R.id.menu_store).setVisible(cache != null && !cache.isOffline());
        menu.findItem(R.id.menu_delete).setVisible(cache != null && cache.isOffline());
        menu.findItem(R.id.menu_refresh).setVisible(cache != null && cache.isOffline());
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
            case R.id.menu_store:
                storeCache();
                return true;
            case R.id.menu_refresh:
                refreshCache();
                return true;
            default:
                if (NavigationAppFactory.onMenuItemSelected(item, this, cache)) {
                    return true;
                }
                if (LoggingUI.onMenuItemSelected(item, this, cache)) {
                    refreshOnResume = true;
                    return true;
                }
        }

        return super.onOptionsItemSelected(item);
    }

    private static final class CacheDetailsGeoDirHandler extends GeoDirHandler {
        private final WeakReference<CacheDetailActivity> activityRef;

        public CacheDetailsGeoDirHandler(final CacheDetailActivity activity) {
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

    private final static class LoadCacheHandler extends SimpleCancellableHandler {

        public LoadCacheHandler(final CacheDetailActivity activity, final Progress progress) {
            super(activity, progress);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            if (UPDATE_LOAD_PROGRESS_DETAIL == msg.what && msg.obj instanceof String) {
                updateStatusMsg((String) msg.obj);
            } else {
                final CacheDetailActivity activity = ((CacheDetailActivity) activityRef.get());
                if (activity == null) {
                    return;
                }
                if (activity.search == null) {
                    showToast(R.string.err_dwld_details_failed);
                    dismissProgress();
                    finishActivity();
                    return;
                }

                if (activity.search.getError() != null) {
                    activity.showToast(activity.getResources().getString(R.string.err_dwld_details_failed) + " " + activity.search.getError().getErrorString(activity.getResources()) + ".");
                    dismissProgress();
                    finishActivity();
                    return;
                }

                updateStatusMsg(activity.getResources().getString(R.string.cache_dialog_loading_details_status_render));

                // Data loaded, we're ready to show it!
                activity.notifyDataSetChanged();
            }
        }

        private void updateStatusMsg(final String msg) {
            final CacheDetailActivity activity = ((CacheDetailActivity) activityRef.get());
            if (activity == null) {
                return;
            }
            setProgressMessage(activity.getResources().getString(R.string.cache_dialog_loading_details)
                    + "\n\n"
                    + msg);
        }

        @Override
        public void handleCancel(final Object extra) {
            finishActivity();
        }

    }

    private void notifyDataSetChanged() {
        // This might get called asynchronically when the activity is shut down
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
        imagesList = new ImagesList(this, cache.getGeocode());
        createSubscriptions.add(imagesList.loadImages(imageView, cache.getImages(), false));
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

        final private int titleStringId;

        Page(final int titleStringId) {
            this.titleStringId = titleStringId;
        }
    }

    private class AttributeViewBuilder {
        private ViewGroup attributeIconsLayout; // layout for attribute icons
        private ViewGroup attributeDescriptionsLayout; // layout for attribute descriptions
        private boolean attributesShowAsIcons = true; // default: show icons
        /**
         * If the cache is from a non GC source, it might be without icons. Disable switching in those cases.
         */
        private boolean noAttributeIconsFound = false;
        private int attributeBoxMaxWidth;

        public void fillView(final LinearLayout attributeBox) {
            // first ensure that the view is empty
            attributeBox.removeAllViews();

            // maximum width for attribute icons is screen width - paddings of parents
            attributeBoxMaxWidth = Compatibility.getDisplayWidth();
            ViewParent child = attributeBox;
            do {
                if (child instanceof View) {
                    attributeBoxMaxWidth -= ((View) child).getPaddingLeft() + ((View) child).getPaddingRight();
                }
                child = child.getParent();
            } while (child != null);

            // delete views holding description / icons
            attributeDescriptionsLayout = null;
            attributeIconsLayout = null;

            attributeBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    // toggle between attribute icons and descriptions
                    toggleAttributeDisplay(attributeBox, attributeBoxMaxWidth);
                }
            });

            // icons or text?
            //
            // also show icons when noAttributeImagesFound == true. Explanation:
            //  1. no icons could be found in the first invocation of this method
            //  2. user refreshes cache from web
            //  3. now this method is called again
            //  4. attributeShowAsIcons is false but noAttributeImagesFound is true
            //     => try to show them now
            if (attributesShowAsIcons || noAttributeIconsFound) {
                showAttributeIcons(attributeBox, attributeBoxMaxWidth);
            } else {
                showAttributeDescriptions(attributeBox);
            }
        }

        /**
         * lazy-creates the layout holding the icons of the caches attributes
         * and makes it visible
         */
        private void showAttributeIcons(final LinearLayout attribBox, final int parentWidth) {
            if (attributeIconsLayout == null) {
                attributeIconsLayout = createAttributeIconsLayout(parentWidth);
                // no matching icons found? show text
                if (noAttributeIconsFound) {
                    showAttributeDescriptions(attribBox);
                    return;
                }
            }
            attribBox.removeAllViews();
            attribBox.addView(attributeIconsLayout);
            attributesShowAsIcons = true;
        }

        /**
         * lazy-creates the layout holding the descriptions of the caches attributes
         * and makes it visible
         */
        private void showAttributeDescriptions(final LinearLayout attribBox) {
            if (attributeDescriptionsLayout == null) {
                attributeDescriptionsLayout = createAttributeDescriptionsLayout(attribBox);
            }
            attribBox.removeAllViews();
            attribBox.addView(attributeDescriptionsLayout);
            attributesShowAsIcons = false;
        }

        /**
         * toggle attribute descriptions and icons
         */
        private void toggleAttributeDisplay(final LinearLayout attribBox, final int parentWidth) {
            // Don't toggle when there are no icons to show.
            if (noAttributeIconsFound) {
                return;
            }

            // toggle
            if (attributesShowAsIcons) {
                showAttributeDescriptions(attribBox);
            } else {
                showAttributeIcons(attribBox, parentWidth);
            }
        }

        private ViewGroup createAttributeIconsLayout(final int parentWidth) {
            final LinearLayout rows = new LinearLayout(CacheDetailActivity.this);
            rows.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            rows.setOrientation(LinearLayout.VERTICAL);

            LinearLayout attributeRow = newAttributeIconsRow();
            rows.addView(attributeRow);

            noAttributeIconsFound = true;

            for (final String attributeName : cache.getAttributes()) {
                // check if another attribute icon fits in this row
                attributeRow.measure(0, 0);
                final int rowWidth = attributeRow.getMeasuredWidth();
                final FrameLayout fl = (FrameLayout) getLayoutInflater().inflate(R.layout.attribute_image, attributeRow, false);
                final ImageView iv = (ImageView) fl.getChildAt(0);
                if ((parentWidth - rowWidth) < iv.getLayoutParams().width) {
                    // make a new row
                    attributeRow = newAttributeIconsRow();
                    rows.addView(attributeRow);
                }

                final boolean strikeThrough = !CacheAttribute.isEnabled(attributeName);
                final CacheAttribute attrib = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attributeName));
                if (attrib != null) {
                    noAttributeIconsFound = false;
                    Drawable drawable = res.getDrawable(attrib.drawableId);
                    iv.setImageDrawable(drawable);
                    // strike through?
                    if (strikeThrough) {
                        // generate strike through image with same properties as attribute image
                        final ImageView strikeThroughImage = new ImageView(CacheDetailActivity.this);
                        strikeThroughImage.setLayoutParams(iv.getLayoutParams());
                        drawable = res.getDrawable(R.drawable.attribute__strikethru);
                        strikeThroughImage.setImageDrawable(drawable);
                        fl.addView(strikeThroughImage);
                    }
                } else {
                    iv.setImageDrawable(res.getDrawable(R.drawable.attribute_unknown));
                }

                attributeRow.addView(fl);
            }

            return rows;
        }

        private LinearLayout newAttributeIconsRow() {
            final LinearLayout rowLayout = new LinearLayout(CacheDetailActivity.this);
            rowLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            return rowLayout;
        }

        private ViewGroup createAttributeDescriptionsLayout(final LinearLayout parentView) {
            final LinearLayout descriptions = (LinearLayout) getLayoutInflater().inflate(
                    R.layout.attribute_descriptions, parentView, false);
            final TextView attribView = (TextView) descriptions.getChildAt(0);

            final StringBuilder buffer = new StringBuilder();
            for (String attributeName : cache.getAttributes()) {
                final boolean enabled = CacheAttribute.isEnabled(attributeName);
                // search for a translation of the attribute
                final CacheAttribute attrib = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attributeName));
                if (attrib != null) {
                    attributeName = attrib.getL10n(enabled);
                }
                if (buffer.length() > 0) {
                    buffer.append('\n');
                }
                buffer.append(attributeName);
            }

            attribView.setText(buffer);

            return descriptions;
        }
    }

    private void refreshCache() {
        if (progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        if (!Network.isNetworkConnected()) {
            showToast(getString(R.string.err_server));
            return;
        }

        final RefreshCacheHandler refreshCacheHandler = new RefreshCacheHandler(this, progress);

        progress.show(this, res.getString(R.string.cache_dialog_refresh_title), res.getString(R.string.cache_dialog_refresh_message), true, refreshCacheHandler.cancelMessage());

        cache.refresh(refreshCacheHandler, RxUtils.networkScheduler);
    }

    private void dropCache() {
        if (progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        progress.show(this, res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true, null);
        cache.drop(new ChangeNotificationHandler(this, progress));
    }

    private void storeCache() {
        if (progress.isShowing()) {
            showToast(res.getString(R.string.err_detail_still_working));
            return;
        }

        if (Settings.getChooseList()) {
            // let user select list to store cache in
            new StoredList.UserInterface(this).promptForListSelection(R.string.list_title,
                    new Action1<Integer>() {
                        @Override
                        public void call(final Integer selectedListId) {
                            storeCache(selectedListId, new StoreCacheHandler(CacheDetailActivity.this, progress));
                        }
                    }, true, StoredList.TEMPORARY_LIST.id);
        } else {
            storeCache(StoredList.TEMPORARY_LIST.id, new StoreCacheHandler(this, progress));
        }
    }

    /**
     * Creator for details-view.
     */
    private class DetailsViewCreator extends AbstractCachingPageViewCreator<ScrollView> {
        /**
         * Reference to the details list, so that the helper-method can access it without an additional argument
         */
        private LinearLayout detailsList;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            if (cache == null) {
                // something is really wrong
                return null;
            }

            view = (ScrollView) getLayoutInflater().inflate(R.layout.cachedetail_details_page, parentView, false);

            // Start loading preview map
            AndroidObservable.bindActivity(CacheDetailActivity.this, previewMap).subscribeOn(RxUtils.networkScheduler)
                    .subscribe(new Action1<BitmapDrawable>() {
                        @Override
                        public void call(final BitmapDrawable image) {
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
            final Spannable span = (new Spannable.Factory()).newSpannable(cache.getName());
            if (cache.isDisabled() || cache.isArchived()) { // strike
                span.setSpan(new StrikethroughSpan(), 0, span.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (cache.isArchived()) {
                span.setSpan(new ForegroundColorSpan(res.getColor(R.color.archived_cache_color)), 0, span.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            addContextMenu(details.add(R.string.cache_name, span));
            details.add(R.string.cache_type, cache.getType().getL10n());
            details.addSize(cache);
            addContextMenu(details.add(R.string.cache_geocode, cache.getGeocode()));
            details.addCacheState(cache);

            details.addDistance(cache, cacheDistanceView);
            cacheDistanceView = details.getValueView();

            details.addDifficulty(cache);
            details.addTerrain(cache);
            details.addRating(cache);

            // favorite count
            if (cache.getFavoritePoints() > 0) {
                details.add(R.string.cache_favorite, cache.getFavoritePoints() + "×");
            }

            // own rating
            if (cache.getMyVote() > 0) {
                details.addStars(R.string.cache_own_rating, cache.getMyVote());
            }

            // cache author
            if (StringUtils.isNotBlank(cache.getOwnerDisplayName()) || StringUtils.isNotBlank(cache.getOwnerUserId())) {
                final TextView ownerView = details.add(R.string.cache_owner, "");
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
                final TextView valueView = details.add(R.string.cache_coordinates, cache.getCoords().toString());
                valueView.setOnClickListener(new CoordinatesFormatSwitcher(cache.getCoords()));
                addContextMenu(valueView);
            }

            // cache attributes
            if (!cache.getAttributes().isEmpty()) {
                final LinearLayout innerLayout = ButterKnife.findById(view, R.id.attributes_innerbox);
                new AttributeViewBuilder().fillView(innerLayout);
                view.findViewById(R.id.attributes_box).setVisibility(View.VISIBLE);
            }

            updateOfflineBox(view, cache, res, new RefreshCacheClickListener(), new DropCacheClickListener(), new StoreCacheClickListener());

            // watchlist
            final Button buttonWatchlistAdd = ButterKnife.findById(view, R.id.add_to_watchlist);
            final Button buttonWatchlistRemove = ButterKnife.findById(view, R.id.remove_from_watchlist);
            buttonWatchlistAdd.setOnClickListener(new AddToWatchlistClickListener());
            buttonWatchlistRemove.setOnClickListener(new RemoveFromWatchlistClickListener());
            updateWatchlistBox();

            // favorite points
            final Button buttonFavPointAdd = ButterKnife.findById(view, R.id.add_to_favpoint);
            final Button buttonFavPointRemove = ButterKnife.findById(view, R.id.remove_from_favpoint);
            buttonFavPointAdd.setOnClickListener(new FavoriteAddClickListener());
            buttonFavPointRemove.setOnClickListener(new FavoriteRemoveClickListener());
            updateFavPointBox();

            // list
            final Button buttonChangeList = ButterKnife.findById(view, R.id.change_list);
            buttonChangeList.setOnClickListener(new ChangeListClickListener());
            updateListBox();

            // data license
            final IConnector connector = ConnectorFactory.getConnector(cache);
            final String license = connector.getLicenseText(cache);
            if (StringUtils.isNotBlank(license)) {
                view.findViewById(R.id.license_box).setVisibility(View.VISIBLE);
                final TextView licenseView = (ButterKnife.findById(view, R.id.license));
                licenseView.setText(Html.fromHtml(license), BufferType.SPANNABLE);
                licenseView.setClickable(true);
                licenseView.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            } else {
                view.findViewById(R.id.license_box).setVisibility(View.GONE);
            }

            return view;
        }

        private class StoreCacheClickListener implements View.OnClickListener {
            @Override
            public void onClick(final View arg0) {
                storeCache();
            }

        }

        private class RefreshCacheClickListener implements View.OnClickListener {
            @Override
            public void onClick(final View arg0) {
                refreshCache();
            }
        }

        private class DropCacheClickListener implements View.OnClickListener {
            @Override
            public void onClick(final View arg0) {
                dropCache();
            }
        }

        /**
         * Abstract Listener for add / remove buttons for watchlist
         */
        private abstract class AbstractPropertyListener implements View.OnClickListener {

            private final SimpleCancellableHandler handler = new SimpleCancellableHandler(CacheDetailActivity.this, progress) {
                @Override
                public void handleRegularMessage(final Message message) {
                    super.handleRegularMessage(message);
                    updateWatchlistBox();
                    updateFavPointBox();
                }
            };

            public void doExecute(final int titleId, final int messageId, final Action1<SimpleCancellableHandler> action) {
                if (progress.isShowing()) {
                    showToast(res.getString(R.string.err_watchlist_still_managing));
                    return;
                }
                progress.show(CacheDetailActivity.this, res.getString(titleId), res.getString(messageId), true, null);
                RxUtils.networkScheduler.createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
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
                        new Action1<SimpleCancellableHandler>() {
                            @Override
                            public void call(final SimpleCancellableHandler simpleCancellableHandler) {
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
                        new Action1<SimpleCancellableHandler>() {
                            @Override
                            public void call(final SimpleCancellableHandler simpleCancellableHandler) {
                                watchListRemove(simpleCancellableHandler);
                            }
                        });
            }
        }

        /** Add this cache to the watchlist of the user */
        private void watchListAdd(final SimpleCancellableHandler handler) {
            if (ConnectorFactory.getConnector(cache).addToWatchlist(cache)) {
                handler.obtainMessage(MESSAGE_SUCCEEDED).sendToTarget();
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_watchlist_failed);
            }
        }

        /** Remove this cache from the watchlist of the user */
        private void watchListRemove(final SimpleCancellableHandler handler) {
            if (ConnectorFactory.getConnector(cache).removeFromWatchlist(cache)) {
                handler.obtainMessage(MESSAGE_SUCCEEDED).sendToTarget();
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_watchlist_failed);
            }
        }

        /** Add this cache to the favorite list of the user */
        private void favoriteAdd(final SimpleCancellableHandler handler) {
            if (GCConnector.addToFavorites(cache)) {
                handler.obtainMessage(MESSAGE_SUCCEEDED).sendToTarget();
            } else {
                handler.sendTextMessage(MESSAGE_FAILED, R.string.err_favorite_failed);
            }
        }

        /** Remove this cache to the favorite list of the user */
        private void favoriteRemove(final SimpleCancellableHandler handler) {
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
                        new Action1<SimpleCancellableHandler>() {
                            @Override
                            public void call(final SimpleCancellableHandler simpleCancellableHandler) {
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
                        new Action1<SimpleCancellableHandler>() {
                            @Override
                            public void call(final SimpleCancellableHandler simpleCancellableHandler) {
                                favoriteRemove(simpleCancellableHandler);
                            }
                        });
            }
        }

        /**
         * Listener for "change list" button
         */
        private class ChangeListClickListener implements View.OnClickListener {
            @Override
            public void onClick(final View v) {
                new StoredList.UserInterface(CacheDetailActivity.this).promptForListSelection(R.string.list_title,
                        new Action1<Integer>() {
                            @Override
                            public void call(final Integer selectedListId) {
                                switchListById(selectedListId);
                            }
                        }, true, cache.getListId());
            }
        }

        /**
         * move cache to another list
         *
         * @param listId
         *            the ID of the list
         */
        public void switchListById(final int listId) {
            if (listId < 0) {
                return;
            }

            Settings.saveLastList(listId);
            DataStore.moveToList(cache, listId);
            updateListBox();
        }

        /**
         * shows/hides buttons, sets text in watchlist box
         */
        private void updateWatchlistBox() {
            final LinearLayout layout = ButterKnife.findById(view, R.id.watchlist_box);
            final boolean supportsWatchList = cache.supportsWatchList();
            layout.setVisibility(supportsWatchList ? View.VISIBLE : View.GONE);
            if (!supportsWatchList) {
                return;
            }
            final Button buttonAdd = ButterKnife.findById(view, R.id.add_to_watchlist);
            final Button buttonRemove = ButterKnife.findById(view, R.id.remove_from_watchlist);
            final TextView text = ButterKnife.findById(view, R.id.watchlist_text);

            if (cache.isOnWatchlist() || cache.isOwner()) {
                buttonAdd.setVisibility(View.GONE);
                buttonRemove.setVisibility(View.VISIBLE);
                text.setText(R.string.cache_watchlist_on);
            } else {
                buttonAdd.setVisibility(View.VISIBLE);
                buttonRemove.setVisibility(View.GONE);
                text.setText(R.string.cache_watchlist_not_on);
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
         * shows/hides buttons, sets text in watchlist box
         */
        private void updateFavPointBox() {
            final LinearLayout layout = ButterKnife.findById(view, R.id.favpoint_box);
            final boolean supportsFavoritePoints = cache.supportsFavoritePoints();
            layout.setVisibility(supportsFavoritePoints ? View.VISIBLE : View.GONE);
            if (!supportsFavoritePoints || cache.isOwner() || !Settings.isGCPremiumMember()) {
                return;
            }
            final Button buttonAdd = ButterKnife.findById(view, R.id.add_to_favpoint);
            final Button buttonRemove = ButterKnife.findById(view, R.id.remove_from_favpoint);
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

        /**
         * shows/hides/updates list box
         */
        private void updateListBox() {
            final View box = view.findViewById(R.id.list_box);

            if (cache.isOffline()) {
                // show box
                box.setVisibility(View.VISIBLE);

                // update text
                final TextView text = ButterKnife.findById(view, R.id.list_text);
                final StoredList list = DataStore.getList(cache.getListId());
                if (list != null) {
                    text.setText(res.getString(R.string.cache_list_text) + " " + list.title);
                } else {
                    // this should not happen
                    text.setText(R.string.cache_list_unknown);
                }
            } else {
                // hide box
                box.setVisibility(View.GONE);
            }
        }
    }

    private final Observable<BitmapDrawable> previewMap = Observable.create(new OnSubscribe<BitmapDrawable>() {
        @Override
        public void call(final Subscriber<? super BitmapDrawable> subscriber) {
            try {
                // persistent preview from storage
                Bitmap image = StaticMapsProvider.getPreviewMap(cache);

                if (image == null) {
                    if (Settings.isStoreOfflineMaps() && cache.getCoords() != null) {
                        RxUtils.waitForCompletion(StaticMapsProvider.storeCachePreviewMap(cache));
                        image = StaticMapsProvider.getPreviewMap(cache);
                    }
                }

                if (image != null) {
                    subscriber.onNext(ImageUtils.scaleBitmapToFitDisplay(image));
                }
                subscriber.onCompleted();
            } catch (final Exception e) {
                Log.w("CacheDetailActivity.previewMap", e);
                subscriber.onError(e);
            }
        }

    });

    protected class DescriptionViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @InjectView(R.id.personalnote) protected TextView personalNoteView;
        @InjectView(R.id.shortdesc) protected IndexOutOfBoundsAvoidingTextView shortDescView;
        @InjectView(R.id.longdesc) protected IndexOutOfBoundsAvoidingTextView longDescView;
        @InjectView(R.id.show_description) protected Button showDesc;
        @InjectView(R.id.loading) protected View loadingView;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            if (cache == null) {
                // something is really wrong
                return null;
            }

            view = (ScrollView) getLayoutInflater().inflate(R.layout.cachedetail_description_page, parentView, false);
            ButterKnife.inject(this, view);

            // cache short description
            if (StringUtils.isNotBlank(cache.getShortDescription())) {
                loadDescription(cache.getShortDescription(), shortDescView, null);
            }

            // long description
            if (StringUtils.isNotBlank(cache.getDescription())) {
                if (Settings.isAutoLoadDescription()) {
                    loadLongDescription();
                } else {
                    showDesc.setVisibility(View.VISIBLE);
                    showDesc.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View arg0) {
                            loadLongDescription();
                        }
                    });
                }
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
            if (cache.isOffline() && ConnectorFactory.getConnector(cache).supportsPersonalNote()) {
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

            final TextView hintView = (ButterKnife.findById(view, R.id.hint));
            if (StringUtils.isNotBlank(cache.getHint())) {
                if (TextUtils.containsHtml(cache.getHint())) {
                    hintView.setText(Html.fromHtml(cache.getHint(), new HtmlImage(cache.getGeocode(), false, cache.getListId(), false), null), TextView.BufferType.SPANNABLE);
                    hintView.setText(CryptUtils.rot13((Spannable) hintView.getText()));
                }
                else {
                    hintView.setText(CryptUtils.rot13(cache.getHint()));
                }
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

            final TextView spoilerlinkView = (ButterKnife.findById(view, R.id.hint_spoilerlink));
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

                        ImagesActivity.startActivitySpoilerImages(CacheDetailActivity.this, cache.getGeocode(), cache.getSpoilers());
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
            final SimpleCancellableHandler myHandler = new SimpleCancellableHandler(CacheDetailActivity.this, progress);

            final Message cancelMessage = myHandler.cancelMessage(res.getString(R.string.cache_personal_note_upload_cancelled));
            progress.show(CacheDetailActivity.this, res.getString(R.string.cache_personal_note_uploading), res.getString(R.string.cache_personal_note_uploading), true, cancelMessage);

            myHandler.unsubscribeIfCancelled(RxUtils.networkScheduler.createWorker().schedule(new Action0() {
                @Override
                public void call() {
                    final IConnector con = ConnectorFactory.getConnector(cache);
                    if (con.supportsPersonalNote()) {
                        con.uploadPersonalNote(cache);
                    }
                    final Message msg = Message.obtain();
                    final Bundle bundle = new Bundle();
                    bundle.putString(SimpleCancellableHandler.MESSAGE_TEXT, CgeoApplication.getInstance().getString(R.string.cache_personal_note_upload_done));
                    msg.setData(bundle);
                    myHandler.sendMessage(msg);
                }
            }));
        }

        private void loadLongDescription() {
            showDesc.setVisibility(View.GONE);
            showDesc.setOnClickListener(null);
            loadingView.setVisibility(View.VISIBLE);

            final String longDescription = cache.getDescription();
            loadDescription(longDescription, longDescView, loadingView);
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

    }

    // If description has an HTML construct which may be problematic to render, add a note at the end of the long description.
    // Technically, it may not be a table, but a pre, which has the same problems as a table, so the message is ok even though
    // sometimes technically incorrect.
    private void addWarning(final UnknownTagsHandler unknownTagsHandler, final Spanned description) {
        if (unknownTagsHandler.isProblematicDetected()) {
            final int startPos = description.length();
            final IConnector connector = ConnectorFactory.getConnector(cache);
            final Spanned tableNote = Html.fromHtml(res.getString(R.string.cache_description_table_note, "<a href=\"" + cache.getUrl() + "\">" + connector.getName() + "</a>"));
            ((Editable) description).append("\n\n").append(tableNote);
            ((Editable) description).setSpan(new StyleSpan(Typeface.ITALIC), startPos, description.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    /**
     * Load the description in the background.
     * @param descriptionString the HTML description as retrieved from the connector
     * @param descriptionView the view to fill
     * @param loadingIndicatorView the loading indicator view, will be hidden when completed
     */
    private void loadDescription(final String descriptionString, final IndexOutOfBoundsAvoidingTextView descriptionView, final View loadingIndicatorView) {
        try {
            final UnknownTagsHandler unknownTagsHandler = new UnknownTagsHandler();
            final Spanned description = Html.fromHtml(descriptionString, new HtmlImage(cache.getGeocode(), true, cache.getListId(), false, descriptionView), unknownTagsHandler);
            addWarning(unknownTagsHandler, description);
            if (StringUtils.isNotBlank(descriptionString)) {
                try {
                    descriptionView.setText(description, TextView.BufferType.SPANNABLE);
                } catch (final Exception e) {
                    // On 4.1, there is sometimes a crash on measuring the layout: https://code.google.com/p/android/issues/detail?id=35412
                    Log.e("Android bug setting text: ", e);
                    // remove the formatting by converting to a simple string
                    descriptionView.setText(description.toString());
                }
                descriptionView.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
                fixTextColor(descriptionString, descriptionView);
                descriptionView.setVisibility(View.VISIBLE);
                addContextMenu(descriptionView);
                potentiallyHideShortDescription();
            }
            if (null != loadingIndicatorView) {
                loadingIndicatorView.setVisibility(View.GONE);
            }
        } catch (final Exception ignored) {
            showToast(res.getString(R.string.err_load_descr_failed));
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
        final View shortView = ButterKnife.findById(this, R.id.shortdesc);
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
            cache.setListId(StoredList.STANDARD_LIST_ID);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
                    return null;
                }
            }.execute();
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
                    if (null == rowView) {
                        rowView = getLayoutInflater().inflate(R.layout.waypoint_item, parent, false);
                        rowView.setClickable(true);
                        rowView.setLongClickable(true);
                        registerForContextMenu(rowView);
                    }
                    WaypointViewHolder holder = (WaypointViewHolder) rowView.getTag();
                    if (null == holder) {
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
            if (null != wpt.getCoords()) {
                coordinatesView.setOnClickListener(new CoordinatesFormatSwitcher(wpt.getCoords()));
                coordinatesView.setText(wpt.getCoords().toString());
                coordinatesView.setVisibility(View.VISIBLE);
            }
            else {
                coordinatesView.setVisibility(View.GONE);
            }

            // info
            final String waypointInfo = Formatter.formatWaypointInfo(wpt);
            final TextView infoView = holder.infoView;
            if (StringUtils.isNotBlank(waypointInfo)) {
                infoView.setText(waypointInfo);
                infoView.setVisibility(View.VISIBLE);
            }
            else {
                infoView.setVisibility(View.GONE);
            }

            // title
            final TextView nameView = holder.nameView;
            if (StringUtils.isNotBlank(wpt.getName())) {
                nameView.setText(StringEscapeUtils.unescapeHtml4(wpt.getName()));
            } else if (null != wpt.getCoords()) {
                nameView.setText(wpt.getCoords().toString());
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
                    noteView.setText(Html.fromHtml(wpt.getNote()), TextView.BufferType.SPANNABLE);
                }
                else {
                    noteView.setText(wpt.getNote());
                }
            }
            else {
                noteView.setVisibility(View.GONE);
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
                        res.getDrawable(waypointType.markerId),
                        res.getDrawable(R.drawable.tick) });
                ld.setLayerInset(0, 0, 0, visitedInset, visitedInset);
                ld.setLayerInset(1, visitedInset, visitedInset, 0, 0);
                icon = ld;
            } else {
                icon = res.getDrawable(waypointType.markerId);
            }
            nameView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
    }

    private class InventoryViewCreator extends AbstractCachingPageViewCreator<ListView> {

        @Override
        public ListView getDispatchedView(final ViewGroup parentView) {
            if (cache == null) {
                // something is really wrong
                return null;
            }

            view = (ListView) getLayoutInflater().inflate(R.layout.cachedetail_inventory_page, parentView, false);

            // TODO: fix layout, then switch back to Android-resource and delete copied one
            // this copy is modified to respect the text color
            view.setAdapter(new ArrayAdapter<>(CacheDetailActivity.this, R.layout.simple_list_item_1, cache.getInventory()));
            view.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3) {
                    final Object selection = arg0.getItemAtPosition(arg2);
                    if (selection instanceof Trackable) {
                        final Trackable trackable = (Trackable) selection;
                        TrackableActivity.startActivity(CacheDetailActivity.this, trackable.getGuid(), trackable.getGeocode(), trackable.getName());
                    }
                }
            });

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

    @Override
    public void addContextMenu(final View view) {
        view.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                startSupportActionMode(new ActionMode.Callback() {

                    @Override
                    public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
                        switch (view.getId()) {
                            case R.id.value: // coordinates, gc-code, name
                                assert view instanceof TextView;
                                clickedItemText = ((TextView) view).getText();
                                final CharSequence itemTitle = ((TextView) ((View) view.getParent()).findViewById(R.id.name)).getText();
                                buildDetailsContextMenu(actionMode, menu, itemTitle, true);
                                return true;
                            case R.id.shortdesc:
                                clickedItemText = cache.getShortDescription();
                                buildDetailsContextMenu(actionMode, menu, res.getString(R.string.cache_description), false);
                                return true;
                            case R.id.longdesc:
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
                                assert view instanceof TextView;
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
                        // do nothing
                    }

                    @Override
                    public boolean onCreateActionMode(final ActionMode actionMode, final Menu menu) {
                        actionMode.getMenuInflater().inflate(R.menu.details_context, menu);

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
                                return onClipboardItemSelected(actionMode, menuItem, clickedItemText);
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

        final String[] items = new String[] { res.getString(R.string.waypoint_localy_reset_cache_coords), res.getString(R.string.waypoint_reset_local_and_remote_cache_coords) };
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
        RxUtils.networkScheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
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
        if (!cache.getLogs().isEmpty()) {
            pages.add(Page.LOGS);
        }
        if (CollectionUtils.isNotEmpty(cache.getFriendsLogs())) {
            pages.add(Page.LOGSFRIENDS);
        }
        if (CollectionUtils.isNotEmpty(cache.getInventory())) {
            pages.add(Page.INVENTORY);
        }
        if (CollectionUtils.isNotEmpty(cache.getImages())) {
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
                                        final OnClickListener storeCacheClickListener) {
        // offline use
        final TextView offlineText = ButterKnife.findById(view, R.id.offline_text);
        final Button offlineRefresh = ButterKnife.findById(view, R.id.offline_refresh);
        final Button offlineStore = ButterKnife.findById(view, R.id.offline_store);

        if (cache.isOffline()) {
            final long diff = (System.currentTimeMillis() / (60 * 1000)) - (cache.getDetailedUpdate() / (60 * 1000)); // minutes

            final String ago;
            if (diff < 15) {
                ago = res.getString(R.string.cache_offline_time_mins_few);
            } else if (diff < 50) {
                ago = res.getString(R.string.cache_offline_time_about) + " " + diff + " " + res.getString(R.string.cache_offline_time_mins);
            } else if (diff < 90) {
                ago = res.getString(R.string.cache_offline_time_about) + " " + res.getString(R.string.cache_offline_time_hour);
            } else if (diff < (48 * 60)) {
                ago = res.getString(R.string.cache_offline_time_about) + " " + (diff / 60) + " " + res.getString(R.string.cache_offline_time_hours);
            } else {
                ago = res.getString(R.string.cache_offline_time_about) + " " + (diff / (24 * 60)) + " " + res.getString(R.string.cache_offline_time_days);
            }

            offlineText.setText(res.getString(R.string.cache_offline_stored) + "\n" + ago);
            offlineRefresh.setOnClickListener(refreshCacheClickListener);

            offlineStore.setText(res.getString(R.string.cache_offline_drop));
            offlineStore.setClickable(true);
            offlineStore.setOnClickListener(dropCacheClickListener);
        } else {
            offlineText.setText(res.getString(R.string.cache_offline_not_ready));
            offlineRefresh.setOnClickListener(refreshCacheClickListener);

            offlineStore.setText(res.getString(R.string.cache_offline_store));
            offlineStore.setClickable(true);
            offlineStore.setOnClickListener(storeCacheClickListener);
        }
        offlineRefresh.setVisibility(cache.supportsRefresh() ? View.VISIBLE : View.GONE);
        offlineRefresh.setClickable(true);
    }

    public Geocache getCache() {
        return cache;
    }

    private static class StoreCacheHandler extends SimpleCancellableHandler {

        public StoreCacheHandler(final CacheDetailActivity activity, final Progress progress) {
            super(activity, progress);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            if (UPDATE_LOAD_PROGRESS_DETAIL == msg.what && msg.obj instanceof String) {
                updateStatusMsg(R.string.cache_dialog_offline_save_message, (String) msg.obj);
            } else {
                notifyDataSetChanged(activityRef);
            }
        }
    }

    private static final class RefreshCacheHandler extends SimpleCancellableHandler {

        public RefreshCacheHandler(final CacheDetailActivity activity, final Progress progress) {
            super(activity, progress);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            if (UPDATE_LOAD_PROGRESS_DETAIL == msg.what && msg.obj instanceof String) {
                updateStatusMsg(R.string.cache_dialog_refresh_message, (String) msg.obj);
            } else {
                notifyDataSetChanged(activityRef);
            }
        }
    }

    private static final class ChangeNotificationHandler extends SimpleHandler {

        public ChangeNotificationHandler(final CacheDetailActivity activity, final Progress progress) {
            super(activity, progress);
        }

        @Override
        public void handleMessage(final Message msg) {
            notifyDataSetChanged(activityRef);
        }
    }

    private static void notifyDataSetChanged(final WeakReference<AbstractActivity> activityRef) {
        final CacheDetailActivity activity = ((CacheDetailActivity) activityRef.get());
        if (activity != null) {
            activity.notifyDataSetChanged();
        }
    }

    protected void storeCache(final int listId, final StoreCacheHandler storeCacheHandler) {
        progress.show(this, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true, storeCacheHandler.cancelMessage());
        RxUtils.networkScheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                cache.store(listId, storeCacheHandler);
            }
        });
    }

    public static void editPersonalNote(final Geocache cache, final CacheDetailActivity activity) {
        if (cache.isOffline()) {
            final FragmentManager fm = activity.getSupportFragmentManager();
            final EditNoteDialog dialog = EditNoteDialog.newInstance(cache.getPersonalNote());
            dialog.show(fm, "fragment_edit_note");
        }
    }

    @Override
    public void onFinishEditNoteDialog(final String note) {
        final TextView personalNoteView = ButterKnife.findById(this, R.id.personalnote);
        setPersonalNote(personalNoteView, note);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                cache.setPersonalNote(note);
                cache.parseWaypointsFromNote();
                DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                return null;
            }

            @Override
            protected void onPostExecute(final Void v) {
                notifyDataSetChanged();
            }
        }.execute();
    }

    private static void setPersonalNote(final TextView personalNoteView, final String personalNote) {
        personalNoteView.setText(personalNote, TextView.BufferType.SPANNABLE);
        if (StringUtils.isNotBlank(personalNote)) {
            personalNoteView.setVisibility(View.VISIBLE);
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
        CacheListActivity.startActivityCoordinates(this, cache.getCoords());
    }
}
