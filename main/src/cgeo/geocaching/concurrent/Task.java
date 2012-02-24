package cgeo.geocaching.concurrent;


public abstract class Task implements Runnable {
    private String name = null;

    public Task(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
