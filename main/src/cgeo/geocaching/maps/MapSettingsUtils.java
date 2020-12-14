package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.ManualRoute;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.IndividualRouteUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class MapSettingsUtils {

    private static int colorAccent;
    private static boolean isShowCircles;
    private static boolean isAutotargetIndividualRoute;
    private static boolean showAutotargetIndividualRoute;

    private MapSettingsUtils() {
        // utility class
    }

    public static void showSettingsPopup(final Activity activity, @Nullable final ManualRoute route, @NonNull final Action1<Boolean> onMapSettingsPopupFinished, @NonNull final Action1<RoutingMode> setRoutingValue, @NonNull final Action1<Integer> setCompactIconValue) {
        colorAccent = activity.getResources().getColor(R.color.colorAccent);
        isShowCircles = Settings.isShowCircles();
        isAutotargetIndividualRoute = Settings.isAutotargetIndividualRoute();
        showAutotargetIndividualRoute = isAutotargetIndividualRoute || (route != null && route.getNumSegments() > 0);

        final ArrayList<SettingsCheckboxModel> settingsElementsCheckboxes = new ArrayList<>();
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_ownfound, R.drawable.ic_menu_myplaces, Settings.isExcludeMyCaches(), Settings::setExcludeMine, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_disabled, R.drawable.ic_menu_disabled, Settings.isExcludeDisabledCaches(), Settings::setExcludeDisabled, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_archived, R.drawable.ic_menu_archived, Settings.isExcludeArchivedCaches(), Settings::setExcludeArchived, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showwp_original, R.drawable.ic_menu_waypoint, Settings.isExcludeWpOriginal(), Settings::setExcludeWpOriginal, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showwp_parking, R.drawable.ic_menu_parking, Settings.isExcludeWpParking(), Settings::setExcludeWpParking, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showwp_visited, R.drawable.ic_menu_visited, Settings.isExcludeWpVisited(), Settings::setExcludeWpVisited, true));
        if (StringUtils.isNotBlank(Settings.getTrackFile())) {
            settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_show_track, R.drawable.ic_menu_hidetrack, Settings.isHideTrack(), Settings::setHideTrack, true));
        }
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_show_circles, R.drawable.ic_menu_circle, isShowCircles, Settings::setShowCircles, false));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_direction, R.drawable.ic_menu_goto, Settings.isMapDirection(), Settings::setMapDirection, false));

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.map_settings_dialog, null);

        final LinearLayout settingsListview = dialogView.findViewById(R.id.map_settings_listview);
        for (SettingsCheckboxModel element : settingsElementsCheckboxes) {
            final RelativeLayout l = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.map_settings_item, null);
            ((ImageView) l.findViewById(R.id.settings_item_icon)).setImageResource(element.resIcon);
            ((TextView) l.findViewById(R.id.settings_item_text)).setText(element.resTitle);
            final CheckBox checkBox = l.findViewById(R.id.settings_item_checkbox);
            checkBox.setChecked(element.currentValue);
            l.setOnClickListener(v1 -> {
                element.currentValue = !element.currentValue;
                checkBox.setChecked(element.currentValue);
            });
            settingsListview.addView(l);
        }

        final ArrayList<ButtonChoiceModel<Integer>> compactIconChoices = new ArrayList<>();
        compactIconChoices.add(new ButtonChoiceModel<>(R.id.compacticon_off, Settings.COMPACTICON_OFF));
        compactIconChoices.add(new ButtonChoiceModel<>(R.id.compacticon_auto, Settings.COMPACTICON_AUTO));
        compactIconChoices.add(new ButtonChoiceModel<>(R.id.compacticon_on, Settings.COMPACTICON_ON));
        final ButtonController<Integer> compactIcon = new ButtonController<Integer>(dialogView, compactIconChoices, Settings.getCompactIconMode(), setCompactIconValue);

        final ArrayList<ButtonChoiceModel<RoutingMode>> routingChoices = new ArrayList<>();
        routingChoices.add(new ButtonChoiceModel<>(R.id.routing_straight, RoutingMode.STRAIGHT));
        routingChoices.add(new ButtonChoiceModel<>(R.id.routing_walk, RoutingMode.WALK));
        routingChoices.add(new ButtonChoiceModel<>(R.id.routing_bike, RoutingMode.BIKE));
        routingChoices.add(new ButtonChoiceModel<>(R.id.routing_car, RoutingMode.CAR));
        final ButtonController<RoutingMode> routing = new ButtonController<>(dialogView, routingChoices, Settings.getRoutingMode(), setRoutingValue);

        final CheckBox autotargetCheckbox = dialogView.findViewById(R.id.map_settings_autotarget);
        if (showAutotargetIndividualRoute) {
            final View autotargetContainer = dialogView.findViewById(R.id.map_settings_autotarget_container);
            autotargetContainer.setVisibility(View.VISIBLE);
            autotargetCheckbox.setChecked(isAutotargetIndividualRoute);
        }

        Dialogs.newBuilder(activity)
            .setView(dialogView)
            .setTitle(R.string.quick_settings)
            .setOnDismissListener(dialog -> {
                for (SettingsCheckboxModel item : settingsElementsCheckboxes) {
                    item.setValue();
                }
                compactIcon.setValue();
                routing.setValue();
                onMapSettingsPopupFinished.call(isShowCircles != Settings.isShowCircles());
                if (showAutotargetIndividualRoute && isAutotargetIndividualRoute != autotargetCheckbox.isChecked()) {
                    if (route == null) {
                        Settings.setAutotargetIndividualRoute(autotargetCheckbox.isChecked());
                    } else {
                        IndividualRouteUtils.setAutotargetIndividualRoute(activity, route, autotargetCheckbox.isChecked());
                    }
                }
            })
            .create()
            .show();

        compactIcon.init();
        routing.init();
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
            this.currentValue = isNegated ? !currentValue : currentValue;
            this.setValue = setValue;
            this.isNegated = isNegated;
        }

        public void setValue() {
            this.setValue.call(isNegated ? !currentValue : currentValue);
        }
    }

    private static class ButtonChoiceModel<T> {
        public final int resButton;
        public final T assignedValue;
        public View button = null;

        ButtonChoiceModel(final int resButton, final T assignedValue) {
            this.resButton = resButton;
            this.assignedValue = assignedValue;
        }
    }

    private static class ButtonController<T> {
        private final View dialogView;
        private final ArrayList<ButtonChoiceModel<T>> buttons;
        private T originalValue;
        private T currentValue;
        private final Action1<T> setValue;

        ButtonController(final View dialogView, final ArrayList<ButtonChoiceModel<T>> buttons, final T currentValue, final Action1<T> setValue) {
            this.dialogView = dialogView;
            this.buttons = buttons;
            this.originalValue = currentValue;
            this.currentValue = currentValue;
            this.setValue = setValue;
        }

        public void init() {
            for (final ButtonChoiceModel<T> button : buttons) {
                button.button = dialogView.findViewById(button.resButton);
                button.button.setOnClickListener(v -> setLocalValue(button.assignedValue));
            }
            update();
        }

        public void update() {
            for (final ButtonChoiceModel<T> button : buttons) {
                if (currentValue == button.assignedValue) {
                    button.button.setBackgroundColor(colorAccent);
                } else {
                    button.button.setBackgroundColor(0x00000000);
                    button.button.setBackgroundResource(Settings.isLightSkin() ? R.drawable.action_button_light : R.drawable.action_button_dark);
                }
            }
        }

        private void setLocalValue(final T currentValue) {
            this.currentValue = currentValue;
            update();
        }

        public void setValue() {
            if (!originalValue.equals(currentValue)) {
                this.setValue.call(currentValue);
            }
        }
    }
}
