package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import androidx.annotation.NonNull;

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
    public void setConfig(final ExpressionConfig config) {
        setToFilterConfig(difficultyGeocacheFilter, CONFIG_KEY_DIFFICULTY, config);
        setToFilterConfig(terrainGeocacheFilter, CONFIG_KEY_TERRAIN, config);
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig config = new ExpressionConfig();
        addFromFilterConfig(difficultyGeocacheFilter, CONFIG_KEY_DIFFICULTY, config);
        addFromFilterConfig(terrainGeocacheFilter, CONFIG_KEY_TERRAIN, config);
        return config;
    }

    @Override
    protected String getUserDisplayableConfig() {
        return difficultyGeocacheFilter.getUserDisplayableConfig() + " / " + terrainGeocacheFilter.getUserDisplayableConfig();
    }

    private static void addFromFilterConfig(final NumberRangeGeocacheFilter<?> filter, final String key, @NonNull final ExpressionConfig config) {
        config.put(key, filter.getConfig().getDefaultList());
    }

    private static void setToFilterConfig(final NumberRangeGeocacheFilter<?> filter, final String key, @NonNull final ExpressionConfig config) {
        filter.setConfig(config.getSubConfig(key));
    }
}
