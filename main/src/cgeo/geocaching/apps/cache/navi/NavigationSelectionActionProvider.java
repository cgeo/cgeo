package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.ui.AbstractMenuActionProvider;

import android.app.Activity;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;

/**
 * Action provider listing all available navigation actions as sub menu.
 */
public class NavigationSelectionActionProvider extends AbstractMenuActionProvider {

    private Geocache geocache;
    private final Activity activity;

    public NavigationSelectionActionProvider(final Activity activity) {
        super(activity);
        this.activity = activity;
    }

    public void setTarget(final Geocache cache) {
        geocache = cache;
    }

    @Override
    public void onPrepareSubMenu(final SubMenu subMenu) {
        subMenu.clear();
        if (geocache == null) {
            return;
        }
        for (final NavigationAppsEnum app : NavigationAppFactory.getActiveNavigationApps()) {
            if (!(app.app instanceof CacheNavigationApp)) {
                continue;
            }
            final CacheNavigationApp cacheApp = (CacheNavigationApp) app.app;
            if (app.app.isEnabled(geocache)) {
                subMenu.add(Menu.NONE, app.id, Menu.NONE, app.app.getName()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        cacheApp.navigate(activity, geocache);
                        return true;
                    }
                });
            }
        }
    }

    public static void initialize(final MenuItem menuItem, final Geocache cache) {
        final ActionProvider actionProvider = MenuItemCompat.getActionProvider(menuItem);
        if (actionProvider instanceof NavigationSelectionActionProvider) {
            final NavigationSelectionActionProvider navigateAction = (NavigationSelectionActionProvider) actionProvider;
            navigateAction.setTarget(cache);
        }
    }

}
