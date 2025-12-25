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
import cgeo.geocaching.enumerations.CacheAttribute
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set

import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.BooleanUtils

class AttributesGeocacheFilter : BaseGeocacheFilter() {

    private static val CONFIG_KEY_ATTLIST: String = "attributes"
    private static val CONFIG_KEY_INVERSE: String = "inverse"
    private static val CONFIG_KEY_SOURCES: String = "attributesources"

    private val attributes: Map<String, String> = HashMap<>()
    private val attributesRaw: Set<String> = HashSet<>()
    private var inverse: Boolean = false
    private var sources: Int = 3

    public Unit setAttributes(final Map<CacheAttribute, Boolean> atts) {
        this.attributes.clear()
        this.attributesRaw.clear()
        for (Map.Entry<CacheAttribute, Boolean> entry : atts.entrySet()) {
            if (entry.getValue() != null) {
                this.attributes.put(entry.getKey().getValue(entry.getValue()), entry.getValue() ? entry.getKey().rawName : null)
                if (entry.getValue()) {
                    attributesRaw.add(entry.getKey().rawName)
                }
            }
        }
    }

    public Unit setInverse(final Boolean inverse) {
        this.inverse = inverse
    }

    public Boolean isInverse() {
        return inverse
    }

    public Unit setSources(final Int sources) {
        this.sources = sources
    }

    public Int getSources() {
        return sources
    }

    public Map<CacheAttribute, Boolean> getAttributes() {
        val result: Map<CacheAttribute, Boolean> = HashMap<>()
        for (String attString : attributes.keySet()) {
            val ca: CacheAttribute = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attString))
            if (ca != null) {
                result.put(ca, CacheAttribute.isEnabled(attString))
            }
        }
        return result
    }

    override     public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null
        }
        if (attributes.isEmpty()) {
            return true
        }

        //check if cache attributes are not filled -> means that this filter is inconclusive
        if (cache.getAttributes().isEmpty() && !cache.isDetailed()) {
            return null
        }

        Int found = 0
        for (String cacheAtt : cache.getAttributes()) {
            if (attributes.containsKey(cacheAtt) || attributesRaw.contains(cacheAtt)) {
                found++
            }
        }
        return inverse != (found == attributes.size())
    }

    override     public Boolean isFiltering() {
        return !attributes.isEmpty()
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (attributes.isEmpty()) {
            sqlBuilder.addWhereTrue()
        } else {
            if (inverse) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.NOT)
            }
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND)
            for (Map.Entry<String, String> att : attributes.entrySet()) {
                val attTableId: String = sqlBuilder.getNewTableId()
                val whereStart: String = "EXISTS (SELECT " + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableAttributes + " " + attTableId + " WHERE " + attTableId + "." + DataStore.dbField_Geocode + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " AND " + DataStore.dbFieldAttributes_Attribute + " "
                if (att.getValue() == null) {
                    sqlBuilder.addWhere(whereStart + " = ?)", att.getKey())
                } else {
                    sqlBuilder.addWhere(whereStart + " IN (?, ?))", att.getKey(), att.getValue())
                }
            }
            sqlBuilder.closeWhere()
            if (inverse) {
                sqlBuilder.closeWhere()
            }
        }

    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        this.inverse = config.getFirstValue(CONFIG_KEY_INVERSE, false, BooleanUtils::toBoolean)
        this.sources = config.getFirstValue(CONFIG_KEY_SOURCES, Settings.getAttributeFilterSources(), Integer::parseInt)
        attributes.clear()
        attributesRaw.clear()
        for (String value : config.getDefaultList()) {
            val ca: CacheAttribute = CacheAttribute.getByName(value)
            if (ca != null) {
                val isYesValue: Boolean = CacheAttribute.isEnabled(value)
                attributes.put(value, isYesValue ? ca.rawName : null)
                if (isYesValue) {
                    attributesRaw.add(ca.rawName)
                }
            }
        }
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        config.putList(CONFIG_KEY_INVERSE, Boolean.toString(inverse))
        config.putList(CONFIG_KEY_SOURCES, Integer.toString(sources))
        config.putDefaultList(ArrayList<>(attributes.keySet()))
        return config
    }

    override     protected String getUserDisplayableConfig() {
        if (attributes.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
        }
        if (attributes.size() > 1) {
            return (inverse ? "!" : "") + LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, attributes.size())
        }

        return (inverse ? "!" : "") + attributes.keySet().iterator().next()
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setBoolean(node, CONFIG_KEY_INVERSE, inverse)
        JsonUtils.setInt(node, CONFIG_KEY_SOURCES, sources)
        JsonUtils.setTextCollection(node, CONFIG_KEY_ATTLIST, attributes.keySet())
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        this.inverse = JsonUtils.getBoolean(node, CONFIG_KEY_INVERSE, false)
        this.sources = JsonUtils.getInt(node, CONFIG_KEY_SOURCES, Settings.getAttributeFilterSources())
        attributes.clear()
        attributesRaw.clear()
        val attList: List<String> = JsonUtils.getTextList(node, CONFIG_KEY_ATTLIST)
        for (String value : attList) {
            val ca: CacheAttribute = CacheAttribute.getByName(value)
            if (ca != null) {
                val isYesValue: Boolean = CacheAttribute.isEnabled(value)
                attributes.put(value, isYesValue ? ca.rawName : null)
                if (isYesValue) {
                    attributesRaw.add(ca.rawName)
                }
            }
        }
    }
}
