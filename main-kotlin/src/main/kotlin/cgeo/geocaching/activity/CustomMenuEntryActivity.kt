// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.activity

import cgeo.geocaching.enumerations.QuickLaunchItem
import cgeo.geocaching.settings.Settings

import android.app.Activity
import android.content.Intent

/**
 * Similar to AbstractNavigationBarActivity, but
 * - placed in custom menu slot of bottom navigation
 * - and defaults to "bottom navigation not shown"
 */

class CustomMenuEntryActivity : AbstractNavigationBarActivity() {

    override     public Int getSelectedBottomItemId() {
        return MENU_CUSTOM
    }

    public QuickLaunchItem.VALUES getRelatedQuickLaunchItem() {
        return null
    }

    override     protected Unit checkIntentHideNavigationBar() {
        val customBNisQuickLaunchItem: Boolean = customMenuEntryIsRelatedQuickLaunchItem()
        checkIntentHideNavigationBar(!customBNisQuickLaunchItem)
    }

    protected Boolean customMenuEntryIsRelatedQuickLaunchItem() {
        return customMenuEntryisQuickLaunchItem(getRelatedQuickLaunchItem())
    }

    protected static Boolean customMenuEntryisQuickLaunchItem(final QuickLaunchItem.VALUES quickLaunchItem) {
        return quickLaunchItem != null && Settings.getCustomBNitem() == quickLaunchItem.id
    }

    protected static Unit startActivityHelper(final Activity parent, final Intent intent, final QuickLaunchItem.VALUES quickLaunchItem, final Boolean forceHideNavigationBar) {
        val isRelatedCustomItem: Boolean = customMenuEntryisQuickLaunchItem(quickLaunchItem)
        val hideNavBar: Boolean = !isRelatedCustomItem || forceHideNavigationBar

        setIntentHideBottomNavigation(intent, hideNavBar)
        parent.startActivity(intent)
    }


}
