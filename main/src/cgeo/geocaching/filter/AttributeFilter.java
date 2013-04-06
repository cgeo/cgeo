package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;

import android.content.res.Resources;

import java.util.EnumSet;

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
        Geocache fullCache = cgData.loadCache(cache.getGeocode(), EnumSet.of(LoadFlag.LOAD_ATTRIBUTES));
        if (fullCache == null) {
            fullCache = cache;
        }
        return fullCache.getAttributes().contains(attribute);
    }

    public static class Factory implements IFilterFactory {

        @Override
        public IFilter[] getFilters() {
            final String packageName = cgeoapplication.getInstance().getBaseContext().getPackageName();
            final Resources res = cgeoapplication.getInstance().getResources();

            final String[] ids = res.getStringArray(R.array.attribute_ids);
            final IFilter[] filters = new IFilter[ids.length];
            for (int i = 0; i < ids.length; i++) {
                filters[i] = new AttributeFilter(getName("attribute_" + ids[i], res, packageName), ids[i]);
            }
            return filters;
        }

    }
}
