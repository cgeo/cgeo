package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.IAbstractActivity;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RunnableWithArgument;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class FilterUserInterface {

    private static class FactoryEntry {
        private final String name;
        private final Class<? extends IFilterFactory> filterFactory;

        public FactoryEntry(final String name, final Class<? extends IFilterFactory> filterFactory) {
            this.name = name;
            this.filterFactory = filterFactory;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final IAbstractActivity activity;
    private final ArrayList<FactoryEntry> registry;
    private final Resources res;

    public FilterUserInterface(final IAbstractActivity activity) {
        this.activity = activity;
        this.res = cgeoapplication.getInstance().getResources();

        registry = new ArrayList<FactoryEntry>();
        if (Settings.getCacheType() == CacheType.ALL) {
            register(R.string.caches_filter_type, TypeFilter.Factory.class);
        }
        register(R.string.caches_filter_size, SizeFilter.Factory.class);
        register(R.string.cache_terrain, TerrainFilter.Factory.class);
        register(R.string.cache_difficulty, DifficultyFilter.Factory.class);
        register(R.string.cache_attributes, AttributeFilter.Factory.class);
        register(R.string.cache_status, StateFilter.Factory.class);
        register(R.string.caches_filter_track, TrackablesFilter.class);
        register(R.string.caches_filter_modified, ModifiedFilter.class);

        // sort by localized names
        Collections.sort(registry, new Comparator<FactoryEntry>() {

            @Override
            public int compare(FactoryEntry lhs, FactoryEntry rhs) {
                return lhs.name.compareToIgnoreCase(rhs.name);
            }
        });

        // reset shall be last
        register(R.string.caches_filter_clear, null);
    }

    private void register(int resourceId, Class<? extends IFilterFactory> factoryClass) {
        registry.add(new FactoryEntry(res.getString(resourceId), factoryClass));
    }

    public void selectFilter(final RunnableWithArgument<IFilter> runAfterwards) {
        final AlertDialog.Builder builder = new AlertDialog.Builder((Activity) activity);
        builder.setTitle(R.string.caches_filter);

        final ArrayAdapter<FactoryEntry> adapter = new ArrayAdapter<FactoryEntry>((Activity) activity, android.R.layout.select_dialog_item, registry);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int itemIndex) {
                FactoryEntry entry = adapter.getItem(itemIndex);
                // reset?
                if (entry.filterFactory == null) {
                    runAfterwards.run(null);
                }
                else {
                    try {
                        IFilterFactory factoryInstance = entry.filterFactory.newInstance();
                        selectFromFactory(factoryInstance, entry.name, runAfterwards);
                    } catch (Exception e) {
                        Log.e("selectFilter", e);
                    }
                }
            }
        });

        builder.create().show();
    }

    private void selectFromFactory(final IFilterFactory factory, final String menuTitle, final RunnableWithArgument<IFilter> runAfterwards) {
        final IFilter[] filters = factory.getFilters();
        if (filters.length == 1) {
            runAfterwards.run(filters[0]);
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder((Activity) activity);
        builder.setTitle(menuTitle);

        final ArrayAdapter<IFilter> adapter = new ArrayAdapter<IFilter>((Activity) activity, android.R.layout.select_dialog_item, filters);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                runAfterwards.run(filters[item]);
            }
        });

        builder.create().show();
    }

}
