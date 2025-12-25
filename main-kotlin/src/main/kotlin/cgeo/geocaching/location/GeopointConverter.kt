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

package cgeo.geocaching.location

import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collection
import java.util.List

/** Converts geopoints into other classes (of other libaraies etc) also representing geopoint (lat/lon) information */
class GeopointConverter<T> {

    private final Func1<Geopoint, T> convertTo
    private final Func1<T, Geopoint> convertFrom

    public GeopointConverter(final Func1<Geopoint, T> convertTo, final Func1<T, Geopoint> convertFrom) {
        this.convertFrom = convertFrom
        this.convertTo = convertTo
    }

    public T to(final Geopoint gp) {
        return gp == null ? null : convertTo.call(gp)
    }

    public Geopoint from(final T gp) {
        return gp == null ? null : convertFrom.call(gp)
    }

    public List<T> toList(final Collection<Geopoint> gps) {
        val list: List<T> = ArrayList<>()
        if (gps != null) {
            for (Geopoint gp : gps) {
                list.add(to(gp))
            }
        }
        return list
    }

    public List<Geopoint> fromList(final Collection<T> gps) {
        val list: List<Geopoint> = ArrayList<>()
        if (gps != null) {
            for (T gp : gps) {
                list.add(from(gp))
            }
        }
        return list
    }

}
