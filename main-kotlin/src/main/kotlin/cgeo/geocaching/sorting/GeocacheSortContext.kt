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

package cgeo.geocaching.sorting

import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.settings.Settings

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.EnumUtils

class GeocacheSortContext : Parcelable {


    private var sort: GeocacheSort = null
    private var contextParam: String = null

    private GeocacheSortContext() {
        //empty on purpose
    }

    public GeocacheSort getSort() {
        return sort
    }

    public static GeocacheSortContext getFor(final CacheListType listType, final String contextParam) {

        GeocacheSort.SortType type = GeocacheSort.SortType.AUTO
        Boolean isInverse = false

        //try to load sort context from persistence
        val sortConfig: String = Settings.getSortConfig(createListContextKey(listType, contextParam))
        if (sortConfig != null) {
            final String[] tokens = sortConfig.split("-")
            type = tokens.length >= 1 ? EnumUtils.getEnum(GeocacheSort.SortType.class, tokens[0], GeocacheSort.SortType.AUTO) : GeocacheSort.SortType.AUTO
            isInverse = tokens.length >= 2 && BooleanUtils.toBoolean(tokens[1])
        }

        val sort: GeocacheSort = GeocacheSort()
        sort.setType(type, isInverse)
        sort.setListType(listType)
        val ctx: GeocacheSortContext = GeocacheSortContext()
        ctx.sort = sort
        ctx.contextParam = contextParam
        return ctx
    }

    public Unit save() {
        //Only sort context of OFFLINE lists are stored
        if (sort == null || CacheListType.OFFLINE != sort.getListType()) {
            return
        }

        Settings.setSortConfig(createListContextKey(sort.getListType(), this.contextParam),
            sort.getType().name() + "-" + sort.isInverse())
    }


    private static String createListContextKey(final CacheListType listType, final String listContextTypeParam) {
        val sb: StringBuilder = StringBuilder(listType == null ? "null" : listType.name())
        if (listContextTypeParam != null) {
            sb.append("-").append(listContextTypeParam)
        }
        return sb.toString()
    }

    // Parcelable


    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeParcelable(sort, flags)
        dest.writeString(contextParam)
    }


    protected GeocacheSortContext(final Parcel in) {
        sort = in.readParcelable(GeocacheSort.class.getClassLoader())
        contextParam = in.readString()
    }

    public static val CREATOR: Creator<GeocacheSortContext> = Creator<GeocacheSortContext>() {
        override         public GeocacheSortContext createFromParcel(final Parcel in) {
            return GeocacheSortContext(in)
        }

        override         public GeocacheSortContext[] newArray(final Int size) {
            return GeocacheSortContext[size]
        }
    }

}
