package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.ValueGroupGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.utils.functions.Action2;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ValueGroupFilterAccessor<D, F extends IGeocacheFilter> {


    private final List<D> selectableValues = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private D[] selectableValuesAsArray = (D[]) Collections.emptyList().toArray();

    private Func1<F, Collection<D>> filterValueGetter;
    private Action2<F, Collection<D>> filterValueSetter;
    private Func2<F, Geocache, Set<D>> geocacheValueGetter;

    private Func1<D, String> valueDisplayTextGetter;
    private Func1<D, ImageParam> valueDrawableGetter;

    public static <DD, FF extends ValueGroupGeocacheFilter<DD, ?>> ValueGroupFilterAccessor<DD, FF> createForValueGroupFilter() {
        return new ValueGroupFilterAccessor<DD, FF>()
                .setFilterValueGetter(ValueGroupGeocacheFilter::getValues)
                .setFilterValueSetter(ValueGroupGeocacheFilter::setValues)
                .setGeocacheSingleValueGetter(ValueGroupGeocacheFilter::getCacheValue);
    }

    @SuppressWarnings("unchecked")
    public ValueGroupFilterAccessor<D, F> setSelectableValues(final Collection<D> selectableValues) {
        this.selectableValues.clear();
        this.selectableValues.addAll(selectableValues);
        this.selectableValuesAsArray = (D[]) this.selectableValues.toArray();
        return this;
    }

    public ValueGroupFilterAccessor<D, F> setSelectableValues(final D[] selectableValues) {
        return this.setSelectableValues(Arrays.asList(selectableValues));
    }

    public ValueGroupFilterAccessor<D, F> setFilterValueSetter(final Action2<F, Collection<D>> filterValueSetter) {
        this.filterValueSetter = filterValueSetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, F> setFilterValueGetter(final Func1<F, Collection<D>> filterValueGetter) {
        this.filterValueGetter = filterValueGetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, F> setValueDisplayTextGetter(final Func1<D, String> valueDisplayTextGetter) {
        this.valueDisplayTextGetter = valueDisplayTextGetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, F> setValueDrawableGetter(final Func1<D, ImageParam> valueDrawableGetter) {
        this.valueDrawableGetter = valueDrawableGetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, F> setGeocacheValueGetter(final Func2<F, Geocache, Set<D>> geocacheValueGetter) {
        this.geocacheValueGetter = geocacheValueGetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, F> setGeocacheSingleValueGetter(final Func2<F, Geocache, D> geocacheSingleValueGetter) {
        setGeocacheValueGetter((f, c) -> {
            final Set<D> set = new HashSet<>();
            set.add(geocacheSingleValueGetter.call(f, c));
            return set;
        });
        return this;
    }

    public D[] getSelectableValuesAsArray() {
        return selectableValuesAsArray;
    }

    public List<D> getSelectableValues() {
        return selectableValues;
    }

    public Collection<D> getValues(final F filter) {
        return filterValueGetter == null ? Collections.emptySet() : filterValueGetter.call(filter);
    }

    public void setValues(final F filter, final Collection<D> values) {
        if (filterValueSetter != null) {
            filterValueSetter.call(filter, values);
        }
    }

    public String getDisplayText(final D value) {
        if (value == null) {
            return "-";
        }
        if (this.valueDisplayTextGetter != null) {
            return this.valueDisplayTextGetter.call(value);
        }
        return String.valueOf(value);
    }

    public ImageParam getIconFor(final D value) {
        return value == null || this.valueDrawableGetter == null ? null : this.valueDrawableGetter.call(value);
    }

    public boolean hasCacheValueGetter() {
        return this.geocacheValueGetter != null;
    }

    public Set<D> getCacheValues(final F filter, final Geocache cache) {
        if (this.geocacheValueGetter == null || cache == null) {
            return Collections.emptySet();
        }
        return this.geocacheValueGetter.call(filter, cache);
    }

}
