package cgeo.geocaching.utils;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * A PeriodicHandler class helps with the implementation of a periodic
 * action embedded in a thread with a looper such as the UI thread.
 * The onPeriodic() method of the listener will be called periodically.
 * The clock may drift as the implementation does not target real-time
 * actions.
 *
 * The handler will be interrupted if the device goes to sleep.
 *
 * The handler only keeps a weak reference to the listener. If the listener
 * is garbage-collected without having stopped the timer, the handler will
 * stop itself.
 */
final public class PeriodicHandler extends Handler {

    public static interface PeriodicHandlerListener {
        public void onPeriodic();
    }

    final static private int START = 0;
    final static private int STOP = 1;
    final static private int ACT = 2;

    final private long period;

    final private WeakReference<PeriodicHandlerListener> listenerRef;

    /**
     * Create a new PeriodicHandler object.
     *
     * @param period
     *            The period in milliseconds.
     */
    public PeriodicHandler(final long period, final PeriodicHandlerListener listener) {
        this.period = period;
        listenerRef = new WeakReference<PeriodicHandlerListener>(listener);
    }

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
                final PeriodicHandlerListener listener = listenerRef.get();
                if (listener != null) {
                    sendEmptyMessageDelayed(ACT, period);
                    listener.onPeriodic();
                }
                break;
            default:
                throw new UnsupportedOperationException();
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
