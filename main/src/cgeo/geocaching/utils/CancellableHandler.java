package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.StringRes;

/**
 * Handler with a cancel policy. Once cancelled, the handler will not handle
 * any more cancel or regular message.
 */
public abstract class CancellableHandler extends Handler {

    public static final int DONE = -1000;
    protected static final int UPDATE_LOAD_PROGRESS_DETAIL = 42186;
    private volatile boolean cancelled = false;
    private final CompositeSubscription subscriptions = new CompositeSubscription();

    public CancellableHandler(final Looper serviceLooper) {
        super(serviceLooper);
    }

    public CancellableHandler() {
        super();
    }

    private static class CancelHolder {
        final Object payload;

        CancelHolder(final Object payload) {
            this.payload = payload;
        }
    }

    @Override
    public final void handleMessage(final Message message) {
        if (cancelled) {
            return;
        }

        if (message.obj instanceof CancelHolder) {
            cancelled = true;
            subscriptions.unsubscribe();
            handleCancel(((CancelHolder) message.obj).payload);
        } else {
            handleRegularMessage(message);
        }
    }

    /**
     * Add a subscription to the list of subscriptions to be subscribed at cancellation time.
     */
    public final void unsubscribeIfCancelled(final Subscription subscription) {
        subscriptions.add(subscription);
        if (cancelled) {
            // Protect against race conditions
            subscriptions.unsubscribe();
        }
    }

    /**
     * Handle a non-cancel message.<br>
     * Subclasses must implement this to handle messages.
     *
     * @param message
     *            the message to handle
     */
    protected abstract void handleRegularMessage(final Message message);

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
        return obtainMessage(0, new CancelHolder(extra));
    }

    /**
     * Cancel the current handler. This can be called from any thread.
     */
    public void cancel() {
        cancelMessage().sendToTarget();
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

    public static void sendLoadProgressDetail(final Handler handler, @StringRes final int resourceId) {
        if (handler != null) {
            handler.obtainMessage(UPDATE_LOAD_PROGRESS_DETAIL, CgeoApplication.getInstance().getString(resourceId)).sendToTarget();
        }
    }
}
