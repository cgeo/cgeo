package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface GeoItem extends Parcelable {

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

    /** creates a new GeoItem where the given style is applied as default style */
    GeoItem applyDefaultStyle(GeoStyle style);

    boolean touches(@NonNull Geopoint tapped, @Nullable ToScreenProjector toScreenCoordFunc);

}
