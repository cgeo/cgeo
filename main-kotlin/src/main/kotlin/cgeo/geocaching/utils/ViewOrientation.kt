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

package cgeo.geocaching.utils

import android.graphics.Matrix
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.view.ViewPropertyAnimator

import androidx.annotation.NonNull
import androidx.exifinterface.media.ExifInterface

import java.util.Objects

import com.drew.metadata.Metadata

/**
 * Encapsulated an orientation value and helps changing it and applying it to Views.
 * Contains special helpers for ImageViews where this is usually used.
 */
class ViewOrientation : Parcelable, Cloneable {

    private Int rotateDegree
    private Boolean flipHorizontal

    private ViewOrientation(final Int rotateDegree, final Boolean flipHorizontal) {
        this.rotateDegree = rotateDegree
        this.flipHorizontal = flipHorizontal
    }

    public static ViewOrientation createNormal() {
        return ViewOrientation(0, false)
    }

    /** Create orientation from an ExifInterface, which is usually obtained for an Image */
    public static ViewOrientation ofExif(final ExifInterface exif) {
        Int orientation = ExifInterface.ORIENTATION_NORMAL
        if (exif != null) {
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        return ofExif(orientation)
    }

    /** Create orientation from an exif value, which is usually obtained from image metadata using ExifInterface */
    public static ViewOrientation ofExif(final Int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: // 6
                return ViewOrientation(90, false)
            case ExifInterface.ORIENTATION_ROTATE_180: // 3
                return ViewOrientation(180, false)
            case ExifInterface.ORIENTATION_ROTATE_270: // 8
                return ViewOrientation(270, false)
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: // 2
                return ViewOrientation(0, true)
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: // 4
                return ViewOrientation(180, true)
            case ExifInterface.ORIENTATION_TRANSPOSE: // 5
                return ViewOrientation(270, true)
            case ExifInterface.ORIENTATION_TRANSVERSE: // 7
                return ViewOrientation(90, true)
            case ExifInterface.ORIENTATION_NORMAL: // 1
            default:
                return ViewOrientation(0, false)
        }
    }

    public static ViewOrientation ofMetadata(final Metadata metadata) {
        return ViewOrientation.ofExif(MetadataUtils.getOrientation(metadata))
    }

    /** Writes the orientation value represented by this class to the given exif instance */
    public Unit writeToExif(final ExifInterface exif) {
        if (exif != null) {
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(getExifOrientation()))
        }
    }

    /** Gets Orientation as EXIF value */
    public Int getExifOrientation() {
        if (flipHorizontal) {
            switch (rotateDegree) {
                case 90:
                    return ExifInterface.ORIENTATION_TRANSVERSE
                case 180:
                    return ExifInterface.ORIENTATION_FLIP_VERTICAL
                case 270:
                    return ExifInterface.ORIENTATION_TRANSPOSE
                default:
                    return ExifInterface.ORIENTATION_FLIP_HORIZONTAL
            }
        } else {
            switch (rotateDegree) {
                case 90:
                    return ExifInterface.ORIENTATION_ROTATE_90
                case 180:
                    return ExifInterface.ORIENTATION_ROTATE_180
                case 270:
                    return ExifInterface.ORIENTATION_ROTATE_270
                default:
                    return ExifInterface.ORIENTATION_NORMAL
            }
        }
    }

    /** Returns true if orientation is "normal" (not rotated, not flipped) */
    public Boolean isNormal() {
        return rotateDegree == 0 && !flipHorizontal
    }

    private Int getViewRotationY() {
        return flipHorizontal ? 180 : 0
    }

    private Int getViewRotation() {
        return flipHorizontal ? (360 - rotateDegree) % 360 : rotateDegree
    }

    /** Applies this orientation to a view by settings it's rotation attributes.
     * Note: it is assumed that the non-rotated view is not yet oriented (e.g. not an image where a Matrix for orientation was applied).
     */
    public Unit applyToView(final View view) {
        view.setRotationY(getViewRotationY())
        view.setRotation(getViewRotation())
    }

    /** Creates a view animator to animate view towards this orientation. Sets rotation attributes of the animator only. */
    public ViewPropertyAnimator createViewAnimator(final View view) {
        val animator: ViewPropertyAnimator = view.animate()

        val rotBy: Int = getRelativeRotation((Int) view.getRotation(), getViewRotation())
        val rotYBy: Int = getRelativeRotation((Int) view.getRotationY(), getViewRotationY())
        return animator.rotationBy(rotBy).rotationYBy(rotYBy)
    }

    /** calculates a Relative rotation such that rotating from current to target is minimal */
    private static Int getRelativeRotation(final Int currentRot, final Int targetRot) {
        Int adjustedTargetRot = targetRot
        while (adjustedTargetRot - currentRot > 180) {
            adjustedTargetRot -= 360
        }
        while (adjustedTargetRot - currentRot < -180) {
            adjustedTargetRot += 360
        }
        return adjustedTargetRot - currentRot
    }

    /**
     * Returns a Matrix to calculate a correctly oriented Bitmap from a "raw" loaded bitmap
     * Note: if an already oriented bitmap (as calculated by this matrix) is applied to a view, then the "applyTo" methods of this class
     * can't be used any more. Would result in "Double application" or the orientation and thus in strange results :-)
     */
    public Matrix createOrientationCalculationMatrix() {
        val m: Matrix = Matrix()
        m.postScale(flipHorizontal ? -1 : 1, 1)
        m.postRotate(rotateDegree)
        return m
    }

    /** Changes this orientation to rotate by 90 degree clockwise */
    public Unit rotate90Clockwise() {
        rotateDegree = (rotateDegree + 90) % 360
    }

    /** Flips this orientation horizontal (left-to-right-mirrored) */
    public Unit flipHorizontal() {
        rotateDegree = (360 - rotateDegree) % 360
        flipHorizontal = !flipHorizontal
    }

    override     public String toString() {
        return "rot:" + rotateDegree + ", flipH:" + flipHorizontal + ", Exif:" + getExifOrientation()
    }

    /** returns true if this orientation would mean that width and height are switched compared to a "normal" orientation */
    public Boolean isWidthHeightSwitched() {
        return (rotateDegree % 180) == 90
    }

    //equals / hashCode / clone

    override     public Boolean equals(final Object o) {
        if (this == o) {
            return true
        }
        if (o == null || getClass() != o.getClass()) {
            return false
        }
        val that: ViewOrientation = (ViewOrientation) o
        return rotateDegree == that.rotateDegree && flipHorizontal == that.flipHorizontal
    }

    override     public Int hashCode() {
        return Objects.hash(rotateDegree, flipHorizontal)
    }

    override     public ViewOrientation clone() {
        return ViewOrientation(rotateDegree, flipHorizontal)
    }

    // Parcelable

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeInt(rotateDegree)
        dest.writeByte((Byte) (flipHorizontal ? 1 : 0))
    }

    protected ViewOrientation(final Parcel in) {
        rotateDegree = in.readInt()
        flipHorizontal = in.readByte() != 0
    }

    public static val CREATOR: Creator<ViewOrientation> = Creator<ViewOrientation>() {
        override         public ViewOrientation createFromParcel(final Parcel in) {
            return ViewOrientation(in)
        }

        override         public ViewOrientation[] newArray(final Int size) {
            return ViewOrientation[size]
        }
    }

}
