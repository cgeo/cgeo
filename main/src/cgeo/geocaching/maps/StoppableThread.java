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

    /**
     * @return true when this Thread has been requested to stop.
     */

    public boolean isStopped() {
        return stop;
    }

    /**
     * Requests this Thread to stop.
     */

    public void stopIt() {
        stop = true;
    }
}
