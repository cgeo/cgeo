package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.ui.AbstractMenuActionProvider;

import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;

import java.util.List;

public class ListNavigationSelectionActionProvider extends AbstractMenuActionProvider {

    public static interface Callback {
        void onListNavigationSelected(final CacheListApp app);
    }

    private Callback callback;

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
            subMenu.add(Menu.NONE, i, Menu.NONE, app.getName()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    final CacheListApp app = activeApps.get(item.getItemId());
                    callback.onListNavigationSelected(app);
                    return true;
                }
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
