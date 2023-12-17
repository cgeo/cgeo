package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageResult extends StatusResult {

    @NonNull
    private final String imageUri;
    private final String serviceImageId;

    public static ImageResult ok(@NonNull final String imageUri, @Nullable final String serviceImageId) {
        return new ImageResult(StatusCode.NO_ERROR, null, imageUri, serviceImageId);
    }

    public static ImageResult ok(@NonNull final String imageUri) {
        return ok(imageUri, "");
    }

    public static ImageResult error(@NonNull final StatusCode statusCode, final String msg, final Throwable t) {
        return new ImageResult(statusCode == StatusCode.NO_ERROR ? StatusCode.LOGIMAGE_POST_ERROR : statusCode,
            msg + (t == null ? "" : ": " + t), "", "");
    }

    public static ImageResult error(@NonNull final StatusCode statusCode) {
        return error(statusCode, null, null);
    }

    private ImageResult(@NonNull final StatusCode statusCode, final String msg, @NonNull final String imageUri, final String serviceImageId) {
        super(statusCode, msg);
        this.imageUri = imageUri;
        this.serviceImageId = serviceImageId;
    }


    @NonNull
    public String getImageUri() {
        return imageUri;
    }

    public String getServiceImageId() {
        return serviceImageId;
    }
}
