package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.config.LegacyFilterConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class DifficultyTerrainMatrixGeocacheFilter extends BaseGeocacheFilter {

    private static final String CONFIG_KEY_INCLUDE_CACHES_WO_DT = "include-wo-dt";

    private final Set<String> difficultyTerrainCombis = new HashSet<>();

    private boolean includeCachesWoDt = true;

    public void clearDtCombis() {
        difficultyTerrainCombis.clear();
    }

    public void addDtCombi(final float difficulty, final float terrain) {
        difficultyTerrainCombis.add(getDtCombiString(difficulty, terrain));
    }

    public boolean hasDtCombi(final float difficulty, final float terrain) {
        return difficultyTerrainCombis.contains(getDtCombiString(difficulty, terrain));
    }

    public Set<ImmutablePair<Float, Float>> getDtCombis() {
        final Set<ImmutablePair<Float, Float>> result = new HashSet<>();
        for (String combi : difficultyTerrainCombis) {
            final String[] parts = combi.split("-");
            final float d = Float.parseFloat(parts[0]);
            final float t = Float.parseFloat(parts[1]);
            result.add(new ImmutablePair<>(d, t));
        }
        return result;
    }

    private static String getDtCombiString(final float difficulty, final float terrain) {
        return String.format(Locale.US, "%1.1f-%1.1f", difficulty, terrain);
    }

    public void setIncludeCachesWoDt(final boolean includeCachesWoDt) {
        this.includeCachesWoDt = includeCachesWoDt;
    }

    public boolean isIncludeCachesWoDt() {
        return this.includeCachesWoDt;
    }


    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        if (!isFilteringOffline()) {
            return true;
        }

        final float difficulty = cache.getDifficulty();
        final float terrain = cache.getTerrain();
        if (difficulty <= 0 || terrain <= 0) {
            return includeCachesWoDt;
        }

        return difficultyTerrainCombis.isEmpty() || difficultyTerrainCombis.contains(getDtCombiString(difficulty, terrain));
    }

    @Override
    public boolean isFiltering() {
        return isFilteringOffline();
    }

    private boolean isFilteringOffline() {
        return isFilteringMatrix() || !includeCachesWoDt;
    }

    public boolean isFilteringMatrix() {
        return !difficultyTerrainCombis.isEmpty() && difficultyTerrainCombis.size() < 81;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!isFilteringOffline()) {
            sqlBuilder.addWhereTrue();
        } else if (isFilteringMatrix()) {
            if (includeCachesWoDt) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
            }
            final StringBuilder where = new StringBuilder("printf(\"%.1f-%.1f\", difficulty, terrain) IN (");
            final List<String> params = new ArrayList<>();
            boolean first = true;
            for (String dtCombi : difficultyTerrainCombis) {
                if (!first) {
                    where.append(",");
                }
                first = false;
                where.append("?");
                params.add(dtCombi);
            }
            where.append(")");
            sqlBuilder.addWhere(where.toString(), params);
            if (includeCachesWoDt) {
                sqlBuilder.addWhere("difficulty <= 0 AND terrain <= 0");
                sqlBuilder.closeWhere();
            }
        } else if (!includeCachesWoDt) {
            sqlBuilder.addWhere("difficulty > 0 AND terrain > 0");
        }
    }

    @Override
    public void setConfig(final LegacyFilterConfig config) {
        this.includeCachesWoDt = config.getFirstValue(CONFIG_KEY_INCLUDE_CACHES_WO_DT, false, BooleanUtils::toBoolean);
        this.difficultyTerrainCombis.clear();
        this.difficultyTerrainCombis.addAll(config.getDefaultList());
    }

    @Override
    public LegacyFilterConfig getConfig() {
        final LegacyFilterConfig config = new LegacyFilterConfig();
        config.putList(CONFIG_KEY_INCLUDE_CACHES_WO_DT, Boolean.toString(includeCachesWoDt));
        if (isFilteringMatrix()) {
            config.putDefaultList(new ArrayList<>(this.difficultyTerrainCombis));
        }
        return config;
    }

    @Override
    protected String getUserDisplayableConfig() {
        if (difficultyTerrainCombis.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (difficultyTerrainCombis.size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, difficultyTerrainCombis.size());
        }

        return difficultyTerrainCombis.iterator().next();
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setTextCollection(node, "combis", difficultyTerrainCombis);
        JsonUtils.setBoolean(node, CONFIG_KEY_INCLUDE_CACHES_WO_DT, includeCachesWoDt);
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        this.difficultyTerrainCombis.clear();
        this.difficultyTerrainCombis.addAll(JsonUtils.getTextList(node, "combis"));
        this.includeCachesWoDt = JsonUtils.getBoolean(node, CONFIG_KEY_INCLUDE_CACHES_WO_DT, true);
    }
}
