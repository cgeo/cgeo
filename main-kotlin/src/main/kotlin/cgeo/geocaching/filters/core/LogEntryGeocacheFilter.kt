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

import cgeo.geocaching.SearchCacheData
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List
import java.util.Set

import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.BooleanUtils


class LogEntryGeocacheFilter : BaseGeocacheFilter() {

    private static val CONFIG_KEY_INVERSE: String = "inverse"
    private static val CONFIG_KEY_LOG_TEXT: String = "logtext"


    private val foundByFilter: StringFilter = StringFilter()
    private var inverse: Boolean = false
    private val logTextFilter: StringFilter = StringFilter()

    public Boolean isInverse() {
        return inverse
    }

    public Unit setInverse(final Boolean inverse) {
        this.inverse = inverse
    }

    public String getFoundByUser() {
        return foundByFilter.getTextValue()
    }

    public Unit setFoundByUser(final String foundByUser) {
        this.foundByFilter.setTextValue(foundByUser)
    }

    public String getLogText() {
        return logTextFilter.getTextValue()
    }

    public Unit setLogText(final String logText) {
        this.logTextFilter.setTextValue(logText)
    }

    override     public Boolean filter(final Geocache cache) {

        //Check against result of online search
        val scd: SearchCacheData = cache.getSearchData()
        if (scd != null && foundByFilter.isFilled()) {
            val checkSet: Set<String> = inverse ? scd.getNotFoundBy() : scd.getFoundBy()
            for (String check : checkSet) {
                if (foundByFilter.matches(check)) {
                    return true
                }
            }
        }

        // Check against offline stored log data
        val logEntries: List<LogEntry> = cache.getLogs()
        if (logEntries.isEmpty() && !cache.inDatabase()) {
            return inverse ? true : null; //if inverse=true then this might be a gc.com search
        }
        for (LogEntry logEntry : logEntries) {
            if (foundByFilter.matches(logEntry.author) && logTextFilter.matches(logEntry.log)) {
                return !inverse
            }
        }
        return inverse
    }


    override     public Boolean isFiltering() {
        return foundByFilter.isFilled() || logTextFilter.isFilled()
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (!isFiltering()) {
            sqlBuilder.addWhereTrue()
            return
        }

        val tid: String = sqlBuilder.getNewTableId()
        val sb: StringBuilder = StringBuilder()
        if (inverse) {
            sb.append("NOT ")
        }
        sb.append("EXISTS( SELECT ").append(tid).append(".").append(DataStore.dbField_Geocode).append(" FROM ").append(DataStore.dbTableLogs).append(" ").append(tid).append(" WHERE ").append(sqlBuilder.getMainTableId()).append(".").append(DataStore.dbField_Geocode).append(" = ").append(tid).append(".").append(DataStore.dbField_Geocode)
        if (foundByFilter.isFilled()) {
            sb.append(" AND ").append(foundByFilter.getRawLikeSqlExpression(DataStore.dbFieldLogs_author))
        }
        if (logTextFilter.isFilled()) {
            sb.append(" AND ").append(logTextFilter.getRawLikeSqlExpression(DataStore.dbFieldLogs_log))
        }
        sb.append(")")
        sqlBuilder.addWhere(sb.toString())
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        foundByFilter.setConfig(config.getDefaultList())
        inverse = config.getFirstValue(CONFIG_KEY_INVERSE, false, BooleanUtils::toBoolean)
        logTextFilter.setConfig(config.get(CONFIG_KEY_LOG_TEXT))
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        config.putDefaultList(foundByFilter.getConfig())
        config.putList(CONFIG_KEY_INVERSE, Boolean.toString(inverse))
        config.put(CONFIG_KEY_LOG_TEXT, logTextFilter.getConfig())
        return config
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.set(node, "foundBy", foundByFilter.getJsonConfig())
        JsonUtils.setBoolean(node, CONFIG_KEY_INVERSE, inverse)
        JsonUtils.set(node, "logText", logTextFilter.getJsonConfig())
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        foundByFilter.setJsonConfig(JsonUtils.get(node, "foundBy"))
        inverse = JsonUtils.getBoolean(node, CONFIG_KEY_INVERSE, false)
        logTextFilter.setJsonConfig(JsonUtils.get(node, "logText"))
    }

    override     protected String getUserDisplayableConfig() {
        return (inverse ? "-(" : "") +
                (foundByFilter.isFilled() ? foundByFilter.getTextValue() : "") + ":" +
                (logTextFilter.isFilled() ? logTextFilter.getTextValue() : "") +
                (inverse ? ")" : "")
    }
}
