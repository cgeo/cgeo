package cgeo.geocaching.filters.core;

import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ViewportGeocacheFilter extends BaseGeocacheFilter {

    private Viewport viewport;

    public ViewportGeocacheFilter setViewport(final Viewport viewport) {
        this.viewport = viewport;
        return this;
    }

    public Viewport getViewport() {
        return viewport;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        if (viewport == null) {
            return true;
        }
        return viewport.contains(cache);
    }

    @Override
    public boolean isFiltering() {
        return viewport != null;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (viewport != null) {
            sqlBuilder.addWhere(viewport.sqlWhere(sqlBuilder.getMainTableId()).toString());
        } else {
            sqlBuilder.addWhereTrue();
        }
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        if (viewport != null) {
            JsonUtils.set(node, "viewport", viewport.toJson());
        }
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        viewport = Viewport.forJson(JsonUtils.get(node, "viewport"));
    }

    @Override
    protected String getUserDisplayableConfig() {
        return "" + viewport;
    }
}
