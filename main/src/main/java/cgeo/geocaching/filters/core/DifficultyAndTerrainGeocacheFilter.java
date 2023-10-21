package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.config.LegacyFilterConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DifficultyAndTerrainGeocacheFilter extends BaseGeocacheFilter {

    public DifficultyGeocacheFilter difficultyGeocacheFilter;
    public TerrainGeocacheFilter terrainGeocacheFilter;

    private static final String CONFIG_KEY_DIFFICULTY = "d";
    private static final String CONFIG_KEY_TERRAIN = "t";


    public DifficultyAndTerrainGeocacheFilter() {
        difficultyGeocacheFilter = GeocacheFilterType.DIFFICULTY.create();
        terrainGeocacheFilter = GeocacheFilterType.TERRAIN.create();
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }

        final Boolean diffRange = difficultyGeocacheFilter.filter(cache);
        final Boolean terrainRange = terrainGeocacheFilter.filter(cache);
        if (diffRange == null) {
            return terrainRange;
        }
        if (terrainRange == null) {
            return diffRange;
        }

        return diffRange && terrainRange;
    }

    @Override
    public boolean isFiltering() {
        return difficultyGeocacheFilter.isFiltering() || terrainGeocacheFilter.isFiltering();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        difficultyGeocacheFilter.addToSql(sqlBuilder);
        terrainGeocacheFilter.addToSql(sqlBuilder);
    }

    @Override
    public void setConfig(final LegacyFilterConfig config) {
        setToFilterConfig(difficultyGeocacheFilter, CONFIG_KEY_DIFFICULTY, config);
        setToFilterConfig(terrainGeocacheFilter, CONFIG_KEY_TERRAIN, config);
    }

    @Override
    public LegacyFilterConfig getConfig() {
        final LegacyFilterConfig config = new LegacyFilterConfig();
        addFromFilterConfig(difficultyGeocacheFilter, CONFIG_KEY_DIFFICULTY, config);
        addFromFilterConfig(terrainGeocacheFilter, CONFIG_KEY_TERRAIN, config);
        return config;
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.set(node, CONFIG_KEY_DIFFICULTY, difficultyGeocacheFilter.getJsonConfig());
        JsonUtils.set(node, CONFIG_KEY_TERRAIN, terrainGeocacheFilter.getJsonConfig());
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        final JsonNode diffConfig = JsonUtils.get(node, CONFIG_KEY_DIFFICULTY);
        if (diffConfig instanceof ObjectNode) {
            difficultyGeocacheFilter.setJsonConfig((ObjectNode) diffConfig);
        }
        final JsonNode terrainConfig = JsonUtils.get(node, CONFIG_KEY_TERRAIN);
        if (terrainConfig instanceof ObjectNode) {
            terrainGeocacheFilter.setJsonConfig((ObjectNode) terrainConfig);
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        return difficultyGeocacheFilter.getUserDisplayableConfig() + " / " + terrainGeocacheFilter.getUserDisplayableConfig();
    }

    private static void addFromFilterConfig(final NumberRangeGeocacheFilter<?> filter, final String key, @NonNull final LegacyFilterConfig config) {
        config.put(key, filter.getConfig().getDefaultList());
    }

    private static void setToFilterConfig(final NumberRangeGeocacheFilter<?> filter, final String key, @NonNull final LegacyFilterConfig config) {
        filter.setConfig(config.getSubConfig(key));
    }
}
