package cgeo.geocaching.filters.core;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CollectionStream;

import java.util.Arrays;

import org.apache.commons.lang3.BooleanUtils;

public  class FavoritesGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    private boolean percentage = false;

    public FavoritesGeocacheFilter() {
        super(Float::valueOf, f -> f);
    }

    public boolean isPercentage() {
        return percentage;
    }

    public void setPercentage(final boolean percentage) {
        this.percentage = percentage;
    }

    @Override
    public Float getValue(final Geocache cache) {
        if (!percentage) {
            return (float) cache.getFindsCount();
        }
        final int rawFindsCount = cache.getFindsCount();

        return rawFindsCount == 0 ? (float) cache.getFavoritePoints() : ((float) cache.getFavoritePoints()) / rawFindsCount;
    }

    @Override
    protected String getSqlColumnName() {
        return "favourite_cnt";
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!percentage) {
            super.addToSql(sqlBuilder);
        } else {
            final String newTableId = sqlBuilder.getNewTableId();
            sqlBuilder.addJoin("LEFT JOIN (" + getGroupClause(sqlBuilder.getNewTableId()) + ") " + newTableId + " ON " + sqlBuilder.getMainTableId() + ".geocode = " + newTableId + ".geocode");
            addRangeToSqlBuilder(sqlBuilder, getFavoritePercentageStatement(sqlBuilder.getMainTableId() + ".favourite_cnt", newTableId + ".find_count"));
        }
    }

    private static String getGroupClause(final String tid) {
        final String logIds = CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",");
        return "select " + tid + ".geocode, sum(count) as find_count from cg_logCount " + tid + " where " + tid + ".type in (" + logIds + ") group by " + tid + ".geocode";
    }

    private static String getFavoritePercentageStatement(final String favCountColumn, final String findCountColumn) {
        return "(CAST(" + favCountColumn + " AS REAL) / CASE WHEN " + findCountColumn + " IS NULL THEN 1 WHEN " + findCountColumn + " = 0 THEN 1 ELSE " + findCountColumn + " END)";
    }

    @Override
    public void setConfig(final String[] values) {
        super.setConfig(values);
        this.percentage = values != null && values.length > 2 && BooleanUtils.toBoolean(values[2]);
    }

    @Override
    public String[] getConfig() {
        final String[] superConfig = super.getConfig();
        return new String[]{superConfig[0], superConfig[1], Boolean.toString(percentage)};
    }
}
