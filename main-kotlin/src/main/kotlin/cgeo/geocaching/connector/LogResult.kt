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

class LogResult : StatusResult() {

    private final String logId
    private final String serviceLogId

    public static LogResult ok(final String logId, final String serviceLogId) {
        return LogResult(StatusCode.NO_ERROR, null, logId, serviceLogId)
    }

    public static LogResult ok(final String logId) {
        return ok(logId, logId)
    }

    public static LogResult error(final StatusCode statusCode, final String msg, final Throwable t) {
        return LogResult(statusCode == StatusCode.NO_ERROR ? StatusCode.LOG_POST_ERROR : statusCode,
            msg + (t == null ? "" : ": " + t), "", "")
    }

    public static LogResult error(final StatusCode statusCode) {
        return error(statusCode, null, null)
    }

    private LogResult(final StatusCode postLogResult, final String msg, final String logId, final String serviceLogId) {
        super(postLogResult, msg)
        this.logId = logId
        this.serviceLogId = serviceLogId
    }

    public String getLogId() {
        return logId
    }

    public String getServiceLogId() {
        return serviceLogId
    }

    override     public String toString() {
        return "LogResult:" + super.toString() + "/logId=" + logId + "/serviceLogId=" + serviceLogId
    }
}
