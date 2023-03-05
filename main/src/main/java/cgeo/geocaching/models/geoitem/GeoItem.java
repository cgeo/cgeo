package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.utils.functions.Func1;

import android.graphics.Point;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface GeoItem {

    enum GeoType { MARKER, POLYLINE, POLYGON, CIRCLE, GROUP }

    @NonNull
    GeoType getType();

    @SuppressWarnings("unchecked")
    default <T extends GeoItem> T get() {
        return (T) this;
    }

    @Nullable
    Viewport getViewport();

    @Nullable
    default Geopoint getCenter() {
        final Viewport vp = getViewport();
        return vp == null ? null : vp.getCenter();
    }

    boolean isValid();

    boolean touches(@NonNull Geopoint tapped, @Nullable Func1<Geopoint, Point> toScreenCoordFunc);

}
