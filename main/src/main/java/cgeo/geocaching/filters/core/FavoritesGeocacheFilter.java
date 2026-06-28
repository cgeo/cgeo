package cgeo.geocaching.filters.core;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.JsonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class FavoritesGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    public static FavoritesGeocacheFilter create(final Float min, final Float max) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.FAVORITES, min, max);
    }

    public static FavoritesGeocacheFilter create(final Collection<Float> values, final Float minUnlimitedValue, final Float maxUnlimitedValue) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.FAVORITES, values, minUnlimitedValue, maxUnlimitedValue);
    }

    private static final String CONFIG_KEY_PERCENTAGE = "percentage";

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
        if (!percentage || cache.getFavoritePoints() == 0) {
            return (float) cache.getFavoritePoints();
        }
        final int rawFindsCount = cache.getFindsCount();

        return rawFindsCount == 0 ? null : ((float) cache.getFavoritePoints()) / rawFindsCount;
    }

    @Override
    protected String getSqlColumnName() {
        return DataStore.dbFieldCaches_favourite_cnt;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!percentage) {
            super.addToSql(sqlBuilder);
        } else {
            final String newTableId = sqlBuilder.getNewTableId();
            sqlBuilder.addJoin("LEFT JOIN (" + getGroupClause(sqlBuilder.getNewTableId()) + ") " + newTableId + " ON " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " = " + newTableId + "." + DataStore.dbField_Geocode);
            addRangeToSqlBuilder(sqlBuilder, getFavoritePercentageStatement(sqlBuilder.getMainTableId() + "." + DataStore.dbFieldCaches_favourite_cnt, newTableId + ".find_count"));
        }
    }

    private static String getGroupClause(final String tid) {
        final String logIds = CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",");
        return "select " + tid + "." + DataStore.dbField_Geocode + ", sum(" + DataStore.dbFieldLogCount_Count + ") as find_count from " + DataStore.dbTableLogCount + " " + tid + " where " + tid + "." + DataStore.dbFieldLogCount_Type + " in (" + logIds + ") group by " + tid + "." + DataStore.dbField_Geocode;
    }

    private static String getFavoritePercentageStatement(final String favCountColumn, final String findCountColumn) {
        return "(CAST(" + favCountColumn + " AS REAL) / CASE WHEN " + findCountColumn + " IS NULL THEN 1 WHEN " + findCountColumn + " = 0 THEN 1 ELSE " + findCountColumn + " END)";
    }


    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = super.getJsonConfig();
        JsonUtils.setBoolean(node, CONFIG_KEY_PERCENTAGE, percentage);
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode config) {
        super.setJsonConfig(config);
        percentage = JsonUtils.getBoolean(config, CONFIG_KEY_PERCENTAGE, false);
    }

    @Override
    protected String getUserDisplayableConfig() {
        if (percentage) {
            return super.getUserDisplayableConfig(f -> f == null ? null : Math.round(f * 100) + "%");
        }
        return super.getUserDisplayableConfig();
    }


}
