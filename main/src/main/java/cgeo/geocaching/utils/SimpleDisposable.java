package cgeo.geocaching.utils;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.Disposable;

public class SimpleDisposable implements Disposable {

    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    private final Runnable onDispose;

    public SimpleDisposable(final Runnable onDispose) {
        this.onDispose = onDispose;
    }

    @Override
    public void dispose() {
        // instances of this class are handed across threads (see ListenerHelper, Translator), so claim the
        // disposal atomically - otherwise two disposers could both run onDispose
        if (isDisposed.compareAndSet(false, true)) {
            this.onDispose.run();
        }
    }

    @Override
    public boolean isDisposed() {
        return isDisposed.get();
    }
}
