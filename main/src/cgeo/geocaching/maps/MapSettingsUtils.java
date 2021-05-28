package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.MapSettingsDialogBinding;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PersistableUri;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.IndividualRouteUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
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
    public static void showSettingsPopup(final Activity activity, @Nullable final IndividualRoute route, @NonNull final Action1<Boolean> onMapSettingsPopupFinished, @NonNull final Action1<RoutingMode> setRoutingValue, @NonNull final Action1<Integer> setCompactIconValue, @DrawableRes final int alternativeButtonResId) {
        colorAccent = activity.getResources().getColor(R.color.colorAccent);
        isShowCircles = Settings.isShowCircles();
        isAutotargetIndividualRoute = Settings.isAutotargetIndividualRoute();
        showAutotargetIndividualRoute = isAutotargetIndividualRoute || (route != null && route.getNumSegments() > 0);

        final ArrayList<SettingsCheckboxModel> settingsElementsCheckboxes = new ArrayList<>();
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_found, R.drawable.ic_menu_myplaces, Settings.isExcludeFound(), Settings::setExcludeFound, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_own, R.drawable.ic_menu_circle, Settings.isExcludeMyCaches(), Settings::setExcludeMine, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_disabled, R.drawable.ic_menu_disabled, Settings.isExcludeDisabledCaches(), Settings::setExcludeDisabled, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_archived, R.drawable.ic_menu_archived, Settings.isExcludeArchivedCaches(), Settings::setExcludeArchived, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_offlinelog, R.drawable.ic_menu_logic, Settings.isExcludeOfflineLog(), Settings::setExcludeOfflineLog, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showwp_original, R.drawable.ic_menu_waypoint, Settings.isExcludeWpOriginal(), Settings::setExcludeWpOriginal, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showwp_parking, R.drawable.ic_menu_parking, Settings.isExcludeWpParking(), Settings::setExcludeWpParking, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showwp_visited, R.drawable.ic_menu_visited, Settings.isExcludeWpVisited(), Settings::setExcludeWpVisited, true));
        if (PersistableUri.TRACK.hasValue()) {
            settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_show_track, R.drawable.ic_menu_hidetrack, Settings.isHideTrack(), Settings::setHideTrack, true));
        }
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_show_circles, R.drawable.ic_menu_circle, isShowCircles, Settings::setShowCircles, false));

        final MapSettingsDialogBinding dialogView = MapSettingsDialogBinding.inflate(LayoutInflater.from(Dialogs.newContextThemeWrapper(activity)));

        for (SettingsCheckboxModel element : settingsElementsCheckboxes) {
            final RelativeLayout l = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.map_settings_item, dialogView.mapSettingsListview, false);
            ((ImageView) l.findViewById(R.id.settings_item_icon)).setImageResource(element.resIcon);
            ((TextView) l.findViewById(R.id.settings_item_text)).setText(element.resTitle);
            final CheckBox checkBox = l.findViewById(R.id.settings_item_checkbox);
            checkBox.setChecked(element.currentValue);
            l.setOnClickListener(v1 -> {
                element.currentValue = !element.currentValue;
                checkBox.setChecked(element.currentValue);
            });
            dialogView.mapSettingsListview.addView(l);
        }

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
                for (SettingsCheckboxModel item : settingsElementsCheckboxes) {
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
            dialogView.routingInfo.setOnClickListener(v -> {
                Dialogs.confirm(activity, R.string.map_routing_activate_title, R.string.map_routing_activate, (dialog1, which) -> {
                    Settings.setUseInternalRouting(true);
                    Settings.setBrouterAutoTileDownloads(true);
                    configureRoutingButtons(true, routingChoiceWrapper);
                    dialogView.routingInfo.setVisibility(View.GONE);
                });
            });
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

    private static class SettingsCheckboxModel {
        private final int resTitle;
        private final int resIcon;
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
