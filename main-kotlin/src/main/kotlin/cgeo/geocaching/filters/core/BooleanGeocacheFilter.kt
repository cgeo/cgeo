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
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.List

import com.fasterxml.jackson.databind.node.ObjectNode

abstract class BooleanGeocacheFilter : BaseGeocacheFilter() {

    public static val yesFlag: String = "yes"
    public static val noFlag: String = "no"

    private var value: Boolean = null


    override     public Boolean filter(final Geocache cache) {
        return value == null || filter(cache, value)
    }

    public abstract Boolean filter(Geocache cache, Boolean value)


    public Boolean getValue() {
        return value
    }

    public Unit setValue(final Boolean value) {
        this.value = value
    }

    private Unit setConfigInternal(final List<String> configValues) {
        value = null

        for (String value : configValues) {
            if (checkBooleanFlag(yesFlag, value)) {
                this.value = true
            }
            if (checkBooleanFlag(noFlag, value)) {
                this.value = false
            }

        }
    }

    private List<String> getConfigInternal() {
        val result: List<String> = ArrayList<>()
        if (value != null) {
            result.add(value ? yesFlag : noFlag)
        }
        return result
    }


    override     public Boolean isFiltering() {
        return value != null
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (value == null) {
            sqlBuilder.addWhereTrue()
        } else {
            addToSql(sqlBuilder, value)
        }
    }

    public abstract Unit addToSql(SqlBuilder sqlBuilder, Boolean value)

    override     protected String getUserDisplayableConfig() {
        if (value != null) {
            return LocalizationUtils.getString(value ? R.string.cache_filter_status_select_yes : R.string.cache_filter_status_select_no)
        }
        return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setTextCollection(node, "values", getConfigInternal())
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        setConfigInternal(JsonUtils.getTextList(node, "values"))
    }
}
