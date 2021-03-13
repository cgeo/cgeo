package cgeo.geocaching.filters.core;

import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import org.apache.commons.lang3.BooleanUtils;


public class OfflineLogGeocacheFilter extends BaseGeocacheFilter {

    private boolean hasLogFilter = false;
    private final StringFilter logTextFilter = new StringFilter();

    public StringFilter getLogTextFilter() {
        return logTextFilter;
    }

    public boolean hasOfflineLogFilter() {
        return hasLogFilter;
    }

    public void setOfflineLogFilter(final boolean value) {
        this.hasLogFilter = value;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        if (hasLogFilter && !cache.hasLogOffline()) {
            return false;
        }

        if (logTextFilter.isFilled()) {
            final OfflineLogEntry logEntry = cache.getOfflineLog();
            return logEntry != null && logTextFilter.matches(logEntry.log);
        }
        return true;
    }

    @Override
    public void setConfig(final String[] values) {
        hasLogFilter = values.length > 0 ? BooleanUtils.toBoolean(values[0]) : false;
        logTextFilter.setFromConfig(values, 1);
    }

    @Override
    public String[] getConfig() {
        return logTextFilter.addToConfig(new String[]{String.valueOf(hasLogFilter), "", "", ""}, 1);
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (hasLogFilter || logTextFilter.isFilled()) {
            final String logTableId = sqlBuilder.getNewTableId();
            final StringBuilder sb = new StringBuilder("EXISTS ( SELECT geocode FROM cg_logs_offline " + logTableId + " WHERE " + logTableId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode");
            if (logTextFilter.isFilled()) {
                sb.append(" AND ").append(logTextFilter.toSqlExpression(logTableId + ".log"));
            }
            sb.append(")");
            sqlBuilder.addWhere(sb.toString());
        } else {
            sqlBuilder.addWhereAlwaysTrue();
        }
    }
}
