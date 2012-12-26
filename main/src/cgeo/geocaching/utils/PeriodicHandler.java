package cgeo.geocaching.utils;

import android.os.Handler;
import android.os.Message;

/**
 * A PeriodicHandler class helps with the implementation of a periodic
 * action embedded in a thread with a looper such as the UI thread.
 * The act() method will be called periodically. The clock may drift
 * as the implementation does not target real-time actions.
 *
 * The handler will be interrupted if the device goes to sleep.
 *
 */
abstract public class PeriodicHandler extends Handler {

    final static private int START = 0;
    final static private int STOP = 1;
    final static private int ACT = 2;

    final private long period;

    /**
     * Create a new PeriodicHandler object.
     *
     * @param period
     *            The period in milliseconds.
     */
    protected PeriodicHandler(final long period) {
        this.period = period;
    }

    /**
     * Subclasses of PeriodicHandler must implement this method.
     */
    abstract public void act();

    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case START:
                removeMessages(ACT);
                sendEmptyMessage(ACT);
                break;
            case STOP:
                removeMessages(ACT);
                break;
            case ACT:
                sendEmptyMessageDelayed(ACT, period);
                act();
        }
    }

    /**
     * Start the periodic handler.
     *
     * Restarting a handler that is already started will only
     * reset its clock.
     */
    public void start() {
        sendEmptyMessage(START);
    }

    /**
     * Stop the periodic handler.
     *
     * If this method is called from the looper thread, this call is
     * guaranteed to be synchronous.
     */
    public void stop() {
        sendMessageAtFrontOfQueue(obtainMessage(STOP));
    }

}
