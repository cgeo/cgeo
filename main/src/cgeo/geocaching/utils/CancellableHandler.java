package cgeo.geocaching.utils;

import android.os.Handler;
import android.os.Message;

/**
 * Handler with a cancel policy. Once cancelled, the handler will not handle
 * any more cancel or regular message.
 */
public abstract class CancellableHandler extends Handler {

    private boolean cancelled = false;

    private static class CancelHolder {
        final Object payload;

        CancelHolder(final Object payload) {
            this.payload = payload;
        }
    }

    @Override
    final public void handleMessage(final Message message) {
        if (cancelled) {
            return;
        }

        if (message.obj instanceof CancelHolder) {
            cancelled = true;
            handleCancel(((CancelHolder) message.obj).payload);
        } else {
            handleRegularMessage(message);
        }
    }

    /**
     * Handle a non-cancel message.<br>
     * Subclasses must implement this to handle messages.
     *
     * @param message
     *            the message to handle
     */
    abstract protected void handleRegularMessage(final Message message);

    /**
     * Handle a cancel message.
     *
     * @param extra
     *            the additional payload given by the canceller
     */
    protected void handleCancel(final Object extra) {
    }

    /**
     * Get a cancel message that can later be sent to this handler to cancel it.
     *
     * @return a cancel message
     */
    public Message cancelMessage() {
        return cancelMessage(null);
    }

    /**
     * Get a cancel message with an additional parameter that can later be sent to
     * this handler to cancel it.
     *
     * @param extra
     *            the extra parameter to give to the cancel handler
     * @return a cancel message
     */
    public Message cancelMessage(final Object extra) {
        return this.obtainMessage(0, new CancelHolder(extra));
    }

    /**
     * Cancel the current handler. This can be called from any thread.
     */
    public void cancel() {
        cancel(null);
    }

    /**
     * Cancel the current handler. This can be called from any thread.
     *
     * @param extra
     *            the extra parameter to give to the cancel handler
     */
    public void cancel(final Object extra) {
        cancelMessage(extra).sendToTarget();
    }

    /**
     * Check if the current handler has been cancelled.
     *
     * @return true if the handler has been cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Check if a handler has been cancelled.
     *
     * @param handler
     *            a handler, or null
     * @return true if the handler is not null and has been cancelled
     */
    public static boolean isCancelled(final CancellableHandler handler) {
        return handler != null && handler.isCancelled();
    }

}
