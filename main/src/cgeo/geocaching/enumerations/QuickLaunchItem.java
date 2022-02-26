package cgeo.geocaching.enumerations;

import androidx.annotation.DrawableRes;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import cgeo.geocaching.R;

public enum QuickLaunchItem {
    // item names must not be changed
    GOTO("Go to", R.drawable.ic_menu_goto),
    POCKETQUERY("Pocket Queries", R.drawable.ic_menu_pocket_query),
    BOOKMARKLIST("Bookmark Lists", R.drawable.ic_menu_bookmarks),
    SETTINGS("Settings", R.drawable.settings_nut),
    BACKUPRESTORE("Backup / Restore", R.drawable.settings_backup),
    MANUAL("c:geo manual", R.drawable.ic_menu_info_details),
    FAQ("c:geo FAQ", R.drawable.ic_menu_hint),
    VIEWSETTINGS("View Settings", R.drawable.settings_eye);

    public final String info;
    @DrawableRes public int iconRes;

    QuickLaunchItem(final String info, final @DrawableRes int iconRes) {
        this.info = info;
        this.iconRes = iconRes;
    }

    @Nullable
    public static QuickLaunchItem getByName(final String name) {
        for (QuickLaunchItem item : values()) {
            if (StringUtils.equals(item.name(), name)) {
                return item;
            }
        }
        return null;
    }
}
