package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;

import org.eclipse.jdt.annotation.NonNull;

public class LogResult {

    @NonNull
    private final StatusCode postLogResult;
    @NonNull
    private final String logId;

    public LogResult(@NonNull final StatusCode postLogResult, @NonNull final String logId) {
        this.postLogResult = postLogResult;
        this.logId = logId;
    }

    @NonNull
    public StatusCode getPostLogResult() {
        return postLogResult;
    }

    @NonNull
    public String getLogId() {
        return logId;
    }

}
