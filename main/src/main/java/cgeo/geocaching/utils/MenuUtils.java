package cgeo.geocaching.utils;

import cgeo.geocaching.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.content.ContextCompat;

import javax.annotation.Nullable;

public class MenuUtils {

    private MenuUtils() {
        // utility class
    }

    /**
     * Sets visibility for given menu item (without crashing on null item)
     */
    public static void setVisible(final MenuItem menuItem, final boolean visible) {
        if (menuItem == null) {
            return;
        }
        menuItem.setVisible(visible);
    }

    /**
     * Sets enabled state for given menu item (without crashing on null item).
     * Also updates icon tint immediately to reflect the new state.
     */
    public static void setEnabled(final MenuItem menuItem, final boolean enabled) {
        if (menuItem == null) {
            return;
        }
        menuItem.setEnabled(enabled);
        tintSingleIcon(menuItem);
    }

    public static void setVisible(final Menu menu, final int itemId, final boolean visible) {
        setVisible(menu.findItem(itemId), visible);
    }

    public static void setEnabled(final Menu menu, final int itemId, final boolean enabled) {
        setEnabled(menu.findItem(itemId), enabled);
    }

    public static void setVisibleEnabled(final Menu menu, final int itemId, final boolean visible, final boolean enabled) {
        final MenuItem item = menu.findItem(itemId);
        setVisible(item, visible);
        setEnabled(item, enabled);
    }

    @SuppressLint("RestrictedApi") // workaround to make icons visible in overflow menu of toolbar
    public static void enableIconsInOverflowMenu(@Nullable final Menu menu) {
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
    }

    @SuppressLint("RestrictedApi")
    private static void tintSingleIcon(final MenuItem menuItem) {
        if (!(menuItem instanceof MenuItemImpl)) {
            return;
        }
        final MenuItemImpl item = (MenuItemImpl) menuItem;
        final Drawable drw = item.getIcon();
        if (drw == null) {
            return;
        }
        final Context ctx = ColorUtils.getThemedContext();
        final boolean isActionButton = item.isActionButton();
        final ColorStateList tint = ContextCompat.getColorStateList(ctx, isActionButton ? R.color.action_bar_item_text_selector : R.color.menu_icon_tint_selector);
        if (tint == null) {
            return;
        }
        final int[] state = item.isEnabled() ? new int[]{android.R.attr.state_enabled} : new int[]{};
        final int baseColor = tint.getColorForState(state, tint.getDefaultColor());
        // android:alpha in ColorStateList is not applied by getColorForState() - apply manually
        final int color = item.isEnabled() ? baseColor : applyAlpha(baseColor, 0.6f);
        drw.mutate().setTint(color);
        item.setIcon(drw);
    }

    @SuppressLint("RestrictedApi")
    public static void tintToolbarAndOverflowIcons(@Nullable final Menu menu) {
        if (null == menu) {
            return;
        }
        // Menu might not yet have been initialized due to timing issues, only run if there's at least 1 toolbar item
        boolean anyMenuItemVisible = false;
        for (int i = 0; i < menu.size(); i++) {
            final MenuItemImpl item = (MenuItemImpl) menu.getItem(i);
            anyMenuItemVisible = anyMenuItemVisible || item.isActionButton();
        }
        if (!anyMenuItemVisible) {
            return;
        }
        final Context ctx = ColorUtils.getThemedContext();
        final ColorStateList actionBarTint = ContextCompat.getColorStateList(ctx, R.color.action_bar_item_text_selector);
        final ColorStateList menuTint = ContextCompat.getColorStateList(ctx, R.color.menu_icon_tint_selector);
        tintMenuIcons(menu, actionBarTint, menuTint);
    }

    @SuppressLint("RestrictedApi")
    private static void tintMenuIcons(final Menu menu, final ColorStateList actionBarTint, final ColorStateList menuTint) {
        final int[] stateEnabled = new int[]{android.R.attr.state_enabled};
        final int[] stateDisabled = new int[]{};
        for (int i = 0; i < menu.size(); i++) {
            final MenuItemImpl item = (MenuItemImpl) menu.getItem(i);
            final Drawable drw = item.getIcon();
            if (null != drw) {
                final ColorStateList tint = item.isActionButton() ? actionBarTint : menuTint;
                final int[] state = item.isEnabled() ? stateEnabled : stateDisabled;
                final int baseColor = tint.getColorForState(state, tint.getDefaultColor());
                // android:alpha in ColorStateList is not applied by getColorForState() - apply manually
                final int color = item.isEnabled() ? baseColor : applyAlpha(baseColor, 0.6f);
                drw.mutate().setTint(color);
                item.setIcon(drw);
            }
            if (null != item.getSubMenu()) {
                tintMenuIcons(item.getSubMenu(), actionBarTint, menuTint);
            }
        }
    }

    private static int applyAlpha(final int color, final float alpha) {
        return android.graphics.Color.argb(
                Math.round(alpha * android.graphics.Color.alpha(color)),
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color));
    }
}
