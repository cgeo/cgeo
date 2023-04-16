package cgeo.geocaching.models.geoitem;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Immutable value class for GeoItem Style info. Includes some helpers to deal with these objects.
 */
public class GeoStyle implements Parcelable {

    @ColorInt @Nullable private final Integer strokeColor;
    @Nullable private final Float strokeWidth;
    @ColorInt @Nullable private final Integer fillColor;

    private GeoStyle(@Nullable @ColorInt final Integer strokeColor, @Nullable final Float strokeWidth, @Nullable @ColorInt final Integer fillColor) {
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.fillColor = fillColor;
    }

    /** Stroke color as Android Color int (which includes rgb as well as alpha value) */
    @ColorInt
    @Nullable
    public Integer getStrokeColor() {
        return strokeColor;
    }

    /** Stroke width in DP (not pixels) */
    @Nullable
    public Float getStrokeWidth() {
        return strokeWidth;
    }

    /** Fill color as Android Color int (which includes rgb as well as alpha value) */
    @ColorInt
    @Nullable
    public Integer getFillColor() {
        return fillColor;
    }

    public static GeoStyle applyAsDefault(final GeoStyle style, final GeoStyle defaultStyle) {
        if (Objects.equals(style, defaultStyle)) {
            return style;
        }
        return builder()
                .setStrokeColor(getStrokeColor(style, defaultStyle.strokeColor))
                .setStrokeWidth(getStrokeWidth(style, defaultStyle.strokeWidth))
                .setFillColor(getFillColor(style, defaultStyle.fillColor))
                .build();
    }

    /** Returns a non-null strokecolor. If color for this object is null then given defaults are used */
    @ColorInt
    public static int getStrokeColor(@Nullable final GeoStyle style, @ColorInt final Integer ... defaults) {
        if (style != null && style.strokeColor != null) {
            return style.strokeColor;
        }
        final Integer dv = firstNonNull(defaults);
        return dv == null ? Color.BLACK : dv;
    }

    /** Returns a non-null strokewidth. If width for this object is null then given defaults are used */
    public static float getStrokeWidth(@Nullable final GeoStyle style, final Float ... defaults) {
        if (style != null && style.strokeWidth != null) {
            return style.strokeWidth;
        }
        final Float dv = firstNonNull(defaults);
        return dv == null ? 2f : dv;
    }

    /** Returns a non-null fillcolor. If color for this object is null then given defaults are used */
    @ColorInt
    public static int getFillColor(@Nullable final GeoStyle style, @ColorInt final Integer ... defaults) {
        if (style != null && style.fillColor != null) {
            return style.fillColor;
        }
        final Integer dv = firstNonNull(defaults);
        return dv == null ? Color.TRANSPARENT : dv;
    }

    public static int getAlpha(@ColorInt final int color) {
        //copied from Android Color class
        return color >>> 24;
    }

    @Nullable
    private static <T> T firstNonNull(final T[] array) {
        if (array != null) {
            for (T value : array) {
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    public Builder buildUpon() {
        return builder().setStrokeColor(strokeColor).setStrokeWidth(strokeWidth).setFillColor(fillColor);
    }

    public static Builder builder() {
        return new Builder();
    }

    //equals/hashCode

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GeoStyle)) {
            return false;
        }
        final GeoStyle other = (GeoStyle) o;
        return
            Objects.equals(strokeColor, other.strokeColor) &&
            Objects.equals(strokeWidth, other.strokeWidth) &&
            Objects.equals(fillColor, other.fillColor);
    }

    @Override
    public int hashCode() {
        return (strokeColor == null ? 7 : strokeColor) ^ (fillColor == null ? 13 : fillColor);
    }


    //Builder stuff

    public static class Builder {

        @ColorInt @Nullable private Integer strokeColor;
        @Nullable private Float strokeWidth;
        @ColorInt @Nullable public Integer fillColor;

        private Builder() {
            //limit creation
        }

        public Builder setStrokeColor(@ColorInt @Nullable final Integer strokeColor) {
            this.strokeColor = strokeColor;
            return this;
        }

        public Builder setStrokeWidth(@Nullable final Float strokeWidth) {
            this.strokeWidth = strokeWidth;
            return this;
        }

        public Builder setFillColor(@ColorInt @Nullable final Integer fillColor) {
            this.fillColor = fillColor;
            return this;
        }

        public GeoStyle build() {
            return new GeoStyle(strokeColor, strokeWidth, fillColor);
        }
    }

    // parcelable stuff

    protected GeoStyle(final Parcel in) {
        strokeColor = (Integer) in.readValue(getClass().getClassLoader());
        strokeWidth = (Float) in.readValue(getClass().getClassLoader());
        fillColor = (Integer) in.readValue(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeValue(strokeColor);
        dest.writeValue(strokeWidth);
        dest.writeValue(fillColor);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GeoStyle> CREATOR = new Creator<GeoStyle>() {
        @Override
        public GeoStyle createFromParcel(final Parcel in) {
            return new GeoStyle(in);
        }

        @Override
        public GeoStyle[] newArray(final int size) {
            return new GeoStyle[size];
        }
    };
}
