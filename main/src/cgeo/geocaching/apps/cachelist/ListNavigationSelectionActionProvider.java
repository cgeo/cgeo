package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.ui.AbstractMenuActionProvider;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.core.view.ActionProvider;
import androidx.core.view.MenuItemCompat;

import java.util.List;

/**
 * action provider showing a sub menu with all navigation possibilities for a complete list of caches
 */
public class ListNavigationSelectionActionProvider extends AbstractMenuActionProvider {

    private Callback callback;

    public interface Callback {
        void onListNavigationSelected(CacheListApp app);
    }

    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    public ListNavigationSelectionActionProvider(final Context context) {
        super(context);
    }

    public void setCallback(final Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onPrepareSubMenu(final SubMenu subMenu) {
        subMenu.clear();
        if (callback == null) {
            return;
        }
        final List<CacheListApp> activeApps = CacheListApps.getActiveApps();
        for (int i = 0; i < activeApps.size(); i++) {
            final CacheListApp app = activeApps.get(i);
            subMenu.add(Menu.NONE, i, Menu.NONE, app.getName()).setOnMenuItemClickListener(item -> {
                callback.onListNavigationSelected(app);
                return true;
            });
        }
    }

    public static void initialize(final MenuItem menuItem, final Callback callback) {
        final ActionProvider actionProvider = MenuItemCompat.getActionProvider(menuItem);
        if (actionProvider instanceof ListNavigationSelectionActionProvider) {
            final ListNavigationSelectionActionProvider navigateAction = (ListNavigationSelectionActionProvider) actionProvider;
            navigateAction.setCallback(callback);
        }
    }

}
