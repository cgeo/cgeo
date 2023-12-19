package cgeo.geocaching.utils.workertask;

import androidx.annotation.Nullable;

public class DelegateWorkerTaskControl<I> implements WorkerTaskControl<I> {

    private WorkerTaskControl<I> delegate;

    public void setDelegate(final WorkerTaskControl<I> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean start(final I input) {
        return delegate.start(input);
    }

    @Override
    public boolean cancel() {
        return delegate.cancel();
    }

    @Override
    public boolean restart(final I input) {
        return delegate.restart(input);
    }

    @Override
    public void disconnect() {
        delegate.disconnect();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Nullable
    @Override
    public String getGlobalTaskId() {
        return delegate.getGlobalTaskId();
    }


}
