package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PersistableUri;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.IndividualRouteUtils;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.functions.Action1;
import static cgeo.geocaching.filters.gui.GeocacheFilterActivity.EXTRA_FILTER_RESULT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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
import java.util.Collection;

public class MapSettingsUtils {

    private static final CacheListType MAP_STORE_TYPE = CacheListType.POCKET; //do NOT use MAP!

    private static int colorAccent;
    private static boolean isShowCircles;
    private static boolean isAutotargetIndividualRoute;
    private static boolean showAutotargetIndividualRoute;

    private static Action1<String> currentFilterNameSetter = null;


    private MapSettingsUtils() {
        // utility class
    }

    public static CacheListType getMapViewerListType() {
        return MAP_STORE_TYPE;
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"}) // splitting up that method would not help improve readability
    public static void showSettingsPopup(final Activity activity, @Nullable final IndividualRoute route, @NonNull final Action1<Boolean> onMapSettingsPopupFinished, @NonNull final Action1<RoutingMode> setRoutingValue, @NonNull final Action1<Integer> setCompactIconValue, @DrawableRes final int alternativeButtonResId, final Collection<Geocache> caches) {
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
        if (PersistableUri.TRACK.hasValue()) {
            settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_show_track, R.drawable.ic_menu_hidetrack, Settings.isHideTrack(), Settings::setHideTrack, true));
        }
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_show_circles, R.drawable.ic_menu_circle, isShowCircles, Settings::setShowCircles, false));

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.map_settings_dialog, null);
        currentFilterNameSetter = s -> ((TextView) dialogView.findViewById(R.id.map_settings_filter_text)).setText(s);

        final LinearLayout settingsListview = dialogView.findViewById(R.id.map_settings_listview);
        for (SettingsCheckboxModel element : settingsElementsCheckboxes) {
            final RelativeLayout l = (RelativeLayout) activity.getLayoutInflater().inflate(R.layout.map_settings_item, settingsListview, false);
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
        compactIconChoices.add(new ButtonChoiceModel<>(R.id.compacticon_off, Settings.COMPACTICON_OFF, activity.getString(R.string.switch_off)));
        compactIconChoices.add(new ButtonChoiceModel<>(R.id.compacticon_auto, Settings.COMPACTICON_AUTO, activity.getString(R.string.switch_auto)));
        compactIconChoices.add(new ButtonChoiceModel<>(R.id.compacticon_on, Settings.COMPACTICON_ON, activity.getString(R.string.switch_on)));
        final ButtonController<Integer> compactIcon = new ButtonController<>(dialogView, null, compactIconChoices, Settings.getCompactIconMode(), setCompactIconValue);

        final ArrayList<ButtonChoiceModel<RoutingMode>> routingChoices = new ArrayList<>();
        for (RoutingMode mode : RoutingMode.values()) {
            routingChoices.add(new ButtonChoiceModel<>(mode.buttonResId, mode, activity.getString(mode.infoResId)));
        }
        final ButtonController<RoutingMode> routing = new ButtonController<>(dialogView, dialogView.findViewById(R.id.routing_title), routingChoices, Routing.isAvailable() || Settings.getRoutingMode() == RoutingMode.OFF ? Settings.getRoutingMode() : RoutingMode.STRAIGHT, setRoutingValue);

        final CheckBox autotargetCheckbox = dialogView.findViewById(R.id.map_settings_autotarget);
        if (showAutotargetIndividualRoute) {
            final View autotargetContainer = dialogView.findViewById(R.id.map_settings_autotarget_container);
            autotargetContainer.setVisibility(View.VISIBLE);
            autotargetCheckbox.setChecked(isAutotargetIndividualRoute);
        }

        final GeocacheFilter mapFilter = GeocacheFilter.createFromConfig(Settings.getCacheFilterConfig(MAP_STORE_TYPE));

        ((TextView) dialogView.findViewById(R.id.map_settings_filter_text)).setText(mapFilter.toUserDisplayableString());
        dialogView.findViewById(R.id.map_settings_filter_button).setOnClickListener(v ->
            GeocacheFilterActivity.selectFilter(
                activity,
                GeocacheFilter.createFromConfig(Settings.getCacheFilterConfig(MAP_STORE_TYPE)),
                caches, true));

        @SuppressLint("InflateParams") final View customTitle = activity.getLayoutInflater().inflate(R.layout.dialog_title_back, null);
        final AlertDialog dialog = Dialogs.newBuilder(activity)
            .setView(dialogView)
            .setCustomTitle(customTitle)
            .setOnDismissListener(d -> {
                currentFilterNameSetter = null;
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
            .create();
        ((TextView) customTitle.findViewById(R.id.dialog_title_title)).setText(R.string.quick_settings);
        final ImageView backButton = customTitle.findViewById(R.id.dialog_title_back);
        if (alternativeButtonResId != 0) {
            backButton.setImageResource(alternativeButtonResId);
        }
        backButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        compactIcon.init();
        routing.init();

        if (!Routing.isAvailable()) {
            for (final ButtonChoiceModel<RoutingMode> button : routing.buttons) {
                if (!(button.assignedValue == RoutingMode.OFF || button.assignedValue == RoutingMode.STRAIGHT)) {
                    button.button.setEnabled(false);
                    button.button.setAlpha(.3f);
                }
            }

            final TextView brouterTextView = dialogView.findViewById(R.id.brouter_install);
            brouterTextView.setVisibility(View.VISIBLE);
            brouterTextView.setOnClickListener(v -> ProcessUtils.openMarket(activity, activity.getString(R.string.package_brouter)));
        }
    }

    public static boolean onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data, @NonNull final Action1<Boolean> onMapSettingsPopupFinished) {

        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            final String filterConfig = data.getExtras().getString(EXTRA_FILTER_RESULT);
            Settings.setCacheFilterConfig(MAP_STORE_TYPE, filterConfig);

            onMapSettingsPopupFinished.call(false);
            if (currentFilterNameSetter != null) {
                final GeocacheFilter mapFilter = GeocacheFilter.createFromConfig(filterConfig);
                currentFilterNameSetter.call(mapFilter.toUserDisplayableString());
            }
            return true;
        }
        return false;
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

    private static class ButtonController<T> {
        private final View dialogView;
        private final ArrayList<ButtonChoiceModel<T>> buttons;
        private final T originalValue;
        private T currentValue;
        private final Action1<T> setValue;
        private final String titlePreset;
        private final TextView titleView;

        ButtonController(final View dialogView, @Nullable final TextView titleView, final ArrayList<ButtonChoiceModel<T>> buttons, final T currentValue, final Action1<T> setValue) {
            this.dialogView = dialogView;
            this.buttons = buttons;
            this.originalValue = currentValue;
            this.currentValue = currentValue;
            this.setValue = setValue;
            this.titlePreset = titleView == null ? "" : titleView.getText().toString();
            this.titleView = titleView;
        }

        public void init() {
            for (final ButtonChoiceModel<T> button : buttons) {
                button.button = dialogView.findViewById(button.resButton);
                button.button.setOnClickListener(v -> setLocalValue(button.assignedValue));
            }
            update();
        }

        @SuppressLint("SetTextI18n")
        public void update() {
            for (final ButtonChoiceModel<T> button : buttons) {
                if (currentValue == button.assignedValue) {
                    button.button.setBackgroundColor(colorAccent);
                    if (titleView != null) {
                        titleView.setText(String.format(titlePreset, button.info));
                    }
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
