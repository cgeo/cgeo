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

import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

class DifficultyAndTerrainGeocacheFilter : BaseGeocacheFilter() {

    public DifficultyGeocacheFilter difficultyGeocacheFilter
    public TerrainGeocacheFilter terrainGeocacheFilter

    private static val CONFIG_KEY_DIFFICULTY: String = "d"
    private static val CONFIG_KEY_TERRAIN: String = "t"


    public DifficultyAndTerrainGeocacheFilter() {
        difficultyGeocacheFilter = GeocacheFilterType.DIFFICULTY.create()
        terrainGeocacheFilter = GeocacheFilterType.TERRAIN.create()
    }

    override     public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null
        }

        val diffRange: Boolean = difficultyGeocacheFilter.filter(cache)
        val terrainRange: Boolean = terrainGeocacheFilter.filter(cache)
        if (diffRange == null) {
            return terrainRange
        }
        if (terrainRange == null) {
            return diffRange
        }

        return diffRange && terrainRange
    }

    override     public Boolean isFiltering() {
        return difficultyGeocacheFilter.isFiltering() || terrainGeocacheFilter.isFiltering()
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        difficultyGeocacheFilter.addToSql(sqlBuilder)
        terrainGeocacheFilter.addToSql(sqlBuilder)
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        setToFilterConfig(difficultyGeocacheFilter, CONFIG_KEY_DIFFICULTY, config)
        setToFilterConfig(terrainGeocacheFilter, CONFIG_KEY_TERRAIN, config)
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        addFromFilterConfig(difficultyGeocacheFilter, CONFIG_KEY_DIFFICULTY, config)
        addFromFilterConfig(terrainGeocacheFilter, CONFIG_KEY_TERRAIN, config)
        return config
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.set(node, CONFIG_KEY_DIFFICULTY, difficultyGeocacheFilter.getJsonConfig())
        JsonUtils.set(node, CONFIG_KEY_TERRAIN, terrainGeocacheFilter.getJsonConfig())
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        val diffConfig: JsonNode = JsonUtils.get(node, CONFIG_KEY_DIFFICULTY)
        if (diffConfig is ObjectNode) {
            difficultyGeocacheFilter.setJsonConfig((ObjectNode) diffConfig)
        }
        val terrainConfig: JsonNode = JsonUtils.get(node, CONFIG_KEY_TERRAIN)
        if (terrainConfig is ObjectNode) {
            terrainGeocacheFilter.setJsonConfig((ObjectNode) terrainConfig)
        }
    }

    override     protected String getUserDisplayableConfig() {
        return difficultyGeocacheFilter.getUserDisplayableConfig() + " / " + terrainGeocacheFilter.getUserDisplayableConfig()
    }

    private static Unit addFromFilterConfig(final NumberRangeGeocacheFilter<?> filter, final String key, final LegacyFilterConfig config) {
        config.put(key, filter.getConfig().getDefaultList())
    }

    private static Unit setToFilterConfig(final NumberRangeGeocacheFilter<?> filter, final String key, final LegacyFilterConfig config) {
        filter.setConfig(config.getSubConfig(key))
    }
}
