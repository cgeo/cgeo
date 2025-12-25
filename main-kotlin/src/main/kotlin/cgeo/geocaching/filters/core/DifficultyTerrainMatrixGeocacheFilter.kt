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

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Set

import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.tuple.ImmutablePair

class DifficultyTerrainMatrixGeocacheFilter : BaseGeocacheFilter() {

    private static val CONFIG_KEY_INCLUDE_CACHES_WO_DT: String = "include-wo-dt"

    private val difficultyTerrainCombis: Set<String> = HashSet<>()

    private var includeCachesWoDt: Boolean = true

    public Unit clearDtCombis() {
        difficultyTerrainCombis.clear()
    }

    public Unit addDtCombi(final Float difficulty, final Float terrain) {
        difficultyTerrainCombis.add(getDtCombiString(difficulty, terrain))
    }

    public Boolean hasDtCombi(final Float difficulty, final Float terrain) {
        return difficultyTerrainCombis.contains(getDtCombiString(difficulty, terrain))
    }

    public Set<ImmutablePair<Float, Float>> getDtCombis() {
        final Set<ImmutablePair<Float, Float>> result = HashSet<>()
        for (String combi : difficultyTerrainCombis) {
            final String[] parts = combi.split("-")
            val d: Float = Float.parseFloat(parts[0])
            val t: Float = Float.parseFloat(parts[1])
            result.add(ImmutablePair<>(d, t))
        }
        return result
    }

    private static String getDtCombiString(final Float difficulty, final Float terrain) {
        return String.format(Locale.US, "%1.1f-%1.1f", difficulty, terrain)
    }

    public Unit setIncludeCachesWoDt(final Boolean includeCachesWoDt) {
        this.includeCachesWoDt = includeCachesWoDt
    }

    public Boolean isIncludeCachesWoDt() {
        return this.includeCachesWoDt
    }


    override     public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null
        }
        if (!isFilteringOffline()) {
            return true
        }

        val difficulty: Float = cache.getDifficulty()
        val terrain: Float = cache.getTerrain()
        if (difficulty <= 0 || terrain <= 0) {
            return includeCachesWoDt
        }

        return difficultyTerrainCombis.isEmpty() || difficultyTerrainCombis.contains(getDtCombiString(difficulty, terrain))
    }

    override     public Boolean isFiltering() {
        return isFilteringOffline()
    }

    private Boolean isFilteringOffline() {
        return isFilteringMatrix() || !includeCachesWoDt
    }

    public Boolean isFilteringMatrix() {
        return !difficultyTerrainCombis.isEmpty() && difficultyTerrainCombis.size() < 81
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (!isFilteringOffline()) {
            sqlBuilder.addWhereTrue()
        } else if (isFilteringMatrix()) {
            if (includeCachesWoDt) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR)
            }
            val where: StringBuilder = StringBuilder("printf(\"%.1f-%.1f\", difficulty, terrain) IN (")
            val params: List<String> = ArrayList<>()
            Boolean first = true
            for (String dtCombi : difficultyTerrainCombis) {
                if (!first) {
                    where.append(",")
                }
                first = false
                where.append("?")
                params.add(dtCombi)
            }
            where.append(")")
            sqlBuilder.addWhere(where.toString(), params)
            if (includeCachesWoDt) {
                sqlBuilder.addWhere("difficulty <= 0 AND terrain <= 0")
                sqlBuilder.closeWhere()
            }
        } else if (!includeCachesWoDt) {
            sqlBuilder.addWhere("difficulty > 0 AND terrain > 0")
        }
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        this.includeCachesWoDt = config.getFirstValue(CONFIG_KEY_INCLUDE_CACHES_WO_DT, false, BooleanUtils::toBoolean)
        this.difficultyTerrainCombis.clear()
        this.difficultyTerrainCombis.addAll(config.getDefaultList())
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        config.putList(CONFIG_KEY_INCLUDE_CACHES_WO_DT, Boolean.toString(includeCachesWoDt))
        if (isFilteringMatrix()) {
            config.putDefaultList(ArrayList<>(this.difficultyTerrainCombis))
        }
        return config
    }

    override     protected String getUserDisplayableConfig() {
        if (difficultyTerrainCombis.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
        }
        if (difficultyTerrainCombis.size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, difficultyTerrainCombis.size())
        }

        return difficultyTerrainCombis.iterator().next()
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setTextCollection(node, "combis", difficultyTerrainCombis)
        JsonUtils.setBoolean(node, CONFIG_KEY_INCLUDE_CACHES_WO_DT, includeCachesWoDt)
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        this.difficultyTerrainCombis.clear()
        this.difficultyTerrainCombis.addAll(JsonUtils.getTextList(node, "combis"))
        this.includeCachesWoDt = JsonUtils.getBoolean(node, CONFIG_KEY_INCLUDE_CACHES_WO_DT, true)
    }
}
