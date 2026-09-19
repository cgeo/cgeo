package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.utils.ColorUtils;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public record GeoStyleRule(int type, String condition, int intValue, float floatValue) implements Parcelable {

    public static final int STYLE_STROKE_COLOR = 1 << 0;
    public static final int STYLE_FILL_COLOR = 1 << 1;
    public static final int STYLE_STROKE_WIDTH = 1 << 2;

    public static final int DEFAULT_STROKE_COLOR_MARKER = ColorUtils.COLOR_MAIN_VALUE;
    public static final int DEFAULT_FILL_COLOR_MARKER = ColorUtils.COLOR_MAIN_VALUE;
    public static final float DEFAULT_STROKE_WIDTH_MARKER = Float.MAX_VALUE;

    public static GeoStyleRule ofStrokeColor(@ColorInt final int color, final String condition) {
        return new GeoStyleRule(STYLE_STROKE_COLOR, condition, color, 0f);
    }

    public static GeoStyleRule ofFillColor(@ColorInt final int color, final String condition) {
        return new GeoStyleRule(STYLE_FILL_COLOR, condition, color, 0f);
    }

    public static GeoStyleRule ofStrokeWidth(final float width, final String condition) {
        return new GeoStyleRule(STYLE_STROKE_WIDTH, condition, 0, width);
    }

    //parcelable stuff

    public static final Creator<GeoStyleRule> CREATOR = new Creator<>() {
        @Override
        public GeoStyleRule createFromParcel(final Parcel in) {
            return new GeoStyleRule(in);
        }

        @Override
        public GeoStyleRule[] newArray(final int size) {
            return new GeoStyleRule[size];
        }
    };

    private GeoStyleRule(final Parcel in) {
        this(in.readInt(), in.readString(), in.readInt(), in.readFloat());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(type);
        dest.writeString(condition);
        dest.writeInt(intValue);
        dest.writeFloat(floatValue);
    }


}
