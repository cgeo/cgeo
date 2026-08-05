package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.databinding.MapSettingsDialogBinding;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Action2;

import android.annotation.SuppressLint;
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
import java.util.Map;
import java.util.Objects;
import static java.lang.Boolean.TRUE;

import com.google.android.material.button.MaterialButtonToggleGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class MapSettingsUtils {

    private static boolean isAutotargetIndividualRoute;
    private static boolean showAutotargetIndividualRoute;

    private MapSettingsUtils() {
        // utility class
    }

    public static void showRotationMenu(final Activity activity, final Action1<Integer> setRotationMode) {
        final ToggleButtonWrapper<Integer> rotationChoiceWrapper = new ToggleButtonWrapper<>(Settings.getMapRotation(), setRotationMode, activity.findViewById(R.id.rotation_mode_tooglegroup), true);
        rotationChoiceWrapper.add(new ButtonChoiceModel<>(R.id.rotation_mode_off, Settings.MAPROTATION_OFF, activity.getString(R.string.switch_off)));
        rotationChoiceWrapper.add(new ButtonChoiceModel<>(R.id.rotation_mode_manual, Settings.MAPROTATION_MANUAL, activity.getString(R.string.switch_manual)));
        rotationChoiceWrapper.add(new ButtonChoiceModel<>(R.id.rotation_mode_energy_saving, Settings.MAPROTATION_AUTO_LOWPOWER, activity.getString(R.string.switch_auto_lowpower)));
        rotationChoiceWrapper.add(new ButtonChoiceModel<>(R.id.rotation_mode_high_precision, Settings.MAPROTATION_AUTO_PRECISE, activity.getString(R.string.switch_auto_precise)));
        rotationChoiceWrapper.init();
        for (ButtonChoiceModel<Integer> bcm : rotationChoiceWrapper.list) {
            bcm.button.setOnClickListener(v -> rotationChoiceWrapper.setValue());
        }
    }

    // splitting up that method would not help improve readability
    @SuppressLint("SetTextI18n")
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    public static void showSettingsPopup(final Activity activity, @Nullable final IndividualRoute route, @NonNull final Action2<Boolean, Boolean> onMapSettingsPopupFinished, @NonNull final Action1<RoutingMode> setRoutingValue, @NonNull final Action1<Integer> setCompactIconValue, final Runnable configureProximityNotifications, final GeocacheFilterContext filterContext) {
        isAutotargetIndividualRoute = Settings.isAutotargetIndividualRoute();
        showAutotargetIndividualRoute = isAutotargetIndividualRoute || (route != null && route.getNumSegments() > 0);
        final boolean showPNMastertoggle = Settings.showProximityNotificationMasterToggle();

        final GeocacheFilter filter = filterContext.get();
        final Map<GeocacheFilter.QuickFilter, Boolean> quickFilter = filter.getQuickFilter();

        final ArrayList<SettingsCheckboxModel> allCbs = new ArrayList<>();

        final SettingsCheckboxModel foundCb = createCb(allCbs, R.string.map_showc_found, ImageParam.id(R.drawable.marker_found), quickFilter.get(GeocacheFilter.QuickFilter.FOUND), f -> quickFilter.put(GeocacheFilter.QuickFilter.FOUND, f), false);
        final SettingsCheckboxModel offlineLogCb = createCb(allCbs, R.string.map_showc_found_offline, R.drawable.marker_found_offline, quickFilter.get(GeocacheFilter.QuickFilter.HAS_OFFLINE_FOUND_LOG), f -> quickFilter.put(GeocacheFilter.QuickFilter.HAS_OFFLINE_FOUND_LOG, f), false);
        final SettingsCheckboxModel ownCb = createCb(allCbs, R.string.map_showc_own, ImageParam.id(R.drawable.marker_own), quickFilter.get(GeocacheFilter.QuickFilter.OWNED), f -> quickFilter.put(GeocacheFilter.QuickFilter.OWNED, f), false);
        final SettingsCheckboxModel disabledCb = createCb(allCbs, R.string.map_showc_disabled, R.drawable.map_status_disabled, quickFilter.get(GeocacheFilter.QuickFilter.DISABLED), f -> quickFilter.put(GeocacheFilter.QuickFilter.DISABLED, f), false);
        final SettingsCheckboxModel archivedCb = createCb(allCbs, R.string.map_showc_archived, R.drawable.map_status_archived, quickFilter.get(GeocacheFilter.QuickFilter.ARCHIVED), f -> quickFilter.put(GeocacheFilter.QuickFilter.ARCHIVED, f), false);
        final SettingsCheckboxModel wpOriginalCb = createCb(allCbs, R.string.map_showwp_original, ImageParam.drawable(MapMarkerUtils.getWaypointTypeMarker(activity.getResources(), WaypointType.ORIGINAL)), Settings.isExcludeWpOriginal(), Settings::setExcludeWpOriginal, true);
        final SettingsCheckboxModel wpParkingCb = createCb(allCbs, R.string.map_showwp_parking, ImageParam.drawable(MapMarkerUtils.getWaypointTypeMarker(activity.getResources(), WaypointType.PARKING)), Settings.isExcludeWpParking(), Settings::setExcludeWpParking, true);
        final SettingsCheckboxModel wpVisitedCb = createCb(allCbs, R.string.map_showwp_visited, R.drawable.marker_visited, Settings.isExcludeWpVisited(), Settings::setExcludeWpVisited, true);
        final SettingsCheckboxModel circlesCb = createCb(allCbs, R.string.map_show_circles, R.drawable.map_circle, Settings.isShowCircles(), Settings::setShowCircles, false);

        final MapSettingsDialogBinding dialogView = MapSettingsDialogBinding.inflate(LayoutInflater.from(Dialogs.newContextThemeWrapper(activity)));

        final List<LinearLayout> columns = ViewUtils.createAndAddStandardColumnView(activity, dialogView.mapSettingsColumns, null, 2, true);
        final LinearLayout leftColumn = columns.get(0);
        final LinearLayout rightColumn = columns.get(1);

        final boolean disableCacheFilters = !filter.canSetQuickFilterLossless();

        leftColumn.addView(ViewUtils.createTextItem(activity, R.style.map_quicksettings_subtitle, TextParam.id(R.string.map_show_caches_title)));
        foundCb.addToViewGroup(activity, leftColumn, disableCacheFilters);
        offlineLogCb.addToViewGroup(activity, leftColumn, disableCacheFilters);
        ownCb.addToViewGroup(activity, leftColumn, disableCacheFilters);
        disabledCb.addToViewGroup(activity, leftColumn, disableCacheFilters);
        archivedCb.addToViewGroup(activity, leftColumn, disableCacheFilters);

        rightColumn.addView(ViewUtils.createTextItem(activity, R.style.map_quicksettings_subtitle, TextParam.id(R.string.map_show_waypoints_title)));
        wpOriginalCb.addToViewGroup(activity, rightColumn);
        wpParkingCb.addToViewGroup(activity, rightColumn);
        wpVisitedCb.addToViewGroup(activity, rightColumn);
        rightColumn.addView(ViewUtils.createTextItem(activity, R.style.map_quicksettings_subtitle, TextParam.id(R.string.map_show_other_title)));
        circlesCb.addToViewGroup(activity, rightColumn);

        final ToggleButtonWrapper<Integer> compactIconWrapper = new ToggleButtonWrapper<>(Settings.getCompactIconMode(), setCompactIconValue, dialogView.compacticonTooglegroup, false);
        compactIconWrapper.add(new ButtonChoiceModel<>(R.id.compacticon_off, Settings.COMPACTICON_OFF, activity.getString(R.string.switch_off)));
        compactIconWrapper.add(new ButtonChoiceModel<>(R.id.compacticon_auto, Settings.COMPACTICON_AUTO, activity.getString(R.string.switch_auto)));
        compactIconWrapper.add(new ButtonChoiceModel<>(R.id.compacticon_on, Settings.COMPACTICON_ON, activity.getString(R.string.switch_on)));

        final ToggleButtonWrapper<RoutingMode> routingChoiceWrapper = new ToggleButtonWrapper<>(Routing.isAvailable() || Settings.getRoutingMode() == RoutingMode.OFF ? Settings.getRoutingMode() : RoutingMode.STRAIGHT, setRoutingValue, dialogView.routingTooglegroup, false);
        for (RoutingMode mode : RoutingMode.values()) {
            routingChoiceWrapper.add(new ButtonChoiceModel<>(mode.buttonResId, mode, activity.getString(mode.infoResId)));
        }

        // routing profile legend for user-defined entries
        final boolean useInternalRouting = Settings.useInternalRouting();
        boolean useUser1 = false;
        boolean useUser2 = false;
        if (useInternalRouting) {
            final String profileNoneString = activity.getResources().getString(R.string.routingmode_none);
            final StringBuilder sb = new StringBuilder();
            final String temp1 = StringUtils.removeEndIgnoreCase(Settings.getRoutingProfile(RoutingMode.USER1), BRouterConstants.BROUTER_PROFILE_FILEEXTENSION);
            if (StringUtils.isNotBlank(temp1) && !temp1.equals(profileNoneString)) {
                sb.append("1: ").append(temp1);
                useUser1 = true;
            }
            final String temp2 = StringUtils.removeEndIgnoreCase(Settings.getRoutingProfile(RoutingMode.USER2), BRouterConstants.BROUTER_PROFILE_FILEEXTENSION);
            if (StringUtils.isNotBlank(temp2) && !temp2.equals(profileNoneString)) {
                sb.append(StringUtils.isNotBlank(sb) ? " - " : "").append("2: ").append(temp2);
                useUser2 = true;
            }
            if (StringUtils.isNotBlank(sb)) {
                dialogView.routingProfileinfo.setVisibility(View.VISIBLE);
                dialogView.routingProfileinfo.setText("(" + sb + ")");
            }
        }

        if (showAutotargetIndividualRoute || showPNMastertoggle) {
            dialogView.mapSettingsOtherContainer.setVisibility(View.VISIBLE);
            dialogView.mapSettingsAutotarget.setVisibility(showAutotargetIndividualRoute ? View.VISIBLE : View.GONE);
            dialogView.mapSettingsAutotarget.setChecked(isAutotargetIndividualRoute);
            dialogView.mapSettingsProximitynotificationMastertoggle.setVisibility(showPNMastertoggle ? View.VISIBLE : View.GONE);
            dialogView.mapSettingsProximitynotificationMastertoggle.setChecked(Settings.isProximityNotificationMasterToggleOn());
        }

        final Dialog dialog = Dialogs.bottomSheetDialogWithActionbar(activity, dialogView.getRoot(), R.string.quick_settings);
        dialog.setOnDismissListener(d -> {
            boolean filterChanged = false;
            final boolean circleChanged = circlesCb.valueChanged;
            for (SettingsCheckboxModel item : allCbs) {
                if (item.valueChanged() && item != circlesCb) {
                    filterChanged = true;
                }
                item.setValue();
            }
            compactIconWrapper.setValue();
            routingChoiceWrapper.setValue();

            if (filter.canSetQuickFilterLossless() && !filter.hasSameQuickFilter(quickFilter)) {
                filter.setQuickFilterLossless(quickFilter);
                filterContext.set(filter);
            }

            onMapSettingsPopupFinished.call(circleChanged, filterChanged);

            if (showAutotargetIndividualRoute && isAutotargetIndividualRoute != dialogView.mapSettingsAutotarget.isChecked()) {
                if (route == null) {
                    Settings.setAutotargetIndividualRoute(dialogView.mapSettingsAutotarget.isChecked());
                } else {
                    RouteTrackUtils.setAutotargetIndividualRoute(activity, route, dialogView.mapSettingsAutotarget.isChecked());
                }
            }

            if (showPNMastertoggle && Settings.isProximityNotificationMasterToggleOn() != dialogView.mapSettingsProximitynotificationMastertoggle.isChecked()) {
                Settings.enableProximityNotifications(dialogView.mapSettingsProximitynotificationMastertoggle.isChecked());
                if (configureProximityNotifications != null) {
                    configureProximityNotifications.run();
                }
            }
        });
        dialog.show();

        compactIconWrapper.init();
        routingChoiceWrapper.init();
        routingChoiceWrapper.getByResId(R.id.routing_user1).button.setVisibility(useUser1 ? View.VISIBLE : View.GONE);
        routingChoiceWrapper.getByResId(R.id.routing_user2).button.setVisibility(useUser2 ? View.VISIBLE : View.GONE);

        if (!Routing.isAvailable()) {
            configureRoutingButtons(false, routingChoiceWrapper);
            dialogView.routingInfo.setVisibility(View.VISIBLE);
            dialogView.routingInfo.setOnClickListener(v -> SimpleDialog.of(activity).setTitle(R.string.map_routing_activate_title).setMessage(R.string.map_routing_activate).confirm(() -> {
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

    private static SettingsCheckboxModel createCb(final Collection<SettingsCheckboxModel> coll, @StringRes final int resTitle, @DrawableRes final int resIcon, final Boolean currentValue, final Action1<Boolean> setValue, final boolean isNegated) {
        final SettingsCheckboxModel result = new SettingsCheckboxModel(resTitle, resIcon, currentValue, setValue, isNegated);
        coll.add(result);
        return result;
    }

    private static SettingsCheckboxModel createCb(final Collection<SettingsCheckboxModel> coll, @StringRes final int resTitle, final ImageParam imageParam, final Boolean currentValue, final Action1<Boolean> setValue, final boolean isNegated) {
        final SettingsCheckboxModel result = new SettingsCheckboxModel(resTitle, imageParam, currentValue, setValue, isNegated);
        coll.add(result);
        return result;
    }

    private static class SettingsCheckboxModel {
        @StringRes private final int resTitle;
        private final ImageParam imageParam;
        private boolean currentValue;
        private final Action1<Boolean> setValue;
        private final boolean isNegated;

        private boolean valueChanged;

        SettingsCheckboxModel(@StringRes final int resTitle, @DrawableRes final int resIcon, final Boolean currentValue, final Action1<Boolean> setValue, final boolean isNegated) {
            this.resTitle = resTitle;
            this.imageParam = ImageParam.id(resIcon);
            this.currentValue = isNegated != (TRUE.equals(currentValue));
            this.setValue = setValue;
            this.isNegated = isNegated;
            this.valueChanged = false;
        }

        SettingsCheckboxModel(@StringRes final int resTitle, final ImageParam imageParam, final Boolean currentValue, final Action1<Boolean> setValue, final boolean isNegated) {
            this.resTitle = resTitle;
            this.imageParam = imageParam;
            this.currentValue = isNegated != (TRUE.equals(currentValue));
            this.setValue = setValue;
            this.isNegated = isNegated;
            this.valueChanged = false;
        }

        public void setValue() {
            if (setValue != null) {
                this.setValue.call(isNegated != currentValue);
            }
        }

        public void addToViewGroup(final Activity ctx, final ViewGroup viewGroup) {
            addToViewGroup(ctx, viewGroup, false);
        }

        public void addToViewGroup(final Activity ctx, final ViewGroup viewGroup, final boolean disableView) {

            final ImmutablePair<View, CheckBox> ip = ViewUtils.createCheckboxItem(ctx, viewGroup, TextParam.id(resTitle), imageParam, null);
            viewGroup.addView(ip.left);
            if (disableView) {
                ip.right.setEnabled(false);
                ip.left.setOnClickListener(v -> SimpleDialog.of(ctx).setTitle(R.string.map_settings_cache_filter_too_complex_title)
                        .setMessage(R.string.map_settings_cache_filter_too_complex_message).show());
            }

            ip.right.setChecked(currentValue);
            ip.right.setOnCheckedChangeListener((v, c) -> changeValue());
        }

        public boolean valueChanged() {
            return valueChanged;
        }

        private void changeValue() {
            this.currentValue = !this.currentValue;
            this.valueChanged = !this.valueChanged;
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
        private final boolean setValueOnEachClick;

        ToggleButtonWrapper(final T originalValue, final Action1<T> setValue, final MaterialButtonToggleGroup toggleGroup, final boolean setValueOnEachClick) {
            this.originalValue = originalValue;
            this.toggleGroup = toggleGroup;
            this.setValue = setValue;
            this.list = new ArrayList<>();
            this.setValueOnEachClick = setValueOnEachClick;
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
            if (setValue != null && (setValueOnEachClick || !originalValue.equals(currentValue))) {
                this.setValue.call(currentValue);
            }
        }
    }
}
