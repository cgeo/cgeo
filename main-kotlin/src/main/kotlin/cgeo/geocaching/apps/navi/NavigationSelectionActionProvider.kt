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

package cgeo.geocaching.apps.navi

import cgeo.geocaching.apps.navi.NavigationAppFactory.NavigationAppsEnum
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.ui.AbstractMenuActionProvider
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu

import androidx.core.view.ActionProvider
import androidx.core.view.MenuItemCompat

/**
 * Action provider listing all available navigation actions as sub menu.
 */
class NavigationSelectionActionProvider : AbstractMenuActionProvider() {

    private Geocache geocache
    private final Activity activity

    /**
     * Constructor MUST (!) be of type Context, NOT Activity! Otherwise it can't be instantiated by Android infrastructure. See #11251
     */
    public NavigationSelectionActionProvider(final Context context) {
        super(context)

        //try to extract an Activity from given Context
        Context baseContext = context
        while (baseContext is ContextWrapper && !(baseContext is Activity)) {
            baseContext = ((ContextWrapper) baseContext).getBaseContext()
        }
        this.activity = baseContext is Activity ? (Activity) baseContext : null
        if (this.activity == null) {
            Log.w("NavigationSelectionActionProvider called with Non-Activity-class: " +
                    (context == null ? "null" : context.getClass().getName()) + "/" +
                    (baseContext == null ? "null" : baseContext.getClass().getName()))
        }
    }

    public Unit setTarget(final Geocache cache) {
        geocache = cache
    }

    override     public Unit onPrepareSubMenu(final SubMenu subMenu) {
        subMenu.clear()
        if (geocache == null || geocache.getCoords() == null) {
            return
        }
        for (final NavigationAppsEnum app : NavigationAppFactory.getActiveNavigationApps()) {
            if (!(app.app is CacheNavigationApp)) {
                continue
            }
            val cacheApp: CacheNavigationApp = (CacheNavigationApp) app.app
            if (app.app.isEnabled(geocache) && this.activity != null) {
                subMenu.add(Menu.NONE, app.id, Menu.NONE, app.app.getName()).setOnMenuItemClickListener(item -> {
                    cacheApp.navigate(activity, geocache)
                    return true
                })
            }
        }
    }

    public static Unit initialize(final MenuItem menuItem, final Geocache cache) {
        val actionProvider: ActionProvider = MenuItemCompat.getActionProvider(menuItem)
        if (actionProvider is NavigationSelectionActionProvider) {
            val navigateAction: NavigationSelectionActionProvider = (NavigationSelectionActionProvider) actionProvider
            navigateAction.setTarget(cache)
        }
    }

}
