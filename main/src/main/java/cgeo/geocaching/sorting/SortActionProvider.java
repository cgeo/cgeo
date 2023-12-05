package cgeo.geocaching.sorting;

import cgeo.geocaching.utils.functions.Action1;

import android.content.Context;
import android.util.Pair;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.ActionProvider;

/**
 * Provides a sub menu for sorting caches. Register your listener in the onCreateOptionsMenu of the containing activity.
 */
public class SortActionProvider extends ActionProvider implements OnMenuItemClickListener {

    private static final int MENU_GROUP = 1;

    /**
     * Callback triggered on selecting a new sort order.
     */
    private Action1<GeocacheSort.SortType> onClickListener;

    private GeocacheSortContext sortContext;


    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    public SortActionProvider(final Context context) {
        super(context);
    }

    public void setSortContext(final GeocacheSortContext sortContext) {
        this.sortContext = sortContext;
    }


    @Override
    public View onCreateActionView() {
        // must return null, otherwise onPrepareSubMenu is not called
        return null;
    }

    @Override
    public boolean hasSubMenu() {
        return true;
    }

    @Override
    public void onPrepareSubMenu(final SubMenu subMenu) {
        subMenu.clear();
        for (Pair<GeocacheSort.SortType, String> entry : sortContext.getSort().getAvailableTypes()) {
            final int id = entry.first.ordinal();
            final MenuItem menuItem = subMenu.add(MENU_GROUP, id, id, entry.second);
            menuItem.setOnMenuItemClickListener(this).setCheckable(true);
            menuItem.setChecked(entry.first.equals(sortContext.getSort().getType()));
        }

        subMenu.setGroupCheckable(MENU_GROUP, true, true);
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final GeocacheSort.SortType selected = GeocacheSort.SortType.values()[item.getItemId()];
        onSortTypeSelection(selected);
        return true;
    }

    public void setClickListener(@NonNull final Action1<GeocacheSort.SortType> onClickListener) {
        this.onClickListener = onClickListener;
    }

    public boolean onSortTypeSelection(final GeocacheSort.SortType type) {
        onClickListener.call(type);
        return true;
    }
}
