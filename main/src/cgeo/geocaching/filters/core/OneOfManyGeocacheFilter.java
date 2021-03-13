package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import java.util.HashSet;
import java.util.Set;


public abstract class OneOfManyGeocacheFilter<T> extends BaseGeocacheFilter {

    private final Set<T> values = new HashSet<>();

    public abstract T getValue(Geocache cache);

    public abstract T valueFromString(String stringValue);

    public abstract String valueToString(T value);

    public Set<T> getValues() {
        return values;
    }

    public void setValues(final Set<T> set) {
        values.clear();
        values.addAll(set);
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        final T gcValue = getValue(cache);
        if (gcValue == null) {
            return null;
        }
        return values.isEmpty() || values.contains(gcValue);
    }

    @Override
    public void setConfig(final String[] value) {
        values.clear();
        for (String v : value) {
            values.add(valueFromString(v));
        }
    }

    @Override
    public String[] getConfig() {
        final String[] result = new String[values.size()];
        int idx = 0;
        for (T v : this.values) {
            result[idx++] = valueToString(v);
        }
        return result;
    }
}
