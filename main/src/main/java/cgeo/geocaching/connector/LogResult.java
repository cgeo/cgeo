package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LogResult {

    @NonNull
    private final StatusCode postLogResult;
    @NonNull
    private final String logId;
    @Nullable
    private final String serviceLogId;

    public LogResult(@NonNull final StatusCode postLogResult, @NonNull final String logId) {
        this(postLogResult, logId, logId);
    }

    public LogResult(@NonNull final StatusCode postLogResult, @NonNull final String logId, @Nullable final String serviceLogId) {
        this.postLogResult = postLogResult;
        this.logId = logId;
        this.serviceLogId = serviceLogId;
    }

    @NonNull
    public StatusCode getPostLogResult() {
        return postLogResult;
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
