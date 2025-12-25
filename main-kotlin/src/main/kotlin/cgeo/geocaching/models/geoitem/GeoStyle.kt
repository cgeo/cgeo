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

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.ColorInt
import androidx.annotation.Nullable

import java.util.Objects

/**
 * Immutable value class for GeoItem Style info. Includes some helpers to deal with these objects.
 */
class GeoStyle : Parcelable {

    public static val EMPTY: GeoStyle = GeoStyle.builder().build()

    public static val SYSTEM_DEFAULT: GeoStyle = GeoStyle.builder()
            .setStrokeColor(Color.BLACK)
            .setStrokeWidth(2f)
            .setFillColor(Color.TRANSPARENT)
            .build()

    @ColorInt private final Integer strokeColor
    private final Float strokeWidth
    @ColorInt private final Integer fillColor

    private GeoStyle(@ColorInt final Integer strokeColor, final Float strokeWidth, @ColorInt final Integer fillColor) {
        this.strokeColor = strokeColor
        this.strokeWidth = strokeWidth
        this.fillColor = fillColor
    }

    /** Stroke color as Android Color Int (which includes rgb as well as alpha value) */
    @ColorInt
    public Integer getStrokeColor() {
        return strokeColor
    }

    /** Stroke width in DP (not pixels) */
    public Float getStrokeWidth() {
        return strokeWidth
    }

    /** Fill color as Android Color Int (which includes rgb as well as alpha value) */
    @ColorInt
    public Integer getFillColor() {
        return fillColor
    }

    public static GeoStyle applyAsDefault(final GeoStyle style, final GeoStyle defaultStyle) {
        if (Objects == (style, defaultStyle)) {
            return style
        }
        return builder()
                .setStrokeColor(getStrokeColor(style, defaultStyle.strokeColor))
                .setStrokeWidth(getStrokeWidth(style, defaultStyle.strokeWidth))
                .setFillColor(getFillColor(style, defaultStyle.fillColor))
                .build()
    }

    /** Returns a non-null strokecolor. If color for this object is null then given defaults are used */
    @ColorInt
    public static Int getStrokeColor(final GeoStyle style, @ColorInt final Integer ... defaults) {
        if (style != null && style.strokeColor != null) {
            return style.strokeColor
        }
        val dv: Integer = firstNonNull(defaults)
        return dv == null ? SYSTEM_DEFAULT.getStrokeColor() : dv
    }

    /** Returns a non-null strokewidth. If width for this object is null then given defaults are used */
    public static Float getStrokeWidth(final GeoStyle style, final Float ... defaults) {
        if (style != null && style.strokeWidth != null) {
            return style.strokeWidth
        }
        val dv: Float = firstNonNull(defaults)
        return dv == null ? SYSTEM_DEFAULT.getStrokeWidth() : dv
    }

    /** Returns a non-null fillcolor. If color for this object is null then given defaults are used */
    @ColorInt
    public static Int getFillColor(final GeoStyle style, @ColorInt final Integer ... defaults) {
        if (style != null && style.fillColor != null) {
            return style.fillColor
        }
        val dv: Integer = firstNonNull(defaults)
        return dv == null ? SYSTEM_DEFAULT.getFillColor() : dv
    }

    public static Int getAlpha(@ColorInt final Int color) {
        //copied from Android Color class
        return color >>> 24
    }

    private static <T> T firstNonNull(final T[] array) {
        if (array != null) {
            for (T value : array) {
                if (value != null) {
                    return value
                }
            }
        }
        return null
    }

    public Builder buildUpon() {
        return builder().setStrokeColor(strokeColor).setStrokeWidth(strokeWidth).setFillColor(fillColor)
    }

    public static Builder builder() {
        return Builder()
    }

    //helpers

    public static GeoStyle solid(final Int color, final Float width) {
        return GeoStyle.builder()
            .setStrokeColor(color)
            .setFillColor(color)
            .setStrokeWidth(width).build()
    }

    /** creates a style where fill color is a transparent version of stroke color */
    public static GeoStyle transparentFill(final Int strokeColor, final Int fillTransparency, final Float width) {
        return GeoStyle.builder()
            .setStrokeColor(strokeColor)
            .setFillColor(Color.argb(fillTransparency, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor)))
            .setStrokeWidth(width).build()
    }

    //equals/hashCode

    override     public Boolean equals(final Object o) {
        if (!(o is GeoStyle)) {
            return false
        }
        val other: GeoStyle = (GeoStyle) o
        return
            Objects == (strokeColor, other.strokeColor) &&
            Objects == (strokeWidth, other.strokeWidth) &&
            Objects == (fillColor, other.fillColor)
    }

    override     public Int hashCode() {
        return (strokeColor == null ? 7 : strokeColor) ^ (fillColor == null ? 13 : fillColor)
    }


    //Builder stuff

    public static class Builder {

        @ColorInt private Integer strokeColor
        private Float strokeWidth
        @ColorInt public Integer fillColor

        private Builder() {
            //limit creation
        }

        public Builder setStrokeColor(@ColorInt final Integer strokeColor) {
            this.strokeColor = strokeColor
            return this
        }



        public Builder setStrokeWidth(final Float strokeWidth) {
            this.strokeWidth = strokeWidth
            return this
        }

        public Builder setFillColor(@ColorInt final Integer fillColor) {
            this.fillColor = fillColor
            return this
        }

        public GeoStyle build() {
            return GeoStyle(strokeColor, strokeWidth, fillColor)
        }
    }

    // parcelable stuff

    protected GeoStyle(final Parcel in) {
        strokeColor = (Integer) in.readValue(getClass().getClassLoader())
        strokeWidth = (Float) in.readValue(getClass().getClassLoader())
        fillColor = (Integer) in.readValue(getClass().getClassLoader())
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeValue(strokeColor)
        dest.writeValue(strokeWidth)
        dest.writeValue(fillColor)
    }

    override     public Int describeContents() {
        return 0
    }

    public static val CREATOR: Creator<GeoStyle> = Creator<GeoStyle>() {
        override         public GeoStyle createFromParcel(final Parcel in) {
            return GeoStyle(in)
        }

        override         public GeoStyle[] newArray(final Int size) {
            return GeoStyle[size]
        }
    }
}
