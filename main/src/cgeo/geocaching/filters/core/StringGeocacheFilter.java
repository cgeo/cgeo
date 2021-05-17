package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;


public abstract class StringGeocacheFilter extends BaseGeocacheFilter {

    private final StringFilter stringFilter = new StringFilter();

    protected abstract String getValue(Geocache cache);

    protected String getSqlColumnName() {
        return null;
    }

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
    public void setConfig(final String[] values) {
        stringFilter.setFromConfig(values, 0);
    }

    @Override
    public String[] getConfig() {
        return stringFilter.addToConfig(new String[4], 0);
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final String colName = getSqlColumnName();
        if (colName != null) {
            stringFilter.addToSql(sqlBuilder, sqlBuilder.getMainTableId() + "." + colName);
        } else {
            sqlBuilder.addWhereAlwaysInclude();
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        return stringFilter.getUserDisplayableConfig();
    }

}
