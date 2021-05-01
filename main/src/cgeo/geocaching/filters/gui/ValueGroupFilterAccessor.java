package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.ValueGroupGeocacheFilter;
import cgeo.geocaching.models.Geocache;
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

public class ValueGroupFilterAccessor<T, F extends IGeocacheFilter> {


    private final List<T> selectableValues = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private T[] selectableValuesAsArray = (T[]) Collections.emptyList().toArray();

    private Func1<F, Collection<T>> valueGetter;
    private Action2<F, Collection<T>> valueSetter;

    private Func1<T, String> valueDisplayTextGetter;
    private Func1<T, Integer> valueDrawableGetter;
    private Func2<F, Geocache, Set<T>> geocacheValueGetter;

   public static <TT, FF extends ValueGroupGeocacheFilter<TT>>  ValueGroupFilterAccessor<TT, FF> createForValueGroupFilter() {
        return new ValueGroupFilterAccessor<TT, FF>()
            .setValueGetter(ValueGroupGeocacheFilter::getValues)
            .setValueSetter(ValueGroupGeocacheFilter::setValues)
            .setGeocacheSingleValueGetter(ValueGroupGeocacheFilter::getValue);
    }


    @SuppressWarnings("unchecked")
    public ValueGroupFilterAccessor<T, F> setSelectableValues(final Collection<T> selectableValues) {
        this.selectableValues.clear();
        this.selectableValues.addAll(selectableValues);
        this.selectableValuesAsArray = (T[]) this.selectableValues.toArray();
        return this;
    }

    public ValueGroupFilterAccessor<T, F> setSelectableValues(final T[] selectableValues) {
        return this.setSelectableValues(Arrays.asList(selectableValues));
    }

    public ValueGroupFilterAccessor<T, F> setValueSetter(final Action2<F, Collection<T>> valueSetter) {
        this.valueSetter = valueSetter;
        return this;
    }

    public ValueGroupFilterAccessor<T, F> setValueGetter(final Func1<F, Collection<T>> valueGetter) {
        this.valueGetter = valueGetter;
        return this;
    }

    public ValueGroupFilterAccessor<T, F> setValueDisplayTextGetter(final Func1<T, String> valueDisplayTextGetter) {
        this.valueDisplayTextGetter = valueDisplayTextGetter;
        return this;
    }

    public ValueGroupFilterAccessor<T, F> setValueDrawableGetter(final Func1<T, Integer> valueDrawableGetter) {
        this.valueDrawableGetter = valueDrawableGetter;
        return this;
    }

    public ValueGroupFilterAccessor<T, F> setGeocacheValueGetter(final Func2< F, Geocache, Set<T>> geocacheValueGetter) {
        this.geocacheValueGetter = geocacheValueGetter;
        return this;
    }

    public ValueGroupFilterAccessor<T, F> setGeocacheSingleValueGetter(final Func2<F, Geocache, T> geocacheSingleValueGetter) {
        this.geocacheValueGetter = (f, c) -> {
            final Set<T> set = new HashSet<>();
            set.add(geocacheSingleValueGetter.call(f, c));
            return set;
        };
        return this;
    }

    public T[] getSelectableValuesAsArray() {
        return selectableValuesAsArray;
    }

    public List<T> getSelectableValues() {
        return selectableValues;
    }

    public Collection<T> getValues(final F filter) {
        return valueGetter == null ? Collections.emptySet() : valueGetter.call(filter);
    }

    public void setValues(final F filter, final Collection<T> values) {
        if (valueSetter != null) {
            valueSetter.call(filter, values);
        }
    }

    public String getDisplayText(final T value) {
        if (value == null) {
            return "-";
        }
        if (this.valueDisplayTextGetter != null) {
            return this.valueDisplayTextGetter.call(value);
        }
        return String.valueOf(value);
    }

    public int getIconFor(final T value) {
        return value == null || this.valueDrawableGetter == null ? 0 : this.valueDrawableGetter.call(value);
    }

    public boolean hasCacheValueGetter() {
       return this.geocacheValueGetter != null;
    }

    public Set<T> getCacheValues(final F filter, final Geocache cache) {
        if (this.geocacheValueGetter == null || cache == null) {
            return Collections.emptySet();
        }
        return this.geocacheValueGetter.call(filter, cache);
    }
}
