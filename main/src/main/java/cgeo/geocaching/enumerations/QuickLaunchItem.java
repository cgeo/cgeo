package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public enum QuickLaunchItem {
    // item names must not be changed
    GOTO(R.string.any_button, R.drawable.ic_menu_goto, false),
    POCKETQUERY(R.string.menu_lists_pocket_queries, R.drawable.ic_menu_pocket_query, true),
    BOOKMARKLIST(R.string.menu_lists_bookmarklists, R.drawable.ic_menu_bookmarks, true),
    SETTINGS(R.string.menu_settings, R.drawable.settings_nut, false),
    BACKUPRESTORE(R.string.menu_backup, R.drawable.settings_backup, false),
    MANUAL(R.string.about_nutshellmanual, R.drawable.ic_menu_info_details, false),
    FAQ(R.string.faq_title, R.drawable.ic_menu_hint, false),
    VIEWSETTINGS(R.string.view_settings, R.drawable.settings_eye, false);

    @StringRes public final int info;
    @DrawableRes public int iconRes;
    public boolean gcPremiumOnly;

    QuickLaunchItem(final @StringRes int info, final @DrawableRes int iconRes, final boolean gcPremiumOnly) {
        this.info = info;
        this.iconRes = iconRes;
        this.gcPremiumOnly = gcPremiumOnly;
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
