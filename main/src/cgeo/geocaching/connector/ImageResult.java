package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;

public class ImageResult {

    private final StatusCode postResult;
    private final String imageUri;

    public ImageResult(StatusCode postResult, String imageUri) {
        this.postResult = postResult;
        this.imageUri = imageUri;
    }

    public StatusCode getPostResult() {
        return postResult;
    }

    public String getImageUri() {
        return imageUri;
    }

}
