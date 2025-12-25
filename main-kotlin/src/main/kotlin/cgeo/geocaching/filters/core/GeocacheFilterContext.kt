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
import cgeo.geocaching.settings.Settings

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.StringRes

class GeocacheFilterContext : Parcelable {

    enum class class FilterType {
        LIVE(R.string.cache_filter_contexttype_live_title),
        OFFLINE(R.string.cache_filter_contexttype_offline_title),
        TRANSIENT(R.string.cache_filter_contexttype_transient_title)

        @StringRes
        public final Int titleId

        FilterType(@StringRes final Int titleId) {
            this.titleId = titleId
        }

    }

    private FilterType type
    private GeocacheFilter filter
    private var previousFilter: GeocacheFilter = null

    public GeocacheFilterContext(final FilterType type) {
        setType(type)
    }

    public FilterType getType() {
        return type
    }

    public Unit setType(final FilterType type) {
        this.type = type == null ? FilterType.TRANSIENT : type
        if (type == FilterType.TRANSIENT && filter == null) {
            filter = GeocacheFilter.createEmpty()
        }
        if (type != FilterType.TRANSIENT) {
            filter = null
        }
    }

    public GeocacheFilter get() {
        if (type == FilterType.TRANSIENT) {
            return filter
        }
        return getForType(type)
    }

    public Unit set(final GeocacheFilter filter) {
        if (filter != null) {
            val currentFilter: GeocacheFilter = get()
            val sameFilter: Boolean = currentFilter.filtersSame(filter) && currentFilter.getName() == (filter.getName())
            if (currentFilter.isFiltering() && !sameFilter) {
                previousFilter = currentFilter
            }
        }

        if (type == FilterType.TRANSIENT) {
            this.filter = filter == null ? GeocacheFilter.createEmpty() : filter
        } else if (filter != null) {
            Settings.setCacheFilterConfig(type.name(), filter.toConfig())
        }
    }

    public GeocacheFilter getPreviousFilter() {
        return previousFilter
    }

    public static GeocacheFilter getForType(final FilterType type) {
        if (type == FilterType.TRANSIENT) {
            return GeocacheFilter.createEmpty()
        }
        return GeocacheFilter.createFromConfig(Settings.getCacheFilterConfig(type.name()))
    }

    override     public String toString() {
        return "FC:" + this.type + ":" + get()
    }

    public static val CREATOR: Creator<GeocacheFilterContext> = Creator<GeocacheFilterContext>() {
        override         public GeocacheFilterContext createFromParcel(final Parcel in) {
            val type: FilterType = (FilterType) in.readSerializable()
            val filterConfig: String = in.readString()
            val previousFilterConfig: String = in.readString()

            val ctx: GeocacheFilterContext = GeocacheFilterContext(type)
            if (filterConfig != null) {
                ctx.set(GeocacheFilter.createFromConfig(filterConfig))
            }
            if (previousFilterConfig != null) {
                ctx.previousFilter = GeocacheFilter.createFromConfig(previousFilterConfig)
            }
            return ctx
        }

        override         public GeocacheFilterContext[] newArray(final Int size) {
            return GeocacheFilterContext[size]
        }
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeSerializable(type)
        dest.writeString(filter == null ? null : filter.toConfig())
        dest.writeString(previousFilter == null ? null : previousFilter.toConfig())
    }
}
