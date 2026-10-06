package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** Filters caches by their user assigned custom icon. */
public class CacheIconGeocacheFilter extends BaseGeocacheFilter {

    private final List<String> icons = new ArrayList<>();

    public List<String> getIcons() {
        return icons;
    }

    public void setIcons(final Collection<String> selectedIcons) {
        icons.clear();
        if (selectedIcons != null) {
            for (String icon : selectedIcons) {
                if (icon != null && !icon.isEmpty() && !icons.contains(icon)) {
                    icons.add(icon);
                }
            }
        }
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        return icons.isEmpty() || icons.contains(cache.getAssignedEmoji());
    }

    @Override
    public boolean isFiltering() {
        return !icons.isEmpty();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (icons.isEmpty()) {
            sqlBuilder.addWhereTrue();
            return;
        }
        final List<String> args = new ArrayList<>(icons);
        final StringBuilder where = new StringBuilder(sqlBuilder.getMainTableId()).append(".emojiString IN (");
        for (int i = 0; i < icons.size(); i++) {
            if (i > 0) {
                where.append(',');
            }
            where.append('?');
        }
        where.append(')');
        sqlBuilder.addWhere(where.toString(), args);
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setTextCollection(node, "icons", icons);
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        setIcons(JsonUtils.getTextList(node, "icons"));
    }

    @Override
    protected String getUserDisplayableConfig() {
        return String.join(", ", icons);
    }
}
