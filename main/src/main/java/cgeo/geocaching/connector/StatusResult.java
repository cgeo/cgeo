package cgeo.geocaching.connector;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StatusResult {

    public static final StatusResult OK = new StatusResult(StatusCode.NO_ERROR, null);

    @NonNull
    private final StatusCode postResult;

    @Nullable
    private final String postServerMessage;

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

    @NonNull
    public String getErrorString() {
        String errorString = postResult.getErrorString();
        if (postServerMessage != null) {
            errorString += "\n\n" + LocalizationUtils.getString(R.string.err_website_message) + "\n" + postServerMessage;
        }
        return errorString;
    }

    public boolean isOk() {
        return StatusCode.NO_ERROR == postResult;
    }

    @NonNull
    @Override
    public String toString() {
        return postResult + (postServerMessage == null ? "" : "(" + postServerMessage + ")");
    }
}
