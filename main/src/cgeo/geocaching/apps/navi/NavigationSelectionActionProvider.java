package cgeo.geocaching.apps.navi;

import cgeo.geocaching.apps.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.AbstractMenuActionProvider;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.core.view.ActionProvider;
import androidx.core.view.MenuItemCompat;

/**
 * Action provider listing all available navigation actions as sub menu.
 */
public class NavigationSelectionActionProvider extends AbstractMenuActionProvider {

    private Geocache geocache;
    private final Activity activity;

    /**
     * Constructor MUST (!) be of type Context, NOT Activity! Otherwise it can't be instantiated by Android infrastructure. See #11251
     */
    public NavigationSelectionActionProvider(final Context context) {
        super(context);

        //try to extract an Activity from given Context
        Context baseContext = context;
        while (baseContext instanceof ContextWrapper && !(baseContext instanceof Activity)) {
            baseContext = ((ContextWrapper) baseContext).getBaseContext();
        }
        this.activity = baseContext instanceof Activity ? (Activity) baseContext : null;
        if (this.activity == null) {
            Log.w("NavigationSelectionActionProvider called with Non-Activity-class: " +
                    (context == null ? "null" : context.getClass().getName()) + "/" +
                    (baseContext == null ? "null" : baseContext.getClass().getName()));
        }
    }

    public void setTarget(final Geocache cache) {
        geocache = cache;
    }

    @Override
    public void onPrepareSubMenu(final SubMenu subMenu) {
        subMenu.clear();
        if (geocache == null || geocache.getCoords() == null) {
            return;
        }
        for (final NavigationAppsEnum app : NavigationAppFactory.getActiveNavigationApps()) {
            if (!(app.app instanceof CacheNavigationApp)) {
                continue;
            }
            final CacheNavigationApp cacheApp = (CacheNavigationApp) app.app;
            if (app.app.isEnabled(geocache) && this.activity != null) {
                subMenu.add(Menu.NONE, app.id, Menu.NONE, app.app.getName()).setOnMenuItemClickListener(item -> {
                    cacheApp.navigate(activity, geocache);
                    return true;
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
