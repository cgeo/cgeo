package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.bettercacher.Category;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.config.LegacyFilterConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class CategoryGeocacheFilter extends BaseGeocacheFilter {

    private final Set<Category> categories = new HashSet<>();

    public void setCategories(final Collection<Category> cats) {
        categories.clear();
        categories.addAll(cats);
    }

    public Set<Category> getCategories() {
        return categories;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        if (categories.isEmpty()) {
            return true;
        }

        for (Category cat : cache.getCategories()) {
            if (categories.contains(cat)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFiltering() {
        return !categories.isEmpty();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (categories.isEmpty()) {
            sqlBuilder.addWhereTrue();
        } else {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            for (Category cat : categories) {
                final String catTableId = sqlBuilder.getNewTableId();
                sqlBuilder.addWhere("EXISTS (SELECT " + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableCategories + " " + catTableId + " WHERE " + catTableId + "." + DataStore.dbField_Geocode + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " AND " + DataStore.dbFieldCategories_Category + " = ?)", cat.getRaw());
            }
            sqlBuilder.closeWhere();
        }

    }

    @Override
    public void setConfig(final LegacyFilterConfig config) {
        categories.clear();
        for (String value : config.getDefaultList()) {
            final Category cat = Category.getByName(value);
            if (cat != Category.UNKNOWN) {
                categories.add(cat);
            }
        }
    }

    @Override
    public LegacyFilterConfig getConfig() {
        final LegacyFilterConfig config = new LegacyFilterConfig();
        if (!categories.isEmpty()) {
            config.putDefaultList(CollectionStream.of(categories).map(Category::getRaw).toList());
        }
        return config;
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        final List<String> cats = CollectionStream.of(categories).map(Category::getRaw).toList();
        JsonUtils.setTextCollection(node, "cat", cats);
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        categories.clear();
        final List<String> cats = JsonUtils.getTextList(node, "cat");
        for (String value : cats) {
            final Category cat = Category.getByName(value);
            if (cat != Category.UNKNOWN) {
                categories.add(cat);
            }
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        if (categories.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (categories.size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, categories.size());
        }

        return categories.iterator().next().getI18nText();
    }
}
