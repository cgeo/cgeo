package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.export.IndividualRouteExport;
import cgeo.geocaching.files.GPXIndividualRouteImporter;
import cgeo.geocaching.files.GPXTrackOrRouteImporter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.RouteSortActivity;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.ButtonChoiceModel;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.Trackfiles;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Action2;
import cgeo.geocaching.utils.functions.Func0;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class RouteTrackUtils {

    private static final int REQUEST_SORT_INDIVIDUAL_ROUTE = 4712;
    private static final String STATE_CSAH_ROUTE = "csah_route";
    private static final String STATE_CSAH_TRACK = "csah_track";

    private final Activity activity;
    private final ContentStorageActivityHelper fileSelectorRoute;
    private final ContentStorageActivityHelper fileSelectorTrack;
    private View popup = null;
    private Tracks tracks = null;
    private ButtonChoiceModel.ToggleButtonWrapper<RoutingMode> routingChoiceWrapper = null;

    private final Runnable reloadIndividualRoute;
    private final Runnable clearIndividualRoute;

    private final Tracks.UpdateTrack updateTrack;
    private final Route.CenterOnPosition centerOnPosition;
    private final Action1<RoutingMode> setRoutingValue;

    private final Func0<Boolean> isTargetSet;

    public RouteTrackUtils(final Activity activity, final Bundle savedState, final Route.CenterOnPosition centerOnPosition, final Runnable clearIndividualRoute, final Runnable reloadIndividualRoute, final Tracks.UpdateTrack updateTrack, final Func0<Boolean> isTargetSet, @NonNull final Action1<RoutingMode> setRoutingValue) {
        this.activity = activity;
        this.centerOnPosition = centerOnPosition;
        this.clearIndividualRoute = clearIndividualRoute;
        this.reloadIndividualRoute = reloadIndividualRoute;
        this.updateTrack = updateTrack;
        this.isTargetSet = isTargetSet;
        this.setRoutingValue = setRoutingValue;

        this.fileSelectorRoute = new ContentStorageActivityHelper(activity, savedState == null ? null : savedState.getBundle(STATE_CSAH_ROUTE))
            .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE, Uri.class, this::importIndividualRoute);
        this.fileSelectorTrack = new ContentStorageActivityHelper(activity, savedState == null ? null : savedState.getBundle(STATE_CSAH_TRACK))
            .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE_MULTIPLE, List.class, this::importTracks);
    }

    private void importIndividualRoute(final Uri uri) {
        if (uri != null) {
            GPXIndividualRouteImporter.doImport(activity, uri);
            reloadIndividualRoute.run();
        }
    }

    private void importTracks(final List<Uri> uris) {
        if (uris != null && this.updateTrack != null) {
            for (Uri uri : uris) {
                GPXTrackOrRouteImporter.doImport(activity, uri, (route) -> {
                    final String key = tracks.add(activity, uri, updateTrack);
                    tracks.setRoute(key, route);
                    updateTrack.updateRoute(key, route);
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
     *
     */
    public void showPopup(final IndividualRoute individualRoute, final Action2<Geopoint, String> setTarget) {
        this.popup = activity.getLayoutInflater().inflate(R.layout.routes_tracks_dialog, null);
        updateDialog(this.popup, individualRoute, tracks, setTarget);

        routingChoiceWrapper = new ButtonChoiceModel.ToggleButtonWrapper<>(Routing.isAvailable() || Settings.getRoutingMode() == RoutingMode.OFF ? Settings.getRoutingMode() : RoutingMode.STRAIGHT, setRoutingValue, popup.findViewById(R.id.routing_tooglegroup));
        for (RoutingMode mode : RoutingMode.values()) {
            routingChoiceWrapper.add(new ButtonChoiceModel<>(mode.buttonResId, mode, activity.getString(mode.infoResId)));
        }

        final BottomSheetDialog dialog = Dialogs.bottomSheetDialogWithActionbar(activity, this.popup, R.string.routes_tracks_dialog_title);
        dialog.setOnDismissListener(dialog1 -> {
            routingChoiceWrapper.setValue();
            popup = null;
        });
        dialog.show();
        routingChoiceWrapper.init();
    }

    private void updateDialog(final View dialog, final IndividualRoute individualRoute, final Tracks tracks, final Action2<Geopoint, String> setTarget) {
        updateDialogRoutingMode(dialog);
        updateDialogIndividualRoute(dialog, individualRoute, setTarget);
        updateDialogTracks(dialog, tracks);
        updateDialogClearTargets(dialog, individualRoute, setTarget);
    }

    private void startFileSelectorIndividualRoute() {
        fileSelectorRoute.selectFile(null, PersistableFolder.GPX.getUri());
    }

    private void updateDialogRoutingMode(final View dialog) {
        if (!Routing.isAvailable()) {
            configureRoutingButtons(false, routingChoiceWrapper);
            final View routingInfo = dialog.findViewById(R.id.routing_info);
            routingInfo.setVisibility(View.VISIBLE);
            routingInfo.setOnClickListener(v -> SimpleDialog.of(activity).setTitle(R.string.map_routing_activate_title).setMessage(R.string.map_routing_activate).confirm((dialog1, which) -> {
                Settings.setUseInternalRouting(true);
                Settings.setBrouterAutoTileDownloads(true);
                configureRoutingButtons(true, routingChoiceWrapper);
                routingInfo.setVisibility(View.GONE);
            }));
        }
    }

    private static void configureRoutingButtons(final boolean enabled, final ButtonChoiceModel.ToggleButtonWrapper<RoutingMode> routingChoiceWrapper) {
        for (final ButtonChoiceModel<RoutingMode> button : routingChoiceWrapper.getList()) {
            if (!(button.assignedValue == RoutingMode.OFF || button.assignedValue == RoutingMode.STRAIGHT)) {
                button.button.setEnabled(enabled);
                button.button.setAlpha(enabled ? 1f : .3f);
            }
        }
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
                    (d, w) -> startFileSelectorIndividualRoute());
            }
        });

        if (isRouteNonEmpty(individualRoute)) {
            dialog.findViewById(R.id.indivroute).setVisibility(View.VISIBLE);

            final View vSort = dialog.findViewById(R.id.item_sort);
            vSort.setVisibility(View.VISIBLE);
            vSort.setOnClickListener(v1 -> activity.startActivityForResult(new Intent(activity, RouteSortActivity.class), REQUEST_SORT_INDIVIDUAL_ROUTE));

            dialog.findViewById(R.id.item_center).setOnClickListener(v1 -> individualRoute.setCenter(centerOnPosition));

            final ImageButton vVisibility = dialog.findViewById(R.id.item_visibility);
            vVisibility.setVisibility(View.VISIBLE);
            setVisibilityInfo(vVisibility, individualRoute.isHidden());
            vVisibility.setOnClickListener(v -> {
                final boolean newValue = !individualRoute.isHidden();
                setVisibilityInfo(vVisibility, newValue);
                individualRoute.setHidden(newValue);
                reloadIndividualRoute.run();
            });

            dialog.findViewById(R.id.item_delete).setOnClickListener(v1 -> SimpleDialog.of(activity).setTitle(R.string.map_clear_individual_route).setMessage(R.string.map_clear_individual_route_confirm).confirm((d, w) -> {
                clearIndividualRoute.run();
                updateDialogIndividualRoute(dialog, individualRoute, setTarget);
            }));

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
        dialog.findViewById(R.id.trackroute_load).setOnClickListener(v1 -> fileSelectorTrack.selectMultipleFiles(null, PersistableFolder.GPX.getUri()));

        tracks.traverse((key, route) -> {
            final View vt = activity.getLayoutInflater().inflate(R.layout.routes_tracks_item, null);
            ((TextView) vt.findViewById(R.id.item_title)).setText(tracks.getDisplayname(key));
            vt.findViewById(R.id.item_center).setOnClickListener(v1 -> {
                if (null != route) {
                    route.setCenter(centerOnPosition);
                }
            });

            final ImageButton vVisibility = vt.findViewById(R.id.item_visibility);
            if (route == null) {
                vVisibility.setVisibility(View.GONE);
            } else {
                vVisibility.setVisibility(View.VISIBLE);
                setVisibilityInfo(vVisibility, route.isHidden());
                vVisibility.setOnClickListener(v -> {
                    final boolean newValue = !route.isHidden();
                    setVisibilityInfo(vVisibility, newValue);
                    route.setHidden(newValue);
                    updateTrack.updateRoute(key, route);
                    tracks.hide(key, newValue);
                });
            }

            vt.findViewById(R.id.item_delete).setOnClickListener(v1 -> SimpleDialog.of(activity).setTitle(R.string.map_clear_track).setMessage(TextParam.text(String.format(activity.getString(R.string.map_clear_track_confirm), tracks.getDisplayname(key)))).confirm((d, w) -> {
                tracks.remove(key);
                updateDialogTracks(dialog, tracks);
                updateTrack.updateRoute(key, null);
            }));
            tracklist.addView(vt);
        });
    }

    private void setVisibilityInfo (final ImageButton v, final boolean isHidden) {
        v.setImageResource(isHidden ? R.drawable.visibility_off : R.drawable.visibility);
        TooltipCompat.setTooltipText(v, activity.getString(isHidden ? R.string.make_visible : R.string.hide));
    };

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

    private boolean isRouteNonEmpty(final Route route) {
        return route != null && route.getNumSegments() > 0;
    }

    public void reloadTrack(final String key, final Tracks.UpdateTrack updateTrack) {
        final Uri uri = Trackfiles.getUriFromKey(key);
        GPXTrackOrRouteImporter.doImport(activity, uri, (route) -> {
            updateDialogTracks(popup, tracks);
            updateTrack.updateRoute(key, route);
        });
    }

    public void showTrackInfo(final Route route) {
        if (null != route) {
            final int numPoints = route.getNumPoints();
            Toast.makeText(activity, activity.getResources().getQuantityString(R.plurals.load_track_success, numPoints, numPoints), Toast.LENGTH_SHORT).show();
        }
    }

    public void onPrepareOptionsMenu(final Menu menu, final View anchor, final IndividualRoute route, final Tracks tracks) {
        final AtomicBoolean someTrackAvailable = new AtomicBoolean(isRouteNonEmpty(route) || isTargetSet.call());
        tracks.traverse((key, r) -> {
            if (!someTrackAvailable.get() && isRouteNonEmpty(r)) {
                someTrackAvailable.set(true);
            }
        });
        anchor.setVisibility(someTrackAvailable.get() ? View.VISIBLE : View.GONE);
    }

    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (fileSelectorRoute.onActivityResult(requestCode, resultCode, data)) {
            return true;
        }
        if (requestCode == REQUEST_SORT_INDIVIDUAL_ROUTE) {
            reloadIndividualRoute.run();
            return true;
        }
        return (fileSelectorTrack.onActivityResult(requestCode, resultCode, data));
    }

    public Bundle getState() {
        final Bundle bundle = new Bundle();
        bundle.putBundle(STATE_CSAH_ROUTE, this.fileSelectorRoute.getState());
        bundle.putBundle(STATE_CSAH_TRACK, this.fileSelectorTrack.getState());
        return bundle;
    }

    public static void setAutotargetIndividualRoute(final Activity activity, final IndividualRoute route, final boolean newValue) {
        Settings.setAutotargetIndividualRoute(newValue);
        route.triggerTargetUpdate(!Settings.isAutotargetIndividualRoute());
        ActivityMixin.invalidateOptionsMenu(activity);
    }
}
