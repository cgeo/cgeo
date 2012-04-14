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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

public final class FilterUserInterface {

    private final IAbstractActivity activity;
    private final HashMap<Integer, Class<? extends IFilterFactory>> registry;
    private final Resources res;

    public FilterUserInterface(final IAbstractActivity activity) {
        this.activity = activity;
        this.res = cgeoapplication.getInstance().getResources();

        registry = new HashMap<Integer, Class<? extends IFilterFactory>>();
        if (Settings.getCacheType() == CacheType.ALL) {
            registry.put(R.string.caches_filter_type, TypeFilter.Factory.class);
        }
        registry.put(R.string.caches_filter_size, SizeFilter.Factory.class);
        registry.put(R.string.cache_terrain, TerrainFilter.Factory.class);
        registry.put(R.string.cache_difficulty, DifficultyFilter.Factory.class);
        registry.put(R.string.cache_attributes, AttributeFilter.Factory.class);
        registry.put(R.string.cache_status, StateFilter.Factory.class);
        registry.put(R.string.caches_filter_track, TrackablesFilter.class);
        registry.put(R.string.caches_filter_modified, ModifiedFilter.class);
    }

    public void selectFilter(final RunnableWithArgument<IFilter> runAfterwards) {
        final AlertDialog.Builder builder = new AlertDialog.Builder((Activity) activity);
        builder.setTitle(R.string.caches_filter);

        ArrayList<String> names = new ArrayList<String>(registry.size() + 1);
        for (Entry<Integer, Class<? extends IFilterFactory>> entry : registry.entrySet()) {
            names.add(res.getString(entry.getKey()));
        }
        Collections.sort(names);
        names.add(res.getString(R.string.caches_filter_clear));
        final String[] array = names.toArray(new String[names.size()]);
        builder.setItems(array, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int itemIndex) {
                // no filter selected?
                if (itemIndex >= registry.size()) {
                    runAfterwards.run(null);
                }
                else {
                    final String name = array[itemIndex];
                    for (Entry<Integer, Class<? extends IFilterFactory>> entry : registry.entrySet()) {
                        if (name.equals(res.getString(entry.getKey()))) {
                            Class<? extends IFilterFactory> producer = entry.getValue();
                            try {
                                IFilterFactory factory = producer.newInstance();
                                selectFromFactory(factory, name, runAfterwards);
                            } catch (Exception e) {
                                Log.e("selectFilter", e);
                            }
                            return;
                        }
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

        final String[] names = new String[filters.length];
        for (int i = 0; i < filters.length; i++) {
            names[i] = filters[i].getName();
        }
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                runAfterwards.run(filters[item]);
            }
        });

        builder.create().show();
    }

}
