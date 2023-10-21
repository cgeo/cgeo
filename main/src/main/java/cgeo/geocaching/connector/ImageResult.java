package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;

import androidx.annotation.NonNull;

public class ImageResult extends StatusResult {

    @NonNull
    private final String imageUri;

    public ImageResult(@NonNull final StatusCode statusCode, @NonNull final String imageUri) {
        super(statusCode);
        this.imageUri = imageUri;
    }

    @NonNull
    public String getImageUri() {
        return imageUri;
    }

}
