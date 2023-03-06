package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;

import android.graphics.Point;
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

/** Represents a drawable primitive GeoItem such as a point, polyline or polygon */
public class GeoGroup implements GeoItem, Parcelable {

    //immutable
    @NonNull private final List<GeoItem> items;

    //lazy-calculated
    private Viewport viewport;
    private boolean viewportTried = false;

    private GeoGroup(@NonNull final List<GeoItem> items) {
        this.items = Collections.unmodifiableList(items);
    }

    @NonNull
    @Override
    public GeoType getType() {
        return GeoType.GROUP;
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

    public static Builder builder() {
        return new GeoGroup.Builder();
    }

    public Builder buildUpon() {
        return builder().addItems(getItems());
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
    public boolean touches(@NonNull final Geopoint tapped, @Nullable final Func1<Geopoint, Point> toScreenCoordFunc) {
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


    //equals/HashCode

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GeoGroup)) {
            return false;
        }
        final GeoGroup other = (GeoGroup) o;
        return
            Objects.equals(items, other.items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return getType() + "[" + getItems() + "]";
    }

    //implements Builder

    public static class Builder {
        private final List<GeoItem> items = new ArrayList<>();


        private Builder() {
            // no free instantiation
        }

        public Builder addItems(final Collection<GeoItem> items) {
            this.items.addAll(items);
            return this;
        }

        public Builder addItems(final GeoItem ... items) {
            return addItems(Arrays.asList(items));
        }

        public GeoGroup build() {
            return new GeoGroup(items);
        }

    }


    // implements Parcelable

    protected GeoGroup(final Parcel in) {
        final List<GeoItem> itemsReadWrite = new ArrayList<>();
        in.readList(itemsReadWrite, GeoItem.class.getClassLoader());
        items = Collections.unmodifiableList(itemsReadWrite);
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
    }



}
