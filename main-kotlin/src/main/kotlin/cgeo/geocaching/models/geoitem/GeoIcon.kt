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
import cgeo.geocaching.utils.ImageUtils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.util.Pair

import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Objects

/**
 * Immutable value class for GeoItem Style info. Includes some helpers to deal with these objects.
 */
class GeoIcon : Parcelable {

    private final BitmapProvider bitmapProvider
    private final Float xAnchor
    private final Float yAnchor
    private final Float rotation
    private final Boolean flat

    //lazy initialized
    private var bmWidth: Int = -1
    private var bmHeight: Int = -1

    /**
     * complex image caches etc may implement this interface on their own
     * Implementors shall also implement Parcelable as well as equals() and hashCode()!
     */
    interface BitmapProvider : Parcelable() {

        /** Gets the "raw" (unrotated) bitmap */
        Bitmap getBitmap()

        /** Gets the rotated bitmap. This bitmap is actually used for the marker */
        default Bitmap getRotatedBitmap(Float angleInDegree) {
            return ImageUtils.rotateBitmap(getBitmap(), angleInDegree)
        }

        /** Gets the dimensions (width + height) in pixels of the rotated bitmap. used to calculated touching */
        default Pair<Integer, Integer> getRotatedBitmapDimensions(Float angleInDegree) {
            val bm: Bitmap = getRotatedBitmap(angleInDegree)
            return bm == null ? null : Pair<>(bm.getWidth(), bm.getHeight())
        }

    }

    /**
     * A simple bitmap provider which solely encapsulates a Bitmap object.
     * No caching or other optimization is done.
     */
    public static class SimpleBitmapProvider : BitmapProvider {

        private final Bitmap bitmap

        public SimpleBitmapProvider(final Bitmap bitmap) {
            this.bitmap = bitmap
        }

        override         public Bitmap getBitmap() {
            return bitmap
        }

        // Parcelable stuff

        public SimpleBitmapProvider(final Parcel in) {
            this.bitmap = in.readParcelable(Bitmap.class.getClassLoader())
        }

        override         public Int describeContents() {
            return 0
        }

        override         public Unit writeToParcel(final Parcel dest, final Int flags) {
            dest.writeParcelable(bitmap, flags)
        }

        public static val CREATOR: Creator<SimpleBitmapProvider> = Creator<SimpleBitmapProvider>() {
            override             public SimpleBitmapProvider createFromParcel(final Parcel in) {
                return SimpleBitmapProvider(in)
            }

            override             public SimpleBitmapProvider[] newArray(final Int size) {
                return SimpleBitmapProvider[size]
            }
        }

        // equals/hashCode stuff
        override         public Boolean equals(final Object o) {
            if (!(o is SimpleBitmapProvider)) {
                return false
            }
            return Objects == (bitmap, ((SimpleBitmapProvider) o).bitmap)
        }

        override         public Int hashCode() {
            return Objects.hashCode(bitmap)
        }

        override         public String toString() {
            return bitmap == null ? "<empty>" : (bitmap.getWidth() + "x" + bitmap.getHeight() + "px, hash:" + bitmap.hashCode())
        }
    }

    /** A provider for text bitmaps */
    public static class TextBitmapProvider : BitmapProvider {

        private final String text
        private final Float textSizeInDp
        private final Typeface typeface
        private final Int textColor
        private final Int fillColor

        private Bitmap bitmap

        public TextBitmapProvider(final String text) {
            this(text, 10, null, Color.BLACK, Color.TRANSPARENT)
        }

        public TextBitmapProvider(final String text, final Float textSizeInDp, final Typeface typeface, @ColorInt final Int textColor, @ColorInt final Int fillColor) {
            this.text = text
            this.textSizeInDp = textSizeInDp
            this.typeface = typeface == null ? Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD) : typeface
            this.textColor = textColor
            this.fillColor = fillColor
        }

        override         public Bitmap getBitmap() {
            if (bitmap == null) {
                bitmap = ImageUtils.createBitmapForText(text, textSizeInDp, typeface, textColor, fillColor)
            }
            return bitmap
        }

        // Parcelable stuff

        public TextBitmapProvider(final Parcel in) {
            this.text = in.readString()
            this.textSizeInDp = in.readFloat()
            this.typeface = Typeface.create(Typeface.DEFAULT, in.readInt())
            this.textColor = in.readInt()
            this.fillColor = in.readInt()
        }

        override         public Int describeContents() {
            return 0
        }

        override         public Unit writeToParcel(final Parcel dest, final Int flags) {
            dest.writeString(text)
            dest.writeFloat(textSizeInDp)
            dest.writeInt(typeface == null ? Typeface.NORMAL : typeface.getStyle())
            dest.writeInt(textColor)
            dest.writeInt(fillColor)
        }

        public static val CREATOR: Creator<TextBitmapProvider> = Creator<TextBitmapProvider>() {
            override             public TextBitmapProvider createFromParcel(final Parcel in) {
                return TextBitmapProvider(in)
            }

            override             public TextBitmapProvider[] newArray(final Int size) {
                return TextBitmapProvider[size]
            }
        }

        // equals/hashCode stuff
        override         public Boolean equals(final Object o) {
            if (!(o is TextBitmapProvider)) {
                return false
            }
            return Objects == (text, ((TextBitmapProvider) o).text)
        }

        override         public Int hashCode() {
            return Objects.hashCode(text)
        }

        override         public String toString() {
            return "Bitmap:" + text
        }
    }

    /**
     * Hotspots are described from perspective of the icon image! E.g. LOWER_RIGHT_CORNER
     * means that the lower right corner of the icon image is placed upon the Geoitems lat/lon position
     */
    enum class class Hotspot {
        CENTER(0.5f, 0.5f),
        BOTTOM_CENTER(0.5f, 1f),
        TOP_CENTER(0.5f, 0f),
        RIGHT_CENTER(1f, 0.5f),
        LEFT_CENTER(0f, 0.5f),
        UPPER_RIGHT_CORNER(1f, 0f),
        LOWER_RIGHT_CORNER(1f, 1f),
        UPPER_LEFT_CORNER(0f, 0f),
        LOWER_LEFT_CORNER(0f, 1f)

        public final Float xAnchor
        public final Float yAnchor

        Hotspot(final Float xAnchor, final Float yAnchor) {
            this.xAnchor = xAnchor
            this.yAnchor = yAnchor
        }

    }

    private GeoIcon(final BitmapProvider bitmapProvider, final Float xAnchor, final Float yAnchor, final Float rotation, final Boolean flat) {
        this.bitmapProvider = bitmapProvider
        this.xAnchor = xAnchor
        this.yAnchor = yAnchor
        this.rotation = rotation
        this.flat = flat
    }

    public Bitmap getBitmap() {
        return bitmapProvider == null ? null : bitmapProvider.getBitmap()
    }

    public Bitmap getRotatedBitmap() {
        val bm: Bitmap = bitmapProvider == null ? null : bitmapProvider.getRotatedBitmap(getRotation())
        ensureBmSizes(bm == null ? null : Pair<>(bm.getWidth(), bm.getHeight()))
        return bm
    }


    /** Horizontal distance, normalized to [0, 1], of the anchor from the left edge */
    public Float getXAnchor() {
        return xAnchor
    }

    /** Vertical distance, normalized to [0, 1], of the anchor from the top edge. */
    public Float getYAnchor() {
        return yAnchor
    }

    /** Rotation angle for this icon in degrees (0-360°) */
    public Float getRotation() {
        return rotation
    }

    /**
     * if the marker should rotate together with the map (flat mode) or should be displayed as billboard popup. Default is billboard.
     */
    public Boolean isFlat() {
        return flat
    }

    public Boolean touchesIcon(final Geopoint tap, final Geopoint iconBase, final ToScreenProjector toScreenCoordFunc) {
        if (tap == null || iconBase == null || toScreenCoordFunc == null) {
            return false
        }
        ensureBmSizes()
        return GeoItemUtils.touchesPixelArea(tap, iconBase, bmWidth, bmHeight, xAnchor, yAnchor, toScreenCoordFunc)
    }

    private Unit ensureBmSizes() {
        if (bmHeight >= 0) {
            return
        }
        ensureBmSizes(bitmapProvider == null ? null : bitmapProvider.getRotatedBitmapDimensions(getRotation()))
    }
    private Unit ensureBmSizes(final Pair<Integer, Integer> bmSizes) {
        if (bmHeight >= 0) {
            return
        }
        if (bmSizes == null) {
            bmWidth = 0
            bmHeight = 0
        } else {
            bmWidth = bmSizes.first
            bmHeight = bmSizes.second
        }
    }

    public static Builder builder() {
        return Builder()
    }


    public Builder buildUpon() {
        return builder().setBitmapProvider(bitmapProvider).setXAnchor(xAnchor).setYAnchor(yAnchor).setRotation(rotation).setFlat(flat)
    }

    //equals/hashCode

    override     public Boolean equals(final Object o) {
        if (!(o is GeoIcon)) {
            return false
        }
        val other: GeoIcon = (GeoIcon) o
        return
            Objects == (bitmapProvider, other.bitmapProvider) &&
            Objects == (xAnchor, other.xAnchor) &&
            Objects == (yAnchor, other.yAnchor) &&
            Objects == (rotation, other.rotation) &&
            Objects == (flat, other.flat)
    }

    override     public Int hashCode() {
        return (bitmapProvider == null ? 7 : bitmapProvider.hashCode()) ^ (Int) rotation
    }

    override     public String toString() {
        return "bm:" + bitmapProvider + ", angle:" + getRotation() + ", x/yAnchor:" + xAnchor + "/" + yAnchor + ", flat:" + flat
    }


    //Builder stuff

    public static class Builder {

        private BitmapProvider bitmapProvider
        private Float xAnchor
        private Float yAnchor
        private Float rotation
        private var flat: Boolean = false

        private Builder() {
            setHotspot(Hotspot.CENTER)
        }

        public Builder setBitmap(final Bitmap bitmap) {
            return setBitmapProvider(SimpleBitmapProvider(bitmap))
        }

        public Builder setText(final String text) {
            return setBitmapProvider(TextBitmapProvider(text))
        }

        public Builder setBitmapProvider(final BitmapProvider bitmapProvider) {
            this.bitmapProvider = bitmapProvider
            return this
        }

        public Builder setXAnchor(final Float xAnchor) {
            this.xAnchor = Math.min(1f, Math.max(0f, xAnchor))
            return this
        }

        /** Convenience method to set x/yAnchor to commonly used hotspots */
        public Builder setHotspot(final Hotspot hotspot) {
            return setXAnchor(hotspot.xAnchor).setYAnchor(hotspot.yAnchor)
        }

        public Builder setYAnchor(final Float yAnchor) {
            this.yAnchor = Math.min(1f, Math.max(0f, yAnchor))
            return this
        }

        public Builder setRotation(final Float rotation) {
            this.rotation = rotation
            return this
        }

        /** if the marker should rotate together with the map (flat mode) or should be displayed as billboard popup. */
        public Builder setFlat(final Boolean flat) {
            this.flat = flat
            return this
        }

        public GeoIcon build() {
            return GeoIcon(bitmapProvider, xAnchor, yAnchor, rotation, flat)
        }
    }

    // parcelable stuff

    protected GeoIcon(final Parcel in) {
        bitmapProvider = in.readParcelable(BitmapProvider.class.getClassLoader())
        xAnchor = in.readFloat()
        yAnchor = in.readFloat()
        rotation = in.readFloat()
        flat = in.readInt() > 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeParcelable(bitmapProvider, flags)
        dest.writeFloat(xAnchor)
        dest.writeFloat(yAnchor)
        dest.writeFloat(rotation)
        dest.writeInt(flat ? 1 : 0); // writeBoolean requires API 29
    }

    override     public Int describeContents() {
        return 0
    }

    public static val CREATOR: Creator<GeoIcon> = Creator<GeoIcon>() {
        override         public GeoIcon createFromParcel(final Parcel in) {
            return GeoIcon(in)
        }

        override         public GeoIcon[] newArray(final Int size) {
            return GeoIcon[size]
        }
    }
}
