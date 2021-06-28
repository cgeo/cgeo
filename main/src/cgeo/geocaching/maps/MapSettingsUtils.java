package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.MapSettingsDialogBinding;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PersistableUri;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.IndividualRouteUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.android.material.button.MaterialButtonToggleGroup;

public class MapSettingsUtils {

    private static int colorAccent;
    private static boolean isShowCircles;
    private static boolean isAutotargetIndividualRoute;
    private static boolean showAutotargetIndividualRoute;

    private MapSettingsUtils() {
        // utility class
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"}) // splitting up that method would not help improve readability
    public static void showSettingsPopup(final Activity activity, @Nullable final IndividualRoute route, @NonNull final Action1<Boolean> onMapSettingsPopupFinished, @NonNull final Action1<RoutingMode> setRoutingValue, @NonNull final Action1<Integer> setCompactIconValue) {
        colorAccent = activity.getResources().getColor(R.color.colorAccent);
        isShowCircles = Settings.isShowCircles();
        isAutotargetIndividualRoute = Settings.isAutotargetIndividualRoute();
        showAutotargetIndividualRoute = isAutotargetIndividualRoute || (route != null && route.getNumSegments() > 0);

        final ArrayList<SettingsCheckboxModel> allCbs = new ArrayList<>();
        final SettingsCheckboxModel foundCb = createCb(allCbs, R.string.map_showc_found, R.drawable.ic_menu_found, Settings.isExcludeFound(), Settings::setExcludeFound, true);
        final SettingsCheckboxModel ownCb = createCb(allCbs, R.string.map_showc_own, R.drawable.ic_menu_myplaces, Settings.isExcludeMyCaches(), Settings::setExcludeMine, true);
        final SettingsCheckboxModel disabledCb = createCb(allCbs, R.string.map_showc_disabled, R.drawable.ic_menu_disabled, Settings.isExcludeDisabledCaches(), Settings::setExcludeDisabled, true);
        final SettingsCheckboxModel archivedCb = createCb(allCbs, R.string.map_showc_archived, R.drawable.ic_menu_archived, Settings.isExcludeArchivedCaches(), Settings::setExcludeArchived, true);
        final SettingsCheckboxModel offlineLogCb = createCb(allCbs, R.string.map_showc_offlinelog, R.drawable.ic_menu_edit, Settings.isExcludeOfflineLog(), Settings::setExcludeOfflineLog, true);
        final SettingsCheckboxModel wpOriginalCb = createCb(allCbs, R.string.map_showwp_original, R.drawable.ic_menu_waypoint, Settings.isExcludeWpOriginal(), Settings::setExcludeWpOriginal, true);
        final SettingsCheckboxModel wpParkingCb = createCb(allCbs, R.string.map_showwp_parking, R.drawable.ic_menu_parking, Settings.isExcludeWpParking(), Settings::setExcludeWpParking, true);
        final SettingsCheckboxModel wbVisitedCb = createCb(allCbs, R.string.map_showwp_visited, R.drawable.ic_menu_visited, Settings.isExcludeWpVisited(), Settings::setExcludeWpVisited, true);
        final SettingsCheckboxModel trackCb = !PersistableUri.TRACK.hasValue() ? null : createCb(allCbs, R.string.map_show_track, R.drawable.ic_menu_hidetrack, Settings.isHideTrack(), Settings::setHideTrack, true);
        final SettingsCheckboxModel circlesCb = createCb(allCbs, R.string.map_show_circles, R.drawable.ic_menu_circle, isShowCircles, Settings::setShowCircles, false);

        final MapSettingsDialogBinding dialogView = MapSettingsDialogBinding.inflate(LayoutInflater.from(Dialogs.newContextThemeWrapper(activity)));

        final List<LinearLayout> columns = ViewUtils.createAndAddStandardColumnView(activity, dialogView.mapSettingsColumns, null, 2, true);
        final LinearLayout leftColumn = columns.get(0);
        final LinearLayout rightColumn = columns.get(1);

        leftColumn.addView(ViewUtils.createSubtitleTextItem(activity, R.style.map_quicksettings_subtitle, TextParam.id(R.string.map_show_caches_title)));
        foundCb.addToViewGroup(activity, leftColumn);
        ownCb.addToViewGroup(activity, leftColumn);
        disabledCb.addToViewGroup(activity, leftColumn);
        archivedCb.addToViewGroup(activity, leftColumn);
        offlineLogCb.addToViewGroup(activity, leftColumn);

        rightColumn.addView(ViewUtils.createSubtitleTextItem(activity, R.style.map_quicksettings_subtitle, TextParam.id(R.string.map_show_waypoints_title)));
        wpOriginalCb.addToViewGroup(activity, rightColumn);
        wpParkingCb.addToViewGroup(activity, rightColumn);
        wbVisitedCb.addToViewGroup(activity, rightColumn);
        rightColumn.addView(ViewUtils.createSubtitleTextItem(activity, R.style.map_quicksettings_subtitle, TextParam.id(R.string.map_show_other_title)));
        if (trackCb != null) {
            trackCb.addToViewGroup(activity, rightColumn);
        }
        circlesCb.addToViewGroup(activity, rightColumn);

        final ToggleButtonWrapper<Integer> compactIconWrapper = new ToggleButtonWrapper<>(Settings.getCompactIconMode(), setCompactIconValue, dialogView.compacticonTooglegroup);
        compactIconWrapper.add(new ButtonChoiceModel<>(R.id.compacticon_off, Settings.COMPACTICON_OFF, activity.getString(R.string.switch_off)));
        compactIconWrapper.add(new ButtonChoiceModel<>(R.id.compacticon_auto, Settings.COMPACTICON_AUTO, activity.getString(R.string.switch_auto)));
        compactIconWrapper.add(new ButtonChoiceModel<>(R.id.compacticon_on, Settings.COMPACTICON_ON, activity.getString(R.string.switch_on)));

        final ToggleButtonWrapper<RoutingMode> routingChoiceWrapper = new ToggleButtonWrapper<>(Routing.isAvailable() || Settings.getRoutingMode() == RoutingMode.OFF ? Settings.getRoutingMode() : RoutingMode.STRAIGHT, setRoutingValue, dialogView.routingTooglegroup);
        final ArrayList<ButtonChoiceModel<RoutingMode>> routingChoices = new ArrayList<>();
        for (RoutingMode mode : RoutingMode.values()) {
            routingChoiceWrapper.add(new ButtonChoiceModel<>(mode.buttonResId, mode, activity.getString(mode.infoResId)));
        }

        if (showAutotargetIndividualRoute) {
            dialogView.mapSettingsAutotargetContainer.setVisibility(View.VISIBLE);
            dialogView.mapSettingsAutotarget.setChecked(isAutotargetIndividualRoute);
        }

        final Dialog dialog = Dialogs.bottomSheetDialogWithActionbar(activity, dialogView.getRoot(), R.string.quick_settings);
        dialog.setOnDismissListener(d -> {
                for (SettingsCheckboxModel item : allCbs) {
                    item.setValue();
                }
                compactIconWrapper.setValue();
                routingChoiceWrapper.setValue();
                onMapSettingsPopupFinished.call(isShowCircles != Settings.isShowCircles());
                if (showAutotargetIndividualRoute && isAutotargetIndividualRoute != dialogView.mapSettingsAutotarget.isChecked()) {
                    if (route == null) {
                        Settings.setAutotargetIndividualRoute(dialogView.mapSettingsAutotarget.isChecked());
                    } else {
                        IndividualRouteUtils.setAutotargetIndividualRoute(activity, route, dialogView.mapSettingsAutotarget.isChecked());
                    }
                }
            });
        dialog.show();

        compactIconWrapper.init();
        routingChoiceWrapper.init();

        if (!Routing.isAvailable()) {
            configureRoutingButtons(false, routingChoiceWrapper);
            dialogView.routingInfo.setVisibility(View.VISIBLE);
            dialogView.routingInfo.setOnClickListener(v -> SimpleDialog.of(activity).setTitle(R.string.map_routing_activate_title).setMessage(R.string.map_routing_activate).confirm((dialog1, which) -> {
                Settings.setUseInternalRouting(true);
                Settings.setBrouterAutoTileDownloads(true);
                configureRoutingButtons(true, routingChoiceWrapper);
                dialogView.routingInfo.setVisibility(View.GONE);
            }));
        }
    }

    private static void configureRoutingButtons(final boolean enabled, final ToggleButtonWrapper<RoutingMode> routingChoiceWrapper) {
        for (final ButtonChoiceModel<RoutingMode> button : routingChoiceWrapper.list) {
            if (!(button.assignedValue == RoutingMode.OFF || button.assignedValue == RoutingMode.STRAIGHT)) {
                button.button.setEnabled(enabled);
                button.button.setAlpha(enabled ? 1f : .3f);
            }
        }
    }

    private static SettingsCheckboxModel createCb(final Collection<SettingsCheckboxModel> coll, @StringRes final int resTitle, @DrawableRes final int resIcon, final boolean currentValue, final Action1<Boolean> setValue, final boolean isNegated) {
        final SettingsCheckboxModel result = new SettingsCheckboxModel(resTitle, resIcon, currentValue, setValue, isNegated);
        coll.add(result);
        return result;
    }

    private static class SettingsCheckboxModel {
        @StringRes private final int resTitle;
        @DrawableRes private final int resIcon;
        private boolean currentValue;
        private final Action1<Boolean> setValue;
        private final boolean isNegated;

        SettingsCheckboxModel(@StringRes final int resTitle, @DrawableRes final int resIcon, final boolean currentValue, final Action1<Boolean> setValue, final boolean isNegated) {
            this.resTitle = resTitle;
            this.resIcon = resIcon;
            this.currentValue = isNegated != currentValue;
            this.setValue = setValue;
            this.isNegated = isNegated;
        }

        public void setValue() {
            this.setValue.call(isNegated != currentValue);
        }

        public void addToViewGroup(final Activity ctx, final ViewGroup viewGroup) {
            final CheckBox cb = ViewUtils.addCheckboxItem(ctx, viewGroup, TextParam.id(resTitle), resIcon);
            cb.setChecked(currentValue);
            cb.setOnCheckedChangeListener((v, c) -> this.currentValue = !this.currentValue);
        }
    }

    private static class ButtonChoiceModel<T> {
        public final int resButton;
        public final T assignedValue;
        public final String info;
        public View button = null;

        ButtonChoiceModel(final int resButton, final T assignedValue, final String info) {
            this.resButton = resButton;
            this.assignedValue = assignedValue;
            this.info = info;
        }
    }

    private static class ToggleButtonWrapper<T> {
        private final MaterialButtonToggleGroup toggleGroup;
        private final ArrayList<ButtonChoiceModel<T>> list;
        private final Action1<T> setValue;
        private final T originalValue;

        ToggleButtonWrapper(final T originalValue, final Action1<T> setValue, final MaterialButtonToggleGroup toggleGroup) {
            this.originalValue = originalValue;
            this.toggleGroup = toggleGroup;
            this.setValue = setValue;
            this.list = new ArrayList<>();
        }

        public void add(final ButtonChoiceModel<T> item) {
            list.add(item);
        }

        public ButtonChoiceModel<T> getByResId(final int id) {
            for (ButtonChoiceModel<T> item : list) {
                if (item.resButton == id) {
                    return item;
                }
            }
            return null;
        }

        public ButtonChoiceModel<T> getByAssignedValue(final T value) {
            for (ButtonChoiceModel<T> item : list) {
                if (Objects.equals(item.assignedValue, value)) {
                    return item;
                }
            }
            return null;
        }

        public void init() {
            for (final ButtonChoiceModel<T> button : list) {
                button.button = toggleGroup.findViewById(button.resButton);
            }
            toggleGroup.check(getByAssignedValue(originalValue).resButton);
        }

        public void setValue() {
            final T currentValue = getByResId(toggleGroup.getCheckedButtonId()).assignedValue;
            if (setValue != null && !originalValue.equals(currentValue)) {
                this.setValue.call(currentValue);
            }
        }
    }
}
