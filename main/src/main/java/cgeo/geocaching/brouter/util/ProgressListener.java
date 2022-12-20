package cgeo.geocaching.brouter.util;

public interface ProgressListener {
    void updateProgress(String progress);

    boolean isCanceled();
}
