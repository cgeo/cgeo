package cgeo.geocaching.location;

import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Converts geopoints into other classes (of other libaraies etc) also representing geopoint (lat/lon) information */
public class GeopointConverter<T> {

    private Func1<Geopoint, T> convertTo;
    private Func1<T, Geopoint> convertFrom;

    public GeopointConverter(@NonNull final Func1<Geopoint, T> convertTo, @NonNull final Func1<T, Geopoint> convertFrom) {
        this.convertFrom = convertFrom;
        this.convertTo = convertTo;
    }

    @Nullable
    public T to(@Nullable final Geopoint gp) {
        return gp == null ? null : convertTo.call(gp);
    }

    @Nullable
    public Geopoint from(@Nullable final T gp) {
        return gp == null ? null : convertFrom.call(gp);
    }

    @NonNull
    public List<T> toList(@Nullable final Collection<Geopoint> gps) {
        final List<T> list = new ArrayList<>();
        if (gps != null) {
            for (Geopoint gp : gps) {
                list.add(to(gp));
            }
        }
        return list;
    }

    @NonNull
    public List<Geopoint> fromList(@Nullable final Collection<T> gps) {
        final List<Geopoint> list = new ArrayList<>();
        if (gps != null) {
            for (T gp : gps) {
                list.add(from(gp));
            }
        }
        return list;
    }

    public List<List<T>> toListList(final IGeoDataProvider gg) {
        final List<List<T>> list = new ArrayList<>();
        for (GeoObject go : gg.getGeoData()) {
            list.add(toList(go.points));
        }
        return list;
    }

}
