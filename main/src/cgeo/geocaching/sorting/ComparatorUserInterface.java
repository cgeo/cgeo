package cgeo.geocaching.sorting;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RunnableWithArgument;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ComparatorUserInterface {
    private final Activity activity;
    private final ArrayList<ComparatorEntry> registry;
    private final Resources res;

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

    public ComparatorUserInterface(final Activity activity) {
        this.activity = activity;
        res = activity.getResources();

        registry = new ArrayList<ComparatorUserInterface.ComparatorEntry>(20);

        register(R.string.caches_sort_distance, null);
        register(R.string.caches_sort_difficulty, DifficultyComparator.class);
        register(R.string.caches_sort_terrain, TerrainComparator.class);
        register(R.string.caches_sort_size, SizeComparator.class);
        register(R.string.caches_sort_favorites, PopularityComparator.class);
        register(R.string.caches_sort_name, NameComparator.class);
        register(R.string.caches_sort_gccode, GeocodeComparator.class);
        register(R.string.caches_sort_rating, RatingComparator.class);
        register(R.string.caches_sort_vote, VoteComparator.class);
        register(R.string.caches_sort_inventory, InventoryComparator.class);
        register(R.string.caches_sort_date_hidden, DateComparator.class);
        register(R.string.caches_sort_date_logged, VisitComparator.class);
        register(R.string.caches_sort_finds, FindsComparator.class);
        register(R.string.caches_sort_state, StateComparator.class);
        register(R.string.caches_sort_storage, StorageTimeComparator.class);

        // sort the menu labels alphabetically for easier reading
        Collections.sort(registry, new Comparator<ComparatorEntry>() {

            @Override
            public int compare(ComparatorEntry lhs, ComparatorEntry rhs) {
                return lhs.name.compareToIgnoreCase(rhs.name);
            }
        });
    }

    private void register(final int resourceId, Class<? extends CacheComparator> comparatorClass) {
        registry.add(new ComparatorEntry(res.getString(resourceId), comparatorClass));
    }

    public void selectComparator(final CacheComparator current, final RunnableWithArgument<CacheComparator> runAfterwards) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.caches_sort_title);

        // adapter doesn't work correctly here, therefore using the string array based method
        final String[] items = new String[registry.size()];
        for (int i = 0; i < items.length; i++) {
            items[i] = registry.get(i).name;
        }
        builder.setSingleChoiceItems(items, getCurrentIndex(current), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int itemIndex) {
                ComparatorEntry entry = registry.get(itemIndex);
                try {
                    if (entry.cacheComparator == null) {
                        runAfterwards.run(null);
                    }
                    else {
                        CacheComparator comparator = entry.cacheComparator.newInstance();
                        runAfterwards.run(comparator);
                    }
                } catch (Exception e) {
                    Log.e("selectComparator", e);
                }
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private int getCurrentIndex(final CacheComparator current) {
        for (int index = 0; index < registry.size(); index++) {
            final ComparatorEntry entry = registry.get(index);
            if (current == null) {
                if (entry.cacheComparator == null) {
                    return index;
                }
            }
            else if (current.getClass().equals(entry.cacheComparator)) {
                return index;
            }
        }
        return -1;
    }

}
