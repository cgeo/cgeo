package cgeo.geocaching.sorting;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

import org.eclipse.jdt.annotation.NonNull;

import rx.functions.Action1;

import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Provides a sub menu for sorting caches. Register your listener in the onCreateOptionsMenu of the containing activity.
 *
 */
public class SortActionProvider extends ActionProvider implements OnMenuItemClickListener {

    private static final int MENU_GROUP = 1;
    private final Context mContext;
    private final ArrayList<ComparatorEntry> registry = new ArrayList<>(20);
    /**
     * Callback triggered on selecting a new sort order.
     */
    private Action1<CacheComparator> onClickListener;
    /**
     * Currently selected filter. Used for radio button indication.
     */
    private CacheComparator selection;

    private static final class ComparatorEntry {
        private final String name;
        private final Class<? extends CacheComparator> cacheComparator;

        public ComparatorEntry(final String name, final Class<? extends CacheComparator> cacheComparator) {
            this.name = name;
            this.cacheComparator = cacheComparator;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public SortActionProvider(final Context context) {
        super(context);
        mContext = context;
        registerComparators();
    }

    private void register(final int resourceId, final Class<? extends CacheComparator> comparatorClass) {
        registry.add(new ComparatorEntry(mContext.getString(resourceId), comparatorClass));
    }

    private void registerComparators() {
        register(R.string.caches_sort_distance, null);
        register(R.string.caches_sort_date_hidden, DateComparator.class);
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
        Collections.sort(registry, new Comparator<ComparatorEntry>() {

            @Override
            public int compare(final ComparatorEntry lhs, final ComparatorEntry rhs) {
                return lhs.name.compareToIgnoreCase(rhs.name);
            }
        });
    }

    @Override
    public View onCreateActionView(){
        // must return null, otherwise onPrepareSubMenu is not called
        return null;
    }

    @Override
    public boolean hasSubMenu(){
        return true;
    }

    @Override
    public void onPrepareSubMenu(final SubMenu subMenu){
        subMenu.clear();
        for (int i = 0; i < registry.size(); i++) {
            final ComparatorEntry comparatorEntry = registry.get(i);
            final MenuItem menuItem = subMenu.add(MENU_GROUP, i, i, comparatorEntry.name);
            menuItem.setOnMenuItemClickListener(this).setCheckable(true);
            if (selection == null) {
                if (comparatorEntry.cacheComparator == null) {
                    menuItem.setChecked(true);
                }
            }
            else {
                if (selection.getClass().equals(comparatorEntry.cacheComparator)) {
                    menuItem.setChecked(true);
                }
            }
        }
        subMenu.setGroupCheckable(MENU_GROUP, true, true);
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item){
        callListener(registry.get(item.getItemId()).cacheComparator);
        return true;
    }

    private void callListener(final Class<? extends CacheComparator> cacheComparator) {
        try {
            if (cacheComparator == null) {
                onClickListener.call(null);
            }
            else {
                final CacheComparator comparator = cacheComparator.newInstance();
                onClickListener.call(comparator);
            }
        } catch (final InstantiationException e) {
            Log.e("selectComparator", e);
        } catch (final IllegalAccessException e) {
            Log.e("selectComparator", e);
        }
    }

    public void setClickListener(final @NonNull Action1<CacheComparator> onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setSelection(final CacheComparator selection) {
        this.selection = selection;
    }
}