package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;

import androidx.annotation.NonNull;

public class ImageResult {

    @NonNull
    private final StatusCode postResult;
    @NonNull
    private final String imageUri;

    public ImageResult(@NonNull final StatusCode postResult, @NonNull final String imageUri) {
        this.postResult = postResult;
        this.imageUri = imageUri;
    }

    @NonNull
    public StatusCode getPostResult() {
        return postResult;
    }

    @NonNull
    public String getImageUri() {
        return imageUri;
    }

}
