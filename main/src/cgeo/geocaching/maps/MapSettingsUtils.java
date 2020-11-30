package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class MapSettingsUtils {

    private MapSettingsUtils() {
        // utility class
    }

    public static void showSettingsPopup(final Activity activity, final Runnable onMapSettingsPopupFinished) {
        final ArrayList<SettingsElementModel> settingsElements = new ArrayList<>();
        settingsElements.add(new SettingsElementModel(R.string.map_mycaches_hide , R.drawable.ic_menu_myplaces, Settings.isExcludeMyCaches(), Settings::setExcludeMine));
        settingsElements.add(new SettingsElementModel(R.string.map_disabled_hide , R.drawable.ic_menu_disabled, Settings.isExcludeDisabledCaches(), Settings::setExcludeDisabled));
        settingsElements.add(new SettingsElementModel(R.string.map_archived_hide , R.drawable.ic_menu_archived, Settings.isExcludeArchivedCaches(), Settings::setExcludeArchived));
        settingsElements.add(new SettingsElementModel(R.string.map_hidewp_original , R.drawable.ic_menu_waypoint, Settings.isExcludeWpOriginal(), Settings::setExcludeWpOriginal));
        settingsElements.add(new SettingsElementModel(R.string.map_hidewp_parking , R.drawable.ic_menu_parking, Settings.isExcludeWpParking(), Settings::setExcludeWpParking));
        settingsElements.add(new SettingsElementModel(R.string.map_hidewp_visited , R.drawable.ic_menu_visited, Settings.isExcludeWpVisited(), Settings::setExcludeWpVisited));
        if (StringUtils.isNotBlank(Settings.getTrackFile())) {
            settingsElements.add(new SettingsElementModel(R.string.map_hide_track, R.drawable.ic_menu_hidetrack, Settings.isHideTrack(), Settings::setHideTrack));
        }

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.map_settings_dialog, null);
        ((ListView) dialogView.findViewById(R.id.map_settings_listview)).setAdapter(new MapSettingsAdapter(activity, settingsElements));

        Dialogs.newBuilder(activity)
            .setView(dialogView)
            .setTitle(R.string.quick_settings)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
            .setOnDismissListener(dialog -> {
                for (SettingsElementModel item : settingsElements) {
                    item.setValue.call(item.currentValue);
                }
                onMapSettingsPopupFinished.run();
            })
            .create()
            .show();
    }

    private static class SettingsElementModel {
        public final int resTitle;
        public final int resIcon;
        public boolean currentValue;
        public final Action1<Boolean> setValue;

        SettingsElementModel(@StringRes final int resTitle, @DrawableRes final int resIcon, final boolean currentValue, final Action1<Boolean> setValue) {
            this.resTitle = resTitle;
            this.resIcon = resIcon;
            this.currentValue = currentValue;
            this.setValue = setValue;
        }
    }

    private static class MapSettingsAdapter extends ArrayAdapter<SettingsElementModel> {

        private final ArrayList<SettingsElementModel> statusList;

        MapSettingsAdapter(final Context context, final ArrayList<SettingsElementModel> statusList) {
            super(context, R.layout.map_settings_dialog, statusList);
            this.statusList = statusList;
        }

        @Override
        public @NonNull View getView(final int position, final @Nullable View convertView, final @NonNull ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.map_settings_item, null);
            }

            final SettingsElementModel item = statusList.get(position);
            if (item != null) {
                ((ImageView) v.findViewById(R.id.settings_item_icon)).setImageResource(item.resIcon);
                ((TextView) v.findViewById(R.id.settings_item_text)).setText(item.resTitle);
                final CheckBox checkBox = v.findViewById(R.id.settings_item_checkbox);
                checkBox.setChecked(item.currentValue);
                v.setOnClickListener(v1 -> {
                    item.currentValue = !item.currentValue;
                    checkBox.setChecked(item.currentValue);
                });
            }
            return v;
        }

    }

}
