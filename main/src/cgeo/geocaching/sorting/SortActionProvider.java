package cgeo.geocaching.sorting;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.content.Context;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.view.ActionProvider;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Provides a sub menu for sorting caches. Register your listener in the onCreateOptionsMenu of the containing activity.
 *
 */
public class SortActionProvider extends ActionProvider implements OnMenuItemClickListener {

    private static final int MENU_GROUP = 1;
    private final Context context;
    private final ArrayList<ComparatorEntry> registry = new ArrayList<>(20);
    /**
     * Callback triggered on selecting a new sort order.
     */
    private Action1<CacheComparator> onClickListener;
    /**
     * Currently selected filter. Used for radio button indication.
     */
    private CacheComparator selection;

    // Used to change menu Filter label
    private boolean isEventsOnly = false;

    private static final class ComparatorEntry {
        private final String name;
        private final Class<? extends CacheComparator> cacheComparator;

        ComparatorEntry(final String name, final Class<? extends CacheComparator> cacheComparator) {
            this.name = name;
            this.cacheComparator = cacheComparator;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context
     *            Context for accessing resources.
     */
    public SortActionProvider(final Context context) {
        super(context);
        this.context = context;
    }

    private void register(@StringRes final int resourceId, final Class<? extends CacheComparator> comparatorClass) {
        registry.add(new ComparatorEntry(context.getString(resourceId), comparatorClass));
    }

    private void registerComparators() {
        registry.clear();
        register(R.string.caches_sort_distance, DistanceComparator.class);
        if (isEventsOnly) {
            register(R.string.caches_sort_eventdate, EventDateComparator.class);
        } else {
            register(R.string.caches_sort_date_hidden, DateComparator.class);
        }
        register(R.string.caches_sort_difficulty, DifficultyComparator.class);
        register(R.string.caches_sort_finds, FindsComparator.class);
        register(R.string.caches_sort_geocode, GeocodeComparator.class);
        register(R.string.caches_sort_inventory, InventoryComparator.class);
        register(R.string.caches_sort_name, NameComparator.class);
        register(R.string.caches_sort_favorites, PopularityComparator.class);
        register(R.string.caches_sort_favorites_ratio, PopularityRatioComparator.class);
        register(R.string.caches_sort_rating, RatingComparator.class);
        register(R.string.caches_sort_size, SizeComparator.class);
        register(R.string.caches_sort_state, StateComparator.class);
        register(R.string.caches_sort_storage, StorageTimeComparator.class);
        register(R.string.caches_sort_terrain, TerrainComparator.class);
        register(R.string.caches_sort_date_logged, VisitComparator.class);
        register(R.string.caches_sort_vote, VoteComparator.class);

        // sort the menu labels alphabetically for easier reading
        Collections.sort(registry, (lhs, rhs) -> TextUtils.COLLATOR.compare(lhs.name, rhs.name));
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
        registerComparators();
        for (int i = 0; i < registry.size(); i++) {
            final ComparatorEntry comparatorEntry = registry.get(i);
            final MenuItem menuItem = subMenu.add(MENU_GROUP, i, i, comparatorEntry.name);
            menuItem.setOnMenuItemClickListener(this).setCheckable(true);
            if (selection == null) {
                if (comparatorEntry.cacheComparator == null) {
                    menuItem.setChecked(true);
                }
            } else if (selection.getClass().equals(comparatorEntry.cacheComparator)) {
                menuItem.setChecked(true);
            }
        }
        subMenu.setGroupCheckable(MENU_GROUP, true, true);
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        callListener(registry.get(item.getItemId()).cacheComparator);
        return true;
    }

    private void callListener(final Class<? extends CacheComparator> cacheComparator) {
        try {
            if (cacheComparator == null) {
                onClickListener.call(null);
            } else {
                final CacheComparator comparator = cacheComparator.newInstance();
                onClickListener.call(comparator);
            }
        } catch (Exception e) { // no multi-catch below SDK 19
            Log.e("selectComparator", e);
        }
    }

    public void setClickListener(@NonNull final Action1<CacheComparator> onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setSelection(final CacheComparator selection) {
        this.selection = selection;
    }

    public void setIsEventsOnly(final boolean isEventsOnly) {
        this.isEventsOnly = isEventsOnly;
    }
}
