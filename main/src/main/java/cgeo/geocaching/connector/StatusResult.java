package cgeo.geocaching.connector;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.StatusCode;

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StatusResult {

    @NonNull
    private final StatusCode postResult;

    @Nullable
    private String postServerMessage;

    public StatusResult(@NonNull final StatusCode postResult) {
        this(postResult, null);
    }

    public StatusResult(@NonNull final StatusCode postResult, @Nullable final String postServerMessage) {
        this.postResult = postResult;
        this.postServerMessage = postServerMessage;
    }

    @NonNull
    public StatusCode getStatusCode() {
        return postResult;
    }

    @Nullable
    public String getPostServerMessage() {
        return postServerMessage;
    }

    public void setPostServerMessage(@NonNull final String postServerMessage) {
        this.postServerMessage = postServerMessage;
    }

    @NonNull
    public String getErrorString(final Resources res) {
        String errorString = postResult.getErrorString(res);
        if (postServerMessage != null) {
            errorString += "\n\n" + res.getString(R.string.err_website_message) + "\n" + postServerMessage;
        }
        return errorString;
    }
}
