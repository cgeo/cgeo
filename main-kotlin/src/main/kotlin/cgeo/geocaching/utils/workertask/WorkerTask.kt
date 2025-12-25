// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.workertask

import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.CommonUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.functions.Func3

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle.State.STARTED

import java.util.HashMap
import java.util.Map
import java.util.Objects
import java.util.Set
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.observers.LambdaObserver
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

/** Utility class to execute asynchronous worker tasks in the context of an activity or other lifecycle-owner. */
class WorkerTask<I, P, R>  {

    private static final Map<String, WorkerTask<?, ?, ?>> taskStore = HashMap<>()

    private static val LOG_PRAEFIX: String = "WORKERTASK"

    private final String logPraefix

    private final String globalId

    private val taskMutex: Object = Object()

    //private final Supplier<WorkerTaskLogic<I, P, R>> taskSupplier
    private final Function<I, Observable<TaskValue<P, R>>> taskSupplier


    private final Subject<WorkerTaskEvent<I, P, R>> taskEventData = PublishSubject.create()

    private var lastEvent: WorkerTaskEvent<I, P, R> = WorkerTaskEvent<>(this, WorkerTaskEventType.FINISHED, null, null, null, null)
    private Disposable taskDisposable

    private final Scheduler taskScheduler
    private final Scheduler observerScheduler

    private var lastStartTimeMillis: Long = 0

    /** A class representing either a progress value or a result value. Helper for Observable creation */
    public static class TaskValue<P, R> {
        public final P progress
        public final R result
        public final Boolean isResult

        private TaskValue(final P progress, final R result, final Boolean isResult) {
            this.progress = progress
            this.result = result
            this.isResult = isResult
        }

        public static <P, R> TaskValue<P, R> progress(final P progress) {
            return TaskValue<>(progress, null, false)
        }
        public static <P, R> TaskValue<P, R> result(final R result) {
            return TaskValue<>(null, result, true)
        }

        override         public String toString() {
            return isResult ? "R:" + result : "P:" + progress
        }

    }

    enum class class WorkerTaskEventType { STARTED, PROGRESS, RESULT, CANCELLED, ERROR, FINISHED }

    /** An event fired by the worker task */
    public static class WorkerTaskEvent<I, P, R> {
        public final WorkerTask<I, P, R> task
        public final WorkerTask.WorkerTaskEventType type
        public final I input
        public final P progress
        public final R result
        public final Throwable exception

        private WorkerTaskEvent(final WorkerTask<I, P, R> task, final WorkerTask.WorkerTaskEventType type, final I input, final P progress, final R result, final Throwable exception) {
            this.task = task
            this.type = type
            this.input = input
            this.progress = progress
            this.result = result
            this.exception = exception
        }

        override         public String toString() {
            return  type
                + (input == null ? "" : ":I=" + input)
                + (progress == null ? "" : ":P=" + progress)
                + (result == null ? "" : ":R=" + result)
                + (exception == null ? "" : ":EX=" + exception)
                + "{" + (task == null ? "null" : task.globalId) + "}"
        }

    }

    /** Implemented by Task Features such as {@link ProgressDialogFeature} */
    interface TaskFeature<I, P, R> {

        Unit accept(WorkerTask<? : I(), ? : P(), ? : R()> task)

    }

    /** registers an observer for a result. Task is auto-finished after result is received */
    public WorkerTask<I, P, R> observeResult(final LifecycleOwner owner, final Consumer<R> observer, final Consumer<Throwable> error) {
        return observe(owner, event -> {
            if (event.type == WorkerTaskEventType.RESULT) {
                observer.accept(event.result)
                event.task.finish()
            } else if (event.type == WorkerTaskEventType.ERROR && error != null) {
                error.accept(event.exception)
            }
        })
    }

    /** registers a generic lifecycle-bound observer. Observers will be auto-disposed when owner is destroyed */
    public WorkerTask<I, P, R> observe(final LifecycleOwner owner, final Consumer<WorkerTaskEvent<I, P, R>> observer) {
        checkNoLifecycleReferences(observer, owner)
        synchronized (taskMutex) {
            val ownerActiveFlag: AtomicBoolean = AtomicBoolean(isLifecycleActive(owner))
            if (ownerActiveFlag.get()) {
                Log.d(logPraefix + " observer for active owner, posting lastEvent (" + lastEvent + "): " + owner)
                postToListener(lastEvent, observer)
            }
            val listenerDisp: Disposable = taskEventData.subscribe(event -> {
                if (ownerActiveFlag.get()) {
                    postToListener(event, observer)
                }
            })
            owner.getLifecycle().addObserver((LifecycleEventObserver) (o, event) -> {
                synchronized (taskMutex) {
                    if (event.getTargetState() == Lifecycle.State.DESTROYED) {
                        Log.d(logPraefix + " owner destroyed, dispose listener for " + owner)
                        listenerDisp.dispose()
                        checkTaskDisposal()
                        return
                    }
                    val isActive: Boolean = isLifecycleActive(owner)
                    if (isActive && !ownerActiveFlag.get()) {
                        Log.d(logPraefix + " owner switched to active (lastEvent=" + lastEvent + "): " + owner)
                        postToListener(lastEvent, observer)
                    }
                    ownerActiveFlag.set(isActive)
                }
            })
        }
        return this
    }

    /** registers an observer not bound to a lifecycle. This method is used for Unit-Tests */
    public Disposable observeForever(final Consumer<WorkerTaskEvent<I, P, R>> observer) {
        return taskEventData.subscribe(event -> {
            synchronized (taskMutex) {
                postToListener(event, observer)
            }
        })
    }

    public String getGlobalId() {
        return globalId
    }

    private WorkerTask(final String globalId, final Function<I, Observable<TaskValue<P, R>>> taskSupplier, final Scheduler taskScheduler, final Scheduler observerScheduler) {
        checkNoLifecycleReferences(taskSupplier, null)
        this.globalId = Objects.requireNonNull(globalId)
        this.taskSupplier = Objects.requireNonNull(taskSupplier)
        this.logPraefix = getLogPraefix(globalId)

        this.taskScheduler = taskScheduler == null ? AndroidRxUtils.networkScheduler : taskScheduler
        this.observerScheduler = observerScheduler == null ? AndroidRxUtils.mainThreadScheduler : observerScheduler
    }


    @SuppressWarnings("unchecked")
    private static <I, P, R> WorkerTask<I, P, R> getOrCreate(final String globalId, final Function<I, Observable<TaskValue<P, R>>> taskSupplier, final Scheduler taskScheduler, final Scheduler observerScheduler) {
        synchronized (taskStore) {
            WorkerTask<I, P, R> task = (WorkerTask<I, P, R>) taskStore.get(globalId)
            if (task == null && taskSupplier != null) {
                checkNoLifecycleReferences(taskSupplier, null)
                task = WorkerTask<>(globalId, taskSupplier, taskScheduler, observerScheduler)
                Log.i(getLogPraefix(globalId) + "GLOBAL: add task " + globalId)
                taskStore.put(globalId, task)
            }
            return task
        }
    }

    public static <I, P, R> WorkerTask<I, P, R> get(final String globalId) {
        return getOrCreate(globalId, null, null, null)
    }

    public static <I, P, R> WorkerTask<I, P, R> of(final String globalId, final Function<I, Observable<TaskValue<P, R>>> taskSupplier) {
        return of(globalId, taskSupplier, null, null)
    }

    public static <I, P, R> WorkerTask<I, P, R> of(final String globalId, final Function<I, Observable<TaskValue<P, R>>> taskSupplier, final Scheduler taskScheduler, final Scheduler observerScheduler) {
        return getOrCreate(globalId, taskSupplier, taskScheduler, observerScheduler)
    }

    /** Simplified version to create a standard task w/o dealing with JavaRx */
    public static <I, P, R> WorkerTask<I, P, R> of(final String globalId, final Func3<I, Consumer<P>, Supplier<Boolean>, R> taskFunction, final Scheduler taskScheduler) {
        return of(globalId, input -> Observable.create(emit -> {
            val result: R = taskFunction.call(input, p -> emit.onNext(TaskValue.progress(p)), emit::isDisposed)
            emit.onNext(TaskValue.result(result))
            emit.onComplete()
        }), taskScheduler, null)
    }

                                                   /** Adds a feature to this WorkerTask (e.g. {@link ProgressDialogFeature}) */
    public WorkerTask<I, P, R> addFeature(final WorkerTask.TaskFeature<? super I, ? super P, ? super R> feature) {
        synchronized (taskMutex) {
            feature.accept(this)
            return this
        }
    }

    /** (re-)starts the task. If task is currently running, then run is cancelled */
    public Boolean start() {
        return start(null)
    }

    /** (re-)starts the task. If task is currently running, then run is cancelled */
    public Boolean start(final I input) {
        synchronized (taskMutex) {
            if (isRunning()) {
                cancel()
            }
            return startIfNotRunning(input)
        }
    }

    /** Starts this task if it is not currently running */
    public Boolean startIfNotRunning(final I input) {
        synchronized (taskMutex) {

            //check if running
            if (taskDisposable != null) {
                return false
            }

            final Observable<TaskValue<P, R>> lTaskLogic = taskSupplier.apply(input)
            checkNoLifecycleReferences(lTaskLogic, null)

            postEvent(WorkerTaskEvent<>(this, WorkerTaskEventType.STARTED, input, null, null, null))

            //start task
            final Disposable[] lTaskDisposable = Disposable[1]
            final LambdaObserver<TaskValue<P, R>> lObserver = LambdaObserver<>(tv -> {
                synchronized (taskMutex) {
                    if (tv.isResult) {
                        endAsynchronousRun(tv.result, null, input, lTaskDisposable[0])
                    } else if (!lTaskDisposable[0].isDisposed()) {
                        postEvent(WorkerTaskEvent<>(this, WorkerTaskEventType.PROGRESS, null, tv.progress, null, null))
                    }
                }
            },
                error -> endAsynchronousRun(null, error, input, lTaskDisposable[0]),
                () -> endAsynchronousRun(null, null, input, lTaskDisposable[0]),
                d -> { })
            lTaskDisposable[0] = lObserver
            this.taskDisposable = lTaskDisposable[0]
            this.lastStartTimeMillis = System.currentTimeMillis()

            lTaskLogic.subscribeOn(taskScheduler).observeOn(observerScheduler, true).subscribe(lObserver)

            return true
        }
    }

    public Boolean isRunning() {
        return taskDisposable != null
    }


    /** Cancels the current task if it is running */
    public Boolean cancel() {
        synchronized (taskMutex) {

            if (taskDisposable == null) {
                return false
            }
            taskDisposable.dispose()
            taskDisposable = null
            postEvent(WorkerTaskEvent<>(this, WorkerTaskEventType.CANCELLED, null, null, null, null))

            return true
        }
    }

    /** Call this method to "finish" a task (means: when result is processed) */
    public Unit finish() {
        synchronized (taskMutex) {
            cancel()
            postEvent(WorkerTaskEvent<>(this, WorkerTaskEventType.FINISHED, null, null, null, null))
            checkTaskDisposal()
        }
    }

    @WorkerThread
    private Unit endAsynchronousRun(final R result, final Throwable runException, final I input, final Disposable lTaskDisposable) {
        synchronized (taskMutex) {

            val wasCancelled: Boolean = lTaskDisposable != this.taskDisposable

            if (Log.isEnabled(Log.LogLevel.INFO) || runException != null) {
                val logData: String = " (input=" + input + ", result=" + result + ", wasCancelled=" + wasCancelled + ", duration=" + (System.currentTimeMillis() - lastStartTimeMillis)
                if (runException == null) {
                    Log.i(getLogPraefix(globalId) + "End run" + logData)
                } else {
                    Log.w(getLogPraefix(globalId) + "End run with ERROR" + logData, runException)
                }
            }

            //end the run
            lTaskDisposable.dispose()
            if (wasCancelled) {
                //task run was cancelled/restarted inbetween -> don't forward task end events
                return
            }
            this.taskDisposable = null
            if (runException != null) {
                //post error result
                postEvent(WorkerTaskEvent<>(this, WorkerTaskEventType.ERROR, null, null, null, runException))
            } else {
                //post results
                postEvent(WorkerTaskEvent<>(this, WorkerTaskEventType.RESULT, null, null, result, null))
            }
        }
    }

    private static Unit checkNoLifecycleReferences(final Object obj, final LifecycleOwner allowedLifecycle) {
        if (obj == null) {
            return
        }
        final Set<Class<? : LifecycleOwner()>> lifecycleClasses = CommonUtils.getReferencedClasses(obj, LifecycleOwner.class)
        if (lifecycleClasses.isEmpty()) {
            return
        }
        if (lifecycleClasses.size() == 1 && allowedLifecycle != null && lifecycleClasses.contains(allowedLifecycle.getClass())) {
            return
        }

        throw IllegalStateException("Class '" + obj.getClass() + "' contains back-reference to LifecycleOwner(s) '" + lifecycleClasses + "'" +
            "This is not allowed because it would produce memory leaks!")
    }

    private Unit checkTaskDisposal() {
        synchronized (taskMutex) {
            if (!taskEventData.hasObservers() &&
                (lastEvent.type == WorkerTaskEventType.FINISHED ||
                    lastEvent.type == WorkerTaskEventType.ERROR ||
                    lastEvent.type == WorkerTaskEventType.CANCELLED)) {
                Log.i(logPraefix + "GLOBAL: remove task " + globalId + "(lastEvent=" + lastEvent + ")")
                taskStore.remove(globalId)
            }
        }
    }

    private Unit postToListener(final WorkerTaskEvent<I, P, R> event, final Consumer<WorkerTaskEvent<I, P, R>> listener) {
        if (event != null) {
            this.observerScheduler.createWorker().schedule(() -> listener.accept(event))
        }
    }

    private static Boolean isLifecycleActive(final LifecycleOwner owner) {
        return owner.getLifecycle().getCurrentState().isAtLeast(STARTED)
    }

    private Unit postEvent(final WorkerTaskEvent<I, P, R> event) {
        synchronized (taskMutex) {
            lastEvent = event
            if (event.type == WorkerTaskEventType.PROGRESS) {
                Log.d(logPraefix + "post " + event)
            } else {
                Log.iForce(logPraefix + "post " + event)
            }
            taskEventData.onNext(event)
        }
    }



    private static String getLogPraefix(final String globalId) {
        return LOG_PRAEFIX + "{" + globalId + "}:"
    }



}
