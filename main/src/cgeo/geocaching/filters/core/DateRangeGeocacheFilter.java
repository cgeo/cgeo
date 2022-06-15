package cgeo.geocaching.filters.core;

import org.apache.commons.lang3.time.DateUtils;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

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

    public void setRelativeMinMaxDays(final int daysBeforeToday, final int daysAfterToday) {
        setMinMaxDate(DateUtils.addDays(new Date(), -1 * daysBeforeToday), DateUtils.addDays(new Date(), daysAfterToday));
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        dateFilter.setConfig(config.get(null));
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig config = new ExpressionConfig();
        config.put(null, dateFilter.getConfig());
        return config;
    }

    @Override
    public boolean isFiltering() {
        return dateFilter.isFilled();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        addToSql(sqlBuilder, getSqlColumnName() == null ? null : sqlBuilder.getMainTableId() + "." + getSqlColumnName());
    }

    protected void addToSql(final SqlBuilder sqlBuilder, final String valueExpression) {
        dateFilter.addToSql(sqlBuilder, valueExpression);
    }

    @Override
    protected String getUserDisplayableConfig() {
        return dateFilter.getUserDisplayableConfig();
    }

}
