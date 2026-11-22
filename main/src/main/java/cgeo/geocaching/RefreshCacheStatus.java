package cgeo.geocaching;

final class RefreshCacheStatus {

    private boolean failed;

    void markFailed() {
        failed = true;
    }

    String getCompletionText(final String successText) {
        return failed ? null : successText;
    }
}
