package cgeo.geocaching.maps;

/**
 * Thread that offers the infrastructure for stopping.
 *
 * @author keith.paterson
 *
 */

public class StoppableThread extends Thread {

    /** When set to true the subclasses should stop. */
    private boolean stop = false;

    public StoppableThread() {
    }

    public StoppableThread(String name) {
        super(name);
    }

    public boolean isStopped() {
        return stop;
    }

    public synchronized void stopIt() {
        stop = true;
    }
}
