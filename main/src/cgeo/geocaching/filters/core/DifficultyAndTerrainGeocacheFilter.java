package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

public class DifficultyAndTerrainGeocacheFilter extends BaseGeocacheFilter {

    public DifficultyGeocacheFilter difficultyGeocacheFilter = new DifficultyGeocacheFilter();
    public TerrainGeocacheFilter terrainGeocacheFilter = new TerrainGeocacheFilter();

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
        final ExpressionConfig diffConfig = new ExpressionConfig();
        diffConfig.putDefaultList(config.get("difficulty"));
        difficultyGeocacheFilter.setConfig(diffConfig);

        final ExpressionConfig terrainConfig = new ExpressionConfig();
        terrainConfig.putDefaultList(config.get("terrain"));
        terrainGeocacheFilter.setConfig(terrainConfig);
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig config = new ExpressionConfig();
        config.put("difficulty", difficultyGeocacheFilter.getConfig().getDefaultList());
        config.put("terrain", terrainGeocacheFilter.getConfig().getDefaultList());
        return config;
    }

    @Override
    protected String getUserDisplayableConfig() {
        return difficultyGeocacheFilter.getUserDisplayableConfig() + " / " + terrainGeocacheFilter.getUserDisplayableConfig();
    }
}
