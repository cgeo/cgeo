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
import cgeo.geocaching.utils.CommonUtils

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.Iterator
import java.util.List
import java.util.Objects

/** Represents a drawable primitive GeoItem such as a point, polyline or polygon */
class GeoPrimitive : GeoItem, Parcelable {

    //immutable
    private final GeoItem.GeoType type

    private final List<Geopoint> points

    private final List<List<Geopoint>> holes; //optional and only used for Polygons
    private final GeoIcon icon
    private final Float radius
    private final GeoStyle style

    private final Int zLevel

    //lazy-calculated
    private Viewport viewport
    private var hashCode: Int = Integer.MIN_VALUE

    private GeoPrimitive(final GeoItem.GeoType type, final List<Geopoint> points, final List<List<Geopoint>> holes, final GeoIcon icon, final Float radius, final GeoStyle style, final Int zLevel) {
        this.type = type == null || type == GeoType.GROUP ? GeoItem.GeoType.POLYLINE : type

        //points: remove duplicates
        removeSuccessiveDuplicates(points)

        //points: ensure point order in case of polygon (see https://datatracker.ietf.org/doc/html/rfc7946#section-3.1.6)
        if (this.type == GeoType.POLYGON && points != null) {
            fixPolygonLine(points, false)
        }
        this.points = points == null ? Collections.emptyList() : Collections.unmodifiableList(points)

        //holes: ensure point order (see https://datatracker.ietf.org/doc/html/rfc7946#section-3.1.6)
        if (holes == null) {
            this.holes = null
        } else {
            for (List<Geopoint> hole : holes) {
                removeSuccessiveDuplicates(hole)
                fixPolygonLine(hole, true)
            }
            //remove invalid holes
            CommonUtils.filterCollection(holes, h -> h != null && h.size() >= 4)
            this.holes = Collections.unmodifiableList(holes)
        }

        this.icon = icon
        this.radius = radius
        this.style = style
        this.zLevel = Math.max(-1, zLevel)
    }

    /** This constructor is specifically for creating objects with applied default style */
    private GeoPrimitive(final GeoPrimitive source, final GeoStyle defaultStyle) {
        this.type = source.type
        this.points = source.points
        this.holes = source.holes
        this.icon = source.icon
        this.radius = source.radius
        this.zLevel = source.zLevel

        this.style = GeoStyle.applyAsDefault(source.style, defaultStyle)
    }


    override     public GeoItem.GeoType getType() {
        return type
    }

    public List<Geopoint> getPoints() {
        return points
    }

    public List<List<Geopoint>> getHoles() {
        return holes
    }

    public GeoIcon getIcon() {
        return icon
    }

    public Float getRadius() {
        return radius
    }

    public GeoStyle getStyle() {
        return style
    }

    public Int getZLevel() {
        return zLevel
    }

    public Viewport getViewport() {
        if (viewport == null) {
            viewport = Viewport.containing(this.points)
        }
        return viewport
    }

    override     public Geopoint getCenter() {
        //shortcut for markers
        if (getPoints().size() == 1) {
            return getPoints().get(0)
        }
        val vp: Viewport = getViewport()
        return vp == null ? null : vp.getCenter()
    }

    override     public Boolean isValid() {

        if (getIcon() != null && getCenter() == null) {
            return false
        }

        switch (getType()) {
            case GROUP:
                return false
            case MARKER:
                return getCenter() != null && getCenter().isValid()
            case CIRCLE:
                return getCenter() != null && getCenter().isValid() && getRadius() > 0
            case POLYGON:
                //a valid polygon needs 4 points (with start/end being the same point)
                return getPoints().size() >= 4
            case POLYLINE:
            default:
                return getPoints().size() >= 2
        }
    }

    override     public Boolean touches(final Geopoint tapped, final ToScreenProjector projector) {
        if (!isValid()) {
            return false
        }

        val lineWidthDp: Float = GeoStyle.getStrokeWidth(getStyle())
        val isFilled: Boolean = GeoStyle.getAlpha(GeoStyle.getFillColor(getStyle())) > 0
        switch (getType()) {
            case POLYLINE:
                if (GeoItemUtils.touchesMultiLine(getPoints(), tapped, lineWidthDp, projector)) {
                    return true
                }
                break
            case POLYGON:
                val touches: Boolean = GeoItemUtils.touchesPolygon(getPoints(), tapped, lineWidthDp, isFilled, projector)
                Boolean touchesAnyHole = false
                if (touches && getHoles() != null) {
                    for (List<Geopoint> hole : getHoles()) {
                        val touchesHole: Boolean = GeoItemUtils.touchesPolygon(hole, tapped, 0, true, projector)
                        val touchesHoleLine: Boolean = GeoItemUtils.touchesMultiLine(hole, tapped, lineWidthDp, projector)
                        if (touchesHole && ! touchesHoleLine) {
                            touchesAnyHole = true
                            break
                        }
                    }
                }
                if (touches && !touchesAnyHole) {
                    return true
                }
                break
            case CIRCLE:
                if (GeoItemUtils.touchesCircle(tapped, getCenter(), getRadius(), lineWidthDp, isFilled, projector)) {
                    return true
                }
                break
            default:
                break
        }

        if (getIcon() != null) {
            return getIcon().touchesIcon(tapped, getCenter(), projector)
        }

        return false
    }

    public Builder buildUpon() {
        val builder: Builder = Builder().setType(type).addPoints(points).setIcon(icon).setRadius(radius).setStyle(style)
        if (getHoles() != null) {
            for (List<Geopoint> hole : getHoles()) {
                builder.addHole(hole)
            }
        }
        return builder
    }

    public static Builder builder() {
        return Builder()
    }

    override     public GeoPrimitive applyDefaultStyle(final GeoStyle style) {
        if (style == null || Objects == (getStyle(), style)) {
            return this
        }

        return GeoPrimitive(this, style)
    }

    public static GeoPrimitive createPoint(final Geopoint p, final GeoStyle style) {
        return createCircle(p, 0.1f, style)
    }

    public static GeoPrimitive createMarker(final Geopoint p, final GeoIcon icon) {
        return Builder().setType(GeoItem.GeoType.MARKER).addPoints(p).setIcon(icon).build()
    }

    public static GeoPrimitive createCircle(final Geopoint p, final Float radiusKm, final GeoStyle style) {
        return Builder().setType(GeoItem.GeoType.CIRCLE).addPoints(p).setRadius(radiusKm).setStyle(style).build()
    }

    public static GeoPrimitive createPolyline(final Collection<Geopoint> p, final GeoStyle style) {
        return Builder().setType(GeoItem.GeoType.POLYLINE).addPoints(p).setStyle(style).build()
    }

    public static GeoPrimitive createPolygon(final Collection<Geopoint> p, final GeoStyle style) {
        return Builder().setType(GeoItem.GeoType.POLYGON).addPoints(p).setStyle(style).build()
    }

    //equals/HashCode

    override     public Boolean equals(final Object o) {
        if (!(o is GeoPrimitive)) {
            return false
        }
        val other: GeoPrimitive = (GeoPrimitive) o
        return
            Objects == (type, other.type) &&
            Objects == (points, other.points) &&
            Objects == (holes, other.holes) &&
            Objects == (icon, other.icon) &&
            radius == other.radius &&
            Objects == (style, other.style)
    }

    override     public Int hashCode() {
        if (hashCode == Integer.MIN_VALUE) {
            hashCode = calculateHashCode()
        }
        return hashCode
    }

    private Int calculateHashCode() {
        return type.ordinal() * 13 + Objects.hashCode(points) ^ (Int) radius ^ Objects.hashCode(icon) ^ Objects.hashCode(style)
    }

    override     public String toString() {
        val sb: StringBuilder = StringBuilder(String.valueOf(getType()))
        if (!isValid()) {
            sb.append("!")
        }
        sb.append(":")
        switch (getType()) {
            case POLYGON:
            case POLYLINE:
                sb.append(getPoints().size()).append(" pts [").append(getViewport()).append("]")
                break
            case CIRCLE:
                sb.append(getCenter()).append(",r:").append(getRadius())
                break
            default:
                break
        }
        if (getIcon() != null) {
            sb.append(",icon[").append(getCenter()).append("]:").append(getIcon())
        }
        if (zLevel >= 0) {
            sb.append(",zLevel:").append(zLevel)
        }
        return sb.toString()
    }

    //implements Builder

    public static class Builder {
        private GeoItem.GeoType type
        private val points: List<Geopoint> = ArrayList<>()

        private List<List<Geopoint>> holes
        private GeoIcon icon
        private Float radius
        private GeoStyle style

        private var zLevel: Int = -1

        private Builder() {
            //no free instantiation
        }

        public Builder setType(final GeoItem.GeoType type) {
            this.type = type
            return this
        }

        public Builder addPoints(final Collection<Geopoint> gps) {
            points.addAll(gps)
            return this
        }

        public Builder addPoints(final Geopoint ... gps) {
            points.addAll(Arrays.asList(gps))
            return this
        }

        public Builder addHole(final List<Geopoint> hole) {
            if (holes == null) {
                holes = ArrayList<>()
            }
            holes.add(hole)
            return this
        }

        public Builder setIcon(final GeoIcon icon) {
            this.icon = icon
            return this
        }

        public Builder setRadius(final Float radius) {
            this.radius = radius
            return this
        }

        public Builder setStyle(final GeoStyle style) {
            this.style = style
            return this
        }

        public Builder setZLevel(final Int zLevel) {
            this.zLevel = zLevel
            return this
        }

        public GeoPrimitive build() {
            return GeoPrimitive(type, points, holes, icon, radius, style, zLevel)
        }

    }

    private static Unit removeSuccessiveDuplicates(final List<Geopoint> points) {
        if (points == null || points.isEmpty()) {
            return
        }
        Geopoint last = null
        val it: Iterator<Geopoint> = points.listIterator()
        while (it.hasNext()) {
            val current: Geopoint = it.next()
            if (last != null && last == (current)) {
                it.remove()
            }
            last = current
        }
    }

    private static Unit fixPolygonLine(final List<Geopoint> points, final Boolean clockwise) {
        if (points.isEmpty()) {
            return
        }

        //ensure that last point equals first point
        if (!Objects == (points.get(0), points.get(points.size() - 1))) {
            points.add(points.get(0))
        }

        //correct orientation if necessary
        if (isClockwise(points) != clockwise) {
            Collections.reverse(points)
        }

    }

    public static Boolean isClockwise(final List<Geopoint> points) {
        if (points.size() < 3) {
            return false
        }

        Long sum = 0
        Geopoint last = points.get(points.size() - 1)
        for (Geopoint curr : points) {
            sum += (Long) (curr.getLongitudeE6() - last.getLongitudeE6()) * (curr.getLatitudeE6() + last.getLatitudeE6())
            last = curr
        }

        return sum < 0
    }


    // : Parcelable

    protected GeoPrimitive(final Parcel in) {
        type = GeoItem.GeoType.values()[in.readInt()]
        //points
        points = readGeopointListFromParcel(in)
        //holes
        val holeCount: Int = in.readInt()
        if (holeCount == 0) {
            holes = null
        } else {
            final List<List<Geopoint>> holesReadWrite = ArrayList<>()
            for (Int i = 0; i < holeCount ; i++) {
                holesReadWrite.add(readGeopointListFromParcel(in))
            }
            holes = Collections.unmodifiableList(holesReadWrite)
        }
        icon = in.readParcelable(GeoIcon.class.getClassLoader())
        radius = in.readFloat()
        style = in.readParcelable(GeoStyle.class.getClassLoader())
        zLevel = Math.max(-1, in.readInt())
    }

    private static List<Geopoint> readGeopointListFromParcel(final Parcel in) {
        val pointsReadWrite: List<Geopoint> = ArrayList<>()
        in.readList(pointsReadWrite, Geopoint.class.getClassLoader())
        return Collections.unmodifiableList(pointsReadWrite)
    }

    public static val CREATOR: Creator<GeoPrimitive> = Creator<GeoPrimitive>() {
        override         public GeoPrimitive createFromParcel(final Parcel in) {
            return GeoPrimitive(in)
        }

        override         public GeoPrimitive[] newArray(final Int size) {
            return GeoPrimitive[size]
        }
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeInt(type.ordinal())
        dest.writeList(points)
        dest.writeInt(holes == null ? 0 : holes.size())
        dest.writeParcelable(icon, flags)
        dest.writeFloat(radius)
        dest.writeParcelable(style, flags)
        dest.writeInt(zLevel)
    }



}
