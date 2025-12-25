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

import cgeo.geocaching.R
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.utils.LocalizationUtils

import androidx.annotation.NonNull
import androidx.annotation.Nullable

class StatusResult {

    public static val OK: StatusResult = StatusResult(StatusCode.NO_ERROR, null)

    private final StatusCode postResult

    private final String postServerMessage

    public StatusResult(final StatusCode postResult, final String postServerMessage) {
        this.postResult = postResult
        this.postServerMessage = postServerMessage
    }

    public StatusCode getStatusCode() {
        return postResult
    }

    public String getPostServerMessage() {
        return postServerMessage
    }

    public String getErrorString() {
        String errorString = postResult.getErrorString()
        if (postServerMessage != null) {
            errorString += "\n\n" + LocalizationUtils.getString(R.string.err_website_message) + "\n" + postServerMessage
        }
        return errorString
    }

    public Boolean isOk() {
        return StatusCode.NO_ERROR == postResult
    }

    override     public String toString() {
        return postResult + (postServerMessage == null ? "" : "(" + postServerMessage + ")")
    }
}
