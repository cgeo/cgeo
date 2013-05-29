package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;

public class LogResult {

    private final StatusCode postLogResult;
    private final String logId;

    public LogResult(StatusCode postLogResult, String logId) {
        this.postLogResult = postLogResult;
        this.logId = logId;
    }

    public StatusCode getPostLogResult() {
        return postLogResult;
    }

    public String getLogId() {
        return logId;
    }

}
