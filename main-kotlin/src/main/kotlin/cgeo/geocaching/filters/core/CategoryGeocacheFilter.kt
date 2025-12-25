// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.filters.core

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.bettercacher.Category
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collection
import java.util.HashSet
import java.util.List
import java.util.Set

import com.fasterxml.jackson.databind.node.ObjectNode

class CategoryGeocacheFilter : BaseGeocacheFilter() {

    private val categories: Set<Category> = HashSet<>()

    public Unit setCategories(final Collection<Category> cats) {
        categories.clear()
        categories.addAll(cats)
    }

    public Set<Category> getCategories() {
        return categories
    }

    override     public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null
        }
        if (categories.isEmpty()) {
            return true
        }

        for (Category cat : cache.getCategories()) {
            if (categories.contains(cat)) {
                return true
            }
        }
        return false
    }

    override     public Boolean isFiltering() {
        return !categories.isEmpty()
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (categories.isEmpty()) {
            sqlBuilder.addWhereTrue()
        } else {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND)
            for (Category cat : categories) {
                val catTableId: String = sqlBuilder.getNewTableId()
                sqlBuilder.addWhere("EXISTS (SELECT " + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableCategories + " " + catTableId + " WHERE " + catTableId + "." + DataStore.dbField_Geocode + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " AND " + DataStore.dbFieldCategories_Category + " = ?)", cat.getRaw())
            }
            sqlBuilder.closeWhere()
        }

    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        categories.clear()
        for (String value : config.getDefaultList()) {
            val cat: Category = Category.getByName(value)
            if (cat != Category.UNKNOWN) {
                categories.add(cat)
            }
        }
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        if (!categories.isEmpty()) {
            config.putDefaultList(CollectionStream.of(categories).map(Category::getRaw).toList())
        }
        return config
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        val cats: List<String> = CollectionStream.of(categories).map(Category::getRaw).toList()
        JsonUtils.setTextCollection(node, "cat", cats)
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        categories.clear()
        val cats: List<String> = JsonUtils.getTextList(node, "cat")
        for (String value : cats) {
            val cat: Category = Category.getByName(value)
            if (cat != Category.UNKNOWN) {
                categories.add(cat)
            }
        }
    }

    override     protected String getUserDisplayableConfig() {
        if (categories.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
        }
        if (categories.size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, categories.size())
        }

        return categories.iterator().next().getI18nText()
    }
}
