package cgeo.geocaching.utils;

import android.graphics.Matrix;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewPropertyAnimator;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import java.util.Objects;

/**
 * Encapsulated an orientation value and helps changing it and applying it to Views.
 * Contains special helpers for ImageViews where this is usually used.
 */
public class ViewOrientation implements Parcelable, Cloneable {

    private int rotateDegree;
    private boolean flipHorizontal;

    private ViewOrientation(final int rotateDegree, final boolean flipHorizontal) {
        this.rotateDegree = rotateDegree;
        this.flipHorizontal = flipHorizontal;
    }

    public static ViewOrientation createNormal() {
        return new ViewOrientation(0, false);
    }

    /** Create orientation from an ExifInterface, which is usually obtained for an Image */
    public static ViewOrientation ofExif(final ExifInterface exif) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        if (exif != null) {
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }
        return ofExif(orientation);
    }

    /** Create orientation from an exif value, which is usually obtained from image metadata using ExifInterface */
    public static ViewOrientation ofExif(final int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: // 6
                return new ViewOrientation(90, false);
            case ExifInterface.ORIENTATION_ROTATE_180: // 3
                return new ViewOrientation(180, false);
            case ExifInterface.ORIENTATION_ROTATE_270: // 8
                return new ViewOrientation(270, false);
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: // 2
                return new ViewOrientation(0, true);
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: // 4
                return new ViewOrientation(180, true);
            case ExifInterface.ORIENTATION_TRANSPOSE: // 5
                return new ViewOrientation(270, true);
            case ExifInterface.ORIENTATION_TRANSVERSE: // 7
                return new ViewOrientation(90, true);
            case ExifInterface.ORIENTATION_NORMAL: // 1
            default:
                return new ViewOrientation(0, false);
        }
    }

    /** Writes the orientation value represented by this class to the given exif instance */
    public void writeToExif(final ExifInterface exif) {
        if (exif != null) {
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(getExifOrientation()));
        }
    }

    /** Gets Orientation as EXIF value */
    public int getExifOrientation() {
        if (flipHorizontal) {
            switch (rotateDegree) {
                case 90:
                    return ExifInterface.ORIENTATION_TRANSVERSE;
                case 180:
                    return ExifInterface.ORIENTATION_FLIP_VERTICAL;
                case 270:
                    return ExifInterface.ORIENTATION_TRANSPOSE;
                default:
                    return ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
            }
        } else {
            switch (rotateDegree) {
                case 90:
                    return ExifInterface.ORIENTATION_ROTATE_90;
                case 180:
                    return ExifInterface.ORIENTATION_ROTATE_180;
                case 270:
                    return ExifInterface.ORIENTATION_ROTATE_270;
                default:
                    return ExifInterface.ORIENTATION_NORMAL;
            }
        }
    }

    /** Returns true if orientation is "normal" (not rotated, not flipped) */
    public boolean isNormal() {
        return rotateDegree == 0 && !flipHorizontal;
    }

    private int getViewRotationY() {
        return flipHorizontal ? 180 : 0;
    }

    private int getViewRotation() {
        return flipHorizontal ? (360 - rotateDegree) % 360 : rotateDegree;
    }

    /** Applies this orientation to a view by settings it's rotation attributes.
     * Note: it is assumed that the non-rotated view is not yet oriented (e.g. not an image where a Matrix for orientation was applied).
     */
    public void applyToView(final View view) {
        view.setRotationY(getViewRotationY());
        view.setRotation(getViewRotation());
    }

    /** Creates a view animator to animate view towards this orientation. Sets rotation attributes of the animator only. */
    public ViewPropertyAnimator createViewAnimator(final View view) {
        final ViewPropertyAnimator animator = view.animate();

        final int rotBy = getRelativeRotation((int) view.getRotation(), getViewRotation());
        final int rotYBy = getRelativeRotation((int) view.getRotationY(), getViewRotationY());
        return animator.rotationBy(rotBy).rotationYBy(rotYBy);
    }

    /** calculates a Relative rotation such that rotating from current to target is minimal */
    private static int getRelativeRotation(final int currentRot, final int targetRot) {
        int adjustedTargetRot = targetRot;
        while (adjustedTargetRot - currentRot > 180) {
            adjustedTargetRot -= 360;
        }
        while (adjustedTargetRot - currentRot < -180) {
            adjustedTargetRot += 360;
        }
        return adjustedTargetRot - currentRot;
    }

    /**
     * Returns a Matrix to calculate a correctly oriented Bitmap from a "raw" loaded bitmap
     * Note: if an already oriented bitmap (as calculated by this matrix) is applied to a view, then the "applyTo" methods of this class
     * can't be used any more. Would result in "double application" or the orientation and thus in strange results :-)
     */
    public Matrix createOrientationCalculationMatrix() {
        final Matrix m = new Matrix();
        m.postScale(flipHorizontal ? -1 : 1, 1);
        m.postRotate(rotateDegree);
        return m;
    }

    /** Changes this orientation to rotate by 90 degree clockwise */
    public void rotate90Clockwise() {
        rotateDegree = (rotateDegree + 90) % 360;
    }

    /** Changes this orientation to rotate by 180 degree */
    public void rotate180() {
        rotate90Clockwise();
        rotate90Clockwise();
    }

    /** Changes this orientation to rotate by 270 degree clockwise */
    public void rotate270Clockwise() {
        rotate180();
        rotate90Clockwise();
    }

    /** Flips this orientation horizontal (left-to-right-mirrored) */
    public void flipHorizontal() {
        rotateDegree = (360 - rotateDegree) % 360;
        flipHorizontal = !flipHorizontal;
    }

    /** Flip this orientation vertical (top-to-bottom-mirrored) */
    public void flipVertical() {
        rotate90Clockwise();
        flipHorizontal();
        rotate90Clockwise();
    }

    @Override
    @NonNull
    public String toString() {
        return "rot:" + rotateDegree + ", flipH:" + flipHorizontal + ", Exif:" + getExifOrientation();
    }

    /** returns true if this orientation would mean that width and height are switched compared to a "normal" orientation */
    public boolean isWidthHeightSwitched() {
        return (rotateDegree % 180) == 90;
    }

    //equals / hashCode / clone

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ViewOrientation that = (ViewOrientation) o;
        return rotateDegree == that.rotateDegree && flipHorizontal == that.flipHorizontal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rotateDegree, flipHorizontal);
    }

    @NonNull
    @Override
    public ViewOrientation clone() {
        return new ViewOrientation(rotateDegree, flipHorizontal);
    }

    // Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(rotateDegree);
        dest.writeByte((byte) (flipHorizontal ? 1 : 0));
    }

    protected ViewOrientation(final Parcel in) {
        rotateDegree = in.readInt();
        flipHorizontal = in.readByte() != 0;
    }

    public static final Creator<ViewOrientation> CREATOR = new Creator<ViewOrientation>() {
        @Override
        public ViewOrientation createFromParcel(final Parcel in) {
            return new ViewOrientation(in);
        }

        @Override
        public ViewOrientation[] newArray(final int size) {
            return new ViewOrientation[size];
        }
    };

}
