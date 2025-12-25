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

package cgeo.geocaching.apps.cachelist

import cgeo.geocaching.ui.AbstractMenuActionProvider

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu

import androidx.core.view.ActionProvider
import androidx.core.view.MenuItemCompat

import java.util.List

/**
 * action provider showing a sub menu with all navigation possibilities for a complete list of caches
 */
class ListNavigationSelectionActionProvider : AbstractMenuActionProvider() {

    private Callback callback

    interface Callback {
        Unit onListNavigationSelected(CacheListApp app)
    }

    /**
     * Creates a instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    public ListNavigationSelectionActionProvider(final Context context) {
        super(context)
    }

    public Unit setCallback(final Callback callback) {
        this.callback = callback
    }

    override     public Unit onPrepareSubMenu(final SubMenu subMenu) {
        subMenu.clear()
        if (callback == null) {
            return
        }
        val activeApps: List<CacheListApp> = CacheListApps.getActiveApps()
        for (Int i = 0; i < activeApps.size(); i++) {
            val app: CacheListApp = activeApps.get(i)
            subMenu.add(Menu.NONE, i, Menu.NONE, app.getName()).setOnMenuItemClickListener(item -> {
                callback.onListNavigationSelected(app)
                return true
            })
        }
    }

    public static Unit initialize(final MenuItem menuItem, final Callback callback) {
        val actionProvider: ActionProvider = MenuItemCompat.getActionProvider(menuItem)
        if (actionProvider is ListNavigationSelectionActionProvider) {
            val navigateAction: ListNavigationSelectionActionProvider = (ListNavigationSelectionActionProvider) actionProvider
            navigateAction.setCallback(callback)
        }
    }

}
