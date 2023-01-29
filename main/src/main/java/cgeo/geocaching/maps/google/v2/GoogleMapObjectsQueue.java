package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.utils.Log;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.android.gms.maps.GoogleMap;

/**
 * Represents and manages a group of drawable objects (such as markers, circles, polylines and polygons) on a concrete Google Map instance.
 *
 * This class provides methods to add and remove drawable objects to this map. It manages under the hood
 * and asynchronous process to perform those add/removals in the background and with minimal impact on the Android GUI thread.
 *
 * This class works with instances of {@link MapObjectOptions} to encapsulate individual drawable objects. It relies heavily on
 * equals() and hashCode() methods of this class.
 */
public class GoogleMapObjectsQueue {

    /** magic number of milliseconds. maximum allowed time of adding or removing items to googlemap on UI thread */
    protected static final long TIME_MAX = 40;

    private static final String LOG_PREFIX = GoogleMapObjectsQueue.class.getSimpleName() + ": ";

    //Google map with which this Group is associated
    private final GoogleMap googleMap;
    private final ICommandExecutor commandExecutor;

    //Runner instance which performs updates on google map asynchronous
    private final RepaintRunner repaintRunner = new RepaintRunner();
    private boolean repaintRequested = false;

    //Storage for currently drawn objects as well as requested actions.
    //Implementation note: every access to these objects must be done under lock and leave them in a consistent state!

    //Currently drawn/visible objects. Value Map is the Google Object representation (needed when removal is requested later)
    private final Map<MapObjectOptions, Object> drawnObjects = new HashMap<>();
    //All drawable objects which are currently requested to be added to map
    private final Set<MapObjectOptions> requestedToAdd = new HashSet<>();
    //All drawable objects which are currently requested to be removed from map
    private final Set<MapObjectOptions> requestedToRemove = new HashSet<>();
    //Current command queue. Contains not-yet-processed add/remove commands as Pair (options, flag), where flag=true means ADD and flag=false means REMOVE
    // Commands may be outdated at time of processing e.g. if same object was requested to be added and removed in this order
    // and those commands were not processed yet. In such a case, commands must be skipped at processing time
    // according to content of requestedToAdd/requestedToRemove
    private final Queue<Pair<MapObjectOptions, Boolean>> commandProcessQueue = new LinkedList<>();
    // LOCK to acquire before any access to any of the above members to ensure correct concurrent behaviour
    private final Lock lock = new ReentrantLock();

    //Executor of commands. This abstraction is used to inject another executor for unit testing
    protected interface ICommandExecutor {
        Object addToMap(GoogleMap map, MapObjectOptions obj);
        void removeFromMap(Object mapObject);
        void runOnUIThread(Runnable runnable);
        boolean continueCommandExecution(long startTime, int queueLength);
    }

    private static final ICommandExecutor PROD_EXECUTOR = new ICommandExecutor() {
        @Override
        public Object addToMap(final GoogleMap map, final MapObjectOptions obj) {
            return obj.addToGoogleMap(map);
        }

        @Override
        public void removeFromMap(final Object mapObject) {
            MapObjectOptions.removeFromGoogleMap(mapObject);
        }

        @Override
        public void runOnUIThread(final Runnable runnable) {
            // inspired by http://stackoverflow.com/questions/12850143/android-basics-running-code-in-the-ui-thread/25250494#25250494
            // modifications of google map must be run on main (UI) thread
            new Handler(Looper.getMainLooper()).post(runnable);
        }

        @Override
        public boolean continueCommandExecution(final long startTime, final int queueLength) {
            return System.currentTimeMillis() - startTime < TIME_MAX;
        }
    };

    public GoogleMapObjectsQueue(final GoogleMap googleMap) {
        this(googleMap, PROD_EXECUTOR);
    }

    /** For usage in Unit-tests only! */
    protected GoogleMapObjectsQueue(final GoogleMap googleMap, final ICommandExecutor commandExecutor) {
        this.googleMap = googleMap;
        this.commandExecutor = commandExecutor;
    }

    /** adds drawable objects to this MapObjectGroup */
    public void add(final Collection<? extends MapObjectOptions> toAdd) {
        requestChange(() -> {
            for (MapObjectOptions opt : toAdd) {
                addSingle(opt);
            }
        });
    }

    /** adds drawable object to this MapObjectGroup */
    public void add(final MapObjectOptions toAdd) {
        add(Collections.singleton(toAdd));
    }

    /** removes drawable objects to this MapObjectGroup */
    public void remove(final Collection<? extends MapObjectOptions> toRemove) {
        requestChange(() -> {
            for (MapObjectOptions opt : toRemove) {
                removeSingle(opt);
            }
        });
    }

    /** removes ALL drawable objects from this MapObjectGroup */
    public void removeAll() {
        requestChange(() -> {
            //clear all existing requests to add/remove
            requestedToAdd.clear();
            requestedToRemove.clear();
            commandProcessQueue.clear();
            //refill pipeline with requests to remove everything which is drawn
            requestedToRemove.addAll(drawnObjects.keySet());
            for (MapObjectOptions o : requestedToRemove) {
                commandProcessQueue.add(new Pair<>(o, false));
            }
        });
    }


    /**
     * replaces drawable objects in this group completely with given objects
     *
     * Same effect could be reached by calling removeAll() followed by add(newObjects),
     * but performance is better when calling replace() method instead
     */
    public void replace(final Collection<? extends MapObjectOptions> newObjects) {
        requestChange(() -> {
            //calculate what to add/remove to currently drawn set to reach toReplace state finally
            final Set<MapObjectOptions> toAdd = new HashSet<>(newObjects);
            toAdd.removeAll(drawnObjects.keySet());
            final Set<MapObjectOptions> toRemove = new HashSet<>(drawnObjects.keySet());
            toRemove.removeAll(newObjects);
            //clear all existing requests to add/remove
            requestedToAdd.clear();
            requestedToRemove.clear();
            commandProcessQueue.clear();
            //refill pipeline with requests to add/remove
            requestedToAdd.addAll(toAdd);
            requestedToRemove.addAll(toRemove);
            for (MapObjectOptions o : toRemove) {
                commandProcessQueue.add(new Pair<>(o, false));
            }
            for (MapObjectOptions o : toAdd) {
                commandProcessQueue.add(new Pair<>(o, true));
            }
        });
    }

    //CALL ONLY WITH ACQUIRED LOCK!
    private void addSingle(final MapObjectOptions options) {
        //make sure that any pending removal requests for this object are removed
        requestedToRemove.remove(options);

        //we need to add only if object is not already drawn
        if (!drawnObjects.containsKey(options) && requestedToAdd.add(options)) {
            //only if there was no add request yet we need to add a command to processqueue
            commandProcessQueue.add(new Pair<>(options, true));
        }
    }

    //CALL ONLY WITH ACQUIRED LOCK!
    private void removeSingle(final MapObjectOptions options) {
        //makke sure that any pending add requests for this object are removed
        requestedToAdd.remove(options);

        //we need to delete only if object currently exists
        if (drawnObjects.containsKey(options) && requestedToRemove.add(options)) {
            //only if there was no remove request yet we need to add a command to processqueue
            commandProcessQueue.add(new Pair<>(options, false));
        }
    }

    private void requestChange(final Runnable changeAction) {
        lock.lock();
        try {
            changeAction.run();
            if (!repaintRequested) {
                repaintRequested = true;
                commandExecutor.runOnUIThread(repaintRunner);
            }
        } finally {
            lock.unlock();
        }
    }

    private class RepaintRunner implements Runnable {

        //CALL ONLY WITH ACQUIRED LOCK!!!
        private boolean processQueue() {
            final long time = System.currentTimeMillis();
            Pair<MapObjectOptions, Boolean> request;
            while ((request = commandProcessQueue.poll()) != null) {
                if (request.second) {
                    //ADD request
                    processAddCommand(request.first);
                } else {
                    //REMOVE request
                    processRemoveCommand(request.first);
                }
                if (!commandExecutor.continueCommandExecution(time, commandProcessQueue.size())) {
                    // removing and adding objects to Google Maps are time costly operations and we don't want to block UI thread
                    commandExecutor.runOnUIThread(this);
                    return false;
                }
            }
            return true;
        }

        //CALL ONLY WITH ACQUIRED LOCK!!!
        private void processRemoveCommand(final MapObjectOptions options) {
            final boolean stillValid = requestedToRemove.remove(options);
            //if stillValid = false, then the process command was outdated by later changes to requestedToRemove -> in this case ignore it
            if (stillValid) {
                final Object obj = drawnObjects.remove(options);
                if (obj != null) {
                    commandExecutor.removeFromMap(obj);
                } else {
                    // could not remove, is it enqueued to be draw?
                    Log.e(LOG_PREFIX + "requesting non-existing object for removal -> must be programming bug!");
                }
            }
        }
        //CALL ONLY WITH ACQUIRED LOCK!!!
        private void processAddCommand(final MapObjectOptions options) {
            final boolean stillValid = requestedToAdd.remove(options);
            //if stillValid = false, then the process command was outdated by later changes to requestedToRemove -> in this case ignore it
            if (stillValid) {
                final Object drawn = commandExecutor.addToMap(googleMap, options);
                drawnObjects.put(options, drawn);
            }
        }

        @Override
        public void run() {
            lock.lock();
            try {
                if (repaintRequested && processQueue()) {
                    // repaint successful, set flag to false
                    repaintRequested = false;
                }
            } finally {
                lock.unlock();
            }
        }

    }
}
