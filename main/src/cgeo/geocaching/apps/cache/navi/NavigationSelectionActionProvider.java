package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.apps.App;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;

import java.util.List;

public class NavigationSelectionActionProvider extends ActionProvider {

    private Geocache geocache;
    private final Activity activity;

    public NavigationSelectionActionProvider(final Context context) {
        super(context);
        activity = (Activity) context;
    }

    @Override
    public boolean hasSubMenu() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        // must return null, otherwise the menu will not work
        return null;
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
        for (final NavigationAppsEnum app : NavigationAppFactory.getInstalledNavigationApps()) {
            if (app.app.isEnabled(geocache)) {
                subMenu.add(Menu.NONE, app.id, Menu.NONE, app.app.getName()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        final CacheNavigationApp app = (CacheNavigationApp) getNavigationAppForId(item.getItemId());
                        app.navigate(activity, geocache);
                        return true;
                    }
                });
            }
        }
    }

    private static App getNavigationAppForId(final int navigationAppId) {
        final List<NavigationAppsEnum> installedNavigationApps = NavigationAppFactory.getInstalledNavigationApps();

        for (final NavigationAppsEnum navigationApp : installedNavigationApps) {
            if (navigationApp.id == navigationAppId) {
                return navigationApp.app;
            }
        }
        // default navigation tool wasn't set already or couldn't be found (not installed any more for example)
        return NavigationAppsEnum.COMPASS.app;
    }

    public static void initialize(final MenuItem menuItem, final Geocache cache) {
        final ActionProvider actionProvider = MenuItemCompat.getActionProvider(menuItem);
        if (actionProvider instanceof NavigationSelectionActionProvider) {
            final NavigationSelectionActionProvider navigateAction = (NavigationSelectionActionProvider) actionProvider;
            navigateAction.setTarget(cache);
        }
    }

}
