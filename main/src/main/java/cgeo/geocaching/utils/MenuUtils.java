package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatDelegate;
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
        final Resources res = getThemedContext().getResources();
        tintMenuIcons(menu, res.getColor(R.color.colorIconActionBar), res.getColor(R.color.colorIconMenu));
    }

    private static Context getThemedContext() {
        final Context ctx = CgeoApplication.getInstance();
        final Resources res = ctx.getResources();
        final Configuration configuration = new Configuration(ctx.getResources().getConfiguration());
        final int nightNode = AppCompatDelegate.getDefaultNightMode();
        if (nightNode == AppCompatDelegate.MODE_NIGHT_NO) {
            configuration.uiMode = Configuration.UI_MODE_NIGHT_NO | (res.getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        } else if (nightNode == AppCompatDelegate.MODE_NIGHT_YES) {
            configuration.uiMode = Configuration.UI_MODE_NIGHT_YES | (res.getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        } else {
            configuration.uiMode = res.getConfiguration().uiMode;
        }
        return ctx.createConfigurationContext(configuration);
    }

    @SuppressLint("RestrictedApi")
    private static void tintMenuIcons(final Menu menu, final int actionBarColor, final int menuColor) {
        for (int i = 0; i < menu.size(); i++) {
            final MenuItemImpl item = (MenuItemImpl) menu.getItem(i);
            final Drawable drw = item.getIcon();
            if (null != drw) {
                drw.mutate().setTint(item.isActionButton() ? actionBarColor :  menuColor);
                item.setIcon(drw);
            }
            if (null != item.getSubMenu()) {
                tintMenuIcons(item.getSubMenu(), actionBarColor, menuColor);
            }
        }
    }
}
