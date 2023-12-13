package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;

import androidx.annotation.NonNull;

public class ImageResult extends StatusResult {

    @NonNull
    private final String imageUri;
    private final String serviceImageId;

    public ImageResult(@NonNull final StatusCode statusCode, @NonNull final String imageUri, final String serviceImageId) {
        super(statusCode);
        this.imageUri = imageUri;
        this.serviceImageId = serviceImageId;
    }

    public ImageResult(@NonNull final StatusCode statusCode) {
        this(statusCode, "", "");
    }

    @NonNull
    public String getImageUri() {
        return imageUri;
    }

    public String getServiceImageId() {
        return serviceImageId;
    }
}
