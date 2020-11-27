package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.apache.commons.lang3.StringUtils;

public class MapSettingsUtils {

    private MapSettingsUtils() {
        // utility class
    }

    public static void showSettingsPopup(final Activity activity, final Runnable onMapSettingsPopupFinished) {

        final PopupMenu popup = new PopupMenu(activity, activity.findViewById(R.id.map_settings_popup), Gravity.CENTER);
        popup.getMenuInflater().inflate(R.menu.map_settings_popup, popup.getMenu());
        final Menu menu = popup.getMenu();
        menu.findItem(R.id.menu_mycaches_mode).setChecked(Settings.isExcludeMyCaches());
        menu.findItem(R.id.menu_disabled_mode).setChecked(Settings.isExcludeDisabledCaches());
        menu.findItem(R.id.menu_archived_mode).setChecked(Settings.isExcludeArchivedCaches());
        menu.findItem(R.id.menu_hidewp_original).setChecked(Settings.isExcludeWpOriginal());
        menu.findItem(R.id.menu_hidewp_parking).setChecked(Settings.isExcludeWpParking());
        menu.findItem(R.id.menu_hidewp_visited).setChecked(Settings.isExcludeWpVisited());

        final boolean trackfileSet = StringUtils.isNotBlank(Settings.getTrackFile());
        final MenuItem hideTrack = menu.findItem(R.id.menu_hide_track);
        hideTrack.setVisible(trackfileSet);
        hideTrack.setChecked(Settings.isHideTrack());

        popup.setOnMenuItemClickListener(item -> {
            final int itemId = item.getItemId();
            if (itemId == R.id.menu_confirm_settings) {
                Settings.setExcludeMine(menu.findItem(R.id.menu_mycaches_mode).isChecked());
                Settings.setExcludeDisabled(menu.findItem(R.id.menu_disabled_mode).isChecked());
                Settings.setExcludeArchived(menu.findItem(R.id.menu_archived_mode).isChecked());
                Settings.setExcludeWpOriginal(menu.findItem(R.id.menu_hidewp_original).isChecked());
                Settings.setExcludeWpParking(menu.findItem(R.id.menu_hidewp_parking).isChecked());
                Settings.setExcludeWpVisited(menu.findItem(R.id.menu_hidewp_visited).isChecked());

                if (null != hideTrack && hideTrack.isVisible()) {
                    Settings.setHideTrack(hideTrack.isChecked());
                }

                onMapSettingsPopupFinished.run();
                return true;
            } else {
                item.setChecked(!item.isChecked());
                // avoid closing popup menu
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                item.setActionView(new View(activity));
                item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(final MenuItem item) {
                        return false;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(final MenuItem item) {
                        return false;
                    }
                });
            }
            return false;
        });
        popup.show();
    }

}
