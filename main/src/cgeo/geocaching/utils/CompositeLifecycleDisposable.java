package cgeo.geocaching.utils;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.disposables.DisposableContainer;

/**
 * Collect {@link Disposable} and clear them when a certain lifecycle event is reached.
 */
public class CompositeLifecycleDisposable implements LifecycleEventObserver, DisposableContainer {

    private final Lifecycle.Event untilEvent;
    private final CompositeDisposable disposable = new CompositeDisposable();

    public CompositeLifecycleDisposable(final Lifecycle.Event untilEvent) {
        this.untilEvent = untilEvent;
    }

    public CompositeLifecycleDisposable(final LifecycleOwner source, final Lifecycle.Event untilEvent) {
        this.untilEvent = untilEvent;
        source.getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@NonNull final LifecycleOwner source, @NonNull final Lifecycle.Event event) {
        if (event == untilEvent) {
            disposable.clear();
        }
    }

    @Override
    public boolean add(@NonNull final Disposable d) {
        return disposable.add(d);
    }

    public boolean addAll(@NonNull final Disposable... disposables) {
        return disposable.addAll(disposables);
    }

    @Override
    public boolean remove(@NonNull final Disposable d) {
        return disposable.remove(d);
    }

    @Override
    public boolean delete(@NonNull final Disposable d) {
        return disposable.delete(d);
    }

    public void clear() {
        disposable.clear();
    }
}
