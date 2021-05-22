package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import java.util.Date;


public abstract class DateRangeGeocacheFilter extends BaseGeocacheFilter {

    private final DateFilter dateFilter = new DateFilter();

    protected abstract Date getDate(Geocache cache);

    protected String getSqlColumnName() {
        return null;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        return dateFilter.matches(getDate(cache));
    }

    public Date getMinDate() {
        return dateFilter.getMinDate();
    }

    public Date getMaxDate() {
        return dateFilter.getMaxDate();
    }

    public void setMinMaxDate(final Date min, final Date max) {
        this.dateFilter.setMinMaxDate(min, max);
    }

    @Override
    public void setConfig(final String[] values) {
        dateFilter.setFromConfig(values, 0);
    }

    @Override
    public String[] getConfig() {
        return dateFilter.addToConfig(new String[2], 0);
    }

    @Override
    public boolean isFiltering() {
        return dateFilter.isFilled();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        dateFilter.addToSql(sqlBuilder, getSqlColumnName() == null ? null : sqlBuilder.getMainTableId() + "." + getSqlColumnName());
    }

    @Override
    protected String getUserDisplayableConfig() {
        return dateFilter.getUserDisplayableConfig();
    }

}
