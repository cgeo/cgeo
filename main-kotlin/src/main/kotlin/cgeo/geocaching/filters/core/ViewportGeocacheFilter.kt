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

import cgeo.geocaching.location.Viewport
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.JsonUtils

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import com.fasterxml.jackson.databind.node.ObjectNode

class ViewportGeocacheFilter : BaseGeocacheFilter() {

    private Viewport viewport

    public ViewportGeocacheFilter setViewport(final Viewport viewport) {
        this.viewport = viewport
        return this
    }

    public Viewport getViewport() {
        return viewport
    }

    override     public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null
        }
        if (viewport == null) {
            return true
        }
        return viewport.contains(cache)
    }

    override     public Boolean isFiltering() {
        return viewport != null
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (viewport != null) {
            sqlBuilder.addWhere(viewport.sqlWhere(sqlBuilder.getMainTableId()).toString())
        } else {
            sqlBuilder.addWhereTrue()
        }
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        if (viewport != null) {
            JsonUtils.set(node, "viewport", viewport.toJson())
        }
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        viewport = Viewport.forJson(JsonUtils.get(node, "viewport"))
    }

    override     protected String getUserDisplayableConfig() {
        return "" + viewport
    }
}
