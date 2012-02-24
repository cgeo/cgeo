package cgeo.geocaching.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * Helper class for setting Thread priority in ThreadPool.
 */
public class PriorityThreadFactory implements ThreadFactory {
    private int priority;

    public PriorityThreadFactory(int priority) {
        this.priority = priority;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread result = new Thread(r);
        result.setPriority(this.priority);
        return result;
    }

}
