package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.ToScreenProjector;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

/** Interface to be implemented by a map-specific provider of geoitem layers */
public interface IProviderGeoItemLayer<C> {

    /**
     * Called once to initialize the map layer.
     *
     * Implementor is expected to set up necessary initialization within its concrete map viewer implementation.
     * Framework guarantees that this method is called only once, before any call to other methods of this interface
     */
    void init(int zLevel);

    /**
     * adds the given item to the map viewer layer. Note that this method expects different objects to
     * be created on the map even if it is called multiple times with two equal items!
     * Implementor should create and return a map-specific context object with which it can identify
     * created objects in follow-up methods.
     */
    C add(GeoPrimitive item);

    /** Removes a previously created item on the map, identified by the given context.
     * For convenience, the implementor also receives the original item from which the object was created from.
     */
    void remove(GeoPrimitive item, C context);

    /** Optional method to handle replacements of one object with another. */
    default C replace(GeoPrimitive oldItem, C oldContext, GeoPrimitive newItem) {
        remove(oldItem, oldContext);
        return add(newItem);
    }

    /**
     * Hook to destroy object and free all allocated resources
     * Framework guarantees that this method is only called once and that after this call no other method
     * will be called. Framework will dispose all instances of this class after calling "destroy"
     */
    void destroy();

    /** Provides a function which maps lat-lon-geopoints to actual screen pixel coordinates */
    ToScreenProjector getScreenCoordCalculator();

    default void runCommandChain(final Runnable runnable) {
        AndroidRxUtils.computationScheduler.scheduleDirect(runnable);
    }

    default void runMapChanges(final Runnable runnable) {
        // inspired by http://stackoverflow.com/questions/12850143/android-basics-running-code-in-the-ui-thread/25250494#25250494
        // modifications of google map must be run on main (UI) thread
        //new Handler(Looper.getMainLooper()).post(runnable);
        Log.iForce("AsyncMapWrapper: request Thread for: " + runnable.getClass().getName());

        AndroidRxUtils.runOnUi(runnable);
    }

    default boolean continueMapChangeExecutions(final long startTime, final  int queueLength) {
        return System.currentTimeMillis() - startTime < 40;
    }
}
