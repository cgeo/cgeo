package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.apps.cache.GeneralAppsFactory;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import android.R.color;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StrikethroughSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Activity to display all details of a cache like owner, difficulty, description etc.
 *
 */
public class cgeodetail extends AbstractActivity {

    private static final int MENU_SHARE = 12;
    private static final int MENU_CALENDAR = 11;
    private static final int MENU_CACHES_AROUND = 10;
    private static final int MENU_BROWSER = 7;
    private static final int MENU_NAVIGATE = 2;
    private static final int CONTEXT_MENU_WAYPOINT_DELETE = 1235;
    private static final int CONTEXT_MENU_WAYPOINT_DUPLICATE = 1234;

    public cgeodetail() {
        super("c:geo-cache-details");
    }

    public cgSearch search = null;
    public cgCache cache = null;
    public String geocode = null;
    public String name = null;
    public String guid = null;
    private LayoutInflater inflater = null;
    private cgGeo geo = null;
    private cgUpdateLoc geoUpdate = new update();
    private float pixelRatio = 1;
    private TextView cacheDistance = null;
    private String contextMenuUser = null;
    private Spanned longDesc = null;
    private boolean longDescDisplayed = false;
    private loadCache threadCache = null;
    private loadLongDesc threadLongDesc = null;
    private Thread storeThread = null;
    private Thread refreshThread = null;
    private Thread watchlistThread = null; // thread for watchlist add/remove
    private Map<Integer, String> calendars = new HashMap<Integer, String>();

    private ViewGroup attributeIconsLayout; // layout for attribute icons
    private ViewGroup attributeDescriptionsLayout; // layout for attribute descriptions
    private boolean attributesShowAsIcons = true; // default: show icons
    /**
     * <code>noAttributeImagesFound</code> This will be the case if the cache was imported with an older version of
     * c:geo.
     * These older versions parsed the attribute description from the tooltip in the web
     * page and put them into the DB. No icons can be matched for these.
     */
    private boolean noAttributeIconsFound = false;
    private int attributeBoxMaxWidth;
    /**
     * differentiate between whether we are starting the activity for a cache or if we return to the activity from
     * another activity that we started in front
     */
    private boolean disableResumeSetView = false;

    private Progress progress = new Progress();

    private class StoreCacheHandler extends CancellableHandler {
        @Override
        public void handleRegularMessage(Message msg) {
            storeThread = null;

            try {
                cache = app.getCache(search); // reload cache details
            } catch (Exception e) {
                showToast(res.getString(R.string.err_store_failed));

                Log.e(Settings.tag, "cgeodetail.storeCacheHandler: " + e.toString());
            }

            setView();
        }
    }

    private class RefreshCacheHandler extends CancellableHandler {
        @Override
        public void handleRegularMessage(Message msg) {
            refreshThread = null;

            try {
                cache = app.getCache(search); // reload cache details
            } catch (Exception e) {
                showToast(res.getString(R.string.err_refresh_failed));

                Log.e(Settings.tag, "cgeodetail.refreshCacheHandler: " + e.toString());
            }

            setView();
        }
    }

    private Handler dropCacheHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                cache = app.getCache(search); // reload cache details
            } catch (Exception e) {
                showToast(res.getString(R.string.err_drop_failed));

                Log.e(Settings.tag, "cgeodetail.dropCacheHandler: " + e.toString());
            }

            setView();
        }
    };

    private class LoadCacheHandler extends CancellableHandler {
        @Override
        public void handleRegularMessage(final Message msg) {
            if (cgBase.UPDATE_LOAD_PROGRESS_DETAIL == msg.what && msg.obj instanceof String) {
                updateStatusMsg((String) msg.obj);
            } else {
                if (search == null) {
                    showToast(res.getString(R.string.err_dwld_details_failed));

                    finish();
                    return;
                }

                if (cgeoapplication.getError(search) != null) {
                    showToast(res.getString(R.string.err_dwld_details_failed_reason) + " " + cgeoapplication.getError(search) + ".");

                    finish();
                    return;
                }

                updateStatusMsg(res.getString(R.string.cache_dialog_loading_details_status_render));

                setView();

                if (Settings.isAutoLoadDescription()) {
                    try {
                        loadLongDesc();
                    } catch (Exception e) {
                        // activity is not visible
                    }
                }

                (new loadMapPreview(loadMapPreviewHandler)).start();
            }
        }

        private void updateStatusMsg(final String msg) {
            progress.setMessage(res.getString(R.string.cache_dialog_loading_details)
                    + "\n\n"
                    + msg);
        }

        @Override
        public void handleCancel(final Object extra) {
            finish();
        }

    }

    final Handler loadMapPreviewHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            BitmapDrawable image = (BitmapDrawable) message.obj;
            if (image == null) {
                return;
            }
            ScrollView scroll = (ScrollView) findViewById(R.id.details_list_box);
            final ImageView view = (ImageView) findViewById(R.id.map_preview);
            if (view == null) {
                return;
            }
            Bitmap bitmap = image.getBitmap();
            if (bitmap == null || bitmap.getWidth() <= 10) {
                return;
            }
            view.setImageDrawable(image);

            if (scroll.getScrollY() == 0) {
                scroll.scrollTo(0, (int) (80 * pixelRatio));
            }
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        registerForContextMenu(view);
                        openContextMenu(view);
                    } catch (Exception e) {
                        // nothing
                    }
                }
            });
        }
    };

    private Handler loadDescriptionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            parseLongDescription();

            if (longDesc != null) {
                ((LinearLayout) findViewById(R.id.desc_box)).setVisibility(View.VISIBLE);
                TextView descView = (TextView) findViewById(R.id.description);
                if (StringUtils.isNotBlank(cache.getDescription())) {
                    descView.setVisibility(View.VISIBLE);
                    descView.setText(longDesc, TextView.BufferType.SPANNABLE);
                    descView.setMovementMethod(LinkMovementMethod.getInstance());
                    // handle caches with black font color
                    if (!Settings.isLightSkin()) {
                        if (cache.getDescription().contains("color=\"#000000")) {
                            descView.setBackgroundResource(color.darker_gray);
                        }
                        else {
                            descView.setBackgroundResource(color.black);
                        }
                    }
                }
                else {
                    descView.setVisibility(View.GONE);
                }

                Button showDesc = (Button) findViewById(R.id.show_description);
                showDesc.setVisibility(View.GONE);
                showDesc.setOnTouchListener(null);
                showDesc.setOnClickListener(null);
            } else {
                showToast(res.getString(R.string.err_load_descr_failed));
            }

            progress.dismiss();

            longDescDisplayed = true;
        }
    };

    /**
     * shows/hides buttons, sets text in watchlist box
     */
    private void updateWatchlistBox() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.watchlist_box);
        boolean supportsWatchList = cache.supportsWatchList();
        layout.setVisibility(supportsWatchList ? View.VISIBLE : View.GONE);
        if (!supportsWatchList) {
            return;
        }
        Button buttonAdd = (Button) findViewById(R.id.add_to_watchlist);
        Button buttonRemove = (Button) findViewById(R.id.remove_from_watchlist);
        TextView text = (TextView) findViewById(R.id.watchlist_text);

        if (cache.isOnWatchlist()) {
            buttonAdd.setVisibility(View.GONE);
            buttonRemove.setVisibility(View.VISIBLE);
            text.setText(R.string.cache_watchlist_on);
        } else {
            buttonAdd.setVisibility(View.VISIBLE);
            buttonRemove.setVisibility(View.GONE);
            text.setText(R.string.cache_watchlist_not_on);
        }
    }

    /**
     * Handler, called when watchlist add or remove is done
     */
    private Handler WatchlistHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            watchlistThread = null;
            progress.dismiss();
            if (msg.what == -1) {
                showToast(res.getString(R.string.err_watchlist_failed));
            } else {
                updateWatchlistBox();
            }
        }
    };
    private LinearLayout detailsList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.detail);
        setTitle(res.getString(R.string.cache));

        init();

        // get parameters
        final Bundle extras = getIntent().getExtras();
        final Uri uri = getIntent().getData();

        // try to get data from extras
        if (geocode == null && extras != null) {
            geocode = extras.getString("geocode");
            name = extras.getString("name");
            guid = extras.getString("guid");
        }

        // try to get data from URI
        if (geocode == null && guid == null && uri != null) {
            String uriHost = uri.getHost().toLowerCase();
            String uriPath = uri.getPath().toLowerCase();
            String uriQuery = uri.getQuery();

            if (uriQuery != null) {
                Log.i(Settings.tag, "Opening URI: " + uriHost + uriPath + "?" + uriQuery);
            } else {
                Log.i(Settings.tag, "Opening URI: " + uriHost + uriPath);
            }

            if (uriHost.contains("geocaching.com")) {
                geocode = uri.getQueryParameter("wp");
                guid = uri.getQueryParameter("guid");

                if (StringUtils.isNotBlank(geocode)) {
                    geocode = geocode.toUpperCase();
                    guid = null;
                } else if (StringUtils.isNotBlank(guid)) {
                    geocode = null;
                    guid = guid.toLowerCase();
                } else {
                    showToast(res.getString(R.string.err_detail_open));
                    finish();
                    return;
                }
            } else if (uriHost.contains("coord.info")) {
                if (uriPath != null && uriPath.startsWith("/gc")) {
                    geocode = uriPath.substring(1).toUpperCase();
                } else {
                    showToast(res.getString(R.string.err_detail_open));
                    finish();
                    return;
                }
            }
        }

        // no given data
        if (geocode == null && guid == null) {
            showToast(res.getString(R.string.err_detail_cache));
            finish();
            return;
        }

        app.setAction(geocode);

        final LoadCacheHandler loadCacheHandler = new LoadCacheHandler();

        try {
            String title = res.getString(R.string.cache);
            if (StringUtils.isNotBlank(name)) {
                title = name;
            } else if (StringUtils.isNotBlank(geocode)) {
                title = geocode.toUpperCase();
            }
            progress.show(this, title, res.getString(R.string.cache_dialog_loading_details), true, loadCacheHandler.cancelMessage());
        } catch (Exception e) {
            // nothing, we lost the window
        }

        disableResumeSetView = true;
        threadCache = new loadCache(loadCacheHandler);
        threadCache.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setView();
    }

    @Override
    public void onResume() {
        super.onResume();


        if (geo == null) {
            geo = app.startGeo(this, geoUpdate, 0, 0);
        }
        if (!disableResumeSetView) {
            setView();
        }
        disableResumeSetView = false;
    }

    @Override
    public void onDestroy() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onDestroy();
    }

    @Override
    public void onStop() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onStop();
    }

    @Override
    public void onPause() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onPause();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();
        switch (viewId) {
            case R.id.author:
            case R.id.value:
                if (viewId == R.id.author) { // Author of a log entry
                    contextMenuUser = ((TextView) view).getText().toString();
                } else if (viewId == R.id.value) { // The owner of the cache
                    if (StringUtils.isNotBlank(cache.getOwnerReal())) {
                        contextMenuUser = cache.getOwnerReal();
                    } else {
                        contextMenuUser = cache.getOwner();
                    }
                }

                menu.setHeaderTitle(res.getString(R.string.user_menu_title) + " " + contextMenuUser);
                menu.add(viewId, 1, 0, res.getString(R.string.user_menu_view_hidden));
                menu.add(viewId, 2, 0, res.getString(R.string.user_menu_view_found));
                menu.add(viewId, 3, 0, res.getString(R.string.user_menu_open_browser));
                break;
            case R.id.map_preview:
                menu.setHeaderTitle(res.getString(R.string.cache_menu_navigate));
                addNavigationMenuItems(menu);
                break;
            case -1:
                if (null != cache.getWaypoints()) {
                    try {
                        final ViewGroup parent = ((ViewGroup) view.getParent());
                        for (int i = 0; i < parent.getChildCount(); i++) {
                            if (parent.getChildAt(i) == view) {
                                final List<cgWaypoint> sortedWaypoints = new ArrayList<cgWaypoint>(cache.getWaypoints());
                                Collections.sort(sortedWaypoints);
                                final cgWaypoint waypoint = sortedWaypoints.get(i);
                                final int index = cache.getWaypoints().indexOf(waypoint);
                                menu.setHeaderTitle(res.getString(R.string.waypoint));
                                menu.add(CONTEXT_MENU_WAYPOINT_DUPLICATE, index, 0, R.string.waypoint_duplicate);
                                if (waypoint.isUserDefined()) {
                                    menu.add(CONTEXT_MENU_WAYPOINT_DELETE, index, 0, R.string.waypoint_delete);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int groupId = item.getGroupId();
        final int index = item.getItemId();
        switch (groupId) {
            case R.id.author:
            case R.id.value:
                final int itemId = item.getItemId();
                switch (itemId) {
                    case 1:
                        cgeocaches.startActivityOwner(this, contextMenuUser);
                        return true;
                    case 2:
                        cgeocaches.startActivityUserName(this, contextMenuUser);
                        return true;
                    case 3:
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/profile/?u=" + URLEncoder.encode(contextMenuUser))));
                        return true;
                    default:
                        break;
                }
                break;
            case CONTEXT_MENU_WAYPOINT_DUPLICATE:
                if (null != cache.getWaypoints() && index < cache.getWaypoints().size()) {
                    final cgWaypoint copy = new cgWaypoint(cache.getWaypoints().get(index));
                    copy.setUserDefined();
                    copy.setName(res.getString(R.string.waypoint_copy_of) + " " + copy.getName());
                    cache.getWaypoints().add(index + 1, copy);
                    app.saveOwnWaypoint(-1, cache.getGeocode(), copy);
                    app.removeCacheFromCache(geocode);
                    setView(); // refresh
                }
                break;
            case CONTEXT_MENU_WAYPOINT_DELETE:
                if (null != cache.getWaypoints() && index < cache.getWaypoints().size()) {
                    final cgWaypoint waypoint = cache.getWaypoints().get(index);
                    if (waypoint.isUserDefined()) {
                        cache.getWaypoints().remove(index);
                        app.deleteWaypoint(waypoint.getId());
                        app.removeCacheFromCache(geocode);
                        setView(); // refresh
                    }
                }
                break;
            default:
                return onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (null != cache) {
            menu.add(0, MENU_NAVIGATE, 0, res.getString(R.string.cache_menu_compass)).setIcon(android.R.drawable.ic_menu_compass); // compass
            final SubMenu subMenu = menu.addSubMenu(1, 0, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_mapmode);
            addNavigationMenuItems(subMenu);
            menu.add(1, MENU_CALENDAR, 0, res.getString(R.string.cache_menu_event)).setIcon(android.R.drawable.ic_menu_agenda); // add event to calendar
            addVisitMenu(menu, cache);
            menu.add(0, MENU_CACHES_AROUND, 0, res.getString(R.string.cache_menu_around)).setIcon(android.R.drawable.ic_menu_rotate); // caches around
            menu.add(1, MENU_BROWSER, 0, res.getString(R.string.cache_menu_browser)).setIcon(R.drawable.ic_menu_globe); // browser
            menu.add(0, MENU_SHARE, 0, res.getString(R.string.cache_menu_share)).setIcon(android.R.drawable.ic_menu_share); // share cache
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(MENU_NAVIGATE).setVisible(null != cache.getCoords());
        menu.findItem(MENU_CALENDAR).setVisible(cache.canBeAddedToCalendar());
        menu.findItem(MENU_CACHES_AROUND).setVisible(null != cache.getCoords() && cache.supportsCachesAround());
        menu.findItem(MENU_BROWSER).setVisible(cache.canOpenInBrowser());
        return super.onPrepareOptionsMenu(menu);
    }

    private void addNavigationMenuItems(final Menu menu) {
        NavigationAppFactory.addMenuItems(menu, this, res);
        GeneralAppsFactory.addMenuItems(menu, this, res, cache);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int menuItem = item.getItemId();

        // no menu selected, but a new sub menu shown
        if (menuItem == 0) {
            return false;
        }

        if (menuItem == MENU_NAVIGATE) {
            navigateTo();
            return true;
        } else if (menuItem == MENU_LOG_VISIT) {
            logVisit();
            return true;
        } else if (menuItem == MENU_BROWSER) {
            cache.openInBrowser(this);
            return true;
        } else if (menuItem == MENU_CACHES_AROUND) {
            cachesAround();
            return true;
        } else if (menuItem == MENU_CALENDAR) {
            addToCalendar();
            return true;
        } else if (menuItem == MENU_SHARE) {
            if (cache != null) {
                cache.shareCache(this, res);
                return true;
            }
            return false;
        }
        if (NavigationAppFactory.onMenuItemSelected(item, geo, this, res, cache, search, null, null)) {
            return true;
        }
        if (GeneralAppsFactory.onMenuItemSelected(item, this, cache)) {
            return true;
        }

        int logType = menuItem - MENU_LOG_VISIT_OFFLINE;
        cache.logOffline(this, logType, base);
        return true;
    }

    private void init() {
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        pixelRatio = dm.density;

        if (inflater == null) {
            inflater = getLayoutInflater();
        }
        if (geo == null) {
            geo = app.startGeo(this, geoUpdate, 0, 0);
        }

        if (search != null) {
            cache = app.getCache(search);
            if (cache != null && cache.getGeocode() != null) {
                geocode = cache.getGeocode();
            }
        }

        if (StringUtils.isNotBlank(geocode)) {
            app.setAction(geocode);
        }
    }

    private void setView() {
        if (search == null) {
            return;
        }

        cache = app.getCache(search);

        if (cache == null) {
            progress.dismiss();

            if (StringUtils.isNotBlank(geocode)) {
                showToast(res.getString(R.string.err_detail_cache_find) + " " + geocode + ".");
            } else {
                geocode = null;
                showToast(res.getString(R.string.err_detail_cache_find_some));
            }

            finish();
            return;
        }

        try {

            if (geocode == null && StringUtils.isNotBlank(cache.getGeocode())) {
                geocode = cache.getGeocode();
            }

            if (guid == null && StringUtils.isNotBlank(cache.getGuid())) {
                guid = cache.getGuid();
            }

            setTitle(cache.getGeocode().toUpperCase());

            inflater = getLayoutInflater();

            ScrollView scroll = (ScrollView) findViewById(R.id.details_list_box);
            scroll.setVisibility(View.VISIBLE);

            detailsList = (LinearLayout) findViewById(R.id.details_list);
            detailsList.removeAllViews();

            // actionbar icon, default mystery
            ((TextView) findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(cgBase.getCacheIcon(cache.getCacheType())), null, null, null);

            // cache name (full name)
            Spannable span = (new Spannable.Factory()).newSpannable(Html.fromHtml(cache.getName()).toString());
            if (cache.isDisabled() || cache.isArchived()) { // strike
                span.setSpan(new StrikethroughSpan(), 0, span.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            addCacheDetail(R.string.cache_name, span);

            String cacheType;
            // cache type
            if (cgBase.cacheTypesInv.containsKey(cache.getCacheType())) { // cache icon
                cacheType = cgBase.cacheTypesInv.get(cache.getCacheType());
            } else {
                cacheType = cgBase.cacheTypesInv.get(CacheType.MYSTERY); // TODO: or UNKNOWN?
            }
            addCacheDetail(R.string.cache_type, cacheType);

            // size
            if (null != cache.getSize() && cache.showSize()) {
                addCacheDetail(R.string.cache_size, res.getString(cache.getSize().stringId));
            }

            // gc-code
            addCacheDetail(R.string.cache_geocode, cache.getGeocode().toUpperCase());

            // cache state
            if (cache.isLogOffline() || cache.isArchived() || cache.isDisabled() || cache.isMembers() || cache.isFound()) {
                final StringBuilder state = new StringBuilder();
                if (cache.isLogOffline()) {
                    state.append(res.getString(R.string.cache_status_offline_log));
                }
                if (cache.isFound()) {
                    if (state.length() > 0) {
                        state.append(", ");
                    }
                    state.append(res.getString(R.string.cache_status_found));
                }
                if (cache.isArchived()) {
                    if (state.length() > 0) {
                        state.append(", ");
                    }
                    state.append(res.getString(R.string.cache_status_archived));
                }
                if (cache.isDisabled()) {
                    if (state.length() > 0) {
                        state.append(", ");
                    }
                    state.append(res.getString(R.string.cache_status_disabled));
                }
                if (cache.isMembers()) {
                    if (state.length() > 0) {
                        state.append(", ");
                    }
                    state.append(res.getString(R.string.cache_status_premium));
                }

                addCacheDetail(R.string.cache_status, state.toString());
            }

            // distance
            cacheDistance = addCacheDetail(R.string.cache_distance, cache.getDistance() != null ? "~" + cgBase.getHumanDistance(cache.getDistance()) : "--");

            // difficulty
            if (cache.getDifficulty() != null && cache.getDifficulty() > 0) {
                addStarRating(R.string.cache_difficulty, cache.getDifficulty());
            }

            // terrain
            if (cache.getTerrain() != null && cache.getTerrain() > 0) {
                addStarRating(R.string.cache_terrain, cache.getTerrain());
            }

            // rating
            if (cache.getRating() != null && cache.getRating() > 0) {
                final RelativeLayout itemLayout = addStarRating(R.string.cache_rating, cache.getRating());
                if (cache.getVotes() != null) {
                    final TextView itemAddition = (TextView) itemLayout.findViewById(R.id.addition);
                    itemAddition.setText("(" + cache.getVotes() + ")");
                    itemAddition.setVisibility(View.VISIBLE);
                }
            }

            // favourite count
            if (cache.getFavouriteCnt() != null) {
                addCacheDetail(R.string.cache_favourite, String.format("%d", cache.getFavouriteCnt()) + "×");
            }

            // cache author
            if (StringUtils.isNotBlank(cache.getOwner()) || StringUtils.isNotBlank(cache.getOwnerReal())) {
                TextView ownerView = addCacheDetail(R.string.cache_owner, "");
                if (StringUtils.isNotBlank(cache.getOwner())) {
                    ownerView.setText(Html.fromHtml(cache.getOwner()), TextView.BufferType.SPANNABLE);
                } else if (StringUtils.isNotBlank(cache.getOwnerReal())) {
                    ownerView.setText(Html.fromHtml(cache.getOwnerReal()), TextView.BufferType.SPANNABLE);
                }
                ownerView.setOnClickListener(new userActions());
            }

            // cache hidden
            if (cache.getHidden() != null && cache.getHidden().getTime() > 0) {
                addCacheDetail(cache.isEventCache() ? R.string.cache_event : R.string.cache_hidden, base.formatFullDate(cache.getHidden().getTime()));
            }

            // cache location
            if (StringUtils.isNotBlank(cache.getLocation())) {
                addCacheDetail(R.string.cache_location, cache.getLocation());
            }

            // cache coordinates
            if (cache.getCoords() != null) {
                addCacheDetail(R.string.cache_coordinates, cache.getCoords().toString())
                        .setOnClickListener(new View.OnClickListener() {
                            private int position = 0;
                            private GeopointFormatter.Format[] availableFormats = new GeopointFormatter.Format[] {
                                    GeopointFormatter.Format.LAT_LON_DECMINUTE,
                                    GeopointFormatter.Format.LAT_LON_DECSECOND,
                                    GeopointFormatter.Format.LAT_LON_DECDEGREE
                            };

                            // rotate coordinate formats on click
                            @Override
                            public void onClick(View view) {
                                position = (position + 1) % availableFormats.length;

                                final TextView valueView = (TextView) view.findViewById(R.id.value);
                                valueView.setText(cache.getCoords().format(availableFormats[position]));
                            }
                        });
            }

            // cache attributes
            if (CollectionUtils.isNotEmpty(cache.getAttributes())) {

                final LinearLayout attribBox = (LinearLayout) findViewById(
                        R.id.attributes_innerbox);

                // maximum width for attribute icons is screen width - paddings of parents
                attributeBoxMaxWidth = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay().getWidth();
                ViewParent child = attribBox;
                do {
                    if (child instanceof View) {
                        attributeBoxMaxWidth = attributeBoxMaxWidth - ((View) child).getPaddingLeft()
                                - ((View) child).getPaddingRight();
                    }
                    child = child.getParent();
                } while (child != null);

                // delete views holding description / icons
                attributeDescriptionsLayout = null;
                attributeIconsLayout = null;

                attribBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // toggle between attribute icons and descriptions
                        toggleAttributeDisplay(attribBox, attributeBoxMaxWidth);
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
                    showAttributeIcons(attribBox, attributeBoxMaxWidth);
                } else {
                    showAttributeDescriptions(attribBox);
                }

                findViewById(R.id.attributes_box).setVisibility(View.VISIBLE);
            }

            // cache inventory
            if (CollectionUtils.isNotEmpty(cache.getInventory())) {
                final LinearLayout inventBox = (LinearLayout) findViewById(R.id.inventory_box);
                final TextView inventView = (TextView) findViewById(R.id.inventory);

                StringBuilder inventoryString = new StringBuilder();
                for (cgTrackable inventoryItem : cache.getInventory()) {
                    if (inventoryString.length() > 0) {
                        inventoryString.append('\n');
                    }
                    inventoryString.append(StringEscapeUtils.unescapeHtml4(inventoryItem.getName()));
                }
                inventView.setText(inventoryString);
                inventBox.setClickable(true);
                inventBox.setOnClickListener(new selectTrackable());
                inventBox.setVisibility(View.VISIBLE);
            }

            // offline use
            final TextView offlineText = (TextView) findViewById(R.id.offline_text);
            final Button offlineRefresh = (Button) findViewById(R.id.offline_refresh);
            final Button offlineStore = (Button) findViewById(R.id.offline_store);

            if (cache.getReason() >= 1) {
                Long diff = (System.currentTimeMillis() / (60 * 1000)) - (cache.getDetailedUpdate() / (60 * 1000)); // minutes

                String ago = "";
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
                offlineRefresh.setOnClickListener(new storeCache());

                offlineStore.setText(res.getString(R.string.cache_offline_drop));
                offlineStore.setClickable(true);
                offlineStore.setOnClickListener(new dropCache());
            } else {
                offlineText.setText(res.getString(R.string.cache_offline_not_ready));
                offlineRefresh.setOnClickListener(new refreshCache());

                offlineStore.setText(res.getString(R.string.cache_offline_store));
                offlineStore.setClickable(true);
                offlineStore.setOnClickListener(new storeCache());
            }
            offlineRefresh.setVisibility(cache.supportsRefresh() ? View.VISIBLE : View.GONE);
            offlineRefresh.setClickable(true);

            // cache personal note
            if (StringUtils.isNotBlank(cache.getPersonalNote())) {
                ((LinearLayout) findViewById(R.id.personalnote_box)).setVisibility(View.VISIBLE);

                TextView personalNoteText = (TextView) findViewById(R.id.personalnote);
                personalNoteText.setVisibility(View.VISIBLE);
                personalNoteText.setText(cache.getPersonalNote(), TextView.BufferType.SPANNABLE);
                personalNoteText.setMovementMethod(LinkMovementMethod.getInstance());
            }
            else {
                ((LinearLayout) findViewById(R.id.personalnote_box)).setVisibility(View.GONE);
            }

            // cache short desc
            if (StringUtils.isNotBlank(cache.getShortdesc())) {
                ((LinearLayout) findViewById(R.id.desc_box)).setVisibility(View.VISIBLE);

                TextView descView = (TextView) findViewById(R.id.shortdesc);
                descView.setVisibility(View.VISIBLE);
                descView.setText(Html.fromHtml(cache.getShortdesc().trim(), new HtmlImage(this, geocode, true, cache.getReason(), false), null), TextView.BufferType.SPANNABLE);
                descView.setMovementMethod(LinkMovementMethod.getInstance());
            }

            // cache long desc
            if (longDescDisplayed) {
                parseLongDescription();

                if (StringUtils.isNotBlank(longDesc)) {
                    ((LinearLayout) findViewById(R.id.desc_box)).setVisibility(View.VISIBLE);

                    TextView descView = (TextView) findViewById(R.id.description);
                    descView.setVisibility(View.VISIBLE);
                    descView.setText(longDesc, TextView.BufferType.SPANNABLE);
                    descView.setMovementMethod(LinkMovementMethod.getInstance());

                    Button showDesc = (Button) findViewById(R.id.show_description);
                    showDesc.setVisibility(View.GONE);
                    showDesc.setOnTouchListener(null);
                    showDesc.setOnClickListener(null);
                }
            } else if (!longDescDisplayed && StringUtils.isNotBlank(cache.getDescription())) {
                ((LinearLayout) findViewById(R.id.desc_box)).setVisibility(View.VISIBLE);

                Button showDesc = (Button) findViewById(R.id.show_description);
                showDesc.setVisibility(View.VISIBLE);
                showDesc.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View arg0) {
                        loadLongDesc();
                    }
                });
            }

            // watchlist
            Button buttonWatchlistAdd = (Button) findViewById(R.id.add_to_watchlist);
            Button buttonWatchlistRemove = (Button) findViewById(R.id.remove_from_watchlist);
            buttonWatchlistAdd.setOnClickListener(new AddToWatchlistClickListener());
            buttonWatchlistRemove.setOnClickListener(new RemoveFromWatchlistClickListener());
            updateWatchlistBox();

            // waypoints
            LinearLayout waypoints = (LinearLayout) findViewById(R.id.waypoints);
            waypoints.removeAllViews();

            if (CollectionUtils.isNotEmpty(cache.getWaypoints())) {
                LinearLayout waypointView;

                // sort waypoints: PP, Sx, FI, OWN
                List<cgWaypoint> sortedWaypoints = new ArrayList<cgWaypoint>(cache.getWaypoints());
                Collections.sort(sortedWaypoints);

                for (cgWaypoint wpt : sortedWaypoints) {
                    waypointView = (LinearLayout) inflater.inflate(R.layout.waypoint_item, null);
                    final TextView identification = (TextView) waypointView.findViewById(R.id.identification);

                    ((TextView) waypointView.findViewById(R.id.type)).setText(cgBase.waypointTypes.get(wpt.getWaypointType()));
                    if (!wpt.getPrefix().equalsIgnoreCase("OWN")) {
                        identification.setText(wpt.getPrefix().trim() + "/" + wpt.getLookup().trim());
                    } else {
                        identification.setText(res.getString(R.string.waypoint_custom));
                    }

                    TextView nameView = (TextView) waypointView.findViewById(R.id.name);
                    if (StringUtils.isBlank(wpt.getName())) {
                        nameView.setText(wpt.getCoords().toString());
                    } else {
                        nameView.setText(StringEscapeUtils.unescapeHtml4(wpt.getName()));
                    }
                    wpt.setIcon(res, nameView);

                    TextView noteView = (TextView) waypointView.findViewById(R.id.note);
                    if (containsHtml(wpt.getNote())) {
                        noteView.setText(Html.fromHtml(wpt.getNote().trim()), TextView.BufferType.SPANNABLE);
                    }
                    else {
                        noteView.setText(wpt.getNote().trim());
                    }
                    waypointView.setOnClickListener(new waypointInfo(wpt.getId()));
                    registerForContextMenu(waypointView);

                    waypoints.addView(waypointView);
                }
            }

            Button addWaypoint = (Button) findViewById(R.id.add_waypoint);
            addWaypoint.setClickable(true);
            addWaypoint.setOnClickListener(new addWaypoint());

            // cache hint
            if (StringUtils.isNotBlank(cache.getHint()) || CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                ((LinearLayout) findViewById(R.id.hint_box)).setVisibility(View.VISIBLE);
            } else {
                ((LinearLayout) findViewById(R.id.hint_box)).setVisibility(View.GONE);
            }

            if (StringUtils.isNotBlank(cache.getHint())) {
                TextView hintView = ((TextView) findViewById(R.id.hint));
                hintView.setText(CryptUtils.rot13(cache.getHint().trim()));
                hintView.setVisibility(View.VISIBLE);
                hintView.setClickable(true);
                hintView.setOnClickListener(new codeHint());
            } else {
                TextView hintView = ((TextView) findViewById(R.id.hint));
                hintView.setVisibility(View.GONE);
                hintView.setClickable(false);
                hintView.setOnClickListener(null);
            }

            if (CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                TextView spoilerlinkView = ((TextView) findViewById(R.id.hint_spoilerlink));
                spoilerlinkView.setVisibility(View.VISIBLE);
                spoilerlinkView.setClickable(true);
                spoilerlinkView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        showSpoilers();
                    }
                });
            } else {
                TextView spoilerlinkView = ((TextView) findViewById(R.id.hint_spoilerlink));
                spoilerlinkView.setVisibility(View.GONE);
                spoilerlinkView.setClickable(true);
                spoilerlinkView.setOnClickListener(null);
            }

            if (geo != null && geo.coordsNow != null && cache != null && cache.getCoords() != null) {
                cacheDistance.setText(cgBase.getHumanDistance(geo.coordsNow.distanceTo(cache.getCoords())));
                cacheDistance.bringToFront();
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeodetail.setView: " + e.toString());
        }

        progress.dismiss();

        displayLogs();

        // data license
        IConnector connector = ConnectorFactory.getConnector(cache);
        if (connector != null) {
            String license = connector.getLicenseText(cache);
            if (StringUtils.isNotBlank(license)) {
                ((LinearLayout) findViewById(R.id.license_box)).setVisibility(View.VISIBLE);
                TextView licenseView = ((TextView) findViewById(R.id.license));
                licenseView.setText(Html.fromHtml(license), BufferType.SPANNABLE);
                licenseView.setClickable(true);
                licenseView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                ((LinearLayout) findViewById(R.id.license_box)).setVisibility(View.GONE);
            }
        }

        if (geo != null) {
            geoUpdate.updateLoc(geo);
        }
    }

    private TextView addCacheDetail(final int nameId, final CharSequence value) {
        final RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
        ((TextView) layout.findViewById(R.id.name)).setText(res.getString(nameId));
        final TextView valueView = (TextView) layout.findViewById(R.id.value);
        valueView.setText(value);
        detailsList.addView(layout);
        return valueView;
    }

    static private boolean containsHtml(final String str) {
        return str.indexOf('<') != -1 || str.indexOf('&') != -1;
    }

    private void parseLongDescription() {
        if (longDesc == null && cache != null) {
            String description = cache.getDescription();
            if (description != null) {
                longDesc = Html.fromHtml(description.trim(), new HtmlImage(this, geocode, true, cache.getReason(), false), new UnknownTagsHandler());
            }
        }
    }

    private RelativeLayout addStarRating(final int nameId, final float value) {
        final RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.cache_layout, null);
        TextView viewName = (TextView) layout.findViewById(R.id.name);
        TextView viewValue = (TextView) layout.findViewById(R.id.value);
        LinearLayout layoutStars = (LinearLayout) layout.findViewById(R.id.stars);

        viewName.setText(res.getString(nameId));
        viewValue.setText(String.format("%.1f", value) + ' ' + res.getString(R.string.cache_rating_of) + " 5");
        layoutStars.addView(cgBase.createStarRating(value, 5, this), 1);

        detailsList.addView(layout);
        return layout;
    }

    private void displayLogs() {
        // cache logs
        TextView textView = (TextView) findViewById(R.id.logcount);
        int logCounter = 0;
        if (cache != null && cache.getLogCounts() != null) {
            final StringBuffer buff = new StringBuffer();
            buff.append(res.getString(R.string.cache_log_types));
            buff.append(": ");

            // sort the log counts by type id ascending. that way the FOUND, DNF log types are the first and most visible ones
            List<Entry<Integer, Integer>> sortedLogCounts = new ArrayList<Entry<Integer, Integer>>();
            sortedLogCounts.addAll(cache.getLogCounts().entrySet());
            Collections.sort(sortedLogCounts, new Comparator<Entry<Integer, Integer>>() {

                @Override
                public int compare(Entry<Integer, Integer> logCountItem1,
                        Entry<Integer, Integer> logCountItem2) {
                    return logCountItem1.getKey().compareTo(logCountItem2.getKey());
                }
            });
            for (Entry<Integer, Integer> pair : sortedLogCounts) {
                int logTypeId = pair.getKey().intValue();
                String logTypeLabel = cgBase.logTypes1.get(logTypeId);
                // it may happen that the label is unknown -> then avoid any output for this type
                if (logTypeLabel != null) {
                    if (logCounter > 0) {
                        buff.append(", ");
                    }
                    buff.append(pair.getValue().intValue());
                    buff.append("× ");
                    buff.append(logTypeLabel);
                }
                logCounter++;
            }
            textView.setText(buff.toString());
        }
        // it may happen, that the logCounts map is available, but every log type has zero counts,
        // therefore check again for the number of counted logs
        if (logCounter > 0) {
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }

        // cache logs
        LinearLayout listView = (LinearLayout) findViewById(R.id.log_list);
        listView.removeAllViews();

        RelativeLayout rowView;

        if (cache != null && cache.getLogs() != null) {
            for (cgLog log : cache.getLogs()) {
                rowView = (RelativeLayout) inflater.inflate(R.layout.log_item, null);

                if (log.date > 0) {
                    ((TextView) rowView.findViewById(R.id.added)).setText(base.formatShortDate(log.date));
                } else {
                    ((TextView) rowView.findViewById(R.id.added)).setVisibility(View.GONE);
                }

                if (cgBase.logTypes1.containsKey(log.type)) {
                    ((TextView) rowView.findViewById(R.id.type)).setText(cgBase.logTypes1.get(log.type));
                } else {
                    ((TextView) rowView.findViewById(R.id.type)).setText(cgBase.logTypes1.get(4)); // note if type is unknown
                }
                ((TextView) rowView.findViewById(R.id.author)).setText(StringEscapeUtils.unescapeHtml4(log.author));

                if (log.found == -1) {
                    ((TextView) rowView.findViewById(R.id.count)).setVisibility(View.GONE);
                } else if (log.found == 0) {
                    ((TextView) rowView.findViewById(R.id.count)).setText(res.getString(R.string.cache_count_no));
                } else if (log.found == 1) {
                    ((TextView) rowView.findViewById(R.id.count)).setText(res.getString(R.string.cache_count_one));
                } else {
                    ((TextView) rowView.findViewById(R.id.count)).setText(log.found + " " + res.getString(R.string.cache_count_more));
                }
                // avoid parsing HTML if not necessary
                if (containsHtml(log.log)) {
                    ((TextView) rowView.findViewById(R.id.log)).setText(Html.fromHtml(log.log, new HtmlImage(this, null, false, cache.getReason(), false), null), TextView.BufferType.SPANNABLE);
                }
                else {
                    ((TextView) rowView.findViewById(R.id.log)).setText(log.log);
                }
                // add LogImages
                LinearLayout logLayout = (LinearLayout) rowView.findViewById(R.id.log_layout);

                if (CollectionUtils.isNotEmpty(log.logImages)) {

                    final ArrayList<cgImage> logImages = new ArrayList<cgImage>(log.logImages);

                    final View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            cgeoimages.startActivityLogImages(cgeodetail.this, geocode, logImages);
                        }
                    };

                    ArrayList<String> titles = new ArrayList<String>();
                    for (int i_img_cnt = 0; i_img_cnt < log.logImages.size(); i_img_cnt++) {
                        String img_title = log.logImages.get(i_img_cnt).getTitle();
                        if (!StringUtils.isBlank(img_title)) {
                            titles.add(img_title);
                        }
                    }
                    if (titles.isEmpty()) {
                        titles.add(res.getString(R.string.cache_log_image_default_title));
                    }

                    LinearLayout log_imgView = (LinearLayout) inflater.inflate(R.layout.log_img, null);
                    TextView log_img_title = (TextView) log_imgView.findViewById(R.id.title);
                    log_img_title.setText(StringUtils.join(titles.toArray(new String[titles.size()]), ", "));
                    log_img_title.setOnClickListener(listener);
                    logLayout.addView(log_imgView);
                }

                // Add colored mark
                final ImageView logMark = (ImageView) rowView.findViewById(R.id.log_mark);
                if (log.type == cgBase.LOG_FOUND_IT
                        || log.type == cgBase.LOG_WEBCAM_PHOTO_TAKEN
                        || log.type == cgBase.LOG_ATTENDED)
                {
                    logMark.setImageResource(R.drawable.mark_green);
                }
                else if (log.type == cgBase.LOG_PUBLISH_LISTING
                        || log.type == cgBase.LOG_ENABLE_LISTING
                        || log.type == cgBase.LOG_OWNER_MAINTENANCE)
                {
                    logMark.setImageResource(R.drawable.mark_green_more);
                }
                else if (log.type == cgBase.LOG_DIDNT_FIND_IT
                        || log.type == cgBase.LOG_NEEDS_MAINTENANCE
                        || log.type == cgBase.LOG_NEEDS_ARCHIVE)
                {
                    logMark.setImageResource(R.drawable.mark_red);
                }
                else if (log.type == cgBase.LOG_TEMP_DISABLE_LISTING
                        || log.type == cgBase.LOG_ARCHIVE)
                {
                    logMark.setImageResource(R.drawable.mark_red_more);
                }
                else
                {
                    logMark.setVisibility(View.GONE);
                }

                ((TextView) rowView.findViewById(R.id.author)).setOnClickListener(new userActions());
                ((TextView) logLayout.findViewById(R.id.log)).setOnClickListener(new decryptLog());

                listView.addView(rowView);
            }

            if (cache.getLogs().size() > 0) {
                ((LinearLayout) findViewById(R.id.log_box)).setVisibility(View.VISIBLE);
            }
        }
    }

    private class loadCache extends Thread {

        private CancellableHandler handler = null;

        public loadCache(CancellableHandler handlerIn) {
            handler = handlerIn;

            if (geocode == null && guid == null) {
                showToast(res.getString(R.string.err_detail_cache_forgot));

                finish();
                return;
            }
        }

        @Override
        public void run() {
            if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
                return;
            }
            search = base.searchByGeocode(geocode, StringUtils.isBlank(geocode) ? guid : null, 0, false, handler);
            handler.sendMessage(new Message());
        }
    }

    private class loadMapPreview extends Thread {
        private Handler handler = null;

        public loadMapPreview(Handler handlerIn) {
            handler = handlerIn;
        }

        @Override
        public void run() {
            if (cache == null || cache.getCoords() == null) {
                return;
            }

            BitmapDrawable image = null;

            try {
                final String latlonMap = cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
                final Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

                int width = display.getWidth();
                int height = (int) (90 * pixelRatio);

                String markerUrl = cgBase.urlencode_rfc3986("http://cgeo.carnero.cc/_markers/my_location_mdpi.png");

                HtmlImage mapGetter = new HtmlImage(cgeodetail.this, cache.getGeocode(), false, 0, false);
                image = mapGetter.getDrawable("http://maps.google.com/maps/api/staticmap?center=" + latlonMap + "&zoom=15&size=" + width + "x" + height + "&maptype=terrain&markers=icon%3A" + markerUrl + "%7C" + latlonMap + "&sensor=false");
                Message message = handler.obtainMessage(0, image);
                handler.sendMessage(message);
            } catch (Exception e) {
                Log.w(Settings.tag, "cgeodetail.loadMapPreview.run: " + e.toString());
            }
        }
    }

    public void loadLongDesc() {
        progress.show(this, null, res.getString(R.string.cache_dialog_loading_description), true, null);

        threadLongDesc = new loadLongDesc(loadDescriptionHandler);
        threadLongDesc.start();
    }

    private class loadLongDesc extends Thread {
        private Handler handler = null;

        public loadLongDesc(Handler handlerIn) {
            handler = handlerIn;
        }

        @Override
        public void run() {
            if (cache == null || cache.getDescription() == null || handler == null) {
                return;
            }
            parseLongDescription();
            handler.sendMessage(new Message());
        }
    }

    public List<cgCoord> getCoordinates() {
        cgCoord coords = null;
        List<cgCoord> coordinates = new ArrayList<cgCoord>();

        try {
            // cache
            coords = new cgCoord();
            coords.setType("cache");
            if (StringUtils.isNotBlank(name)) {
                coords.setName(name);
            } else {
                coords.setName(geocode.toUpperCase());
            }
            coords.setCoords(cache.getCoords());
            coordinates.add(coords);
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeodetail.getCoordinates (cache): " + e.toString());
        }

        try {
            // waypoints
            if (null != cache.getWaypoints()) {
                for (cgWaypoint waypoint : cache.getWaypoints()) {
                    if (null != waypoint.getCoords()) {
                        coords = new cgCoord();
                        coords.setType("waypoint");
                        coords.setName(waypoint.getName());
                        coords.setCoords(waypoint.getCoords());
                        coordinates.add(coords);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeodetail.getCoordinates (waypoint): " + e.toString());
        }

        return coordinates;
    }

    private void cachesAround() {
        cgeocaches.startActivityCachesAround(this, cache.getCoords());

        finish();
    }

    private void addToCalendar() {
        String[] projection = new String[] { "_id", "displayName" };
        Uri calendarProvider = Compatibility.getCalendarProviderURI();

        Cursor cursor = managedQuery(calendarProvider, projection, "selected=1", null, null);

        calendars.clear();
        int cnt = 0;
        if (cursor != null) {
            cnt = cursor.getCount();

            if (cnt > 0) {
                cursor.moveToFirst();

                int calId = 0;
                String calIdPre = null;
                String calName = null;
                int calIdIn = cursor.getColumnIndex("_id");
                int calNameIn = cursor.getColumnIndex("displayName");

                do {
                    calIdPre = cursor.getString(calIdIn);
                    if (calIdPre != null) {
                        calId = new Integer(calIdPre);
                    }
                    calName = cursor.getString(calNameIn);

                    if (calId > 0 && calName != null) {
                        calendars.put(calId, calName);
                    }
                } while (cursor.moveToNext());
            }
        }

        final CharSequence[] items = calendars.values().toArray(new CharSequence[calendars.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.cache_calendars);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                addToCalendarFn(item);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void addToCalendarFn(int index) {
        if (MapUtils.isEmpty(calendars)) {
            return;
        }

        try {
            Uri calendarProvider = Compatibility.getCalenderEventsProviderURI();

            final Integer[] keys = calendars.keySet().toArray(new Integer[calendars.size()]);
            final Integer calId = keys[index];

            final Date eventDate = cache.getHidden();
            eventDate.setHours(0);
            eventDate.setMinutes(0);
            eventDate.setSeconds(0);

            StringBuilder description = new StringBuilder();
            description.append(cache.getUrl());
            description.append("\n\n");
            if (StringUtils.isNotBlank(cache.getShortdesc())) {
                description.append(Html.fromHtml(cache.getShortdesc()).toString());
            }

            if (StringUtils.isNotBlank(cache.getPersonalNote())) {
                description.append("\n\n" + Html.fromHtml(cache.getPersonalNote()).toString());
            }

            ContentValues event = new ContentValues();
            event.put("calendar_id", calId);
            event.put("dtstart", eventDate.getTime() + 43200000); // noon
            event.put("dtend", eventDate.getTime() + 43200000 + 3600000); // + one hour
            event.put("eventTimezone", "UTC");
            event.put("title", Html.fromHtml(cache.getName()).toString());
            event.put("description", description.toString());
            String location = "";
            if (cache.getCoords() != null) {
                location += cache.getLatitude() + " " + cache.getLongitude();
            }
            if (StringUtils.isNotBlank(cache.getLocation())) {
                boolean addParenteses = false;
                if (location.length() > 0) {
                    addParenteses = true;
                    location += " (";
                }

                location += Html.fromHtml(cache.getLocation()).toString();
                if (addParenteses) {
                    location += ")";
                }
            }
            if (location.length() > 0) {
                event.put("eventLocation", location);
            }
            event.put("allDay", 1);
            event.put("hasAlarm", 0);

            getContentResolver().insert(calendarProvider, event);

            showToast(res.getString(R.string.event_success));
        } catch (Exception e) {
            showToast(res.getString(R.string.event_fail));

            Log.e(Settings.tag, "cgeodetail.addToCalendarFn: " + e.toString());
        }
    }

    private void navigateTo() {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
        }

        cgeonavigate.startActivity(this, cache.getGeocode(), cache.getName(), cache.getCoords(), getCoordinates());
    }

    private class waypointInfo implements View.OnClickListener {
        private int id = -1;

        public waypointInfo(int idIn) {
            id = idIn;
        }

        public void onClick(View arg0) {
            Intent waypointIntent = new Intent(cgeodetail.this, cgeowaypoint.class);
            waypointIntent.putExtra("waypoint", id);
            waypointIntent.putExtra("geocode", cache.getGeocode());
            startActivity(waypointIntent);
        }
    }

    private void logVisit() {
        cache.logVisit(this);
    }

    private void showSpoilers() {
        if (cache == null || CollectionUtils.isEmpty(cache.getSpoilers())) {
            showToast(res.getString(R.string.err_detail_no_spoiler));
        }

        cgeoimages.startActivitySpoilerImages(cgeodetail.this, geocode, cache.getSpoilers());
    }

    public class codeHint implements View.OnClickListener {
        public void onClick(View arg0) {
            // code hint
            TextView hintView = ((TextView) findViewById(R.id.hint));
            hintView.setText(CryptUtils.rot13(hintView.getText().toString()));
        }
    }

    private class update extends cgUpdateLoc {
        @Override
        public void updateLoc(cgGeo geo) {
            if (geo == null) {
                return;
            }
            if (cacheDistance == null) {
                return;
            }

            try {
                StringBuilder dist = new StringBuilder();

                if (geo.coordsNow != null && cache != null && cache.getCoords() != null) {
                    dist.append(cgBase.getHumanDistance(geo.coordsNow.distanceTo(cache.getCoords())));
                }

                if (cache != null && cache.getElevation() != null) {
                    if (geo.altitudeNow != null) {
                        Double diff = (cache.getElevation() - geo.altitudeNow);
                        if (diff >= 0) {
                            dist.append(" ↗");
                        } else if (diff < 0) {
                            dist.append(" ↘");
                        }
                        if (Settings.isUseMetricUnits()) {
                            dist.append(String.format("%.0f", (Math.abs(diff))));
                            dist.append(" m");
                        } else {
                            dist.append(String.format("%.0f", (Math.abs(diff) * 3.2808399)));
                            dist.append(" ft");
                        }
                    }
                }

                cacheDistance.setText(dist.toString());
                cacheDistance.bringToFront();
            } catch (Exception e) {
                Log.w(Settings.tag, "Failed to update location.");
            }
        }
    }

    private class selectTrackable implements View.OnClickListener {
        public void onClick(View arg0) {
            // show list of trackables
            try {
                // jump directly into details if there is only one trackable
                if (cache != null && cache.getInventory() != null && cache.getInventory().size() == 1) {
                    cgTrackable trackable = cache.getInventory().get(0);
                    cgeotrackable.startActivity(cgeodetail.this, trackable.getGuid(), trackable.getGeocode(), trackable.getName());
                }
                else {
                    Intent trackablesIntent = new Intent(cgeodetail.this, cgeotrackables.class);
                    trackablesIntent.putExtra("geocode", geocode.toUpperCase());
                    startActivity(trackablesIntent);
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeodetail.selectTrackable: " + e.toString());
            }
        }
    }

    private class storeCache implements View.OnClickListener {
        public void onClick(View arg0) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return;
            }

            final StoreCacheHandler storeCacheHandler = new StoreCacheHandler();

            progress.show(cgeodetail.this, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true, storeCacheHandler.cancelMessage());

            if (storeThread != null) {
                storeThread.interrupt();
            }

            storeThread = new storeCacheThread(storeCacheHandler);
            storeThread.start();
        }
    }

    private class refreshCache implements View.OnClickListener {
        public void onClick(View arg0) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return;
            }

            final RefreshCacheHandler refreshCacheHandler = new RefreshCacheHandler();

            progress.show(cgeodetail.this, res.getString(R.string.cache_dialog_refresh_title), res.getString(R.string.cache_dialog_refresh_message), true, refreshCacheHandler.cancelMessage());

            if (refreshThread != null) {
                refreshThread.interrupt();
            }

            refreshThread = new refreshCacheThread(refreshCacheHandler);
            refreshThread.start();
        }
    }

    private class storeCacheThread extends Thread {
        final private CancellableHandler handler;

        public storeCacheThread(final CancellableHandler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            int reason = cache.getReason() > 1 ? cache.getReason() : 1;
            base.storeCache(app, cgeodetail.this, cache, null, reason, handler);
        }
    }

    private class refreshCacheThread extends Thread {
        final private CancellableHandler handler;

        public refreshCacheThread(final CancellableHandler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            app.removeCacheFromCache(geocode);
            search = base.searchByGeocode(cache.getGeocode(), null, 0, true, handler);

            handler.sendEmptyMessage(0);
        }
    }

    private class dropCache implements View.OnClickListener {
        public void onClick(View arg0) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_detail_still_working));
                return;
            }

            progress.show(cgeodetail.this, res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true, null);
            Thread thread = new dropCacheThread(dropCacheHandler);
            thread.start();
        }
    }

    private class dropCacheThread extends Thread {

        private Handler handler = null;

        public dropCacheThread(Handler handlerIn) {
            handler = handlerIn;
        }

        @Override
        public void run() {
            cgBase.dropCache(app, cache, handler);
        }
    }

    /**
     * Abstract Listener for add / remove buttons for watchlist
     */
    private abstract class AbstractWatchlistClickListener implements View.OnClickListener {
        public void doExecute(int titleId, int messageId, Thread thread) {
            if (progress.isShowing()) {
                showToast(res.getString(R.string.err_watchlist_still_managing));
                return;
            }
            progress.show(cgeodetail.this, res.getString(titleId), res.getString(messageId), true, null);

            if (watchlistThread != null) {
                watchlistThread.interrupt();
            }

            watchlistThread = thread;
            watchlistThread.start();
        }
    }

    /**
     * Listener for "add to watchlist" button
     */
    private class AddToWatchlistClickListener extends AbstractWatchlistClickListener {
        public void onClick(View arg0) {
            doExecute(R.string.cache_dialog_watchlist_add_title,
                    R.string.cache_dialog_watchlist_add_message,
                    new WatchlistAddThread(WatchlistHandler));
        }
    }

    /**
     * Listener for "remove from watchlist" button
     */
    private class RemoveFromWatchlistClickListener extends AbstractWatchlistClickListener {
        public void onClick(View arg0) {
            doExecute(R.string.cache_dialog_watchlist_remove_title,
                    R.string.cache_dialog_watchlist_remove_message,
                    new WatchlistRemoveThread(WatchlistHandler));
        }
    }

    /** Thread to add this cache to the watchlist of the user */
    private class WatchlistAddThread extends Thread {
        private final Handler handler;

        public WatchlistAddThread(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            handler.sendEmptyMessage(cgBase.addToWatchlist(cache));
        }
    }

    /** Thread to remove this cache from the watchlist of the user */
    private class WatchlistRemoveThread extends Thread {
        private final Handler handler;

        public WatchlistRemoveThread(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            handler.sendEmptyMessage(cgBase.removeFromWatchlist(cache));
        }
    }

    private class addWaypoint implements View.OnClickListener {

        public void onClick(View view) {
            Intent addWptIntent = new Intent(cgeodetail.this, cgeowaypointadd.class);

            addWptIntent.putExtra("geocode", geocode);
            int wpCount = 0;
            if (cache.getWaypoints() != null) {
                wpCount = cache.getWaypoints().size();
            }
            addWptIntent.putExtra("count", wpCount);

            startActivity(addWptIntent);
        }
    }

    private static class decryptLog implements View.OnClickListener {

        public void onClick(View view) {
            if (view == null) {
                return;
            }

            try {
                final TextView logView = (TextView) view;
                CharSequence text = logView.getText();
                if (text instanceof Spannable) {
                    Spannable span = (Spannable) text;
                    logView.setText(CryptUtils.rot13(span));
                }
                else {
                    String string = (String) text;
                    logView.setText(CryptUtils.rot13(string));
                }
            } catch (Exception e) {
                // nothing
            }
        }
    }

    private class userActions implements View.OnClickListener {

        public void onClick(View view) {
            if (view == null) {
                return;
            }
            if (!cache.supportsUserActions()) {
                return;
            }

            try {
                registerForContextMenu(view);
                openContextMenu(view);
            } catch (Exception e) {
                // nothing
            }
        }
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void goCompass(View view) {
        if (cache == null || cache.getCoords() == null) {
            showToast(res.getString(R.string.cache_coordinates_no));

            return;
        }
        cgeonavigate.startActivity(this, cache.getGeocode(), cache.getName(), cache.getCoords(), getCoordinates());
    }

    /**
     * lazy-creates the layout holding the icons of the chaches attributes
     * and makes it visible
     */
    private void showAttributeIcons(LinearLayout attribBox, int parentWidth) {
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
     * lazy-creates the layout holding the discriptions of the chaches attributes
     * and makes it visible
     */
    private void showAttributeDescriptions(LinearLayout attribBox) {
        if (attributeDescriptionsLayout == null) {
            attributeDescriptionsLayout = createAttributeDescriptionsLayout();
        }
        attribBox.removeAllViews();
        attribBox.addView(attributeDescriptionsLayout);
        attributesShowAsIcons = false;
    }

    /**
     * toggle attribute descriptions and icons
     */
    private void toggleAttributeDisplay(LinearLayout attribBox, int parentWidth) {
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

    private ViewGroup createAttributeIconsLayout(int parentWidth) {
        LinearLayout rows = new LinearLayout(this);
        rows.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        rows.setOrientation(LinearLayout.VERTICAL);

        LinearLayout attributeRow = newAttributeIconsRow();
        rows.addView(attributeRow);

        noAttributeIconsFound = true;

        for (String attributeName : cache.getAttributes()) {
            boolean strikethru = attributeName.endsWith("_no");
            // cut off _yes / _no
            if (attributeName.endsWith("_no") || attributeName.endsWith("_yes")) {
                attributeName = attributeName.substring(0, attributeName.lastIndexOf("_"));
            }
            // check if another attribute icon fits in this row
            attributeRow.measure(0, 0);
            int rowWidth = attributeRow.getMeasuredWidth();
            FrameLayout fl = (FrameLayout) inflater.inflate(R.layout.attribute_image, null);
            ImageView iv = (ImageView) fl.getChildAt(0);
            if ((parentWidth - rowWidth) < iv.getLayoutParams().width) {
                // make a new row
                attributeRow = newAttributeIconsRow();
                rows.addView(attributeRow);
            }

            // dynamically search icon of the attribute
            Drawable d = null;
            int id = res.getIdentifier("attribute_" + attributeName, "drawable",
                    base.context.getPackageName());
            if (id > 0) {
                noAttributeIconsFound = false;
                d = res.getDrawable(id);
                iv.setImageDrawable(d);
                // strike through?
                if (strikethru) {
                    // generate strikethru image with same properties as attribute image
                    ImageView strikethruImage = new ImageView(this);
                    strikethruImage.setLayoutParams(iv.getLayoutParams());
                    d = res.getDrawable(R.drawable.attribute__strikethru);
                    strikethruImage.setImageDrawable(d);
                    fl.addView(strikethruImage);
                }
            } else {
                d = res.getDrawable(R.drawable.attribute_icon_not_found);
                iv.setImageDrawable(d);
            }

            attributeRow.addView(fl);
        }

        return rows;
    }

    private LinearLayout newAttributeIconsRow() {
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        return rowLayout;
    }

    private ViewGroup createAttributeDescriptionsLayout() {
        final LinearLayout descriptions = (LinearLayout) inflater.inflate(
                R.layout.attribute_descriptions, null);
        TextView attribView = (TextView) descriptions.getChildAt(0);

        StringBuilder buffer = new StringBuilder();
        String attribute;
        for (int i = 0; i < cache.getAttributes().size(); i++) {
            attribute = cache.getAttributes().get(i);

            // dynamically search for a translation of the attribute
            int id = res.getIdentifier("attribute_" + attribute, "string",
                    base.context.getPackageName());
            if (id > 0) {
                String translated = res.getString(id);
                if (StringUtils.isNotBlank(translated)) {
                    attribute = translated;
                }
            }
            if (buffer.length() > 0) {
                buffer.append('\n');
            }
            buffer.append(attribute);
        }

        if (noAttributeIconsFound) {
            buffer.append("\n\n").append(res.getString(R.string.cache_attributes_no_icons));
        }

        attribView.setText(buffer);

        return descriptions;
    }

    public static void startActivity(final Context context, final String geocode) {
        final Intent detailIntent = new Intent(context, cgeodetail.class);
        detailIntent.putExtra("geocode", geocode.toUpperCase());
        context.startActivity(detailIntent);
    }
}
