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

package cgeo.geocaching.maps

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.export.IndividualRouteExport
import cgeo.geocaching.files.GPXIndividualRouteImporter
import cgeo.geocaching.files.GPXTrackOrRouteImporter
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.routing.RouteOptimizationHelper
import cgeo.geocaching.maps.routing.RouteSortActivity
import cgeo.geocaching.models.IndividualRoute
import cgeo.geocaching.models.NavigationTargetRoute
import cgeo.geocaching.models.Route
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.RouteSegment
import cgeo.geocaching.models.geoitem.IGeoItemSupplier
import cgeo.geocaching.service.CacheDownloaderService
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorageActivityHelper
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.storage.extension.Trackfiles
import cgeo.geocaching.ui.ColorPickerUI
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.ui.dialog.SimplePopupMenu
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MenuUtils
import cgeo.geocaching.utils.UriUtils
import cgeo.geocaching.utils.functions.Action2
import cgeo.geocaching.utils.functions.Func0

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.widget.Toolbar

import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set
import java.util.concurrent.atomic.AtomicBoolean

import com.google.android.material.bottomsheet.BottomSheetDialog
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils

class RouteTrackUtils {

    private static val REQUEST_SORT_INDIVIDUAL_ROUTE: Int = 4712
    private static val STATE_CSAH: String = "rtu_csah"

    private final Activity activity
    private final ContentStorageActivityHelper csah
    private var popup: View = null
    private var tracks: Tracks = null
    private var dialog: BottomSheetDialog = null

    private final Runnable reloadIndividualRoute
    private final Runnable clearIndividualRoute

    private final Tracks.UpdateTrack updateTrack
    private final Route.CenterOnPosition centerOnPosition

    private final Func0<Boolean> isTargetSet

    public RouteTrackUtils(final Activity activity, final Bundle savedState, final Route.CenterOnPosition centerOnPosition, final Runnable clearIndividualRoute, final Runnable reloadIndividualRoute, final Tracks.UpdateTrack updateTrack, final Func0<Boolean> isTargetSet) {
        Log.d("[RouteTrackDebug] RouteTrackUtils initialized")
        this.activity = activity
        this.centerOnPosition = centerOnPosition
        this.clearIndividualRoute = clearIndividualRoute
        this.reloadIndividualRoute = reloadIndividualRoute
        this.updateTrack = updateTrack
        this.isTargetSet = isTargetSet

        this.csah = ContentStorageActivityHelper(activity, savedState == null ? null : savedState.getBundle(STATE_CSAH))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE, Uri.class, this::importIndividualRoute)
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE_MULTIPLE, List.class, this::importTracks)
    }

    private Unit importIndividualRoute(final Uri uri) {
        if (uri != null) {
            Log.d("[RouteTrackDebug] Start import of individual route")
            GPXIndividualRouteImporter.doImport(activity, uri)
            reloadIndividualRoute.run()
        }
    }

    private Unit importTracks(final List<Uri> uris) {
        if (uris != null && this.updateTrack != null) {
            for (Uri uri : uris) {
                Log.d("[RouteTrackDebug] Start import of track " + uri)
                GPXTrackOrRouteImporter.doImport(activity, uri, UriUtils.getLastPathSegment(uri), (route) -> {
                    Log.d("[RouteTrackDebug] Finished import of track " + uri + ": " + (route == null ? "null returned" : "updating map"))
                    if (route != null) {
                        val key: String = tracks.add(activity, uri, updateTrack)
                        tracks.setRoute(key, route)
                        updateTrack.updateRoute(key, route, tracks.getColor(key), tracks.getWidth(key))
                        updateDialogTracks(popup, tracks, null)
                    }
                })
            }
        }
    }

    public Unit setTracks(final Tracks tracks) {
        this.tracks = tracks
    }

    /**
     * Shows a popup menu for individual route related items
     */
    public Unit showPopup(final IndividualRoute individualRoute, final Action2<Geopoint, String> setTarget, final Action2<Route, Boolean> showElevationChart) {
        popup = activity.getLayoutInflater().inflate(R.layout.routes_tracks_dialog, null)
        updateDialogIndividualRoute(popup, individualRoute, setTarget, showElevationChart)
        updateDialogTracks(popup, tracks, showElevationChart)
        dialog = Dialogs.bottomSheetDialogWithActionbar(activity, popup, R.string.routes_tracks_dialog_title)
        dialog.setOnDismissListener(dialog1 -> popup = null)
        dialog.show()
    }

    // ---------------------------------------------------------------------------------------------
    // route/track context menu-related methods

    /** show a popup for track/individual route, opened by using a Long-tap on that item on the map */
    public Unit showRouteTrackContextMenu(final Int tapX, final Int tapY, final Action2<Route, Boolean> handleLongTapOnRoutesOrTracks, final Route route, final Runnable onDelete) {
        val menu: SimplePopupMenu = SimplePopupMenu.of(activity).setMenuContent(R.menu.map_routetrack_context).setPosition(Point(tapX, tapY), 0)
        menu.setOnCreatePopupMenuListener(menu1 -> configureContextMenu(menu1, handleLongTapOnRoutesOrTracks != null, route, true))
        menu.setOnItemClickListener(item -> handleContextMenuClick(item, handleLongTapOnRoutesOrTracks, route, onDelete))
        menu.show()
    }

    public static Unit configureContextMenu(final Menu menu, final Boolean showElevationChart, final IGeoItemSupplier route, final Boolean hidePerDefault) {
        MenuUtils.enableIconsInOverflowMenu(menu)

        val isIndividualRoute: Boolean = isIndividualRoute(route)
        val isNavigationTargetRoute: Boolean = isNavigationTargetRoute(route)
        val elevationChart: MenuItem = menu.findItem(R.id.menu_showElevationChart)
        configureMenuItem(elevationChart, showElevationChart, null)
        elevationChart.setEnabled(route is Route)
        configureMenuItem(menu.findItem(R.id.menu_edit), isIndividualRoute, hidePerDefault)
        configureMenuItem(menu.findItem(R.id.menu_color), !isIndividualRoute && !isNavigationTargetRoute, hidePerDefault)
        configureMenuItem(menu.findItem(R.id.menu_rename), !isIndividualRoute && !isNavigationTargetRoute, null)
        configureMenuItem(menu.findItem(R.id.menu_center), true, hidePerDefault)
        configureMenuItem(menu.findItem(R.id.menu_optimize), isIndividualRoute, null)
        configureMenuItem(menu.findItem(R.id.menu_refresh), isIndividualRoute, null)
        configureMenuItem(menu.findItem(R.id.menu_invert_order), isIndividualRoute, null)
        configureMenuItem(menu.findItem(R.id.menu_visibility), route != null && !isNavigationTargetRoute, hidePerDefault)
        configureMenuItem(menu.findItem(R.id.menu_delete), !isNavigationTargetRoute, hidePerDefault)
        configureMenuItem(menu.findItem(R.id.menu_clear_target), isNavigationTargetRoute, hidePerDefault)
        configureMenuItem(menu.findItem(R.id.indivroute_export_route), isIndividualRoute, null)
        configureMenuItem(menu.findItem(R.id.indivroute_export_track), isIndividualRoute, null)
        configureMenuItem(menu.findItem(R.id.indivroute_load), isIndividualRoute, null)
        configureVisibility(menu.findItem(R.id.menu_visibility), route != null && route.isHidden())
    }

    @SuppressLint("AlwaysShowAction")
    private static Unit configureMenuItem(final MenuItem item, final Boolean visible, final Boolean hidePerDefault) {
        item.setVisible(visible)
        if (hidePerDefault != null) {
            item.setShowAsAction(hidePerDefault ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
    }

    private static Unit configureVisibility(final MenuItem item, final Boolean isHidden) {
        item.setIcon(isHidden ? R.drawable.visibility_off : R.drawable.visibility)
        item.setTitle(CgeoApplication.getInstance().getString(isHidden ? R.string.make_visible : R.string.hide))
    }

    public Boolean handleContextMenuClick(final MenuItem item, final Action2<Route, Boolean> showElevationChart, final IGeoItemSupplier route, final Runnable onDelete) {
        val id: Int = item.getItemId()
        if (id == R.id.menu_showElevationChart && showElevationChart != null && route is Route) {
            if (dialog != null) {
                dialog.dismiss()
            }
            showElevationChart.call((Route) route, true)
        } else if (id == R.id.menu_edit && isIndividualRoute(route)) {
            activity.startActivityForResult(Intent(activity, RouteSortActivity.class), REQUEST_SORT_INDIVIDUAL_ROUTE)
        } else if (id == R.id.menu_optimize && isIndividualRoute(route)) {
            val roh: RouteOptimizationHelper = RouteOptimizationHelper(((IndividualRoute) route).getRouteItems())
            roh.start(activity, this::storeAndReloadIndividualRoute)
        } else if (id == R.id.menu_invert_order && isIndividualRoute(route)) {
            val newRouteItems: ArrayList<RouteItem> = ArrayList<>(((IndividualRoute) route).getRouteItems())
            Collections.reverse(newRouteItems)
            storeAndReloadIndividualRoute(newRouteItems)
        } else if (id == R.id.menu_color && !isIndividualRoute(route)) {
            tracks.find(route, (key, routeForThisKey) -> setTrackColor(activity, tracks, key, item, updateTrack))
        } else if (id == R.id.menu_rename) {
            tracks.find(route, (key, routeForThisKey) -> SimpleDialog.ofContext(dialog.getContext())
                    .setTitle(TextParam.text(activity.getString(R.string.routes_tracks_change_name)))
                    .input(SimpleDialog.InputOptions().setInitialValue(tracks.getDisplayname(key)), newName -> {
                        if (StringUtils.isNotBlank(newName)) {
                            tracks.setDisplayname(key, newName)
                            updateDialogTracks(popup, tracks, null)
                        }
                    }))
        } else if (id == R.id.menu_visibility) {
            val willBeHidden: Boolean = !route.isHidden()
            route.setHidden(willBeHidden)
            if (isIndividualRoute(route)) {
                reloadIndividualRoute.run()
            } else {
                tracks.find(route, (key, routeForThisKey) -> {
                    updateTrack.updateRoute(key, route, tracks.getColor(key), tracks.getWidth(key))
                    tracks.hide(key, willBeHidden)
                })
            }
            configureVisibility(item, willBeHidden)
        } else if (id == R.id.menu_center) {
            if (route is Route) {
                ((Route) route).setCenter(centerOnPosition)
            } else {
                val vp: Viewport = route.getViewport()
                centerOnPosition.centerOnPosition(vp.getCenter().getLatitude(), vp.getCenter().getLongitude(), vp)
            }
        } else if (id == R.id.menu_delete) {
            if (isIndividualRoute(route)) {
                SimpleDialog.of(activity).setTitle(R.string.map_clear_individual_route).setMessage(R.string.map_clear_individual_route_confirm).confirm(() -> {
                    clearIndividualRoute.run()
                    if (onDelete != null) {
                        onDelete.run()
                    }
                })
            } else {
                tracks.find(route, (key, routeForThisKey) -> SimpleDialog.of(activity).setTitle(R.string.map_clear_track).setMessage(TextParam.text(String.format(activity.getString(R.string.map_clear_track_confirm), tracks.getDisplayname(key)))).confirm(() -> {
                    tracks.remove(key)
                    updateTrack.updateRoute(key, null, tracks.getColor(key), tracks.getWidth(key))
                    if (onDelete != null) {
                        onDelete.run()
                    }
                }))
            }
        } else if (id == R.id.menu_clear_target) {
            if (onDelete != null) {
                onDelete.run()
            }
        } else if (id == R.id.menu_refresh && isIndividualRoute(route)) {
            // create list of geocodes contained in individual route (including geocodes for contained waypoints)
            val geocodes: Set<String> = HashSet<>()
            final RouteSegment[] segments = ((Route) route).getSegments()
            for (RouteSegment segment : segments) {
                if (segment != null) {
                    val routeItem: RouteItem = segment.getItem()
                    if (routeItem != null) {
                        val geocode: String = routeItem.getGeocode()
                        if (StringUtils.isNotBlank(geocode)) {
                            geocodes.add(geocode)
                        }
                    }
                }
            }
            // refresh those caches
            CacheDownloaderService.downloadCaches(activity, geocodes, true, true, null)
        } else if (id == R.id.indivroute_export_route && isIndividualRoute(route)) {
            IndividualRouteExport(activity, (Route) route, false)
        } else if (id == R.id.indivroute_export_track && isIndividualRoute(route)) {
            IndividualRouteExport(activity, (Route) route, true)
        } else if (id == R.id.indivroute_load && isIndividualRoute(route)) {
                if (((Route) route).getNumSegments() == 0) {
                    startFileSelectorIndividualRoute()
                } else {
                    SimpleDialog.of(activity).setTitle(R.string.map_load_from_GPX).setMessage(R.string.map_load_individual_route_confirm).confirm(this::startFileSelectorIndividualRoute)
                }
        } else {
            return false
        }
        return true
    }

    private Unit storeAndReloadIndividualRoute(final ArrayList<RouteItem>newRouteItems) {
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.saveIndividualRoute(newRouteItems), reloadIndividualRoute)
    }

    // ---------------------------------------------------------------------------------------------
    // route/track bottom menu-related methods

    private Unit startFileSelectorIndividualRoute() {
        csah.selectFile(null, PersistableFolder.GPX.getUri())
    }

    private Unit updateDialogIndividualRoute(final View dialog, final IndividualRoute individualRoute, final Action2<Geopoint, String> setTarget, final Action2<Route, Boolean> showElevationChart) {
        if (dialog == null) {
            return
        }
        val hasIndividualRoute: Boolean = isRouteNonEmpty(individualRoute)
        dialog.findViewById(R.id.indivroute).setVisibility(hasIndividualRoute ? View.VISIBLE : View.GONE)
        val v: View = dialog.findViewById(R.id.indivroute_load)
        v.setVisibility(hasIndividualRoute ? View.GONE : View.VISIBLE)
        v.setOnClickListener(hasIndividualRoute ? null : (view) -> {
            startFileSelectorIndividualRoute()
            this.dialog.dismiss()
        })

        if (hasIndividualRoute) {
            val tb: Toolbar = dialog.findViewById(R.id.routes_track_item)
            if (tb.getMenu() == null || tb.getMenu().size() == 0) {
                tb.inflateMenu(R.menu.map_routetrack_context)
            }
            tb.setOnMenuItemClickListener(item -> handleContextMenuClick(item, showElevationChart, individualRoute, () -> updateDialogIndividualRoute(dialog, individualRoute, setTarget, showElevationChart)))
            val menu: Menu = tb.getMenu()
            configureContextMenu(menu, true, individualRoute, false)

            val vAutoTarget: CheckBox = dialog.findViewById(R.id.auto_target)
            vAutoTarget.setChecked(Settings.isAutotargetIndividualRoute())
            vAutoTarget.setOnClickListener(v1 -> {
                setAutotargetIndividualRoute(activity, individualRoute, !Settings.isAutotargetIndividualRoute())
                updateDialogClearTargets(popup, individualRoute, setTarget, showElevationChart)
            })
        }
        updateDialogClearTargets(dialog, individualRoute, setTarget, showElevationChart)
    }

    private Unit updateDialogTracks(final View dialog, final Tracks tracks, final Action2<Route, Boolean> showElevationChart) {
        if (dialog == null) {
            return
        }
        val tracklist: LinearLayout = dialog.findViewById(R.id.tracklist)
        tracklist.removeAllViews()
        dialog.findViewById(R.id.trackroute_load).setOnClickListener(v1 -> csah.selectMultipleFiles(null, PersistableFolder.GPX.getUri()))

        tracks.traverse((key, geoData) -> {
            val tb: Toolbar = activity.getLayoutInflater().inflate(R.layout.routes_tracks_item, null).findViewById(R.id.routes_track_item)
            tb.inflateMenu(R.menu.map_routetrack_context)
            tb.setOnMenuItemClickListener(item -> handleContextMenuClick(item, showElevationChart, geoData, () -> updateDialogTracks(dialog, tracks, showElevationChart)))
            configureContextMenu(tb.getMenu(), true, geoData, false)

            val displayName: TextView = tb.findViewById(R.id.item_title)
            displayName.setText(tracks.getDisplayname(key))

            val vColor: MenuItem = tb.getMenu().findItem(R.id.menu_color)
            setColorIcon(vColor, tracks.getColor(key))
            vColor.getActionView().setOnClickListener(view -> setTrackColor(activity, tracks, key, vColor, updateTrack))

            tracklist.addView(tb)
        })
    }

    private static Unit setTrackColor(final Activity activity, final Tracks tracks, final String key, final MenuItem item, final Tracks.UpdateTrack updateTrack) {
        ColorPickerUI(activity, tracks.getColor(key), tracks.getWidth(key), false, 0, 0, true, true).show((newColor, newWidth) -> {
            tracks.setColor(key, newColor)
            tracks.setWidth(key, newWidth)
            setColorIcon(item, newColor)
            updateTrack.updateRoute(key, tracks.getRoute(key), tracks.getColor(key), tracks.getWidth(key))
        })
    }

    private static Unit setColorIcon(final MenuItem item, final Int newColor) {
        val d: Drawable = ColorPickerUI.getViewImage(null, newColor, false)
        d.mutate().setColorFilter(null)
        val v: ImageButton = ((ImageButton) item.getActionView())
        v.setBackground(d)
        v.setMinimumHeight(ViewUtils.dpToPixel(36))
        v.setMinimumWidth(ViewUtils.dpToPixel(36))
    }

    private Unit updateDialogClearTargets(final View dialog, final IndividualRoute individualRoute, final Action2<Geopoint, String> setTarget, final Action2<Route, Boolean> showElevationChart) {
        val vClearTargets: View = dialog.findViewById(R.id.clear_targets)
        vClearTargets.setVisibility(isTargetSet.call() || Settings.isAutotargetIndividualRoute() ? View.VISIBLE : View.GONE)
        vClearTargets.setOnClickListener(v1 -> {
            if (setTarget != null) {
                setTarget.call(null, null)
            }
            Settings.setAutotargetIndividualRoute(false)
            individualRoute.triggerTargetUpdate(true)
            ActivityMixin.showToast(activity, R.string.map_manual_targets_cleared)
            updateDialogIndividualRoute(dialog, individualRoute, setTarget, showElevationChart)
        })
    }

    private Boolean isRouteNonEmpty(final IGeoItemSupplier route) {
        return route != null && (!(route is Route) || ((Route) route).getNumSegments() > 0)
    }

    public Unit reloadTrack(final Trackfiles trackfile, final Tracks.UpdateTrack updateTrack) {
        val uri: Uri = Trackfiles.getUriFromKey(trackfile.getKey())
        Log.d("[RouteTrackDebug] Start reloading track from trackfile " + trackfile.getFilename())
        GPXTrackOrRouteImporter.doImport(activity, uri, trackfile.getDisplayname(), (route) -> {
            if (route != null) {
                Log.d("[RouteTrackDebug] Reloading track from trackfile " + trackfile.getFilename() + " finished, updating map")
                route.setHidden(trackfile.isHidden())
                updateDialogTracks(popup, tracks, null)
                updateTrack.updateRoute(trackfile.getKey(), route, trackfile.getColor(), trackfile.getWidth())
            } else {
                Log.d("[RouteTrackDebug] Reloading track from trackfile " + trackfile.getFilename() + " returned null")
            }
        })
    }

    public Unit updateRouteTrackButtonVisibility(final View button, final IndividualRoute route) {
        updateRouteTrackButtonVisibility(button, route, tracks)
    }

    public Unit updateRouteTrackButtonVisibility(final View button, final IndividualRoute route, final Tracks tracks) {
        val someTrackAvailable: AtomicBoolean = AtomicBoolean(isRouteNonEmpty(route) || isTargetSet.call())
        if (tracks != null) {
            tracks.traverse((key, r) -> {
                if (!someTrackAvailable.get() && isRouteNonEmpty(r)) {
                    someTrackAvailable.set(true)
                }
            })
        }
        button.setVisibility(someTrackAvailable.get() ? View.VISIBLE : View.GONE)
    }

    public Boolean onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        if (csah.onActivityResult(requestCode, resultCode, data)) {
            return true
        }
        if (requestCode == REQUEST_SORT_INDIVIDUAL_ROUTE) {
            reloadIndividualRoute.run()
            return true
        }
        return (csah.onActivityResult(requestCode, resultCode, data))
    }

    public Bundle getState() {
        val bundle: Bundle = Bundle()
        bundle.putBundle(STATE_CSAH, this.csah.getState())
        return bundle
    }

    public static Unit setAutotargetIndividualRoute(final Activity activity, final IndividualRoute route, final Boolean newValue) {
        Settings.setAutotargetIndividualRoute(newValue)
        route.triggerTargetUpdate(!Settings.isAutotargetIndividualRoute())
        ActivityMixin.invalidateOptionsMenu(activity)
    }

    public static Boolean isIndividualRoute(final IGeoItemSupplier route) {
        return route is IndividualRoute
    }

    public static Boolean isNavigationTargetRoute(final IGeoItemSupplier route) {
        return route is NavigationTargetRoute
    }
}
