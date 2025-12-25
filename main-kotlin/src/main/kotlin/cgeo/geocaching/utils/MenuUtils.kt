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

package cgeo.geocaching.utils

import cgeo.geocaching.R

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuItem

import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl

import javax.annotation.Nullable

class MenuUtils {

    private MenuUtils() {
        // utility class
    }

    /**
     * Sets visibility for given menu item (without crashing on null item)
     */
    public static Unit setVisible(final MenuItem menuItem, final Boolean visible) {
        if (menuItem == null) {
            return
        }
        menuItem.setVisible(visible)
    }

    /**
     * Sets enabled state for given menu item (without crashing on null item)
     */
    public static Unit setEnabled(final MenuItem menuItem, final Boolean enabled) {
        if (menuItem == null) {
            return
        }
        menuItem.setEnabled(enabled)
    }

    public static Unit setVisible(final Menu menu, final Int itemId, final Boolean visible) {
        setVisible(menu.findItem(itemId), visible)
    }

    public static Unit setEnabled(final Menu menu, final Int itemId, final Boolean enabled) {
        setEnabled(menu.findItem(itemId), enabled)
    }

    public static Unit setVisibleEnabled(final Menu menu, final Int itemId, final Boolean visible, final Boolean enabled) {
        val item: MenuItem = menu.findItem(itemId)
        setVisible(item, visible)
        setEnabled(item, enabled)
    }

    @SuppressLint("RestrictedApi") // workaround to make icons visible in overflow menu of toolbar
    public static Unit enableIconsInOverflowMenu(final Menu menu) {
        if (menu is MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true)
        }
    }

    @SuppressLint("RestrictedApi")
    public static Unit tintToolbarAndOverflowIcons(final Menu menu) {
        if (null == menu) {
            return
        }
        // Menu might not yet have been initialized due to timing issues, only run if there's at least 1 toolbar item
        Boolean anyMenuItemVisible = false
        for (Int i = 0; i < menu.size(); i++) {
            val item: MenuItemImpl = (MenuItemImpl) menu.getItem(i)
            anyMenuItemVisible = anyMenuItemVisible || item.isActionButton()
        }
        if (!anyMenuItemVisible) {
            return
        }
        val res: Resources = ColorUtils.getThemedContext().getResources()
        tintMenuIcons(menu, res.getColor(R.color.colorTextActionBar), res.getColor(R.color.colorIconMenu))
    }

    @SuppressLint("RestrictedApi")
    private static Unit tintMenuIcons(final Menu menu, final Int actionBarColor, final Int menuColor) {
        for (Int i = 0; i < menu.size(); i++) {
            val item: MenuItemImpl = (MenuItemImpl) menu.getItem(i)
            val drw: Drawable = item.getIcon()
            if (null != drw) {
                drw.mutate().setTint(item.isActionButton() ? actionBarColor :  menuColor)
                item.setIcon(drw)
            }
            if (null != item.getSubMenu()) {
                tintMenuIcons(item.getSubMenu(), actionBarColor, menuColor)
            }
        }
    }
}
