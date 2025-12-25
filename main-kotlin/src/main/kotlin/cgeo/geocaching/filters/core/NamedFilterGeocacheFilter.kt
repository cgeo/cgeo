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

import java.util.HashSet
import java.util.Set

import com.fasterxml.jackson.databind.node.ObjectNode

class NamedFilterGeocacheFilter : BaseGeocacheFilter() {

    private var namedFilterName: String = null
    private var namedFilter: GeocacheFilter = null

    private static final ThreadLocal<Set<String>> nestingTracker = ThreadLocal<>()

    public GeocacheFilter getNamedFilter() {
        if (namedFilter != null) {
            return namedFilter
        }
        if (namedFilterName != null) {
            setNamedFilter(GeocacheFilter.Storage.get(namedFilterName))
        }
        return namedFilter
    }

    public Unit setNamedFilter(final GeocacheFilter filter) {
        this.namedFilter = filter != null && filter.isNamed() ? filter : null
        this.namedFilterName = this.namedFilter == null ? null : this.namedFilter.getName()
    }

    override     public Boolean filter(final Geocache cache) {
        if (!hasValidFilter()) {
            return true
        }

        try {
            if (startNested()) {
                return true
            } else {
                return getNamedFilter().filter(cache)
            }
        } finally {
            stopNested()
        }
    }

    override     public Boolean isFiltering() {
        try {
            if (startNested()) {
                return false
            }
            return hasValidFilter() && getNamedFilter().isFiltering()
        } finally {
            stopNested()
        }
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (!hasValidFilter()) {
            sqlBuilder.addWhereTrue()
        } else {
            try {
                if (startNested()) {
                    sqlBuilder.addWhereTrue()
                } else {
                    getNamedFilter().getTree().addToSql(sqlBuilder)
                }
            } finally {
                stopNested()
            }
        }
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setText(node, "name",
                getNamedFilter() == null  ? null : getNamedFilter().getName())
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        val configName: String = JsonUtils.getText(node, "name", null)

        //only set name here, not filter. Reason: stored filters are not available on time of calling
        this.namedFilterName = configName
        this.namedFilter = null
    }

    override     protected String getUserDisplayableConfig() {
        return getNamedFilter() == null ?
                LocalizationUtils.getString(R.string.cache_filter_userdisplay_none) :
                getNamedFilter().getName()
    }

    private Boolean hasValidFilter() {
        return getNamedFilter() != null && getNamedFilter().isNamed() && getNamedFilter().getTree() != null
    }

    /**
     * checks whether a call to this filter is nested within another call to same filter.
     * This is to avoid infinite loops if same Named Filters is nested within other named filters.
     * Also sets a mark for deeper-level calls that this filter was used
     */
    private Boolean startNested() {
        if (!hasValidFilter()) {
            return false
        }
        if (getNestedSet().contains(getNamedFilter().getName())) {
            //Nesting found! Report it to prevent infinity loop / Stack overflow
            return true
        }
        getNestedSet().add(getNamedFilter().getName())
        return false
    }

    /** removes the mark set by call to checkAndAddNested (if any was set) */
    private Unit stopNested() {
        if (hasValidFilter()) {
            getNestedSet().remove(getNamedFilter().getName())
        }
    }


    private Set<String> getNestedSet() {
        Set<String> nestSet = nestingTracker.get()
        if (nestSet == null) {
            nestSet = HashSet<>()
            nestingTracker.set(nestSet)
        }
        return nestSet
    }
}
