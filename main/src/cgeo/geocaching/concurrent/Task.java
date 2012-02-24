package cgeo.geocaching.concurrent;

/**
 * Basic class for Runnables added to ThreadPool.
 */
public abstract class Task implements Runnable {
    private String name = null;

    public Task(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
