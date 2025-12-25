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

package cgeo.geocaching.connector

import cgeo.geocaching.enumerations.StatusCode

import androidx.annotation.NonNull
import androidx.annotation.Nullable

class ImageResult : StatusResult() {

    private final String imageUri
    private final String serviceImageId

    public static ImageResult ok(final String imageUri, final String serviceImageId) {
        return ImageResult(StatusCode.NO_ERROR, null, imageUri, serviceImageId)
    }

    public static ImageResult ok(final String imageUri) {
        return ok(imageUri, "")
    }

    public static ImageResult error(final StatusCode statusCode, final String msg, final Throwable t) {
        return ImageResult(statusCode == StatusCode.NO_ERROR ? StatusCode.LOGIMAGE_POST_ERROR : statusCode,
            msg + (t == null ? "" : ": " + t), "", "")
    }

    public static ImageResult error(final StatusCode statusCode) {
        return error(statusCode, null, null)
    }

    private ImageResult(final StatusCode statusCode, final String msg, final String imageUri, final String serviceImageId) {
        super(statusCode, msg)
        this.imageUri = imageUri
        this.serviceImageId = serviceImageId
    }


    public String getImageUri() {
        return imageUri
    }

    public String getServiceImageId() {
        return serviceImageId
    }

    override     public String toString() {
        return "ImageResult:" + super.toString() + "/serviceImageId=" + serviceImageId + "/imageUri=" + imageUri
    }
}
