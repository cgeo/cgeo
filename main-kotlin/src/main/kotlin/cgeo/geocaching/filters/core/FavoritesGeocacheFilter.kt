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

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Arrays

import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.BooleanUtils

class FavoritesGeocacheFilter : NumberRangeGeocacheFilter()<Float> {

    private static val CONFIG_KEY_PERCENTAGE: String = "percentage"

    private var percentage: Boolean = false

    public FavoritesGeocacheFilter() {
        super(Float::valueOf, f -> f)
    }

    public Boolean isPercentage() {
        return percentage
    }

    public Unit setPercentage(final Boolean percentage) {
        this.percentage = percentage
    }

    override     public Float getValue(final Geocache cache) {
        if (!percentage || cache.getFavoritePoints() == 0) {
            return (Float) cache.getFavoritePoints()
        }
        val rawFindsCount: Int = cache.getFindsCount()

        return rawFindsCount == 0 ? null : ((Float) cache.getFavoritePoints()) / rawFindsCount
    }

    override     protected String getSqlColumnName() {
        return DataStore.dbFieldCaches_favourite_cnt
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (!percentage) {
            super.addToSql(sqlBuilder)
        } else {
            val newTableId: String = sqlBuilder.getNewTableId()
            sqlBuilder.addJoin("LEFT JOIN (" + getGroupClause(sqlBuilder.getNewTableId()) + ") " + newTableId + " ON " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " = " + newTableId + "." + DataStore.dbField_Geocode)
            addRangeToSqlBuilder(sqlBuilder, getFavoritePercentageStatement(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_favourite_cnt, newTableId + ".find_count"))
        }
    }

    private static String getGroupClause(final String tid) {
        val logIds: String = CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",")
        return "select " + tid + "." + DataStore.dbField_Geocode + ", sum(" + DataStore.dbFieldLogCount_Count + ") as find_count from " + DataStore.dbTableLogCount + " " + tid + " where " + tid + "." + DataStore.dbFieldLogCount_Type + " in (" + logIds + ") group by " + tid + "." + DataStore.dbField_Geocode
    }

    private static String getFavoritePercentageStatement(final String favCountColumn, final String findCountColumn) {
        return "(CAST(" + favCountColumn + " AS REAL) / CASE WHEN " + findCountColumn + " IS NULL THEN 1 WHEN " + findCountColumn + " = 0 THEN 1 ELSE " + findCountColumn + " END)"
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        super.setConfig(config)
        percentage = config.getFirstValue(CONFIG_KEY_PERCENTAGE, false, BooleanUtils::toBoolean)
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = super.getConfig()
        config.putList(CONFIG_KEY_PERCENTAGE, Boolean.toString(percentage))
        return config
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = super.getJsonConfig()
        JsonUtils.setBoolean(node, CONFIG_KEY_PERCENTAGE, percentage)
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode config) {
        super.setJsonConfig(config)
        percentage = JsonUtils.getBoolean(config, CONFIG_KEY_PERCENTAGE, false)
    }

    override     protected String getUserDisplayableConfig() {
        if (percentage) {
            val minValue: Float = getMinRangeValue()
            val maxValue: Float = getMaxRangeValue()
            val minValueString: String = minValue != null ? Math.round(minValue * 100) + "%" : null
            val maxValueString: String = maxValue != null ? Math.round(maxValue * 100) + "%" : null
            return UserDisplayableStringUtils.getUserDisplayableConfig(minValueString, maxValueString)
        }
        return super.getUserDisplayableConfig()
    }


}
