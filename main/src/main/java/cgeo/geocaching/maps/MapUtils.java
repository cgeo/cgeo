package cgeo.geocaching.maps;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CachePopupFragment;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.EditWaypointActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SwipeToOpenFragment;
import cgeo.geocaching.WaypointPopupFragment;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.downloader.BRouterTileDownloader;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimplePopupMenu;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.functions.Action2;
import static cgeo.geocaching.brouter.BRouterConstants.BROUTER_TILE_FILEEXTENSION;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.android.material.sidesheet.SideSheetCallback;
import org.apache.commons.lang3.StringUtils;

public class MapUtils {

    private static final String TAG_MAPDETAILS_FRAGMENT = "mapdetails_fragment";
    private static final String TAG_SWIPE_FRAGMENT = "swipetoopen_fragment";

    private MapUtils() {
        // should not be instantiated
    }

    // filter waypoints from owned caches or certain wp types if requested.
    public static void filter(final Set<Waypoint> waypoints, final GeocacheFilterContext filterContext) {

        final GeocacheFilter filter = filterContext.get();

        final boolean excludeWpOriginal = Settings.isExcludeWpOriginal();
        final boolean excludeWpParking = Settings.isExcludeWpParking();
        final boolean excludeWpVisited = Settings.isExcludeWpVisited();

        final List<Waypoint> removeList = new ArrayList<>();
        for (final Waypoint wp : waypoints) {
            final Geocache cache = DataStore.loadCache(wp.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            final WaypointType wpt = wp.getWaypointType();
            if (cache == null ||
                    !filter.filter(cache) ||
                    (excludeWpOriginal && wpt == WaypointType.ORIGINAL) ||
                    (excludeWpParking && wpt == WaypointType.PARKING) ||
                    (excludeWpVisited && wp.isVisited())) {
                removeList.add(wp);
            }
        }
        waypoints.removeAll(removeList);
    }

    /**
     * Applies given filter to cache list. Additionally, creates a second list additionally filtered by own/found/disabled caches if required
     */
    public static void filter(final Collection<Geocache> caches, final GeocacheFilterContext filterContext) {
        final GeocacheFilter filter = filterContext.get();
        filter.filterList(caches);
    }

    public static void updateFilterBar(final Activity activity, final GeocacheFilterContext filterContext) {
        FilterUtils.updateFilterBar(activity, getActiveMapFilterName(filterContext));
    }

    @Nullable
    private static String getActiveMapFilterName(final GeocacheFilterContext filterContext) {
        final GeocacheFilter filter = filterContext.get();
        if (filter.isFiltering()) {
            return filter.toUserDisplayableString();
        }
        return null;
    }

    // one-time messages to be shown for maps
    public static void showMapOneTimeMessages(final Activity activity, final MapMode mapMode) {
        Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_QUICK_SETTINGS);
        if (mapMode == MapMode.LIVE && !Settings.isLiveMap()) {
            Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_LIVE_DISABLED);
        }
    }

    // workaround for colored ActionBar titles/subtitles
    // @todo remove after switching map ActionBar to Toolbar
    public static Spanned getColoredValue(final String value) {
        return Html.fromHtml("<font color=\"" + String.format("#%06X", 0xFFFFFF & CgeoApplication.getInstance().getResources().getColor(R.color.colorTextActionBar)) + "\">" + value + "</font>");
    }

    // check whether routing tile data is available for the whole viewport given
    // and offer to download missing routing data
    public static void checkRoutingData(final Activity activity, final double minLatitude, final double minLongitude, final double maxLatitude, final double maxLongitude) {
        if (Settings.useInternalRouting()) {
            ActivityMixin.showToast(activity, R.string.downloadmap_checking);

            final HashMap<String, String> missingTiles = new HashMap<>();
            final ArrayList<Download> missingDownloads = new ArrayList<>();
            final AtomicBoolean hasUnsupportedTiles = new AtomicBoolean(false);

            AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
                // calculate affected routing tiles
                int curLat = (int) Math.floor(minLatitude / 5) * 5;
                final int maxLat = (int) Math.floor(maxLatitude / 5) * 5;
                final int maxLon = (int) Math.floor(maxLongitude / 5) * 5;
                while (curLat <= maxLat) {
                    int curLon = (int) Math.floor(minLongitude / 5) * 5;
                    while (curLon <= maxLon) {
                        final String filenameBase = (curLon < 0 ? "W" + (-curLon) : "E" + curLon) + "_" + (curLat < 0 ? "S" + (-curLat) : "N" + curLat) + BROUTER_TILE_FILEEXTENSION;
                        missingTiles.put(filenameBase, filenameBase);
                        curLon += 5;
                    }
                    curLat += 5;
                }
                checkTiles(missingTiles, missingDownloads, hasUnsupportedTiles);
            }, () -> {
                // give feedback to the user + offer to download missing tiles (if available)
                if (missingDownloads.isEmpty()) {
                    ActivityMixin.showShortToast(activity, hasUnsupportedTiles.get() ? R.string.check_tiles_unsupported : R.string.check_tiles_found);
                } else {
                    if (hasUnsupportedTiles.get()) {
                        ActivityMixin.showShortToast(activity, R.string.check_tiles_unsupported);
                    }
                    DownloaderUtils.triggerDownloads(activity, R.string.downloadtile_title, R.string.check_tiles_missing, missingDownloads, null);
                }
            });
        } else {
            // if external routing configured: try to open BRouter downloader
            try {
                final String bRouterPackage = activity.getString(R.string.package_brouter);
                final Intent intent = new Intent();
                intent.setComponent(new ComponentName(bRouterPackage, bRouterPackage + ".BInstallerActivity"));
                activity.startActivity(intent);
            } catch (ActivityNotFoundException ignore) {
                ActivityMixin.showShortToast(activity, R.string.cache_not_status_found);
            }
        }
    }

    public static void onPrepareOptionsMenu(final Menu menu) {
        final MenuItem item = menu.findItem(R.id.menu_check_routingdata);
        if (item != null) {
            // use same condition as in checkRoutingData() above
            item.setVisible(Settings.useInternalRouting() || ProcessUtils.isInstalled(CgeoApplication.getInstance().getString(R.string.package_brouter)));
        }
    }

    @WorkerThread
    private static void checkTiles(final HashMap<String, String> missingTiles, final ArrayList<Download> missingDownloads, final AtomicBoolean hasUnsupportedTiles) {
        // read tiles already stored
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.ROUTING_TILES.getFolder());
        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.endsWith(BROUTER_TILE_FILEEXTENSION)) {
                missingTiles.remove(fi.name);
            }
        }

        // read list of available tiles from the server, if necessary
        if (!missingTiles.isEmpty()) {
            final HashMap<String, Download> tiles = BRouterTileDownloader.getInstance().getAvailableTiles();
            final ArrayList<String> filenames = new ArrayList<>(missingTiles.values()); // work on copy to avoid concurrent modification
            for (String filename : filenames) {
                if (tiles.containsKey(filename)) {
                    missingDownloads.add(tiles.get(filename));
                } else {
                    missingTiles.remove(filename);
                    hasUnsupportedTiles.set(true);
                }
            }
        }
    }

    /**
     * @return the complete popup builder without dismiss listener specified
     */
    public static SimplePopupMenu createMapLongClickPopupMenu(final Activity activity, final Geopoint longClickGeopoint, final Point tapXY, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility, final Geocache currentTargetCache, final MapOptions mapOptions, final Action2<Geopoint, String> setTarget) {
        final int offset = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.map_pin, null).getIntrinsicHeight() / 2;

        return SimplePopupMenu.of(activity)
                .setMenuContent(R.menu.map_longclick)
                .setPosition(new Point(tapXY.x, tapXY.y - offset), (int) (offset * 1.25))
                .setOnCreatePopupMenuListener(menu -> {
                    menu.findItem(R.id.menu_add_waypoint).setVisible(currentTargetCache != null);
                    menu.findItem(R.id.menu_add_to_route_start).setVisible(individualRoute.getNumPoints() > 0);
                })
                .addItemClickListener(R.id.menu_udc, item -> InternalConnector.interactiveCreateCache(activity, longClickGeopoint, mapOptions.fromList, true))
                .addItemClickListener(R.id.menu_add_waypoint, item -> EditWaypointActivity.startActivityAddWaypoint(activity, currentTargetCache, longClickGeopoint))
                .addItemClickListener(R.id.menu_coords, item -> {
                    final AtomicReference<TextView> textview = new AtomicReference<>();
                    final AlertDialog dialog = Dialogs.newBuilder(activity)
                            .setTitle(R.string.waypoint_coordinates)
                            .setMessage("") // set a dummy message so that the textview gets created
                            .setPositiveButton(R.string.ok, null)
                            .setNegativeButton(android.R.string.copy, (d, which) -> {
                                ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(textview.get().getText()));
                                Toast.makeText(activity, R.string.clipboard_copy_ok, Toast.LENGTH_SHORT).show();
                            })
                            .show();
                    textview.set(dialog.findViewById(android.R.id.message));
                    new CoordinatesFormatSwitcher().setView(textview.get()).setCoordinate(longClickGeopoint);
                })
                .addItemClickListener(R.id.menu_add_to_route, item -> {
                    individualRoute.toggleItem(activity, new RouteItem(longClickGeopoint), routeUpdater, false);
                    updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility);
                })
                .addItemClickListener(R.id.menu_add_to_route_start, item -> {
                    individualRoute.toggleItem(activity, new RouteItem(longClickGeopoint), routeUpdater, true);
                    updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility);
                })
                .addItemClickListener(R.id.menu_target, item -> {
                    setTarget.call(longClickGeopoint, null);
                    updateRouteTrackButtonVisibility.run();
                })
                .addItemClickListener(R.id.menu_navigate, item -> NavigationAppFactory.showNavigationMenu(activity, null, null, longClickGeopoint, false, true));
    }

    private static void updateRouteTrackButtonVisibility(final Runnable updateRouteTrackButtonVisibility) {
        if (updateRouteTrackButtonVisibility != null) {
            updateRouteTrackButtonVisibility.run();
        }
    }

    public static boolean isPartOfRoute(final RouteItem routeItem, final IndividualRoute individualRoute) {
        final RouteSegment[] segments = (individualRoute != null ? individualRoute.getSegments() : null);
        if (segments == null || segments.length == 0) {
            return false;
        }
        final String routeItemIdentifier = routeItem.getIdentifier();
        for (RouteSegment segment : segments) {
            if (StringUtils.equals(routeItemIdentifier, segment.getItem().getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public static SimplePopupMenu createCacheWaypointLongClickPopupMenu(final Activity activity, final RouteItem routeItem, final int tapX, final int tapY, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {
        final RouteSegment[] segments = (individualRoute != null ? individualRoute.getSegments() : null);
        final boolean routeIsNotEmpty = segments != null && segments.length > 0;
        final int baseId = (segments != null ? segments.length : 0) + 100;

        boolean isEnd = false;
        final SimplePopupMenu menu = SimplePopupMenu.of(activity).setPosition(new Point(tapX, tapY), 0);
        if (routeIsNotEmpty) {
            final String routeItemIdentifier = routeItem.getIdentifier();
            final boolean isStart = StringUtils.equals(routeItemIdentifier, segments[0].getItem().getIdentifier());
            if (isStart) {
                addMenuHelper(activity, menu, 0, activity.getString(R.string.context_map_remove_from_route_start), individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
            }
            for (int i = 1; i < segments.length - 1; i++) {
                if (StringUtils.equals(routeItemIdentifier, segments[i].getItem().getIdentifier())) {
                    addMenuHelper(activity, menu, i, String.format(Locale.getDefault(), activity.getString(R.string.context_map_remove_from_route_pos), i + 1), individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
                }
            }
            isEnd = (segments.length > 1) && StringUtils.equals(routeItemIdentifier, segments[segments.length - 1].getItem().getIdentifier());
            if (isEnd) {
                addMenuHelper(activity, menu, segments.length - 1, activity.getString(R.string.context_map_remove_from_route_end), individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
            }
            if (!isStart) {
                addMenuHelper(activity, menu, baseId, activity.getString(R.string.context_map_add_to_route_start), routeItem, true, individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
            }
        }
        if (!isEnd) {
            addMenuHelper(activity, menu, baseId + 1, activity.getString(R.string.context_map_add_to_route), routeItem, false, individualRoute, routeUpdater, updateRouteTrackButtonVisibility);
        }
        return menu;
    }

    private static void addMenuHelper(final Activity activity, final SimplePopupMenu menu, final int uniqueId, final CharSequence title, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {
        menu.addMenuItem(uniqueId, title, R.drawable.ic_menu_delete);
        menu.addItemClickListener(uniqueId, item -> {
            individualRoute.removeItem(activity, uniqueId, routeUpdater);
            updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility);
        });
    }

    private static void addMenuHelper(final Activity activity, final SimplePopupMenu menu, final int uniqueId, final CharSequence title, final RouteItem routeItem, final boolean addToRouteStart, final IndividualRoute individualRoute, final IndividualRoute.UpdateIndividualRoute routeUpdater, final Runnable updateRouteTrackButtonVisibility) {
        menu.addMenuItem(uniqueId, title, R.drawable.ic_menu_add);
        menu.addItemClickListener(uniqueId, item -> {
            individualRoute.addItem(activity, routeItem, routeUpdater, addToRouteStart);
            updateRouteTrackButtonVisibility(updateRouteTrackButtonVisibility);
        });
    }

    // ---------------------------------------------------------------------------------------------
    // Handling of cache/waypoint details fragments

    public static void showCacheDetails(final AppCompatActivity activity, final String geocode) {
        configureDetailsFragment(CachePopupFragment.newInstance(geocode), activity, () -> CacheDetailActivity.startActivity(activity, geocode));
    }

    public static void showWaypointDetails(final AppCompatActivity activity, final String geocode, final int waypointId) {
        configureDetailsFragment(WaypointPopupFragment.newInstance(geocode, waypointId), activity, () -> CacheDetailActivity.startActivity(activity, geocode));
    }

    private static void configureDetailsFragment(final Fragment fragment, final AppCompatActivity activity, final Runnable onUpSwipeAction) {

        final FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        final SwipeToOpenFragment swipeToOpenFragment = new SwipeToOpenFragment();
        ft.replace(R.id.detailsfragment, swipeToOpenFragment, TAG_SWIPE_FRAGMENT);
        ft.add(R.id.detailsfragment, fragment, TAG_MAPDETAILS_FRAGMENT);

        ft.commit();

        final FrameLayout fl = activity.findViewById(R.id.detailsfragment);
        final ViewGroup.LayoutParams params = fl.getLayoutParams();
        final CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        final boolean isBottomSheet = behavior instanceof BottomSheetBehavior;

        if (isBottomSheet) { // portrait mode uses BottomSheet
            final BottomSheetBehavior<FrameLayout> b = BottomSheetBehavior.from(fl);
            b.setHideable(true);
            b.setSkipCollapsed(false);
            b.setPeekHeight(0); // temporary set to 0 to avoid bumping. Gets updated once view is loaded.
            b.setState(BottomSheetBehavior.STATE_COLLAPSED);

            ft.runOnCommit(() -> {
                final View view = fragment.requireView();
                // make bottom sheet fill whole screen
                swipeToOpenFragment.requireView().setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
                // set the height of collapsed state to height of the details fragment
                view.getViewTreeObserver().addOnGlobalLayoutListener(() -> b.setPeekHeight(view.getHeight()));
            });

            final BottomSheetBehavior.BottomSheetCallback callback = new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull final View bottomSheet, final int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        removeDetailsFragment(activity);
                    }
                    if (newState == BottomSheetBehavior.STATE_EXPANDED && onUpSwipeAction != null) {
                        onUpSwipeAction.run();
                        ActivityMixin.overrideTransitionToFade(activity);
                    }
                }

                @Override
                public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {
                    swipeToOpenFragment.setExpansion(slideOffset, fragment.getView());
                }
            };

            b.addBottomSheetCallback(callback);
            swipeToOpenFragment.setOnStopCallback(() -> b.removeBottomSheetCallback(callback));

        } else { // landscape mode uses SideSheet
            final SideSheetBehavior<FrameLayout> b = SideSheetBehavior.from(fl);
            b.setState(SideSheetBehavior.STATE_EXPANDED);

            final SideSheetCallback callback = new SideSheetCallback() {
                @Override
                public void onStateChanged(@NonNull final View sheet, final int newState) {
                    if (newState == SideSheetBehavior.STATE_HIDDEN) {
                        removeDetailsFragment(activity);
                    }
                }

                @Override
                public void onSlide(@NonNull final View sheet, final float slideOffset) {
                    // nothing
                }
            };

            b.addCallback(callback);
            swipeToOpenFragment.setOnStopCallback(() -> b.removeCallback(callback));

        }

        fl.setVisibility(View.VISIBLE);
    }

    /** removes fragment and view for mapdetails view; returns true, if view got removed */
    public static boolean removeDetailsFragment(final FragmentActivity activity) {
        final FragmentManager fm = activity.getSupportFragmentManager();
        final Fragment f1 = fm.findFragmentByTag(TAG_MAPDETAILS_FRAGMENT);
        if (f1 != null) {
            fm.beginTransaction().remove(f1).commit();
        }
        final Fragment f2 = fm.findFragmentByTag(TAG_SWIPE_FRAGMENT);
        if (f2 != null) {
            fm.beginTransaction().remove(f2).commit();
        }
        final FrameLayout v = activity.findViewById(R.id.detailsfragment);
        if (v != null && v.getVisibility() != View.GONE) {

            final CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) v.getLayoutParams()).getBehavior();
            if (behavior instanceof BottomSheetBehavior) {
                final BottomSheetBehavior<FrameLayout> b = BottomSheetBehavior.from(v);
                b.setState(BottomSheetBehavior.STATE_HIDDEN); // close correctly as it will otherwise conflict with up-swipe behaviour implementation
            }

            v.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

}
