package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;

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

}
