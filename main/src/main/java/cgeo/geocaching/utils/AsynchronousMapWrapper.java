package cgeo.geocaching.utils;

import cgeo.geocaching.utils.functions.Func1;

import android.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wraps and manages a Map of objects which is updated asynchronously.
 *
 * This class provides methods to add, replace and remove objects of type T to the set. It manages under the hood
 * an asynchronous processes to perform those add/removals in the background and with minimal impact on the used thread.
 *
 * Users of this class need to implement interface IMapchangeExecutor to provide implementations for actual add/remove
 * as well as settings for thread usage.
 */
public class AsynchronousMapWrapper<K, V, C> {

    private static final String LOG_PREFIX = AsynchronousMapWrapper.class.getSimpleName() + ": ";

    private final IMapChangeExecutor<K, V, C> changeExecutor;

    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);

    //Command process queue
    private final Queue<Runnable> changeCommandQueue = new LinkedList<>();
    private final CommandRunner commandRunner = new CommandRunner();
    private boolean commandExecutionRunRequested = false;
    private final Lock commandLock = new ReentrantLock();

    //Runner instance which performs updates on map asynchronous
    private final MapChangeRunner mapChangeRunner = new MapChangeRunner();
    private boolean mapChangeRequested = false;

    //Storage for currently stored objects as well as requested actions.
    //Implementation note: every access to these objects must be done under lock and leave them in a consistent state!

    //Currently drawn/visible objects. Value Map is the Google Object representation (needed when removal is requested later)
    private final Map<K, Pair<V, C>> objectMap = new HashMap<>();
    //All drawable objects which are currently requested to be added to map
    private final Map<K, V> requestedToAdd = new HashMap<>();
    //All drawable objects which are currently requested to be removed from map
    private final Set<K> requestedToRemove = new HashSet<>();
    //Current command queue. Contains not-yet-processed add/remove commands as Pair (object, flag), where flag=true means ADD and flag=false means REMOVE
    // Commands may be outdated at time of processing e.g. if same object was requested to be added and removed in this order
    // and those commands were not processed yet. In such a case, commands must be skipped at processing time
    // according to content of requestedToAdd/requestedToRemove
    private final Queue<Pair<K, Boolean>> mapChangeProcessQueue = new LinkedList<>();
    // LOCK to acquire before any access to any of the above members to ensure correct concurrent behaviour
    private final Lock lock = new ReentrantLock();

    //Executor of commands.
    public interface IMapChangeExecutor<K, V, C> {
        C add(K key, V value);
        default C replace(K key, V oldValue, C oldContext, V newValue) {
            remove(key, oldValue, oldContext);
            return add(key, newValue);
        }
        void remove(K key, V value, C context);
        default void runCommandChain(Runnable runnable) {
            runnable.run();
        }
        default void runMapChanges(Runnable runnable) {
            runnable.run();
        }
        default boolean continueMapChangeExecutions(long startTime, int queueLength) {
            return true;
        }

        default void destroy() {
            //empty on purpose
        }
    }

    public AsynchronousMapWrapper(final IMapChangeExecutor<K, V, C> changeExecutor) {
        this.changeExecutor = changeExecutor;
    }

    /** adds objects to this Map */
    public void put(final Map<K, V> toAdd) {
        requestChange(() -> {
            for (Map.Entry<K, V> entry : toAdd.entrySet()) {
                putSingle(entry.getKey(), entry.getValue());
            }
        });
    }

    /** adds object to this Map */
    public void put(final K key, final V value) {
        requestChange(() -> putSingle(key, value));
    }

    public void add(final K key) {
        put(key, null);
    }

    public void remove(final K key) {
        requestChange(() -> removeSingle(key));
    }

    /** removes objects to this Set */
    public void remove(final Collection<? extends K> toRemove) {
        requestChange(() -> {
            for (K opt : toRemove) {
                removeSingle(opt);
            }
        });
    }

    /** removes ALL objects from this Set */
    public void removeAll() {
        requestChange(() -> {
            //clear all existing requests to add/remove
            requestedToAdd.clear();
            requestedToRemove.clear();
            mapChangeProcessQueue.clear();
            //refill pipeline with requests to remove everything which is drawn
            requestedToRemove.addAll(objectMap.keySet());
            for (K o : requestedToRemove) {
                mapChangeProcessQueue.add(new Pair<>(o, false));
            }
        });
    }

    public boolean isDestroyed() {
        return isDestroyed.get();
    }

    public void destroy() {
        isDestroyed.set(true);
        commandLock.lock();
        try {
            changeCommandQueue.clear();
            commandExecutionRunRequested = false;
        } finally {
            commandLock.unlock();
        }
        requestChange(() -> {
            //clear all existing requests to add/remove
            requestedToAdd.clear();
            requestedToRemove.clear();
            mapChangeProcessQueue.clear();
            mapChangeRequested = false;
        });
        changeExecutor.destroy();
    }

    public void replace(final Collection<? extends K> newKeys) {
        replace(newKeys, k -> null);
    }

    /**
     * replaces objects in this Map completely with given objects
     *
     * Same effect could be reached by calling removeAll() followed by add(newObjects),
     * but performance is better when calling replace() method instead
     */
    public void replace(final Map<K, V> newObjects) {
        replace(newObjects.keySet(), newObjects::get);
    }

    private void replace(final Collection<? extends K> newKeys, final Func1<K, V> valueGetter) {
        requestChange(() -> {
            //calculate what to add/remove to currently drawn set to reach toReplace state finally
            final Map<K, V> toAdd = new HashMap<>();
            for (K key : newKeys) {
                toAdd.put(key, valueGetter.call(key));
            }
            final Iterator<Map.Entry<K, V>> it = toAdd.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<K, V> entry = it.next();
                if (!isChange(entry.getKey(), entry.getValue())) {
                    it.remove();
                }
            }

            final Set<K> toRemove = new HashSet<>(objectMap.keySet());
            toRemove.removeAll(newKeys);
            //clear all existing requests to add/remove
            requestedToAdd.clear();
            requestedToRemove.clear();
            mapChangeProcessQueue.clear();
            //refill pipeline with requests to add/remove
            requestedToAdd.putAll(toAdd);
            requestedToRemove.addAll(toRemove);
            for (K o : toRemove) {
                mapChangeProcessQueue.add(new Pair<>(o, false));
            }
            for (K o : toAdd.keySet()) {
                mapChangeProcessQueue.add(new Pair<>(o, true));
            }
        });
    }

    //CALL ONLY WITH ACQUIRED LOCK!
    private void putSingle(final K key, final V value) {
        //make sure that any pending removal requests for this object are removed
        requestedToRemove.remove(key);

        //we need to add only if object is not already there
        if (isChange(key, value)) {
            //only if there was no add request yet we need to add a command to process queue
            if (!requestedToAdd.containsKey(key)) {
                mapChangeProcessQueue.add(new Pair<>(key, true));
            }
            requestedToAdd.put(key, value);
        } else {
            //make sure there is no pending change which might change value
            requestedToAdd.remove(key);
        }
    }

    private boolean isChange(final K key, final V value) {
        if (!objectMap.containsKey(key)) {
            return true;
        }
        final Pair<V, C> currentVc = objectMap.get(key);
        Objects.requireNonNull(currentVc);
        return !Objects.equals(value, currentVc.first);
    }

    //CALL ONLY WITH ACQUIRED LOCK!
    private void removeSingle(final K key) {
        //make sure that any pending add requests for this object are removed
        requestedToAdd.remove(key);

        //we need to delete only if object currently exists
        if (objectMap.containsKey(key) && requestedToRemove.add(key)) {
            //only if there was no remove request yet we need to add a command to process queue
            mapChangeProcessQueue.add(new Pair<>(key, false));
        }
    }

    private void requestChange(final Runnable changeAction) {
        commandLock.lock();
        try {
            changeCommandQueue.add(changeAction);
            if (!commandExecutionRunRequested && !isDestroyed.get()) {
                commandExecutionRunRequested = true;
                this.changeExecutor.runCommandChain(this.commandRunner);
            }
        } finally {
            commandLock.unlock();
        }
    }

    private void processMapChange(final Runnable changeAction) {
        lock.lock();
        try {
            changeAction.run();
            if (!mapChangeRequested && !isDestroyed.get()) {
                mapChangeRequested = true;
                changeExecutor.runMapChanges(mapChangeRunner);
            }
        } finally {
            lock.unlock();
        }
    }

    private class MapChangeRunner implements Runnable {

        //CALL ONLY WITH ACQUIRED LOCK!!!
        private boolean processQueue() {
            final long time = System.currentTimeMillis();
            Pair<K, Boolean> request;
            while ((request = mapChangeProcessQueue.poll()) != null && !isDestroyed.get()) {
                if (request.second) {
                    //ADD request
                    processAddCommand(request.first);
                } else {
                    //REMOVE request
                    processRemoveCommand(request.first);
                }
                if (!changeExecutor.continueMapChangeExecutions(time, mapChangeProcessQueue.size())) {
                    // removing and adding objects to Google Maps are time costly operations and we don't want to block UI thread
                    changeExecutor.runMapChanges(this);
                    return false;
                }
            }
            return true;
        }

        //CALL ONLY WITH ACQUIRED LOCK!!!
        private void processRemoveCommand(final K key) {
            if (isDestroyed.get()) {
                return;
            }
            final boolean stillValid = requestedToRemove.remove(key);
            //if stillValid = false, then the process command was outdated by later changes to requestedToRemove -> in this case ignore it
            if (stillValid) {
                if (objectMap.containsKey(key)) {
                    final Pair<V, C> vc = objectMap.remove(key);
                    Objects.requireNonNull(vc);
                    changeExecutor.remove(key, vc.first, vc.second);
                } else {
                    // at this point, object MUST be in map -> programming bug
                    Log.e(LOG_PREFIX + "requesting non-existing object for removal -> must be programming bug!");
                }
            }
        }
        //CALL ONLY WITH ACQUIRED LOCK!!!
        private void processAddCommand(final K key) {
            if (isDestroyed.get()) {
                return;
            }
            final boolean stillValid = requestedToAdd.containsKey(key);
            //if stillValid = false, then the process command was outdated by later changes to requestedToRemove -> in this case ignore it
            if (stillValid) {
                final V newValue = requestedToAdd.remove(key);
                final boolean alreadyExists = objectMap.containsKey(key);
                final C ctx;
                if (alreadyExists) {
                    final Pair<V, C> oldVc = objectMap.get(key);
                    ctx = changeExecutor.replace(key, oldVc == null ? null : oldVc.first, oldVc == null ? null : oldVc.second, newValue);
                } else {
                    ctx = changeExecutor.add(key, newValue);
                }
                objectMap.put(key, new Pair<>(newValue, ctx));
            }
        }

        @Override
        public void run() {
            Log.iForce("AsyncMapWrapper: run Thread");
            lock.lock();
            try {
                if (!isDestroyed.get() && mapChangeRequested && processQueue()) {
                    // repaint successful, set flag to false
                    mapChangeRequested = false;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private class CommandRunner implements Runnable {

        @Override
        public void run() {
            while (true) {
                final Runnable changeAction;
                commandLock.lock();
                try {
                    changeAction = changeCommandQueue.poll();
                    if (changeAction == null) {
                        commandExecutionRunRequested = false;
                    }
                } finally {
                    commandLock.unlock();
                }
                if (changeAction == null) {
                    break;
                } else {
                    processMapChange(changeAction);
                }
            }
        }
    }
}
