package cgeo.geocaching.utils.workertask;

import cgeo.geocaching.utils.AndroidRxUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Scheduler;

public class WorkerTaskConfiguration<I, P, R> {

    @NonNull
    public final String globalTaskId;

    @NonNull
    public final Supplier<WorkerTaskLogic<I, P, R>> taskSupplier;

    @Nullable
    public LifecycleOwner owner;

    public final List<Consumer<WorkerTaskEvent<I, P, R>>> taskListeners = new ArrayList<>();

    @Nullable
    public Scheduler taskScheduler;

    @Nullable
    public Scheduler listenerScheduler;

    @Nullable
    public Consumer<R> noObserverAction;

    @NonNull
    public final WorkerTaskControl<I> taskControl = new DelegateWorkerTaskControl<>();

    private WorkerTaskConfiguration(@NonNull final String globalTaskId, @NonNull final Supplier<WorkerTaskLogic<I, P, R>> taskSupplier) {
        this.globalTaskId = globalTaskId;
        this.taskSupplier = taskSupplier;
    }

    /** Creates task configuration with given global id and task logic supplier */
    public static <I, P, R> WorkerTaskConfiguration<I, P, R> of(final String globalTaskId, final Supplier<WorkerTaskLogic<I, P, R>> taskSupplier) {
        return new WorkerTaskConfiguration<>(globalTaskId, taskSupplier);
    }

    /** Sets task owner. If this is set, task is auto-disconnected from owner if owner is DESTROYED */
    public WorkerTaskConfiguration<I, P, R> setOwner(@Nullable final LifecycleOwner owner) {
        this.owner = owner;
        return this;
    }


    /** Adds a generic listener to task lifecycle events */
    public WorkerTaskConfiguration<I, P, R> addTaskListener(final Consumer<WorkerTaskEvent<I, P, R>> taskListener) {
        return addTaskListener(null, taskListener);
    }

    /** Adds a generic listener to task lifecycle events */
    public WorkerTaskConfiguration<I, P, R> addTaskListener(final WorkerTaskEventType eventType, final Consumer<WorkerTaskEvent<I, P, R>> taskListener) {
        if (taskListener != null) {
            if (eventType == null) {
                this.taskListeners.add(taskListener);
            } else {
                this.taskListeners.add(event -> {
                    if (event.type == eventType) {
                        taskListener.accept(event);
                    }
                });
            }
        }
        return this;
    }

    /** adds a listener for task progress to this task. Consumer will be executed on UI thread */
    public WorkerTaskConfiguration<I, P, R> addProgressListener(final Consumer<P> consumer) {
        if (consumer != null) {
            addTaskListener(WorkerTaskEventType.PROGRESS, event -> consumer.accept(event.progress));
        }
        return this;
    }

    /** adds a listener for task result to this task. Consumer will be executed on UI thread */
    public WorkerTaskConfiguration<I, P, R> addResultListener(final Consumer<R> consumer) {
        if (consumer != null) {
            addTaskListener(WorkerTaskEventType.FINISHED, event -> consumer.accept(event.result));
        }
        return this;
    }

    /** Sets the scheduler on which the background task is run. Default is {@link AndroidRxUtils#networkScheduler} */
    public WorkerTaskConfiguration<I, P, R> setTaskScheduler(final Scheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        return this;
    }

    /** Sets the scheduler on which listeners are executed. Defaults to Android Main/UI Thread */
    public WorkerTaskConfiguration<I, P, R> setListenerScheduler(final Scheduler listenerScheduler) {
        this.listenerScheduler = listenerScheduler;
        return this;
    }

    /** Sets action to be executed if at time of finishing no observer is connected to task. Will be executed on taskScheduler! */
    public WorkerTaskConfiguration<I, P, R> setNoObserverAction(final Consumer<R> noObserverAction) {
        this.noObserverAction = noObserverAction;
        return this;
    }


    /** Adds a feature to the task, e.g. {@link ProgressDialogFeature} */
    public WorkerTaskConfiguration<I, P, R> addFeature(final Consumer<WorkerTaskConfiguration<?, ?, ?>> featureApplier) {
        if (featureApplier != null) {
            featureApplier.accept(this);
        }
        return this;
    }

    /** Returns an interface which with task can be controlled later */
    public WorkerTaskControl<I> getTaskControl() {
        return this.taskControl;
    }
}
