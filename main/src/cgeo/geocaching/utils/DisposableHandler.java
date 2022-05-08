package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Handler with a dispose policy. Once disposed, the handler will not handle
 * any more dispose or regular message.
 */
public abstract class DisposableHandler extends Handler implements Disposable {

    public static final int DONE = -1000;
    protected static final int UPDATE_LOAD_PROGRESS_DETAIL = 42186;
    protected static final int UPDATE_SHOW_STATUS_TOAST = 42187;
    private final CompositeDisposable disposables = new CompositeDisposable();

    protected DisposableHandler(final Looper serviceLooper) {
        super(serviceLooper);
    }

    protected DisposableHandler() {
        super();
    }

    private static class CancelHolder {
        // CANCEL is used to synchronously dispose the DisposableHandler and call
        // the appropriate callback.
        static final int CANCEL = -1;
        // When dispose() has been called, CANCEL_CALLBACK is used to synchronously
        // call the appropriate callback.
        static final int CANCEL_CALLBACK = -2;

        final int kind;

        CancelHolder(final int kind) {
            this.kind = kind;
        }
    }

    @Override
    public final void handleMessage(final Message message) {
        if (message.obj instanceof CancelHolder) {
            final CancelHolder holder = (CancelHolder) message.obj;
            if (holder.kind == CancelHolder.CANCEL && !isDisposed()) {
                disposables.dispose();
                handleDispose();
            } else if (holder.kind == CancelHolder.CANCEL_CALLBACK) {
                // We have been disposed already but the callback has not been called yet.
                handleDispose();
            }
        } else if (!isDisposed()) {
            handleRegularMessage(message);
        }
    }

    /**
     * Add a disposable to the list of disposables to be disposed at disposition time.
     */
    public final void add(final Disposable disposable) {
        disposables.add(disposable);
    }

    /**
     * Handle a non-dispose message.<br>
     * Subclasses must implement this to handle messages.
     *
     * @param message the message to handle
     */
    protected abstract void handleRegularMessage(Message message);

    /**
     * Handle a dispose message.
     *
     * This is called on the handler looper thread when the handler gets disposed.
     */
    protected void handleDispose() {
        // May be overwritten by inheriting classes.
    }

    /**
     * Get a dispose message that can later be sent to this handler to dispose it.
     *
     * @return a message that, when sent, will dispose the current handler.
     */
    public Message disposeMessage() {
        return obtainMessage(0, new CancelHolder(CancelHolder.CANCEL));
    }

    /**
     * Cancel the current handler. This can be called from any thread. The disposables
     * added with {@link #add(Disposable)} will be disposed immediately, while the
     * {@link #handleDispose()} callback will be called synchronously by the handler.
     */
    @Override
    public void dispose() {
        disposables.dispose();
        obtainMessage(0, new CancelHolder(CancelHolder.CANCEL_CALLBACK)).sendToTarget();
    }

    /**
     * Check if the current handler has been disposed.
     *
     * @return true if the handler has been disposed
     */
    @Override
    public boolean isDisposed() {
        return disposables.isDisposed();
    }

    /**
     * Check if a handler has been disposed.
     *
     * @param handler a handler, or null
     * @return true if the handler is not null and has been disposed
     */
    public static boolean isDisposed(final DisposableHandler handler) {
        return handler != null && handler.isDisposed();
    }

    public static void sendLoadProgressDetail(@Nullable final Handler handler, @StringRes final int resourceId) {
        if (handler != null) {
            handler.obtainMessage(UPDATE_LOAD_PROGRESS_DETAIL, CgeoApplication.getInstance().getString(resourceId)).sendToTarget();
        }
    }

    public static void sendShowStatusToast(@Nullable final Handler handler, @StringRes final int resourceId) {
        if (handler != null) {
            handler.obtainMessage(UPDATE_SHOW_STATUS_TOAST, CgeoApplication.getInstance().getString(resourceId)).sendToTarget();
        }
    }
}
