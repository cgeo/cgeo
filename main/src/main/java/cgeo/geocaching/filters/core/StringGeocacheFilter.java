package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import androidx.annotation.NonNull;


public abstract class StringGeocacheFilter extends BaseGeocacheFilter {

    private final StringFilter stringFilter = new StringFilter();

    protected abstract String getValue(Geocache cache);

    protected String getSqlColumnName() {
        return null;
    }

    @NonNull
    public StringFilter getStringFilter() {
        return stringFilter;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        final String gcValue = getValue(cache);
        if (gcValue == null) {
            return null;
        }
        return stringFilter.matches(gcValue);
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        stringFilter.setConfig(config.get(null));
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig config = new ExpressionConfig();
        config.put(null, stringFilter.getConfig());
        return config;
    }

    @Override
    public boolean isFiltering() {
        return stringFilter.isFilled();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final String colName = getSqlColumnName();
        if (colName != null) {
            stringFilter.addToSql(sqlBuilder, sqlBuilder.getMainTableId() + "." + colName);
        } else {
            sqlBuilder.addWhereTrue();
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        return stringFilter.getUserDisplayableConfig();
    }

}
