package cgeo.geocaching.filters.core;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.expressions.ExpressionConfig;
import static cgeo.geocaching.log.LogType.ATTENDED;
import static cgeo.geocaching.log.LogType.FOUND_IT;
import static cgeo.geocaching.log.LogType.WEBCAM_PHOTO_TAKEN;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;

public class LogsCountGeocacheFilter extends NumberRangeGeocacheFilter<Integer> {

    private static final String CONFIG_KEY_LOGTYPE = "logtype";

    private LogType logType = null;

    public LogsCountGeocacheFilter() {
        super(Integer::valueOf);
    }

    public LogType getLogType() {
        return logType;
    }

    public void setLogType(final LogType logType) {
        this.logType = logType;
    }

    @Override
    public Integer getValue(final Geocache cache) {
        if (cache == null) {
            return null;
        }

        final Map<LogType, Integer> logCounts = cache.getLogCounts();
        if (logCounts.isEmpty()) {
            return null;
        }

        final int sum;
        if (logType == null) {
            sum = getLogCountSum(logCounts, null);
        } else if (logType == FOUND_IT) {
            sum = getLogCountSum(logCounts, new LogType[]{FOUND_IT, ATTENDED, WEBCAM_PHOTO_TAKEN});
        } else {
            sum = getLogCountSum(logCounts, new LogType[]{logType});
        }
        return sum;
    }

    private int getLogCountSum(final Map<LogType, Integer> logCounts, final LogType[] types) {
        int sum = 0;
        LogType[] iterateTypes = types;
        if (iterateTypes == null) {
            iterateTypes = LogType.values();
        }
        for (LogType lt : iterateTypes) {
            if (logCounts.containsKey(lt)) {
                sum += logCounts.get(lt);
            }
        }
        return sum;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final String newTableId = sqlBuilder.getNewTableId();
        sqlBuilder.addJoin("LEFT JOIN (" + getGroupClause(sqlBuilder.getNewTableId()) + ") " + newTableId + " ON " + sqlBuilder.getMainTableId() + ".geocode = " + newTableId + ".geocode");
        addRangeToSqlBuilder(sqlBuilder,
                "CASE WHEN " + newTableId + ".log_count IS NULL THEN 0 ELSE " + newTableId + ".log_count END");
    }

    private String getGroupClause(final String tid) {
        final String logIds;
        if (this.logType == null) {
            logIds = "";
        } else if (this.logType == FOUND_IT) {
            logIds = " where " + tid + ".type in (" + CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",") + ")";
        } else {
            logIds = " where " + tid + ".type = " + this.logType.id;
        }
        return "select " + tid + ".geocode, sum(count) as log_count from cg_logCount " + tid + logIds + " group by " + tid + ".geocode";
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        super.setConfig(config);
        logType = config.getFirstValue(CONFIG_KEY_LOGTYPE, null, s -> EnumUtils.getEnum(LogType.class, s, null));
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig config = super.getConfig();
        config.putList(CONFIG_KEY_LOGTYPE, logType == null ? "" : logType.name());
        return config;
    }

    @Override
    protected String getUserDisplayableConfig() {
        return (logType == null ? "" : logType.getL10n() + ": ") + super.getUserDisplayableConfig();
    }

}
