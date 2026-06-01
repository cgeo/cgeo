package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class HealthScoreGeocacheFilter extends NumberRangeGeocacheFilter<Integer> {

    private static final String CONFIG_KEY_INCLUDE_UNSCORED = "includeUnscored";

    private boolean includeUnscored = true;

    public HealthScoreGeocacheFilter() {
        super(Integer::valueOf, f -> Math.round(f));
    }

    public boolean isIncludeUnscored() {
        return includeUnscored;
    }

    public void setIncludeUnscored(final boolean includeUnscored) {
        this.includeUnscored = includeUnscored;
    }

    @Override
    public Integer getValue(final Geocache cache) {
        return cache.getHealthScore();
    }

    @Override
    protected String getSqlColumnName() {
        return "health_score";
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        final Integer score = cache.getHealthScore();
        if (score == null) {
            return true; // not yet computed — always pass through
        }
        if (score == Geocache.HEALTH_SCORE_UNKNOWN) {
            return includeUnscored;
        }
        return super.filter(cache);
    }

    @Override
    public boolean isFiltering() {
        return !includeUnscored || super.isFiltering();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        // NULL health_score = not yet computed → always include
        sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
        sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".health_score IS NULL");
        if (includeUnscored) {
            // include HEALTH_SCORE_UNKNOWN (-1) OR range
            sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
            sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".health_score = " + Geocache.HEALTH_SCORE_UNKNOWN);
            super.addToSql(sqlBuilder);
            sqlBuilder.closeWhere();
        } else {
            // exclude HEALTH_SCORE_UNKNOWN (-1) AND apply range
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".health_score <> " + Geocache.HEALTH_SCORE_UNKNOWN);
            super.addToSql(sqlBuilder);
            sqlBuilder.closeWhere();
        }
        sqlBuilder.closeWhere();
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = super.getJsonConfig();
        JsonUtils.setBoolean(node, CONFIG_KEY_INCLUDE_UNSCORED, includeUnscored);
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode config) {
        super.setJsonConfig(config);
        includeUnscored = JsonUtils.getBoolean(config, CONFIG_KEY_INCLUDE_UNSCORED, true);
    }

    @Override
    protected String getUserDisplayableConfig() {
        return super.getUserDisplayableConfig(v -> v == null ? null : v + "%");
    }
}
