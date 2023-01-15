package cgeo.geocaching.location;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;

public class GeoObjectStyle implements Parcelable {

    public static final GeoObjectStyle DEFAULT = new GeoObjectStyle(Color.BLACK, 2f, Color.TRANSPARENT);


    @ColorInt private final int strokeColor;
    private final float strokeWidth;
    @ColorInt private final int fillColor;

    public GeoObjectStyle(@ColorInt final int strokeColor, final float strokeWidth, @ColorInt final int fillColor) {
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
         this.fillColor = fillColor;
    }

    @ColorInt
    public int getStrokeColor() {
        return strokeColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    @ColorInt
    public int getFillColor() {
        return fillColor;
    }

    // parcelable stuff

    protected GeoObjectStyle(final Parcel in) {
        strokeColor = in.readInt();
        strokeWidth = in.readFloat();
        fillColor = in.readInt();
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(strokeColor);
        dest.writeFloat(strokeWidth);
        dest.writeInt(fillColor);
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
