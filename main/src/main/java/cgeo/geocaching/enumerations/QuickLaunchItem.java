package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.models.InfoItem;

import android.app.Activity;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Arrays;

public class QuickLaunchItem extends InfoItem {

    // item id must not be changed, order can be adjusted
    public enum VALUES {
        GOTO(1),
        POCKETQUERY(2),
        BOOKMARKLIST(3),
        SETTINGS(4),
        BACKUPRESTORE(5),
        MANUAL(6),
        FAQ(7),
        VIEWSETTINGS(8);

        public final int id;
        VALUES(final int id) {
            this.id = id;
        }
    }

    public static final ArrayList<InfoItem> ITEMS = new ArrayList<>(Arrays.asList(
        new QuickLaunchItem(VALUES.GOTO, R.string.any_button, R.drawable.ic_menu_goto, false),
        new QuickLaunchItem(VALUES.POCKETQUERY, R.string.menu_lists_pocket_queries, R.drawable.ic_menu_pocket_query, true),
        new QuickLaunchItem(VALUES.BOOKMARKLIST, R.string.menu_lists_bookmarklists, R.drawable.ic_menu_bookmarks, true),
        new QuickLaunchItem(VALUES.SETTINGS, R.string.menu_settings, R.drawable.settings_nut, false),
        new QuickLaunchItem(VALUES.BACKUPRESTORE, R.string.menu_backup, R.drawable.settings_backup, false),
        new QuickLaunchItem(VALUES.MANUAL, R.string.about_nutshellmanual, R.drawable.ic_menu_info_details, false),
        new QuickLaunchItem(VALUES.FAQ, R.string.faq_title, R.drawable.ic_menu_hint, false),
        new QuickLaunchItem(VALUES.VIEWSETTINGS, R.string.view_settings, R.drawable.settings_eye, false)
    ));

    @DrawableRes public int iconRes;
    public boolean gcPremiumOnly;

    QuickLaunchItem(final VALUES item, final @StringRes int titleResId, final @DrawableRes int iconRes, final boolean gcPremiumOnly) {
        super(item.id, titleResId);
        this.iconRes = iconRes;
        this.gcPremiumOnly = gcPremiumOnly;
    }

    public static void startActivity(final Activity caller, final @StringRes int title, @StringRes final int prefKey) {
        InfoItem.startActivity(caller, QuickLaunchItem.class.getCanonicalName(), "ITEMS", title, prefKey);
    }
}
