package cgeo.geocaching.utils.workertask;

import androidx.annotation.NonNull;

public class WorkerTaskEvent<I, P, R> {
    public final WorkerTaskLogic<I, P, R> task;
    public final WorkerTaskEventType type;
    public final I input;
    public final P progress;
    public final R result;

    WorkerTaskEvent(final WorkerTaskLogic<I, P, R> task, final WorkerTaskEventType type, final I input, final P progress, final R result) {
        this.task = task;
        this.type = type;
        this.input = input;
        this.progress = progress;
        this.result = result;
    }

    WorkerTaskEvent(final WorkerTaskLogic<I, P, R> task, final WorkerTaskEventType type) {
        this(task, type, null, null, null);
    }

    @NonNull
    @Override
    public String toString() {
        return type + (input == null ? "" : ":I=" + input) + (progress == null ? "" : ":P=" + progress) + (result == null ? "" : ":R=" + result);
    }
}
