package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;


public abstract class ValueGroupGeocacheFilter<T> extends BaseGeocacheFilter {

    private final Set<T> values = new HashSet<>();
    private boolean inverse = false;

    public abstract T getValue(Geocache cache);

    public abstract T valueFromString(String stringValue);

    public abstract String valueToString(T value);

    public String getSqlColumnName() {
        return null;
    }

    public String valueToSqlValue(final T value) {
        return value == null ? null : String.valueOf(value);
    }

    public Set<T> getValues() {
        return values;
    }

    public void setValues(final Collection<T> set) {
        values.clear();
        values.addAll(set);
    }

    public void setInverse(final boolean inverse) {
        this.inverse = inverse;
    }

    public boolean getInverse() {
        return this.inverse;
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
        return values.contains(gcValue) != inverse;
    }

    @Override
    public void setConfig(final String[] value) {
        this.inverse = value != null && value.length >= 1 && BooleanUtils.toBoolean(value[0]);
        values.clear();
        for (int pos = 1; pos < value.length; pos ++) {
            values.add(valueFromString(value[pos]));
        }
    }

    @Override
    public String[] getConfig() {
        final String[] result = new String[values.size() + 1];
        result[0] = BooleanUtils.toStringTrueFalse(this.inverse);
        int idx = 1;
        for (T v : this.values) {
            result[idx++] = valueToString(v);
        }
        return result;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final String colName = getSqlColumnName();
        if (colName != null && !getValues().isEmpty()) {
            final StringBuilder sb = new StringBuilder(sqlBuilder.getMainTableId() + "." + colName + " " + (inverse ? "NOT " : "") + "IN (");
            final List<String> params = new ArrayList<>();
            boolean first = true;
            for (T v : getValues()) {
                if (v == null) {
                    continue;
                }
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("?");
                params.add(valueToSqlValue(v));
            }
            sb.append(")");
            sqlBuilder.addWhere(sb.toString(), params);
        } else {
            sqlBuilder.addWhereAlwaysInclude();
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        if (getValues().isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (getValues().size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, getValues().size());
        }

        return valueToString(getValues().iterator().next());
    }


}
