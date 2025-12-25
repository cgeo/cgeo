// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.filters.core

import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig
import cgeo.geocaching.log.LogType.ATTENDED
import cgeo.geocaching.log.LogType.FOUND_IT
import cgeo.geocaching.log.LogType.WEBCAM_PHOTO_TAKEN

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Arrays
import java.util.Map

import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.EnumUtils

class LogsCountGeocacheFilter : NumberRangeGeocacheFilter()<Integer> {

    private static val CONFIG_KEY_LOGTYPE: String = "logtype"

    private var logType: LogType = null

    public LogsCountGeocacheFilter() {
        super(Integer::valueOf, Math::round)
    }

    public LogType getLogType() {
        return logType
    }

    public Unit setLogType(final LogType logType) {
        this.logType = logType
    }

    override     public Integer getValue(final Geocache cache) {
        if (cache == null) {
            return null
        }

        val logCounts: Map<LogType, Integer> = cache.getLogCounts()
        if (logCounts.isEmpty()) {
            return null
        }

        final Int sum
        if (logType == null) {
            sum = getLogCountSum(logCounts, null)
        } else if (logType == FOUND_IT) {
            sum = getLogCountSum(logCounts, LogType[]{FOUND_IT, ATTENDED, WEBCAM_PHOTO_TAKEN})
        } else {
            sum = getLogCountSum(logCounts, LogType[]{logType})
        }
        return sum
    }

    private Int getLogCountSum(final Map<LogType, Integer> logCounts, final LogType[] types) {
        Int sum = 0
        LogType[] iterateTypes = types
        if (iterateTypes == null) {
            iterateTypes = LogType.values()
        }
        for (LogType lt : iterateTypes) {
            if (logCounts.containsKey(lt)) {
                sum += logCounts.get(lt)
            }
        }
        return sum
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        val newTableId: String = sqlBuilder.getNewTableId()
        sqlBuilder.addJoin("LEFT JOIN (" + getGroupClause(sqlBuilder.getNewTableId()) + ") " + newTableId + " ON " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " = " + newTableId + "." + DataStore.dbField_Geocode)
        addRangeToSqlBuilder(sqlBuilder,
                "CASE WHEN " + newTableId + ".log_count IS NULL THEN 0 ELSE " + newTableId + ".log_count END")
    }

    private String getGroupClause(final String tid) {
        final String logIds
        if (this.logType == null) {
            logIds = ""
        } else if (this.logType == FOUND_IT) {
            logIds = " where " + tid + "." + DataStore.dbFieldLogCount_Type + " in (" + CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",") + ")"
        } else {
            logIds = " where " + tid + "." + DataStore.dbFieldLogCount_Type + " = " + this.logType.id
        }
        return "select " + tid + "." + DataStore.dbField_Geocode + ", sum(" + DataStore.dbFieldLogCount_Count + ") as log_count from " + DataStore.dbTableLogCount + " " + tid + logIds + " group by " + tid + "." + DataStore.dbField_Geocode
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        super.setConfig(config)
        logType = config.getFirstValue(CONFIG_KEY_LOGTYPE, null, s -> EnumUtils.getEnum(LogType.class, s, null))
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = super.getConfig()
        config.putList(CONFIG_KEY_LOGTYPE, logType == null ? "" : logType.name())
        return config
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = super.getJsonConfig()
        JsonUtils.setText(node, CONFIG_KEY_LOGTYPE, logType == null ? null : logType.name())
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode config) {
        super.setJsonConfig(config)
        logType = EnumUtils.getEnum(LogType.class, JsonUtils.getText(config, CONFIG_KEY_LOGTYPE, null), null)
    }

    override     protected String getUserDisplayableConfig() {
        return (logType == null ? "" : logType.getL10n() + ": ") + super.getUserDisplayableConfig()
    }

}
