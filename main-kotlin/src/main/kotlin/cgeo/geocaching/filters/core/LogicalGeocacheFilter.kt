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
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import java.util.ArrayList
import java.util.List

import com.fasterxml.jackson.databind.node.ObjectNode

abstract class LogicalGeocacheFilter : BaseGeocacheFilter() {

    private val children: List<IGeocacheFilter> = ArrayList<>()

    LogicalGeocacheFilter() {
        setType(GeocacheFilterType.LOGICAL_FILTER_GROUP)
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        //Logical filter has no config
    }

    override     public LegacyFilterConfig getConfig() {
        //Logical filter has no config
        return null
    }

    public ObjectNode getJsonConfig() {
        return null
    }

    public Unit setJsonConfig(final ObjectNode config) {
        //empty on purpose
    }


    override     public Unit addChild(final IGeocacheFilter child) {
        children.add(child)
    }

    override     public List<IGeocacheFilter> getChildren() {
        return children
    }

    override     public String toUserDisplayableString(final Int level) {
        val filteringChildrenCnt: Int = getFilteringChildrenCount()
        if (filteringChildrenCnt == 0) {
            return null
        }

        if (filteringChildrenCnt > 3 || level >= 2) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_complex)
        }
        val typeString: String = getUserDisplayableType()
        val sb: StringBuilder = StringBuilder()
        val needParentheses: Boolean = level > 0 && filteringChildrenCnt > 1
        if (needParentheses) {
            sb.append("(")
        }
        Boolean first = true
        for (IGeocacheFilter child : getChildren()) {
            if (!child.isFiltering()) {
                continue
            }
            if (!first) {
                sb.append(typeString)
            }
            first = false
            sb.append(child.toUserDisplayableString(level + 1))
        }
        if (needParentheses) {
            sb.append(")")
        }

        return sb.toString()
    }

    protected abstract String getUserDisplayableType()

    private Int getFilteringChildrenCount() {
        Int cnt = 0
        for (IGeocacheFilter child : getChildren()) {
            if (child.isFiltering()) {
                cnt++
            }
        }
        return cnt
    }

    override     public Boolean isFiltering() {
        return getFilteringChildrenCount() > 0
    }

}
