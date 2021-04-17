package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public abstract class OneOfManyGeocacheFilter<T> extends BaseGeocacheFilter {

    private final Set<T> values = new HashSet<>();

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

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final String colName = getSqlColumnName();
        if (colName != null && !getValues().isEmpty()) {
            final StringBuilder sb = new StringBuilder(sqlBuilder.getMainTableId() + "." + colName + " IN (");
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


}
