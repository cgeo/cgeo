package cgeo.geocaching.utils;

import io.reactivex.rxjava3.disposables.Disposable;

public class SimpleDisposable implements Disposable {

    private boolean isDisposed = false;
    private final Runnable onDispose;

    public SimpleDisposable(final Runnable onDispose) {
        this.onDispose = onDispose;
    }

    @Override
    public void dispose() {
        if (!isDisposed) {
            this.onDispose.run();
        }
        isDisposed = true;
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}
