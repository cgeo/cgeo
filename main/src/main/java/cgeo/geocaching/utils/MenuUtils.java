package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;

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
     * Sets enabled state for given menu item (without crashing on null item)
     */
    public static void setEnabled(final MenuItem menuItem, final boolean enabled) {
        if (menuItem == null) {
            return;
        }
        menuItem.setEnabled(enabled);
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
    public static void tintToolbarAndOverflowIconsAndTitles(@Nullable final Menu menu) {
        if (null == menu) {
            return;
        }
        // slight delay
        ActivityMixin.postDelayed(() -> {
            // Menu might not yet have been initialized due to timing issues, only run if there's at least 1 toolbar item
            boolean anyMenuItemVisible = false;
            for (int i = 0; i < menu.size(); i++) {
                final MenuItemImpl item = (MenuItemImpl) menu.getItem(i);
                anyMenuItemVisible = anyMenuItemVisible || item.isActionButton();
            }
            if (!anyMenuItemVisible) {
                return;
            }
            final Resources res = ColorUtils.getThemedContext().getResources();
            tintMenuIconsAndTitles(menu, res.getColor(R.color.colorTextActionBar), res.getColor(R.color.colorIconMenu));
        }, 100);
    }

    @SuppressLint("RestrictedApi")
    private static void tintMenuIconsAndTitles(final Menu menu, final int actionBarColor, final int menuColor) {
        for (int i = 0; i < menu.size(); i++) {
            final MenuItemImpl item = (MenuItemImpl) menu.getItem(i);
            // choose color depending on state
            final int color = ColorUtils.setAlpha(item.isActionButton() ? actionBarColor : menuColor, item.isEnabled() ? 255 : 128);

            // color menu item title
            final SpannableString s = new SpannableString(item.getTitle());
            s.setSpan(new ForegroundColorSpan(color), 0, s.length(), 0);
            item.setTitle(s);

            // color icon, if present
            final Drawable drw = item.getIcon();
            if (null != drw) {
                drw.mutate().setTint(color);
                item.setIcon(drw);
            }
            if (null != item.getSubMenu()) {
                tintMenuIconsAndTitles(item.getSubMenu(), actionBarColor, menuColor);
            }
        }
    }
}
