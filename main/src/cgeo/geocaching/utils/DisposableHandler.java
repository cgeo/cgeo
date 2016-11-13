package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.StringRes;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Handler with a dispose policy. Once disposed, the handler will not handle
 * any more dispose or regular message.
 */
public abstract class DisposableHandler extends Handler implements Disposable {

    public static final int DONE = -1000;
    protected static final int UPDATE_LOAD_PROGRESS_DETAIL = 42186;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public DisposableHandler(final Looper serviceLooper) {
        super(serviceLooper);
    }

    public DisposableHandler() {
        super();
    }

    private static class CancelHolder {
        static final int CANCEL = -1;
        static final int CANCEL_CALLBACK = -2;

        final int kind;
        final Object payload;

        CancelHolder(final int kind, final Object payload) {
            this.kind = kind;
            this.payload = payload;
        }
    }

    @Override
    public final void handleMessage(final Message message) {
        if (message.obj instanceof CancelHolder) {
            final CancelHolder holder = (CancelHolder) message.obj;
            if (holder.kind == CancelHolder.CANCEL && !isDisposed()) {
                disposables.dispose();
                handleDispose(holder.payload);
            } else if (holder.kind == CancelHolder.CANCEL_CALLBACK) {
                handleDispose(holder.payload);
            }
        } else if (!isDisposed()) {
            handleRegularMessage(message);
        }
    }

    /**
     * Add a disposable to the list of disposables to be disposed at cancellation time.
     */
    public final void add(final Disposable disposable) {
        disposables.add(disposable);
    }

    /**
     * Handle a non-dispose message.<br>
     * Subclasses must implement this to handle messages.
     *
     * @param message
     *            the message to handle
     */
    protected abstract void handleRegularMessage(final Message message);

    /**
     * Handle a dispose message.
     *
     * @param extra
     *            the additional payload given by the canceller
     */
    protected void handleDispose(final Object extra) {
    }

    /**
     * Get a dispose message that can later be sent to this handler to dispose it.
     *
     * @return a dispose message
     */
    public Message cancelMessage() {
        return cancelMessage(null);
    }

    /**
     * Get a dispose message with an additional parameter that can later be sent to
     * this handler to dispose it.
     *
     * @param extra
     *            the extra parameter to give to the dispose handler
     * @return a dispose message
     */
    public Message cancelMessage(final Object extra) {
        return obtainMessage(0, new CancelHolder(CancelHolder.CANCEL, extra));
    }

    /**
     * Cancel the current handler. This can be called from any thread.
     */
    public void dispose() {
        disposables.dispose();
        obtainMessage(0, new CancelHolder(CancelHolder.CANCEL_CALLBACK, null)).sendToTarget();
    }

    /**
     * Check if the current handler has been disposed.
     *
     * @return true if the handler has been disposed
     */
    public boolean isDisposed() {
        return disposables.isDisposed();
    }

    /**
     * Check if a handler has been disposed.
     *
     * @param handler
     *            a handler, or null
     * @return true if the handler is not null and has been disposed
     */
    public static boolean isDisposed(final DisposableHandler handler) {
        return handler != null && handler.isDisposed();
    }

    public static void sendLoadProgressDetail(final Handler handler, @StringRes final int resourceId) {
        if (handler != null) {
            handler.obtainMessage(UPDATE_LOAD_PROGRESS_DETAIL, CgeoApplication.getInstance().getString(resourceId)).sendToTarget();
        }
    }
}
