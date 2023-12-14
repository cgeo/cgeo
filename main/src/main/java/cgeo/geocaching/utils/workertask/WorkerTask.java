package cgeo.geocaching.utils.workertask;

import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * Utility class to execute asynchronous worker tasks in the context of an activity.
 * <br>
 * While worker tasks are usually created and controlled in activities, they may span multiple activites
 * and continue running in the background even when an activity is destroyed. When the same or
 * another activity is accessing the same task while it is still running in background, task is reconnected
 */
public class WorkerTask<I, P, R> implements WorkerTaskControl<I> {

    private static final TaskModelStore taskModelStore = new TaskModelStore();

    private final AtomicBoolean isConnected = new AtomicBoolean(true);

    private SharedTaskModel<I, P, R> sharedModel;

    // The following vars contain references to the owner/activity
    // and thus shall NOT be part of shared model! If we do this, this will result in memory leaks
    private LifecycleOwner owner;

    private List<Consumer<WorkerTaskEvent<I, P, R>>> taskListeners;
    private Disposable taskListenerDisposable;
    private final Scheduler listenerScheduler;


    /** The tasks model to be shared */
    private static class SharedTaskModel<I, P, R> { // extends ViewModel {

        //Immutable properties (set on creation, read-only)
        private final Object taskMutex = new Object();
        private final String globalTaskId;
        private final Supplier<WorkerTaskLogic<I, P, R>> taskSupplier;
        private final Scheduler taskScheduler;
        private final Consumer<R> noObserverAction;

        //mutable properties (inner value changed during task lifetime)
        private final AtomicInteger activeOwnerReferenceCounter = new AtomicInteger(0);
        private final AtomicBoolean isRunning = new AtomicBoolean(false);

        private WorkerTaskLogic<I, P, R> currentTask;

        private AtomicBoolean currentRunCancelFlag;

        private final Subject<WorkerTaskEvent<I, P, R>> taskEventData = PublishSubject.create();

        private SharedTaskModel(final String globalTaskId, final WorkerTaskConfiguration<I, P, R> config) {
            this.globalTaskId = globalTaskId;
            this.taskSupplier = config.taskSupplier;
            this.taskScheduler = config.taskScheduler == null ? AndroidRxUtils.networkScheduler : config.taskScheduler;
            this.noObserverAction = config.noObserverAction;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class TaskModelStore {

        private final Map<String, SharedTaskModel> store = new HashMap<>();

        public <I, P, R> SharedTaskModel<I, P, R> getOrCreate(final String id, final WorkerTaskConfiguration<I, P, R> config) {
            synchronized (store) {
                SharedTaskModel<I, P, R> model = store.get(id);
                if (model == null) {
                    Log.iForce("TaskStore: add TaskModel for id '" + id + "'");
                    model = new SharedTaskModel<>(id, config);
                    store.put(id, model);
                }
                model.activeOwnerReferenceCounter.addAndGet(1);
                return model;
            }
        }

        public void notifyDisconnect(final String id) {
            synchronized (store) {
                final SharedTaskModel model = store.get(id);
                if (model == null) {
                    throw new IllegalStateException("Dereferenced model for id '" + id + "', but was not there");
                }
                model.activeOwnerReferenceCounter.addAndGet(-1);
                checkRemoval(id);
            }
        }

        public void checkRemoval(final String id) {
            synchronized (store) {
                final SharedTaskModel model = store.get(id);
                if (model == null) {
                    return;
                }
                if (model.activeOwnerReferenceCounter.get() == 0 && !model.isRunning.get()) {
                    Log.iForce("TaskStore: remove TaskModel for id '" + id + "'");
                    store.remove(id);
                }
            }
        }

        public boolean exists(final String id) {
            synchronized (store) {
                return store.containsKey(id);
            }
        }

        public boolean runs(final String id) {
            synchronized (store) {
                final SharedTaskModel model = store.get(id);
                return model != null && model.isRunning.get();
            }
        }

    }

    private WorkerTask(final WorkerTaskConfiguration<I, P, R> config) {

        if (config.owner != null && config.owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            throw new IllegalStateException("Can't create task with id = " + config.globalTaskId + "' in state DESTROYED: " + owner);
        }
        checkNoOwnerReferences(config.taskSupplier, config.owner);
        checkNoOwnerReferences(config.noObserverAction, owner);

        this.sharedModel = taskModelStore.getOrCreate(config.globalTaskId, config);
        this.owner = config.owner;

        //take over configuration
        this.taskListeners = new ArrayList<>(config.taskListeners);
        this.listenerScheduler = config.listenerScheduler == null ? AndroidRxUtils.mainThreadScheduler : config.listenerScheduler;

        //connect taskcontrol
        ((DelegateWorkerTaskControl<I>) config.taskControl).setDelegate(this);

        //if reconnecting, then send reconnect-events
        final WorkerTaskLogic<I, P, R> currentTask = getTaskIfRunning();
        if (currentTask != null) {
            sendTaskEventToListeners(new WorkerTaskEvent<>(currentTask, WorkerTaskEventType.RECONNECTED));
        }

        //subscribe to global task-events
        this.taskListenerDisposable = this.sharedModel.taskEventData.subscribe(this::sendTaskEventToListeners);

        //if owner is given, then register it for automatic disconnect
        if (owner != null) {
            owner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
                if (event.getTargetState() == Lifecycle.State.DESTROYED) {
                    this.disconnect();
                }
            });
        }
    }

    private void sendTaskEventToListeners(final WorkerTaskEvent<I, P, R> event) {
        final List<Consumer<WorkerTaskEvent<I, P, R>>> taskListeners = this.taskListeners;
        this.listenerScheduler.createWorker().schedule(() -> {
            for (Consumer<WorkerTaskEvent<I, P, R>> listeners : taskListeners) {
                listeners.accept(event);
            }
        });
    }

    private WorkerTaskLogic<I, P, R> getTaskIfRunning() {
        synchronized (sharedModel.taskMutex) {
            if (sharedModel.isRunning.get()) {
                return sharedModel.currentTask;
            }
        }
        return null;
    }

    public static boolean taskExists(final String id) {
        return taskModelStore.exists(id);
    }

    public static boolean taskIsRunning(final String id) {
        return taskModelStore.runs(id);
    }


    public static <I, P, R> WorkerTask<I, P, R> create(final WorkerTaskConfiguration<I, P, R> config) {
        return new WorkerTask<>(config);
    }

    /** Returns whether task is currently running */
    @Override
    public boolean isRunning() {
        final SharedTaskModel<I, P, R> model = this.sharedModel;
        if (!isConnected()) {
            return false;
        }
        return model.isRunning.get();
    }

    /** Restarts the task (if it is currently running, it will be canceled, then a new instance will be started) */
    @Override
    public boolean restart(final I input) {
        synchronized (this.sharedModel.taskMutex) {
            if (!isConnected()) {
                return false;
            }
            if (isRunning()) {
                cancel();
            }
            return start(input);
        }
    }

    /** starts the task. If task is currently running, then nothing is done */
    @Override
    public boolean start(final I input) {
        synchronized (this.sharedModel.taskMutex) {
            final SharedTaskModel<I, P, R> model = this.sharedModel;
            if (!isConnected()) {
                return false;
            }
            if (isRunning()) {
                return false;
            }
            final WorkerTaskLogic<I, P, R> task = model.taskSupplier.get();
            checkNoOwnerReferences(task, owner);

            final AtomicBoolean cancelFlag = new AtomicBoolean(false);
            model.currentRunCancelFlag = cancelFlag;
            model.isRunning.set(true);
            model.currentTask = task;

            postNewEvent(model, new WorkerTaskEvent<>(task, WorkerTaskEventType.STARTED, input, null, null));
            model.taskScheduler.createWorker().schedule(() -> runAsyncTask(this.sharedModel, task, input, cancelFlag));

            return true;
        }
    }

    private static <I, P, R> void postNewEvent(final SharedTaskModel<I, P, R> sharedModel, final WorkerTaskEvent<I, P, R> event) {
        sharedModel.taskEventData.onNext(event);
    }

    private static <I, P, R> void runAsyncTask(final SharedTaskModel<I, P, R> sharedModel, final WorkerTaskLogic<I, P, R> task, final I input, final AtomicBoolean cancelFlag) {

        //trigger the actual worker thread run
        final R result = task.run(input, m -> {
            if (!cancelFlag.get()) {
                postNewEvent(sharedModel, new WorkerTaskEvent<>(task, WorkerTaskEventType.PROGRESS, null, m, null));
            }
        }, cancelFlag::get);
        final boolean wasCancelled = cancelFlag.get();
        if (!wasCancelled) {
            postNewEvent(sharedModel, new WorkerTaskEvent<>(task, WorkerTaskEventType.FINISHED, null, null, result));
            if (!sharedModel.taskEventData.hasObservers() && sharedModel.noObserverAction != null) {
                sharedModel.noObserverAction.accept(result);
                AndroidRxUtils.runOnUi(() -> sharedModel.noObserverAction.accept(result));
            }
        } else {
            postNewEvent(sharedModel, new WorkerTaskEvent<>(task, WorkerTaskEventType.CANCELLED, null, null, null));
        }

        //end the run
        synchronized (sharedModel.taskMutex) {
            //check whether run was maybe already ended by 'cancel'
            if (sharedModel.isRunning.get() && sharedModel.currentTask == task) {
                // -> no. If we are here, this is a run which ended "normal" (w/o a cancel)
                sharedModel.isRunning.set(false);
                sharedModel.currentTask = null;
                sharedModel.currentRunCancelFlag = null;
            }
        }
        //run ended. Check whether shared taskModel can be removed from storage
        taskModelStore.checkRemoval(sharedModel.globalTaskId);
    }

    /** Cancels the task's execution (if it is currently running) */
    public boolean cancel() {
        final SharedTaskModel<I, P, R> model = this.sharedModel;
        if (!isConnected()) {
            return false;
        }
        synchronized (model.taskMutex) {
            if (!isRunning()) {
                return false;
            }
            model.currentRunCancelFlag.set(true);
            model.isRunning.set(false);
            model.currentTask = null;
            model.currentRunCancelFlag = null;
            return true;
        }
    }

    /** Returns whether this task instance is still connected to the task */
    @Override
    public boolean isConnected() {
        return isConnected.get();
    }

    @Override
    @Nullable
    public String getGlobalTaskId() {
        final SharedTaskModel<I, P, R> model = this.sharedModel;
        if (!isConnected()) {
            return null;
        }
        return model.globalTaskId;
    }

    /** Disconnects this task instance from the actual task logic (which continues running if not cancelled or finished) */
    @Override
    public void disconnect() {
        final SharedTaskModel<I, P, R> model = this.sharedModel;
        synchronized (model.taskMutex) {
            if (!isConnected()) {
                return;
            }
            isConnected.set(false);
            this.taskListenerDisposable.dispose();
            final WorkerTaskLogic<I, P, R> runningTask = getTaskIfRunning();

            this.sharedModel = null;
            this.taskListenerDisposable = null;
            this.owner = null;

            if (runningTask != null) {
                sendTaskEventToListeners(new WorkerTaskEvent<>(runningTask, WorkerTaskEventType.DISCONNECTED));
            }
            this.taskListeners = null;
        }

        taskModelStore.notifyDisconnect(model.globalTaskId);
    }

    private static void checkNoOwnerReferences(@Nullable final Object obj, @Nullable final Object owner) {
        if (owner == null || obj == null) {
            return;
        }
        if (CommonUtils.containsClassReference(obj, owner.getClass())) {
            throw new IllegalStateException("Class '" + obj.getClass() + "' contains back-reference to Owner '" + owner.getClass().getName() + "'" +
                "This is not allowed because it would produce memory leaks! " +
                "Recommendation: produce tasks using static method reference lambda or static class implementation.");
        }
    }


}
