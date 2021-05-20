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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ValueGroupFilterAccessor<D, T, F extends IGeocacheFilter> {


    private final List<D> selectableValues = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private D[] selectableValuesAsArray = (D[]) Collections.emptyList().toArray();

    private Func1<F, Collection<T>> filterValueGetter;
    private Action2<F, Collection<T>> filterValueSetter;
    private Func2<F, Geocache, Set<T>> geocacheValueGetter;

    private Func1<D, String> valueDisplayTextGetter;
    private Func1<D, Integer> valueDrawableGetter;

    private final Map<D, Set<T>> displayToValueMap = new HashMap<>();
    private final Map<T, D> valueToDisplayMap = new HashMap<>();

   public static <DD, TT, FF extends ValueGroupGeocacheFilter<TT>>  ValueGroupFilterAccessor<DD, TT, FF> createForValueGroupFilter() {
        return new ValueGroupFilterAccessor<DD, TT, FF>()
            .setFilterValueGetter(ValueGroupGeocacheFilter::getValues)
            .setFilterValueSetter(ValueGroupGeocacheFilter::setValues)
            .setGeocacheSingleValueGetter(ValueGroupGeocacheFilter::getValue);
    }

    @SuppressWarnings("unchecked")
    public ValueGroupFilterAccessor<D, T, F> setSelectableValues(final Collection<D> selectableValues) {
        this.selectableValues.clear();
        this.selectableValues.addAll(selectableValues);
        this.selectableValuesAsArray = (D[]) this.selectableValues.toArray();
        return this;
    }

    public ValueGroupFilterAccessor<D, T, F> setSelectableValues(final D[] selectableValues) {
        return this.setSelectableValues(Arrays.asList(selectableValues));
    }

    public ValueGroupFilterAccessor<D, T, F> setFilterValueSetter(final Action2<F, Collection<T>> filterValueSetter) {
        this.filterValueSetter = filterValueSetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, T, F> setFilterValueGetter(final Func1<F, Collection<T>> filterValueGetter) {
        this.filterValueGetter = filterValueGetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, T, F> setValueDisplayTextGetter(final Func1<D, String> valueDisplayTextGetter) {
        this.valueDisplayTextGetter = valueDisplayTextGetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, T, F> setValueDrawableGetter(final Func1<D, Integer> valueDrawableGetter) {
        this.valueDrawableGetter = valueDrawableGetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, T, F> setGeocacheValueGetter(final Func2< F, Geocache, Set<T>> geocacheValueGetter) {
        this.geocacheValueGetter = geocacheValueGetter;
        return this;
    }

    public ValueGroupFilterAccessor<D, T, F> setGeocacheSingleValueGetter(final Func2<F, Geocache, T> geocacheSingleValueGetter) {
        setGeocacheValueGetter((f, c) -> {
            final Set<T> set = new HashSet<>();
            set.add(geocacheSingleValueGetter.call(f, c));
            return set;
        });
        return this;
    }

    public ValueGroupFilterAccessor<D, T, F> addDisplayValues(final D displayValue, final T ... rawValues) {
        Set<T> raw = this.displayToValueMap.get(displayValue);
        if (raw == null) {
            raw = new HashSet<>();
            this.displayToValueMap.put(displayValue, raw);
        }
        raw.addAll(Arrays.asList(rawValues));
        for (T rawValue : rawValues) {
            this.valueToDisplayMap.put(rawValue, displayValue);
        }
        return this;
    }

    public D[] getSelectableValuesAsArray() {
        return selectableValuesAsArray;
    }

    public List<D> getSelectableValues() {
        return selectableValues;
    }

    public Collection<D> getValues(final F filter) {
        return rawToDisplay(filterValueGetter == null ? Collections.emptySet() : filterValueGetter.call(filter));
    }

    public void setValues(final F filter, final Collection<D> values) {
        if (filterValueSetter != null) {
            filterValueSetter.call(filter, displayToRaw(values));
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

    public int getIconFor(final D value) {
        return value == null || this.valueDrawableGetter == null ? 0 : this.valueDrawableGetter.call(value);
    }

    public boolean hasCacheValueGetter() {
       return this.geocacheValueGetter != null;
    }

    public Set<D> getCacheValues(final F filter, final Geocache cache) {
        if (this.geocacheValueGetter == null || cache == null) {
            return Collections.emptySet();
        }
        return rawToDisplay(this.geocacheValueGetter.call(filter, cache));
    }

    @SuppressWarnings("unchecked")
    private Set<T> displayToRaw(final Collection<D> values) {
        final Set<T> rawValues = new HashSet<>();
        for (D value : values) {
            final Set<T> raw = this.displayToValueMap.get(value);
            if (raw == null) {
                rawValues.add((T) value);
            } else {
                rawValues.addAll(raw);
            }
        }
        return rawValues;
    }

    @SuppressWarnings("unchecked")
    private Set<D> rawToDisplay(final Collection<T> rawValues) {
        final Set<D> values = new HashSet<>();
        for (T rawValue : rawValues) {
            final D display = this.valueToDisplayMap.get(rawValue);
            if (display == null) {
                values.add((D) rawValue);
            } else {
                values.add(display);
            }
        }
        return values;
    }
}
