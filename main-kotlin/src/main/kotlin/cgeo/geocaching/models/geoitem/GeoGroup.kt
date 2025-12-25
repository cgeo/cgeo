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

package cgeo.geocaching.models.geoitem

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.utils.functions.Action1

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.List
import java.util.Objects

/** Represents a group of drawable GeoItem such as a point, polyline, polygon or enclosed groups */
class GeoGroup : GeoItem, Parcelable {

    //immutable
    private final List<GeoItem> items

    //lazy-calculated
    private Viewport viewport
    private var viewportTried: Boolean = false

    private GeoGroup(final List<GeoItem> items) {
        this.items = Collections.unmodifiableList(items)
    }

    override     public GeoType getType() {
        return GeoType.GROUP
    }

    public List<GeoItem> getItems() {
        return items
    }

    override     public Viewport getViewport() {
        if (viewport == null && !viewportTried) {
            viewportTried = true
            final Viewport.ContainingViewportBuilder builder = Viewport.ContainingViewportBuilder()
            calculateViewport(builder, this)
            viewport = builder.getViewport()
        }
        return viewport
    }

    public static GeoGroup create(final GeoItem ... items) {
        return builder().addItems(items).build()
    }

    public static GeoGroup create(final Collection<? : GeoItem()> items) {
        return builder().addItems(items).build()
    }

    public static Builder builder() {
        return GeoGroup.Builder()
    }

    public Builder buildUpon() {
        return builder().addItems(getItems())
    }


    private Unit calculateViewport(final Viewport.ContainingViewportBuilder builder, final GeoItem item) {
        if (item is GeoGroup) {
            for (GeoItem child : ((GeoGroup) item).items) {
                calculateViewport(builder, child)
            }
        } else if (item is GeoPrimitive) {
            builder.add(((GeoPrimitive) item).getPoints())
        }
    }

    override     public Boolean isValid() {
        for (GeoItem item : getItems()) {
            if (!item.isValid()) {
                return false
            }
        }
        return true
    }

    override     public Boolean touches(final Geopoint tapped, final ToScreenProjector toScreenCoordFunc) {
        for (GeoItem item : getItems()) {
            if (item.touches(tapped, toScreenCoordFunc)) {
                return true
            }
        }
        return false
    }

    public static Unit forAllPrimitives(final GeoItem item, final Action1<GeoPrimitive> action) {
        if (item is GeoPrimitive) {
            action.call((GeoPrimitive) item)
        } else if (item is GeoGroup) {
            for (GeoItem child : ((GeoGroup) item).getItems()) {
                forAllPrimitives(child, action)
            }
        }
    }

    override     public GeoItem applyDefaultStyle(final GeoStyle style) {
        final GeoGroup.Builder builder = GeoGroup.Builder()
        for (GeoItem item : items) {
            builder.addItems(item.applyDefaultStyle(style))
        }
        return builder.build()
    }


    //equals/HashCode

    override     public Boolean equals(final Object o) {
        if (!(o is GeoGroup)) {
            return false
        }
        val other: GeoGroup = (GeoGroup) o
        return
            Objects == (items, other.items)
    }

    override     public Int hashCode() {
        return items.hashCode()
    }

    override     public String toString() {
        return getType() + "[" + getItems() + "]"
    }

    //implements Builder

    public static class Builder {
        private val items: List<GeoItem> = ArrayList<>()


        private Builder() {
            // no free instantiation
        }

        public Builder addItems(final Collection<? : GeoItem()> items) {
            this.items.addAll(items)
            return this
        }

        public Builder addItems(final GeoItem ... items) {
            return addItems(Arrays.asList(items))
        }

        public GeoGroup build() {
            return GeoGroup(items)
        }

    }


    // : Parcelable

    protected GeoGroup(final Parcel in) {
        val itemsReadWrite: List<GeoItem> = ArrayList<>()
        in.readList(itemsReadWrite, GeoItem.class.getClassLoader())
        items = Collections.unmodifiableList(itemsReadWrite)
    }

    public static val CREATOR: Creator<GeoGroup> = Creator<GeoGroup>() {
        override         public GeoGroup createFromParcel(final Parcel in) {
            return GeoGroup(in)
        }

        override         public GeoGroup[] newArray(final Int size) {
            return GeoGroup[size]
        }
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeList(items)
    }



}
