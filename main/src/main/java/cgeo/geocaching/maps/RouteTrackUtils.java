package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.export.IndividualRouteExport;
import cgeo.geocaching.files.GPXIndividualRouteImporter;
import cgeo.geocaching.files.GPXTrackOrRouteImporter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.routing.RouteOptimizationHelper;
import cgeo.geocaching.maps.routing.RouteSortActivity;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;
import cgeo.geocaching.service.CacheDownloaderService;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.Trackfiles;
import cgeo.geocaching.ui.ColorPickerUI;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.dialog.SimplePopupMenu;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Action2;
import cgeo.geocaching.utils.functions.Func0;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;

public class RouteTrackUtils {

    private static final int REQUEST_SORT_INDIVIDUAL_ROUTE = 4712;
    private static final String STATE_CSAH = "rtu_csah";

    private final Activity activity;
    private final ContentStorageActivityHelper csah;
    private View popup = null;
    private Tracks tracks = null;

    private final Runnable reloadIndividualRoute;
    private final Runnable clearIndividualRoute;

    private final Tracks.UpdateTrack updateTrack;
    private final Route.CenterOnPosition centerOnPosition;

    private final Func0<Boolean> isTargetSet;

    public RouteTrackUtils(final Activity activity, final Bundle savedState, final Route.CenterOnPosition centerOnPosition, final Runnable clearIndividualRoute, final Runnable reloadIndividualRoute, final Tracks.UpdateTrack updateTrack, final Func0<Boolean> isTargetSet) {
        Log.d("[RouteTrackDebug] RouteTrackUtils initialized");
        this.activity = activity;
        this.centerOnPosition = centerOnPosition;
        this.clearIndividualRoute = clearIndividualRoute;
        this.reloadIndividualRoute = reloadIndividualRoute;
        this.updateTrack = updateTrack;
        this.isTargetSet = isTargetSet;

        this.csah = new ContentStorageActivityHelper(activity, savedState == null ? null : savedState.getBundle(STATE_CSAH))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE, Uri.class, this::importIndividualRoute)
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE_MULTIPLE, List.class, this::importTracks);
    }

    private void importIndividualRoute(final Uri uri) {
        if (uri != null) {
            Log.d("[RouteTrackDebug] Start import of individual route");
            GPXIndividualRouteImporter.doImport(activity, uri);
            reloadIndividualRoute.run();
        }
    }

    private void importTracks(final List<Uri> uris) {
        if (uris != null && this.updateTrack != null) {
            for (Uri uri : uris) {
                Log.d("[RouteTrackDebug] Start import of track " + uri);
                GPXTrackOrRouteImporter.doImport(activity, uri, UriUtils.getLastPathSegment(uri), (route) -> {
                    Log.d("[RouteTrackDebug] Finished import of track " + uri + ": " + (route == null ? "null returned" : "updating map"));
                    final String key = tracks.add(activity, uri, updateTrack);
                    tracks.setRoute(key, route);
                    updateTrack.updateRoute(key, route, tracks.getColor(key), tracks.getWidth(key));
                    updateDialogTracks(popup, tracks);
                });
            }
        }
    }

    public void setTracks(final Tracks tracks) {
        this.tracks = tracks;
    }

    /**
     * Shows a popup menu for individual route related items
     */
    public void showPopup(final IndividualRoute individualRoute, final Action2<Geopoint, String> setTarget) {
        this.popup = activity.getLayoutInflater().inflate(R.layout.routes_tracks_dialog, null);
        updateDialog(this.popup, individualRoute, tracks, setTarget);
        final BottomSheetDialog dialog = Dialogs.bottomSheetDialogWithActionbar(activity, this.popup, R.string.routes_tracks_dialog_title);
        dialog.setOnDismissListener(dialog1 -> popup = null);
        dialog.show();
    }

    // ---------------------------------------------------------------------------------------------
    // route/track context menu-related methods

    /** show a popup for track/individual route, opened by using a long-tap on that item on the map */
    public void showRouteTrackContextMenu(final int tapX, final int tapY, final Action1<Route> handleLongTapOnRoutesOrTracks, final Route route) {
        final SimplePopupMenu menu = SimplePopupMenu.of(activity).setMenuContent(R.menu.map_routetrack_context).setPosition(new Point(tapX, tapY), 0);
        menu.setOnCreatePopupMenuListener(menu1 -> configureContextMenu(menu1, handleLongTapOnRoutesOrTracks != null, route));
        menu.setOnItemClickListener(item -> handleContextMenuClick(item, handleLongTapOnRoutesOrTracks, route));
        menu.show();
    }

    public static void configureContextMenu(final Menu menu, final boolean showElevationChart, final Route route) {
        final boolean isIndividualRoute = isIndividualRoute(route);
        menu.findItem(R.id.menu_showElevationChart).setVisible(showElevationChart);
        menu.findItem(R.id.menu_edit).setVisible(isIndividualRoute);
        menu.findItem(R.id.menu_optimize).setVisible(isIndividualRoute);
        menu.findItem(R.id.menu_invert_order).setVisible(isIndividualRoute);
        menu.findItem(R.id.menu_color).setVisible(!isIndividualRoute);
    }

    public boolean handleContextMenuClick(final MenuItem item, final Action1<Route> handleLongTapOnRoutesOrTracks, final Route route) {
        final int id = item.getItemId();
        if (id == R.id.menu_showElevationChart && handleLongTapOnRoutesOrTracks != null) {
            handleLongTapOnRoutesOrTracks.call(route);
        } else if (id == R.id.menu_edit) {
            menuEditRoute(route);
        } else if (id == R.id.menu_optimize) {
            if (isIndividualRoute(route)) {
                menuOptimizeRoute((IndividualRoute) route);
            }
        } else if (id == R.id.menu_invert_order) {
            if (isIndividualRoute(route)) {
                menuInvertRoute((IndividualRoute) route);
            }
        } else if (id == R.id.menu_color) {
            if (!isIndividualRoute(route)) {
                tracks.traverse((key, routeForThisKey) -> {
                    if (routeForThisKey.equals(route)) {
                        menuColorTrack(key, null);
                    }
                });
            }
        } else if (id == R.id.item_visibility) {
            final boolean newValue = menuToggleRouteOrTrack(route);
            item.setTitle(newValue ? R.string.make_visible : R.string.hide);
        } else if (id == R.id.item_center) {
            menuCenterRouteOrTrack(route);
        } else if (id == R.id.menu_item_delete) {
            if (isIndividualRoute(route)) {
                menuDeleteRoute(() -> { });
            } else {
                tracks.traverse((key, routeForThisKey) -> {
                    if (routeForThisKey.equals(route)) {
                        menuDeleteTrack(key, null);
                    }
                });
            }
        } else {
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // common methods for both route/track bottom dialog & context menu

    private void menuEditRoute(final Route route) {
        if (isIndividualRoute(route)) {
            activity.startActivityForResult(new Intent(activity, RouteSortActivity.class), REQUEST_SORT_INDIVIDUAL_ROUTE);
        }
    }

    private boolean menuToggleRouteOrTrack(final Route route) {
        final boolean newValue = !route.isHidden();
        if (isIndividualRoute(route)) {
            route.setHidden(newValue);
            reloadIndividualRoute.run();
        } else {
            tracks.traverse((key, routeForThisKey) -> {
                if (routeForThisKey.equals(route)) {
                    menuToggleTrack(key, newValue);
                }
            });
        }
        return newValue;
    }

    private void menuToggleTrack(final String key, final boolean newValue) {
        final IGeoItemSupplier route = tracks.getTrack(key).getRoute();
        route.setHidden(newValue);
        updateTrack.updateRoute(key, route, tracks.getColor(key), tracks.getWidth(key));
        tracks.hide(key, newValue);
    }

    private void menuOptimizeRoute(final IndividualRoute individualRoute) {
        final RouteOptimizationHelper roh = new RouteOptimizationHelper(individualRoute.getRouteItems());
        roh.start(activity, this::storeAndReloadIndividualRoute);
    }

    private void menuInvertRoute(final IndividualRoute individualRoute) {
        final ArrayList<RouteItem> newRouteItems = new ArrayList<>(individualRoute.getRouteItems());
        Collections.reverse(newRouteItems);
        storeAndReloadIndividualRoute(newRouteItems);
    }

    private void storeAndReloadIndividualRoute(final ArrayList<RouteItem>newRouteItems) {
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.saveIndividualRoute(newRouteItems), reloadIndividualRoute);
    }

    private void menuColorTrack(final String key, @Nullable final ImageView vColor) {
        new ColorPickerUI(activity, tracks.getColor(key), tracks.getWidth(key), false, 0, 0, true, true).show((newColor, newWidth) -> {
            tracks.setColor(key, newColor);
            tracks.setWidth(key, newWidth);
            if (vColor != null) {
                ColorPickerUI.setViewColor(vColor, newColor, false);
            }
            updateTrack.updateRoute(key, tracks.getRoute(key), tracks.getColor(key), tracks.getWidth(key));
        });
    }

    private void menuDeleteRoute(final Runnable onDeletion) {
            SimpleDialog.of(activity).setTitle(R.string.map_clear_individual_route).setMessage(R.string.map_clear_individual_route_confirm).confirm(() -> {
            clearIndividualRoute.run();
            onDeletion.run();
        });
    }

    private void menuDeleteTrack(final String key, @Nullable final View dialog) {
        SimpleDialog.of(activity).setTitle(R.string.map_clear_track).setMessage(TextParam.text(String.format(activity.getString(R.string.map_clear_track_confirm), tracks.getDisplayname(key)))).confirm(() -> {
            tracks.remove(key);
            if (dialog != null) {
                updateDialogTracks(dialog, tracks);
            }
            updateTrack.updateRoute(key, null, tracks.getColor(key), tracks.getWidth(key));
        });
    }

    private void menuCenterRouteOrTrack(final Route route) {
        route.setCenter(centerOnPosition);
    }

    // ---------------------------------------------------------------------------------------------
    // route/track bottom menu-related methods

    private void updateDialog(final View dialog, final IndividualRoute individualRoute, final Tracks tracks, final Action2<Geopoint, String> setTarget) {
        updateDialogIndividualRoute(dialog, individualRoute, setTarget);
        updateDialogTracks(dialog, tracks);
        updateDialogClearTargets(dialog, individualRoute, setTarget);
    }

    private void startFileSelectorIndividualRoute() {
        csah.selectFile(null, PersistableFolder.GPX.getUri());
    }

    private void updateDialogIndividualRoute(final View dialog, final IndividualRoute individualRoute, final Action2<Geopoint, String> setTarget) {
        if (dialog == null) {
            return;
        }
        dialog.findViewById(R.id.indivroute_load).setOnClickListener(v1 -> {
            if (null == individualRoute || individualRoute.getNumSegments() == 0) {
                startFileSelectorIndividualRoute();
            } else {
                SimpleDialog.of(activity).setTitle(R.string.map_load_individual_route).setMessage(R.string.map_load_individual_route_confirm).confirm(
                        this::startFileSelectorIndividualRoute);
            }
        });

        if (isRouteNonEmpty(individualRoute)) {
            dialog.findViewById(R.id.indivroute).setVisibility(View.VISIBLE);

            final View vEdit = dialog.findViewById(R.id.item_edit);
            vEdit.setVisibility(View.VISIBLE);
            vEdit.setOnClickListener(v1 -> menuEditRoute(individualRoute));

            final View vRefresh = dialog.findViewById(R.id.item_refresh);
            vRefresh.setVisibility(View.VISIBLE);
            vRefresh.setOnClickListener(v1 -> {
                // create list of geocodes contained in individual route (including geocodes for contained waypoints)
                final Set<String> geocodes = new HashSet<>();
                final RouteSegment[] segments = individualRoute.getSegments();
                for (RouteSegment segment : segments) {
                    if (segment != null) {
                        final RouteItem item = segment.getItem();
                        if (item != null) {
                            final String geocode = item.getGeocode();
                            if (StringUtils.isNotBlank(geocode)) {
                                geocodes.add(geocode);
                            }
                        }
                    }
                }
                // refresh those caches
                CacheDownloaderService.downloadCaches(activity, geocodes, true, true, null);
            });

            dialog.findViewById(R.id.item_center).setOnClickListener(v1 -> menuCenterRouteOrTrack(individualRoute));

            final ImageButton vVisibility = dialog.findViewById(R.id.item_visibility);
            vVisibility.setVisibility(View.VISIBLE);
            vVisibility.setOnClickListener(v -> setVisibilityInfo(vVisibility, menuToggleRouteOrTrack(individualRoute)));
            setVisibilityInfo(vVisibility, individualRoute.isHidden());

            dialog.findViewById(R.id.item_delete).setOnClickListener(v1 -> menuDeleteRoute(() -> updateDialogIndividualRoute(dialog, individualRoute, setTarget)));

            dialog.findViewById(R.id.indivroute_export_route).setOnClickListener(v1 -> new IndividualRouteExport(activity, individualRoute, false));
            dialog.findViewById(R.id.indivroute_export_track).setOnClickListener(v1 -> new IndividualRouteExport(activity, individualRoute, true));

            final CheckBox vAutoTarget = dialog.findViewById(R.id.auto_target);
            vAutoTarget.setChecked(Settings.isAutotargetIndividualRoute());
            vAutoTarget.setOnClickListener(v1 -> {
                setAutotargetIndividualRoute(activity, individualRoute, !Settings.isAutotargetIndividualRoute());
                updateDialogClearTargets(popup, individualRoute, setTarget);
            });
        } else {
            dialog.findViewById(R.id.indivroute).setVisibility(View.GONE);
        }
    }

    private void updateDialogTracks(final View dialog, final Tracks tracks) {
        if (dialog == null) {
            return;
        }
        final LinearLayout tracklist = dialog.findViewById(R.id.tracklist);
        tracklist.removeAllViews();
        dialog.findViewById(R.id.trackroute_load).setOnClickListener(v1 -> csah.selectMultipleFiles(null, PersistableFolder.GPX.getUri()));

        tracks.traverse((key, geoData) -> {
            final View vt = activity.getLayoutInflater().inflate(R.layout.routes_tracks_item, null);
            final TextView displayName = vt.findViewById(R.id.item_title);
            displayName.setText(tracks.getDisplayname(key));
            displayName.setOnClickListener(v -> SimpleDialog.ofContext(dialog.getContext())
                    .setTitle(TextParam.text(activity.getString(R.string.routes_tracks_change_name)))
                    .input(new SimpleDialog.InputOptions().setInitialValue(displayName.getText().toString()), newName -> {
                        if (StringUtils.isNotBlank(newName)) {
                            tracks.setDisplayname(key, newName);
                            displayName.setText(newName);
                        }
                    }));

            final ImageButton vColor = vt.findViewById(R.id.item_color);
            ColorPickerUI.setViewColor(vColor, tracks.getColor(key), false);
            vColor.setVisibility(View.VISIBLE);
            vColor.setOnClickListener(view -> menuColorTrack(key, vColor));

            vt.findViewById(R.id.item_center).setOnClickListener(v1 -> {
                if (null != geoData) {
                    final Viewport vp = geoData.getViewport();
                    centerOnPosition.centerOnPosition(vp.getCenter().getLatitude(), vp.getCenter().getLongitude(), vp);
                }
            });

            final ImageButton vVisibility = vt.findViewById(R.id.item_visibility);
            if (geoData == null) {
                vVisibility.setVisibility(View.GONE);
            } else {
                vVisibility.setVisibility(View.VISIBLE);
                vVisibility.setOnClickListener(v -> {
                    setVisibilityInfo(vVisibility, !geoData.isHidden());
                    menuToggleTrack(key, !geoData.isHidden());
                });
                setVisibilityInfo(vVisibility, geoData.isHidden());
            }

            vt.findViewById(R.id.item_delete).setOnClickListener(v1 -> menuDeleteTrack(key, dialog));
            tracklist.addView(vt);
        });
    }

    private void setVisibilityInfo(final ImageButton v, final boolean isHidden) {
        v.setImageResource(isHidden ? R.drawable.visibility_off : R.drawable.visibility);
        TooltipCompat.setTooltipText(v, activity.getString(isHidden ? R.string.make_visible : R.string.hide));
    }

    private void updateDialogClearTargets(final View dialog, final IndividualRoute individualRoute, final Action2<Geopoint, String> setTarget) {
        final View vClearTargets = dialog.findViewById(R.id.clear_targets);
        vClearTargets.setEnabled(isTargetSet.call() || Settings.isAutotargetIndividualRoute());
        vClearTargets.setOnClickListener(v1 -> {
            if (setTarget != null) {
                setTarget.call(null, null);
            }
            Settings.setAutotargetIndividualRoute(false);
            individualRoute.triggerTargetUpdate(true);
            ActivityMixin.showToast(activity, R.string.map_manual_targets_cleared);
            updateDialogIndividualRoute(dialog, individualRoute, setTarget);
        });
    }

    private boolean isRouteNonEmpty(final IGeoItemSupplier route) {
        return route != null && (!(route instanceof Route) || ((Route) route).getNumSegments() > 0);
    }

    public void reloadTrack(final Trackfiles trackfile, final Tracks.UpdateTrack updateTrack) {
        final Uri uri = Trackfiles.getUriFromKey(trackfile.getKey());
        Log.d("[RouteTrackDebug] Start reloading track from trackfile " + trackfile.getFilename());
        GPXTrackOrRouteImporter.doImport(activity, uri, trackfile.getDisplayname(), (route) -> {
            if (route != null) {
                Log.d("[RouteTrackDebug] Reloading track from trackfile " + trackfile.getFilename() + " finished, updating map");
                route.setHidden(trackfile.isHidden());
                updateDialogTracks(popup, tracks);
                updateTrack.updateRoute(trackfile.getKey(), route, trackfile.getColor(), trackfile.getWidth());
            } else {
                Log.d("[RouteTrackDebug] Reloading track from trackfile " + trackfile.getFilename() + " returned null");
            }
        });
    }

    public void updateRouteTrackButtonVisibility(final View button, final IndividualRoute route) {
        updateRouteTrackButtonVisibility(button, route, tracks);
    }

    public void updateRouteTrackButtonVisibility(final View button, final IndividualRoute route, final Tracks tracks) {
        final AtomicBoolean someTrackAvailable = new AtomicBoolean(isRouteNonEmpty(route) || isTargetSet.call());
        if (tracks != null) {
            tracks.traverse((key, r) -> {
                if (!someTrackAvailable.get() && isRouteNonEmpty(r)) {
                    someTrackAvailable.set(true);
                }
            });
        }
        button.setVisibility(someTrackAvailable.get() ? View.VISIBLE : View.GONE);
    }

    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (csah.onActivityResult(requestCode, resultCode, data)) {
            return true;
        }
        if (requestCode == REQUEST_SORT_INDIVIDUAL_ROUTE) {
            reloadIndividualRoute.run();
            return true;
        }
        return (csah.onActivityResult(requestCode, resultCode, data));
    }

    public Bundle getState() {
        final Bundle bundle = new Bundle();
        bundle.putBundle(STATE_CSAH, this.csah.getState());
        return bundle;
    }

    public static void setAutotargetIndividualRoute(final Activity activity, final IndividualRoute route, final boolean newValue) {
        Settings.setAutotargetIndividualRoute(newValue);
        route.triggerTargetUpdate(!Settings.isAutotargetIndividualRoute());
        ActivityMixin.invalidateOptionsMenu(activity);
    }

    public static boolean isIndividualRoute(final Route route) {
        return (route != null && route.getName().isEmpty());
    }
}
