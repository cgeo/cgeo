package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Category;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CategoryGeocacheFilter extends BaseGeocacheFilter {

    private Set<Category> categories = new HashSet<>();

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
                sqlBuilder.addWhere("EXISTS (SELECT geocode FROM cg_categories " + catTableId + " WHERE " + catTableId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode AND category = ?)", cat.getRaw());
            }
            sqlBuilder.closeWhere();
        }

    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        categories.clear();
        for (String value : config.getDefaultList()) {
            final Category cat = Category.getByName(value);
            if (cat != Category.UNKNOWN) {
                categories.add(cat);
            }
        }
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig config = new ExpressionConfig();
        if (!categories.isEmpty()) {
            config.putDefaultList(CollectionStream.of(categories).map(Category::getRaw).toList());
        }
        return config;
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
