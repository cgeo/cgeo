package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;

import org.apache.commons.lang3.StringUtils;

import android.content.res.Resources;

public class AttributeFilter extends AbstractFilter {

    private final String attribute;

    public AttributeFilter(final String name, final String attribute) {
        super(name);
        this.attribute = attribute;
    }

    private static String getName(final String attribute, final Resources res, final String packageName) {
        // dynamically search for a translation of the attribute
        final int id = res.getIdentifier(attribute, "string", packageName);
        if (id > 0) {
            final String translated = res.getString(id);
            if (StringUtils.isNotBlank(translated)) {
                return translated;
            }
        }
        return attribute;
    }

    @Override
    public boolean accepts(final cgCache cache) {
        final cgCache fullCache = cgeoapplication.getInstance().getCacheByGeocode(cache.getGeocode());
        return fullCache.getAttributes().contains(attribute);
    }

    public static IFilter[] getAllFilters() {
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
