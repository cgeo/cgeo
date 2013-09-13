package cgeo.geocaching.filter;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;

import android.content.res.Resources;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

class AttributeFilter extends AbstractFilter {

    private final String attribute;

    public AttributeFilter(final String name, final String attribute) {
        super(name);
        this.attribute = attribute;
    }

    private static String getName(final String attribute, final Resources res, final String packageName) {
        // dynamically search for a translation of the attribute
        final int id = res.getIdentifier(attribute, "string", packageName);
        return id > 0 ? res.getString(id) : attribute;
    }

    @Override
    public boolean accepts(final Geocache cache) {
        Geocache fullCache = DataStore.loadCache(cache.getGeocode(), EnumSet.of(LoadFlag.LOAD_ATTRIBUTES));
        if (fullCache == null) {
            fullCache = cache;
        }
        return fullCache.getAttributes().contains(attribute);
    }

    public static class Factory implements IFilterFactory {

        @Override
        public List<IFilter> getFilters() {
            final String packageName = cgeoapplication.getInstance().getBaseContext().getPackageName();
            final Resources res = cgeoapplication.getInstance().getResources();

            final List<IFilter> filters = new LinkedList<IFilter>();
            for (final String id: res.getStringArray(R.array.attribute_ids)) {
                filters.add(new AttributeFilter(getName("attribute_" + id, res, packageName), id));
            }
            return filters;
        }

    }
}
