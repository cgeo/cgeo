package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.config.LegacyConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.node.ObjectNode;


public abstract class StringGeocacheFilter extends BaseGeocacheFilter {

    private final StringFilter stringFilter = new StringFilter();

    protected abstract String getValue(Geocache cache);

    protected String getSqlColumnName() {
        return null;
    }

    @NonNull
    public StringFilter getStringFilter() {
        return stringFilter;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        final String gcValue = getValue(cache);
        if (gcValue == null) {
            return null;
        }
        return stringFilter.matches(gcValue);
    }

    @Override
    public void setConfig(final LegacyConfig config) {
        stringFilter.setConfig(config.get(null));
    }

    @Override
    public LegacyConfig getConfig() {
        final LegacyConfig config = new LegacyConfig();
        config.put(null, stringFilter.getConfig());
        return config;
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
       return stringFilter.getJsonConfig();
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode config) {
        stringFilter.setJsonConfig(config);
    }

    @Override
    public boolean isFiltering() {
        return stringFilter.isFilled();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final String colName = getSqlColumnName();
        if (colName != null) {
            stringFilter.addToSql(sqlBuilder, sqlBuilder.getMainTableId() + "." + colName);
        } else {
            sqlBuilder.addWhereTrue();
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        return stringFilter.getUserDisplayableConfig();
    }

}
