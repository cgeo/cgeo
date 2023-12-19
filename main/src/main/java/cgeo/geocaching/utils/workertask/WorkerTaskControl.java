package cgeo.geocaching.utils.workertask;

import androidx.annotation.Nullable;

public interface WorkerTaskControl<I> {
    boolean start(I input);

    boolean cancel();

    boolean restart(I input);

    void disconnect();

    boolean isConnected();

    boolean isRunning();

    @Nullable
    String getGlobalTaskId();
}
