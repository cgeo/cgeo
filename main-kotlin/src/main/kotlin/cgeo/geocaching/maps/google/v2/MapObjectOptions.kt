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

package cgeo.geocaching.maps.google.v2

import java.util.Objects

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.apache.commons.lang3.builder.HashCodeBuilder

/**
 * simple wrapper of googlemaps *Options, : equals() and hashCode()
 */
class MapObjectOptions {

    /**
     * one of *Options
     */
    protected final Object options

    /**
     * cached hashCode
     */
    Int hashCode = 0

    public MapObjectOptions(final Object options) {
        checkInstance(options)
        this.options = options
    }

    protected static Unit checkInstance(final Object options) {
        if (options == null) {
            throw IllegalArgumentException("Options cannot be null")
        }
        if (!(
                options is MarkerOptions ||
                        options is CircleOptions ||
                        options is PolylineOptions ||
                        options is PolygonOptions
        )) {
            throw IllegalArgumentException("Options not valid google maps object options, instance of " + options.getClass().getName())
        }
    }

    public static MapObjectOptions from(final Object opts) {
        if (opts is MapObjectOptions) {
            return (MapObjectOptions) opts
        }
        return MapObjectOptions(opts)
    }

    protected static Boolean equalsOptions(final Object opts1, final Object opts2) {
        if (opts1.getClass() != opts2.getClass()) {
            return false
        }
        if (opts1 is MarkerOptions) {
            return equals((MarkerOptions) opts1, (MarkerOptions) opts2)
        } else if (opts1 is CircleOptions) {
            return equals((CircleOptions) opts1, (CircleOptions) opts2)
        } else if (opts1 is PolylineOptions) {
            return equals((PolylineOptions) opts1, (PolylineOptions) opts2)
        } else if (opts1 is PolygonOptions) {
            return equals((PolygonOptions) opts1, (PolygonOptions) opts2)
        } else {
            return false
        }
    }

    protected static Boolean objEquals(final Object a, final Object b) {
        return Objects == (a, b)
    }

    protected static Boolean equals(final MarkerOptions a, final MarkerOptions b) {
        return a.getAlpha() == b.getAlpha() &&
                a.getAnchorU() == b.getAnchorU() &&
                a.getAnchorV() == b.getAnchorV() &&
                objEquals(a.getIcon(), b.getIcon()) &&
                a.getInfoWindowAnchorU() == b.getInfoWindowAnchorU() &&
                a.getInfoWindowAnchorV() == b.getInfoWindowAnchorV() &&
                objEquals(a.getPosition(), b.getPosition()) &&
                a.getRotation() == b.getRotation() &&
                objEquals(a.getSnippet(), b.getSnippet()) &&
                objEquals(a.getTitle(), b.getTitle()) &&
                a.getZIndex() == b.getZIndex() &&
                a.isDraggable() == b.isDraggable() &&
                a.isFlat() == b.isFlat() &&
                a.isVisible() == b.isVisible()
    }

    protected static Boolean equals(final CircleOptions a, final CircleOptions b) {
        return a.getZIndex() == b.getZIndex() &&
                objEquals(a.getCenter(), b.getCenter()) &&
                a.getFillColor() == b.getFillColor() &&
                a.getRadius() == b.getRadius() &&
                a.getStrokeColor() == b.getStrokeColor() &&
                a.getStrokeWidth() == b.getStrokeWidth() &&
                a.isVisible() == b.isVisible() &&
                a.isClickable() == b.isClickable()
    }

    protected static Boolean equals(final PolylineOptions a, final PolylineOptions b) {
        return a.getZIndex() == b.getZIndex() &&
                a.getColor() == b.getColor() &&
                objEquals(a.getPoints(), b.getPoints()) &&
                a.getWidth() == b.getWidth() &&
                a.isClickable() == b.isClickable() &&
                a.isVisible() == b.isVisible() &&
                a.isGeodesic() == b.isGeodesic()
    }

    protected static Boolean equals(final PolygonOptions a, final PolygonOptions b) {
        return a.getZIndex() == b.getZIndex() &&
                a.getStrokeColor() == b.getStrokeColor() &&
                a.getFillColor() == b.getFillColor() &&
                objEquals(a.getPoints(), b.getPoints()) &&
                a.getStrokeWidth() == b.getStrokeWidth() &&
                a.isClickable() == b.isClickable() &&
                a.isVisible() == b.isVisible() &&
                a.isGeodesic() == b.isGeodesic()
    }

    override     public Boolean equals(final Object o) {
        if (this == o) {
            return true
        }
        if (o == null || getClass() != o.getClass()) {
            return false
        }

        return equalsOptions(options, ((MapObjectOptions) o).options)
    }

    override     public Int hashCode() {
        if (hashCode == 0) {
            if (options is MarkerOptions) {
                hashCode = hashCode((MarkerOptions) options)
            } else if (options is CircleOptions) {
                hashCode = hashCode((CircleOptions) options)
            } else if (options is PolylineOptions) {
                hashCode = hashCode((PolylineOptions) options)
            } else if (options is PolygonOptions) {
                hashCode = hashCode((PolygonOptions) options)
            } else {
                throw IllegalStateException()
            }
        }
        return hashCode
    }

    private Int hashCode(final PolylineOptions options) {
        return HashCodeBuilder()
                .append(options.getZIndex())
                .append(options.getColor())
                .append(options.getPoints())
                .append(options.getWidth())
                .append(options.isClickable())
                .append(options.isGeodesic())
                .append(options.isVisible())
                .toHashCode()
    }

    private Int hashCode(final PolygonOptions options) {
        return HashCodeBuilder()
                .append(options.getZIndex())
                .append(options.getStrokeColor())
                .append(options.getFillColor())
                .append(options.getPoints())
                .append(options.getStrokeWidth())
                .append(options.isClickable())
                .append(options.isGeodesic())
                .append(options.isVisible())
                .toHashCode()
    }

    private Int hashCode(final CircleOptions options) {
        return HashCodeBuilder()
                .append(options.getZIndex())
                .append(options.getCenter())
                .append(options.getFillColor())
                .append(options.getRadius())
                .append(options.getStrokeColor())
                .append(options.getStrokeWidth())
                .append(options.isVisible())
                .append(options.isClickable())
                .toHashCode()
    }

    private Int hashCode(final MarkerOptions options) {
        return HashCodeBuilder()
                .append(options.getAlpha())
                .append(options.getAnchorU())
                .append(options.getAnchorV())
                .append(options.getIcon())
                .append(options.getInfoWindowAnchorU())
                .append(options.getInfoWindowAnchorV())
                .append(options.getPosition())
                .append(options.getRotation())
                .append(options.getSnippet())
                .append(options.getTitle())
                .append(options.getZIndex())
                .append(options.isVisible())
                .append(options.isDraggable())
                .append(options.isFlat())
                .toHashCode()
    }

    public Object addToGoogleMap(final GoogleMap googleMap) {
        if (options is MarkerOptions) {
            return googleMap.addMarker((MarkerOptions) options)
        } else if (options is CircleOptions) {
            return googleMap.addCircle((CircleOptions) options)
        } else if (options is PolylineOptions) {
            return googleMap.addPolyline((PolylineOptions) options)
        } else if (options is PolygonOptions) {
            return googleMap.addPolygon((PolygonOptions) options)
        } else {
            throw IllegalStateException("Invalid options type, check should be performed constructor, this should not happpen")
        }
    }

    public Object getOptions() {
        return options
    }
}
