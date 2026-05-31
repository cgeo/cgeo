package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.NamedFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class NamedFilterGeocacheFilter extends BaseGeocacheFilter {

    private int namedFilterId = 0;

    private static final ThreadLocal<Set<Integer>> nestingTracker = new ThreadLocal<>();

    public int getNamedFilterId() {
        return namedFilterId;
    }

    public void setNamedFilterId(final int id) {
        this.namedFilterId = id;
    }

    @Nullable
    private NamedFilter resolveNamedFilter() {
        if (namedFilterId == 0) {
            return null;
        }
        return NamedFilter.getById(namedFilterId);
    }

    @Override
    public Boolean filter(final Geocache cache) {
        final NamedFilter nf = resolveNamedFilter();
        if (nf == null || nf.getFilter() == null) {
            return true;
        }
        try {
            if (startNested()) {
                return true;
            }
            return nf.getFilter().filter(cache);
        } finally {
            stopNested();
        }
    }

    @Override
    public boolean isFiltering() {
        final NamedFilter nf = resolveNamedFilter();
        if (nf == null || nf.getFilter() == null) {
            return false;
        }
        try {
            if (startNested()) {
                return false;
            }
            return nf.getFilter().isFiltering();
        } finally {
            stopNested();
        }
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final NamedFilter nf = resolveNamedFilter();
        if (nf == null || nf.getFilter() == null || nf.getFilter().getTree() == null) {
            sqlBuilder.addWhereTrue();
        } else {
            try {
                if (startNested()) {
                    sqlBuilder.addWhereTrue();
                } else {
                    nf.getFilter().getTree().addToSql(sqlBuilder);
                }
            } finally {
                stopNested();
            }
        }
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setInt(node, "id", namedFilterId);
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        this.namedFilterId = JsonUtils.getInt(node, "id", 0);
    }

    @Override
    protected String getUserDisplayableConfig() {
        final NamedFilter nf = resolveNamedFilter();
        return nf == null ?
            LocalizationUtils.getString(R.string.cache_filter_userdisplay_none) :
            nf.getNameAndMarker();
    }

    private boolean startNested() {
        if (namedFilterId == 0) {
            return false;
        }
        if (getNestedSet().contains(namedFilterId)) {
            return true;
        }
        getNestedSet().add(namedFilterId);
        return false;
    }

    private void stopNested() {
        if (namedFilterId != 0) {
            getNestedSet().remove(namedFilterId);
        }
    }

    private Set<Integer> getNestedSet() {
        Set<Integer> nestSet = nestingTracker.get();
        if (nestSet == null) {
            nestSet = new HashSet<>();
            nestingTracker.set(nestSet);
        }
        return nestSet;
    }
}
