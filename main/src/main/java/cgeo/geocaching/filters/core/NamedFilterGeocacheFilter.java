package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.NamedFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class NamedFilterGeocacheFilter extends BaseGeocacheFilter {

    private final Set<Integer> namedFilterIds = new HashSet<>();

    private static final ThreadLocal<Set<Integer>> nestingTracker = new ThreadLocal<>();

    public Set<NamedFilter> getNamedFilters() {
        final Set<NamedFilter> filterLists = new HashSet<>();
        forEachSelectedNamedFilter(nf -> {
            filterLists.add(nf);
            return null;
        });
        return filterLists;
    }

    public void setNamedFilters(final Collection<NamedFilter> filters) {
        namedFilterIds.clear();
        for (NamedFilter filter : filters) {
            namedFilterIds.add(filter.getId());
        }
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }

        final boolean[] oneFilter = new boolean[] { false };
        final Boolean filterResult = forEachSelectedNamedFilter(nf -> {
            oneFilter[0] = true;
            if (nf.getFilter() == null || nf.getFilter().filter(cache)) {
                return true;
            }
            return null;
        });
        return filterResult == Boolean.TRUE || !oneFilter[0];
    }

    @Override
    public boolean isFiltering() {
        final Boolean filterResult = forEachSelectedNamedFilter(nf -> {
            if (nf.getFilter() != null && nf.getFilter().isFiltering()) {
                return true;
            }
            return null;
        });
        return filterResult == Boolean.TRUE;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (namedFilterIds.isEmpty()) {
            sqlBuilder.addWhereTrue();
            return;
        }
        sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
        forEachSelectedNamedFilter(nf -> {
             if (nf.getFilter() == null || nf.getFilter().getTree() == null) {
                 sqlBuilder.addWhereTrue();
             } else {
                 nf.getFilter().getTree().addToSql(sqlBuilder);
             }
             return null;
        });
        sqlBuilder.closeWhere();
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setCollection(node, "ids", namedFilterIds, JsonUtils::fromInt);
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        this.namedFilterIds.clear();
        this.namedFilterIds.addAll(JsonUtils.getList(node, "ids", n -> JsonUtils.toInt(n, -1))
            .stream().filter(i -> i != -1).collect(Collectors.toList()));
        final int idList = JsonUtils.getInt(node, "id", -1);
        if (idList >= 0) {
            this.namedFilterIds.add(idList);
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        final Set<NamedFilter> selectedFilters = getNamedFilters();

        if (selectedFilters.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (selectedFilters.size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, selectedFilters.size());
        }
        return selectedFilters.iterator().next().getNameAndMarker();
    }

    private <T> T forEachSelectedNamedFilter(final Function<NamedFilter, T> function) {
        for (Integer listId : namedFilterIds) {
            final NamedFilter nf = NamedFilter.getById(listId);
            if (nf == null) {
                continue;
            }
            try {
                if (startNested(nf.getId())) {
                    continue;
                }
                final T result = function.apply(nf);
                if (result != null) {
                    return result;
                }
            } finally {
                stopNested(nf.getId());
            }
        }
        return null;
    }

    // --- Nesting detection to avoid infinite loops in case of circular references ---

    /** returns true if nesting was detected */
    private boolean startNested(final int filterId) {
        final Set<Integer> nestedSet = getNestedSet();
        if (nestedSet.contains(filterId)) {
            //nested filter detected, stop nesting and return true to avoid infinite loop
            return true;
        }
        nestedSet.add(filterId);
        return false;
    }

    private void stopNested(final int filterId) {
        getNestedSet().remove(filterId);
    }

    private Set<Integer> getNestedSet() {
        Set<Integer> nestSet = nestingTracker.get();
        if (nestSet == null) {
            nestSet = new HashSet<>();
            nestingTracker.set(nestSet);
        }
        return nestSet;
    }

    @NonNull
    @Override
    public IGeocacheFilter simplify(@NonNull final Function<IGeocacheFilter, Boolean> criterion) {
        final Boolean crit = criterion.apply(this);
        if (crit != null) {
            return crit ? ConstantGeocacheFilter.ALWAYS_TRUE : ConstantGeocacheFilter.ALWAYS_FALSE;
        }
        final List<IGeocacheFilter> namedFilterList =
            getNamedFilters().stream().filter(nf -> nf.getFilter() != null && nf.getFilter().getTree() != null)
                    .map(nf -> nf.getFilter().getTree()).collect(Collectors.toList());
        if (namedFilterList.isEmpty()) {
            return ConstantGeocacheFilter.ALWAYS_TRUE;
        }
        if (namedFilterList.size() == 1) {
            return namedFilterList.get(0).simplify(criterion);
        }
        final OrGeocacheFilter orFilter = new OrGeocacheFilter();
        orFilter.getChildren().addAll(namedFilterList);
        return orFilter.simplify(criterion);
    }
}
