package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.utils.functions.Action1;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Represents a group of drawable GeoItem such as a point, polyline, polygon or enclosed groups */
public class GeoGroup implements GeoItem, Parcelable {

    private static final AtomicInteger ID_GIVER = new AtomicInteger();

    //immutable
    @NonNull private final List<GeoItem> items;
    @Nullable private final GeoStyle style;
    private final int id;

    //lazy-calculated
    private Viewport viewport;
    private boolean viewportTried = false;

    private GeoGroup(@NonNull final List<GeoItem> items, final GeoStyle style) {
        this.items = Collections.unmodifiableList(items);
        this.style = style;
        this.id = ID_GIVER.incrementAndGet();
    }

    @NonNull
    @Override
    public GeoType getType() {
        return GeoType.GROUP;
    }

    @Override
    public GeoStyle getStyle() {
        return style;
    }

    @NonNull
    public List<GeoItem> getItems() {
        return items;
    }

    @Nullable
    @Override
    public Viewport getViewport() {
        if (viewport == null && !viewportTried) {
            viewportTried = true;
            final Viewport.ContainingViewportBuilder builder = new Viewport.ContainingViewportBuilder();
            calculateViewport(builder, this);
            viewport = builder.getViewport();
        }
        return viewport;
    }

    public static GeoGroup create(final GeoItem ... items) {
        return builder().addItems(items).build();
    }

    public static GeoGroup create(final Collection<? extends GeoItem> items) {
        return builder().addItems(items).build();
    }

    public static Builder builder() {
        return new GeoGroup.Builder();
    }

    public Builder buildUpon() {
        return builder().addItems(getItems()).setStyle(getStyle());
    }


    private void calculateViewport(final Viewport.ContainingViewportBuilder builder, final GeoItem item) {
        if (item instanceof GeoGroup) {
            for (GeoItem child : ((GeoGroup) item).items) {
                calculateViewport(builder, child);
            }
        } else if (item instanceof GeoPrimitive) {
            builder.add(((GeoPrimitive) item).getPoints());
        }
    }

    @Override
    public boolean isValid() {
        for (GeoItem item : getItems()) {
            if (!item.isValid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean touches(@NonNull final Geopoint tapped, @Nullable final ToScreenProjector toScreenCoordFunc) {
        for (GeoItem item : getItems()) {
            if (item.touches(tapped, toScreenCoordFunc)) {
                return true;
            }
        }
        return false;
    }

    public static void forAllPrimitives(final GeoItem item, final Action1<GeoPrimitive> action) {
        if (item instanceof GeoPrimitive) {
            action.call((GeoPrimitive) item);
        } else if (item instanceof GeoGroup) {
            for (GeoItem child : ((GeoGroup) item).getItems()) {
                forAllPrimitives(child, action);
            }
        }
    }

    @Override
    public void recalculateDynamicStyles(final GeoStyle parentStyle, final GeoStyle mainStyle) {
        if (this.style != null) {
            this.style.recalculateDynamic(this, parentStyle, mainStyle);
        }
        for (GeoItem item : items) {
            item.recalculateDynamicStyles(this.style != null ? this.style : parentStyle, mainStyle);
        }
    }


    //equals/HashCode

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GeoGroup other)) {
            return false;
        }
        return
            Objects.equals(id, other.id) && Objects.equals(items, other.items) && Objects.equals(style, other.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, items, style);
    }

    @NonNull
    @Override
    public String toString() {
        return getType() + "[" + getItems() + "] style=" + getStyle();
    }

    //implements Builder

    public static class Builder {
        private final List<GeoItem> items = new ArrayList<>();
        private GeoStyle style;

        private Builder() {
            // no free instantiation
        }

        public Builder addItems(final Collection<? extends GeoItem> items) {
            this.items.addAll(items);
            return this;
        }

        public Builder addItems(final GeoItem ... items) {
            return addItems(Arrays.asList(items));
        }

        public Builder setStyle(final GeoStyle style) {
            this.style = style;
            return this;
        }

        public GeoGroup build() {
            return new GeoGroup(items, style);
        }

    }


    // implements Parcelable

    protected GeoGroup(final Parcel in) {
        final List<GeoItem> itemsReadWrite = new ArrayList<>();
        in.readList(itemsReadWrite, GeoItem.class.getClassLoader());
        items = Collections.unmodifiableList(itemsReadWrite);
        style = in.readParcelable(GeoStyle.class.getClassLoader());
        id = in.readInt();
    }

    public static final Creator<GeoGroup> CREATOR = new Creator<GeoGroup>() {
        @Override
        public GeoGroup createFromParcel(final Parcel in) {
            return new GeoGroup(in);
        }

        @Override
        public GeoGroup[] newArray(final int size) {
            return new GeoGroup[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeList(items);
        dest.writeParcelable(style, flags);
        dest.writeInt(id);
    }



}
