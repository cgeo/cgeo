package cgeo.geocaching.enumerations;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.DBInspectionActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.connector.gc.BookmarkListActivity;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.PocketQueryListActivity;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.models.InfoItem;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.settings.ViewSettingsActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.wherigo.WherigoActivity;

import android.app.Activity;
import android.content.Intent;

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
        RECENTLY_VIEWED(9),
        SETTINGS(4),
        VIEWSETTINGS(8),
        VIEWDATABASE(12),
        BACKUPRESTORE(5),
        MESSAGECENTER(10),
        MANUAL(6),
        FAQ(7),
        WHERIGO(11);

        public final int id;
        VALUES(final int id) {
            this.id = id;
        }
    }

    public static final ArrayList<InfoItem> ITEMS = new ArrayList<>(Arrays.asList(
        new QuickLaunchItem(VALUES.GOTO, R.string.any_button, R.drawable.ic_menu_goto, false),
        new QuickLaunchItem(VALUES.POCKETQUERY, R.string.menu_lists_pocket_queries, R.drawable.ic_menu_pocket_query, true),
        new QuickLaunchItem(VALUES.BOOKMARKLIST, R.string.menu_lists_bookmarklists, R.drawable.ic_menu_bookmarks, true),
        new QuickLaunchItem(VALUES.RECENTLY_VIEWED, R.string.cache_recently_viewed, R.drawable.ic_menu_recent_history, false),
        new QuickLaunchItem(VALUES.SETTINGS, R.string.menu_settings, R.drawable.settings_nut, false),
        new QuickLaunchItem(VALUES.VIEWSETTINGS, R.string.view_settings, R.drawable.settings_eye, false),
        new QuickLaunchItem(VALUES.VIEWDATABASE, R.string.view_database, R.drawable.ic_database, false),
        new QuickLaunchItem(VALUES.BACKUPRESTORE, R.string.menu_backup, R.drawable.settings_backup, false),
        new QuickLaunchItem(VALUES.MESSAGECENTER, R.string.mcpolling_title, R.drawable.ic_menu_email, false),
        new QuickLaunchItem(VALUES.MANUAL, R.string.about_nutshellmanual, R.drawable.ic_menu_info_details, false),
        new QuickLaunchItem(VALUES.FAQ, R.string.faq_title, R.drawable.ic_menu_hint, false)
    ));

    static {
        if (Settings.enableFeatureWherigo()) {
            ITEMS.add(new QuickLaunchItem(VALUES.WHERIGO, R.string.wherigo_short, R.drawable.type_marker_wherigo, false));
        }
    }

    @DrawableRes public int iconRes;
    public boolean gcPremiumOnly;

    QuickLaunchItem(final VALUES item, final @StringRes int titleResId, final @DrawableRes int iconRes, final boolean gcPremiumOnly) {
        super(item.id, titleResId);
        this.iconRes = iconRes;
        this.gcPremiumOnly = gcPremiumOnly;
    }

    public static void startActivity(final Activity caller, final @StringRes int title, @StringRes final int prefKey) {
        InfoItem.startActivity(caller, QuickLaunchItem.class.getCanonicalName(), "ITEMS", title, prefKey, 1);
    }

    public static void launchQuickLaunchItem(final Activity activity, final int which, final boolean hideNavigationBar) {
        if (which == VALUES.GOTO.id) {
            InternalConnector.assertHistoryCacheExists(activity);
            CacheDetailActivity.startActivity(activity, InternalConnector.GEOCODE_HISTORY_CACHE, true);
        } else if (which == VALUES.POCKETQUERY.id) {
            if (Settings.isGCPremiumMember()) {
                final Intent intent = new Intent(activity, PocketQueryListActivity.class);
                AbstractNavigationBarActivity.setIntentHideBottomNavigation(intent, hideNavigationBar);
                activity.startActivity(intent);
            }
        } else if (which == VALUES.BOOKMARKLIST.id) {
            if (Settings.isGCPremiumMember()) {
                final Intent intent = new Intent(activity, BookmarkListActivity.class);
                AbstractNavigationBarActivity.setIntentHideBottomNavigation(intent, hideNavigationBar);
                activity.startActivity(intent);
            }
        } else if (which == VALUES.RECENTLY_VIEWED.id) {
            CacheListActivity.startActivityLastViewed(activity, new SearchResult(DataStore.getLastOpenedCaches()));
        } else if (which == VALUES.SETTINGS.id) {
            final Intent intent = new Intent(activity, SettingsActivity.class);
            AbstractNavigationBarActivity.setIntentHideBottomNavigation(intent, hideNavigationBar);
            activity.startActivityForResult(intent, Intents.SETTINGS_ACTIVITY_REQUEST_CODE);
        } else if (which == VALUES.BACKUPRESTORE.id) {
            SettingsActivity.openForScreen(R.string.preference_screen_backup, activity, hideNavigationBar);
        } else if (which == VALUES.MESSAGECENTER.id) {
            ShareUtils.openUrl(activity, GCConstants.URL_MESSAGECENTER);
        } else if (which == VALUES.MANUAL.id) {
            ShareUtils.openUrl(activity, activity.getString(R.string.manual_link_full));
        } else if (which == VALUES.WHERIGO.id) {
            WherigoActivity.start(activity, hideNavigationBar, 0);
        } else if (which == VALUES.FAQ.id) {
            ShareUtils.openUrl(activity, activity.getString(R.string.faq_link_full));
        } else if (which == VALUES.VIEWSETTINGS.id) {
            final Intent intent = new Intent(activity, ViewSettingsActivity.class);
            AbstractNavigationBarActivity.setIntentHideBottomNavigation(intent, hideNavigationBar);
            activity.startActivity(intent);
        } else if (which == VALUES.VIEWDATABASE.id) {
            activity.startActivity(new Intent(activity, DBInspectionActivity.class));
        } else {
            throw new IllegalStateException("MainActivity: unknown QuickLaunchItem");
        }
    }

}
