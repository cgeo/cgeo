package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.ImageUtils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Immutable value class for GeoItem Style info. Includes some helpers to deal with these objects.
 */
public class GeoIcon implements Parcelable {

    @Nullable private final BitmapProvider bitmapProvider;
    private final float xAnchor;
    private final float yAnchor;
    private final float rotation;
    private final boolean flat;

    //lazy initialized
    private int bmWidth = -1;
    private int bmHeight = -1;

    /**
     * complex image caches etc may implement this interface on their own
     * Implementors shall also implement Parcelable as well as equals() and hashCode()!
     */
    public interface BitmapProvider extends Parcelable {

        /** Gets the "raw" (unrotated) bitmap */
        Bitmap getBitmap();

        /** Gets the rotated bitmap. This bitmap is actually used for the marker */
        default Bitmap getRotatedBitmap(float angleInDegree) {
            return ImageUtils.rotateBitmap(getBitmap(), angleInDegree);
        }

        /** Gets the dimensions (width + height) in pixels of the rotated bitmap. used to calculated touching */
        default Pair<Integer, Integer> getRotatedBitmapDimensions(float angleInDegree) {
            final Bitmap bm = getRotatedBitmap(angleInDegree);
            return bm == null ? null : new Pair<>(bm.getWidth(), bm.getHeight());
        }

    }

    /**
     * A simple bitmap provider which solely encapsulates a Bitmap object.
     * No caching or other optimization is done.
     */
    public static class SimpleBitmapProvider implements BitmapProvider {

        private final Bitmap bitmap;

        public SimpleBitmapProvider(final Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public Bitmap getBitmap() {
            return bitmap;
        }

        // Parcelable stuff

        public SimpleBitmapProvider(final Parcel in) {
            this.bitmap = in.readParcelable(Bitmap.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeParcelable(bitmap, flags);
        }

        public static final Creator<SimpleBitmapProvider> CREATOR = new Creator<SimpleBitmapProvider>() {
            @Override
            public SimpleBitmapProvider createFromParcel(final Parcel in) {
                return new SimpleBitmapProvider(in);
            }

            @Override
            public SimpleBitmapProvider[] newArray(final int size) {
                return new SimpleBitmapProvider[size];
            }
        };

        // equals/hashCode stuff
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof SimpleBitmapProvider)) {
                return false;
            }
            return Objects.equals(bitmap, ((SimpleBitmapProvider) o).bitmap);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(bitmap);
        }

        @Override
        @NonNull
        public String toString() {
            return bitmap == null ? "<empty>" : (bitmap.getWidth() + "x" + bitmap.getHeight() + "px, hash:" + bitmap.hashCode());
        }
    }

    /** A provider for text bitmaps */
    public static class TextBitmapProvider implements BitmapProvider {

        private final String text;
        private final float textSizeInDp;
        private final Typeface typeface;
        private final int textColor;
        private final int fillColor;

        private Bitmap bitmap;

        public TextBitmapProvider(final String text) {
            this(text, 10, null, Color.BLACK, Color.TRANSPARENT);
        }

        public TextBitmapProvider(final String text, final float textSizeInDp, @Nullable final Typeface typeface, @ColorInt final int textColor, @ColorInt final int fillColor) {
            this.text = text;
            this.textSizeInDp = textSizeInDp;
            this.typeface = typeface == null ? Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD) : typeface;
            this.textColor = textColor;
            this.fillColor = fillColor;
        }

        @Override
        public Bitmap getBitmap() {
            if (bitmap == null) {
                bitmap = ImageUtils.createBitmapForText(text, textSizeInDp, typeface, textColor, fillColor);
            }
            return bitmap;
        }

        // Parcelable stuff

        public TextBitmapProvider(final Parcel in) {
            this.text = in.readString();
            this.textSizeInDp = in.readFloat();
            this.typeface = Typeface.create(Typeface.DEFAULT, in.readInt());
            this.textColor = in.readInt();
            this.fillColor = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeString(text);
            dest.writeFloat(textSizeInDp);
            dest.writeInt(typeface == null ? Typeface.NORMAL : typeface.getStyle());
            dest.writeInt(textColor);
            dest.writeInt(fillColor);
        }

        public static final Creator<TextBitmapProvider> CREATOR = new Creator<TextBitmapProvider>() {
            @Override
            public TextBitmapProvider createFromParcel(final Parcel in) {
                return new TextBitmapProvider(in);
            }

            @Override
            public TextBitmapProvider[] newArray(final int size) {
                return new TextBitmapProvider[size];
            }
        };

        // equals/hashCode stuff
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof TextBitmapProvider)) {
                return false;
            }
            return Objects.equals(text, ((TextBitmapProvider) o).text);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(text);
        }

        @Override
        @NonNull
        public String toString() {
            return "Bitmap:" + text;
        }
    }

    /**
     * Hotspots are described from perspective of the icon image! E.g. LOWER_RIGHT_CORNER
     * means that the lower right corner of the icon image is placed upon the Geoitems lat/lon position
     */
    public enum Hotspot {
        CENTER(0.5f, 0.5f),
        BOTTOM_CENTER(0.5f, 1f),
        TOP_CENTER(0.5f, 0f),
        RIGHT_CENTER(1f, 0.5f),
        LEFT_CENTER(0f, 0.5f),
        UPPER_RIGHT_CORNER(1f, 0f),
        LOWER_RIGHT_CORNER(1f, 1f),
        UPPER_LEFT_CORNER(0f, 0f),
        LOWER_LEFT_CORNER(0f, 1f);

        public final float xAnchor;
        public final float yAnchor;

        Hotspot(final float xAnchor, final float yAnchor) {
            this.xAnchor = xAnchor;
            this.yAnchor = yAnchor;
        }

    }

    private GeoIcon(@Nullable final BitmapProvider bitmapProvider, final float xAnchor, final float yAnchor, final float rotation, final boolean flat) {
        this.bitmapProvider = bitmapProvider;
        this.xAnchor = xAnchor;
        this.yAnchor = yAnchor;
        this.rotation = rotation;
        this.flat = flat;
    }

    @Nullable
    public Bitmap getBitmap() {
        return bitmapProvider == null ? null : bitmapProvider.getBitmap();
    }

    @Nullable
    public Bitmap getRotatedBitmap() {
        final Bitmap bm = bitmapProvider == null ? null : bitmapProvider.getRotatedBitmap(getRotation());
        ensureBmSizes(bm == null ? null : new Pair<>(bm.getWidth(), bm.getHeight()));
        return bm;
    }


    /** Horizontal distance, normalized to [0, 1], of the anchor from the left edge */
    public float getXAnchor() {
        return xAnchor;
    }

    /** Vertical distance, normalized to [0, 1], of the anchor from the top edge. */
    public float getYAnchor() {
        return yAnchor;
    }

    /** Rotation angle for this icon in degrees (0-360°) */
    public float getRotation() {
        return rotation;
    }

    /**
     * if the marker should rotate together with the map (flat mode) or should be displayed as billboard popup. Default is billboard.
     */
    public boolean isFlat() {
        return flat;
    }

    public boolean touchesIcon(final Geopoint tap, final Geopoint iconBase, @Nullable final ToScreenProjector toScreenCoordFunc) {
        if (tap == null || iconBase == null || toScreenCoordFunc == null) {
            return false;
        }
        ensureBmSizes();
        return GeoItemUtils.touchesPixelArea(tap, iconBase, bmWidth, bmHeight, xAnchor, yAnchor, toScreenCoordFunc);
    }

    private void ensureBmSizes() {
        if (bmHeight >= 0) {
            return;
        }
        ensureBmSizes(bitmapProvider == null ? null : bitmapProvider.getRotatedBitmapDimensions(getRotation()));
    }
    private void ensureBmSizes(final Pair<Integer, Integer> bmSizes) {
        if (bmHeight >= 0) {
            return;
        }
        if (bmSizes == null) {
            bmWidth = 0;
            bmHeight = 0;
        } else {
            bmWidth = bmSizes.first;
            bmHeight = bmSizes.second;
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    public Builder buildUpon() {
        return builder().setBitmapProvider(bitmapProvider).setXAnchor(xAnchor).setYAnchor(yAnchor).setRotation(rotation).setFlat(flat);
    }

    //equals/hashCode

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GeoIcon)) {
            return false;
        }
        final GeoIcon other = (GeoIcon) o;
        return
            Objects.equals(bitmapProvider, other.bitmapProvider) &&
            Objects.equals(xAnchor, other.xAnchor) &&
            Objects.equals(yAnchor, other.yAnchor) &&
            Objects.equals(rotation, other.rotation) &&
            Objects.equals(flat, other.flat);
    }

    @Override
    public int hashCode() {
        return (bitmapProvider == null ? 7 : bitmapProvider.hashCode()) ^ (int) rotation;
    }

    @Override
    @NonNull
    public String toString() {
        return "bm:" + bitmapProvider + ", angle:" + getRotation() + ", x/yAnchor:" + xAnchor + "/" + yAnchor + ", flat:" + flat;
    }


    //Builder stuff

    public static class Builder {

        @Nullable private BitmapProvider bitmapProvider;
        private float xAnchor;
        private float yAnchor;
        private float rotation;
        private boolean flat = false;

        private Builder() {
            setHotspot(Hotspot.CENTER);
        }

        public Builder setBitmap(@Nullable final Bitmap bitmap) {
            return setBitmapProvider(new SimpleBitmapProvider(bitmap));
        }

        public Builder setText(@Nullable final String text) {
            return setBitmapProvider(new TextBitmapProvider(text));
        }

        public Builder setBitmapProvider(@Nullable final BitmapProvider bitmapProvider) {
            this.bitmapProvider = bitmapProvider;
            return this;
        }

        public Builder setXAnchor(final float xAnchor) {
            this.xAnchor = Math.min(1f, Math.max(0f, xAnchor));
            return this;
        }

        /** Convenience method to set x/yAnchor to commonly used hotspots */
        public Builder setHotspot(final Hotspot hotspot) {
            return setXAnchor(hotspot.xAnchor).setYAnchor(hotspot.yAnchor);
        }

        public Builder setYAnchor(final float yAnchor) {
            this.yAnchor = Math.min(1f, Math.max(0f, yAnchor));
            return this;
        }

        public Builder setRotation(final float rotation) {
            this.rotation = rotation;
            return this;
        }

        /** if the marker should rotate together with the map (flat mode) or should be displayed as billboard popup. */
        public Builder setFlat(final boolean flat) {
            this.flat = flat;
            return this;
        }

        public GeoIcon build() {
            return new GeoIcon(bitmapProvider, xAnchor, yAnchor, rotation, flat);
        }
    }

    // parcelable stuff

    protected GeoIcon(final Parcel in) {
        bitmapProvider = in.readParcelable(BitmapProvider.class.getClassLoader());
        xAnchor = in.readFloat();
        yAnchor = in.readFloat();
        rotation = in.readFloat();
        flat = in.readInt() > 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(bitmapProvider, flags);
        dest.writeFloat(xAnchor);
        dest.writeFloat(yAnchor);
        dest.writeFloat(rotation);
        dest.writeInt(flat ? 1 : 0); // writeBoolean requires API 29
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
