package cgeo.geocaching.models.geoitem;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Immutable value class for GeoItem Style info. Includes some helpers to deal with these objects.
 */
public class GeoIcon implements Parcelable {

    @Nullable private final Bitmap icon;
    private final float xShift;
    private final float yShift;
    private final float angle;

    private GeoIcon(final Bitmap icon, final float xShift, final float yShift, final float angle) {
        this.icon = icon;
        this.xShift = xShift;
        this.yShift = yShift;
        this.angle = angle;
    }

    @Nullable
    public Bitmap getIcon() {
        return icon;
    }

    public float getXShift() {
        return xShift;
    }

    public float getYShift() {
        return yShift;
    }

    public float getAngle() {
        return angle;
    }
    public Builder buildUpon() {
        return new Builder().setIcon(icon).setXShift(xShift).setYShift(yShift).setAngle(angle);
    }

    //equals/hashCode

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GeoIcon)) {
            return false;
        }
        final GeoIcon other = (GeoIcon) o;
        return
            Objects.equals(icon, other.icon) &&
            Objects.equals(xShift, other.xShift) &&
            Objects.equals(yShift, other.yShift) &&
            Objects.equals(angle, other.angle);
    }

    @Override
    public int hashCode() {
        return (icon == null ? 7 : icon.hashCode()) ^ (int) angle;
    }


    //Builder stuff

    public static class Builder {

        @Nullable private Bitmap icon;
        private float xShift;
        private float yShift;
        private float angle;

        public Builder setIcon(@Nullable final Bitmap icon) {
            this.icon = icon;
            return this;
        }

        public Builder setXShift(final float xShift) {
            this.xShift = xShift;
            return this;
        }

        public Builder setYShift(final float yShift) {
            this.yShift = yShift;
            return this;
        }

        public Builder setAngle(final float angle) {
            this.angle = angle;
            return this;
        }

        public GeoIcon build() {
            return new GeoIcon(icon, xShift, yShift, angle);
        }
    }

    // parcelable stuff

    protected GeoIcon(final Parcel in) {
        icon = in.readParcelable(Drawable.class.getClassLoader());
        xShift = in.readFloat();
        yShift = in.readFloat();
        angle = in.readFloat();
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(icon, flags);
        dest.writeFloat(xShift);
        dest.writeFloat(yShift);
        dest.writeFloat(angle);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GeoIcon> CREATOR = new Creator<GeoIcon>() {
        @Override
        public GeoIcon createFromParcel(final Parcel in) {
            return new GeoIcon(in);
        }

        @Override
        public GeoIcon[] newArray(final int size) {
            return new GeoIcon[size];
        }
    };
}
