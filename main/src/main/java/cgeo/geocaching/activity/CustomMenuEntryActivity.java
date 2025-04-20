package cgeo.geocaching.activity;

import cgeo.geocaching.enumerations.QuickLaunchItem;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.content.Intent;

/**
 * Similar to AbstractNavigationBarActivity, but
 * - placed in custom menu slot of bottom navigation
 * - and defaults to "bottom navigation not shown"
 */

public class CustomMenuEntryActivity extends AbstractNavigationBarActivity {

    @Override
    public int getSelectedBottomItemId() {
        return MENU_CUSTOM;
    }

    public QuickLaunchItem.VALUES getRelatedQuickLaunchItem() {
        return null;
    }

    @Override
    protected void checkIntentHideNavigationBar() {
        final boolean customBNisQuickLaunchItem = customMenuEntryIsRelatedQuickLaunchItem();
        checkIntentHideNavigationBar(!customBNisQuickLaunchItem);
    }

    protected boolean customMenuEntryIsRelatedQuickLaunchItem() {
        return customMenuEntryisQuickLaunchItem(getRelatedQuickLaunchItem());
    }

    protected static boolean customMenuEntryisQuickLaunchItem(final QuickLaunchItem.VALUES quickLaunchItem) {
        return quickLaunchItem != null && Settings.getCustomBNitem() == quickLaunchItem.id;
    }

    protected static void startActivityHelper(final Activity parent, final Intent intent, final QuickLaunchItem.VALUES quickLaunchItem, final boolean forceHideNavigationBar) {
        final boolean isRelatedCustomItem = customMenuEntryisQuickLaunchItem(quickLaunchItem);
        final boolean hideNavBar = !isRelatedCustomItem || forceHideNavigationBar;

        setIntentHideBottomNavigation(intent, hideNavBar);
        parent.startActivity(intent);
    }


}
