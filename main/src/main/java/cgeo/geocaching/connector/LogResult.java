package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LogResult extends StatusResult {

    @NonNull
    private final String logId;
    @Nullable
    private final String serviceLogId;

    public static LogResult ok(@NonNull final String logId, @Nullable final String serviceLogId) {
        return new LogResult(StatusCode.NO_ERROR, null, logId, serviceLogId);
    }

    public static LogResult ok(@NonNull final String logId) {
        return ok(logId, logId);
    }

    public static LogResult error(@NonNull final StatusCode statusCode, final String msg, final Throwable t) {
        return new LogResult(statusCode == StatusCode.NO_ERROR ? StatusCode.LOG_POST_ERROR : statusCode,
            msg + (t == null ? "" : ": " + t), "", "");
    }

    public static LogResult error(@NonNull final StatusCode statusCode) {
        return error(statusCode, null, null);
    }

    private LogResult(@NonNull final StatusCode postLogResult, final String msg, @NonNull final String logId, @Nullable final String serviceLogId) {
        super(postLogResult, msg);
        this.logId = logId;
        this.serviceLogId = serviceLogId;
    }

    @NonNull
    public String getLogId() {
        return logId;
    }

    @Nullable
    public String getServiceLogId() {
        return serviceLogId;
    }

}
