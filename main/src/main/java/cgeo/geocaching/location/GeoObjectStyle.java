package cgeo.geocaching.location;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Immutable value class for GeoObjectStyle info. Includes some helpers to deal with these objects.
 */
public class GeoObjectStyle implements Parcelable {

    @ColorInt @Nullable public final Integer strokeColor;
    @Nullable public final Float strokeWidth;
    @ColorInt @Nullable public final Integer fillColor;

    private GeoObjectStyle(@Nullable @ColorInt final Integer strokeColor, @Nullable final Float strokeWidth, @Nullable @ColorInt final Integer fillColor) {
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.fillColor = fillColor;
    }

    /** Returns a non-null strokecolor. If color for this object is null then given defaults are used */
    public static int getStrokeColor(@Nullable final GeoObjectStyle style, @ColorInt final Integer ... defaults) {
        if (style != null && style.strokeColor != null) {
            return style.strokeColor;
        }
        final Integer dv = firstNonNull(defaults);
        return dv == null ? Color.BLACK : dv;
    }

    /** Returns a non-null strokewidth. If width for this object is null then given defaults are used */
    public static float getStrokeWidth(@Nullable final GeoObjectStyle style, final Float ... defaults) {
        if (style != null && style.strokeWidth != null) {
            return style.strokeWidth;
        }
        final Float dv = firstNonNull(defaults);
        return dv == null ? 2f : dv;
    }

    /** Returns a non-null fillcolor. If color for this object is null then given defaults are used */
    public static int getFillColor(@Nullable final GeoObjectStyle style, @ColorInt final Integer ... defaults) {
        if (style != null && style.fillColor != null) {
            return style.fillColor;
        }
        final Integer dv = firstNonNull(defaults);
        return dv == null ? Color.TRANSPARENT : dv;
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
        return new Builder().setStrokeColor(strokeColor).setStrokeWidth(strokeWidth).setFillColor(fillColor);
    }

    //equals/hashCode

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GeoObjectStyle)) {
            return false;
        }
        final GeoObjectStyle other = (GeoObjectStyle) o;
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

        public GeoObjectStyle build() {
            return new GeoObjectStyle(strokeColor, strokeWidth, fillColor);
        }
    }

    // parcelable stuff

    protected GeoObjectStyle(final Parcel in) {
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

    public static final Creator<GeoObjectStyle> CREATOR = new Creator<GeoObjectStyle>() {
        @Override
        public GeoObjectStyle createFromParcel(final Parcel in) {
            return new GeoObjectStyle(in);
        }

        @Override
        public GeoObjectStyle[] newArray(final int size) {
            return new GeoObjectStyle[size];
        }
    };
}
