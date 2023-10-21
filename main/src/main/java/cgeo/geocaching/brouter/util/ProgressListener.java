package cgeo.geocaching.brouter.util;

public interface ProgressListener {
    void updateProgress(String task, int progress);

    boolean isCanceled();
}
