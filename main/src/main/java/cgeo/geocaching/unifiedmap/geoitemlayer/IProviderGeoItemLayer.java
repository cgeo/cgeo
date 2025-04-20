package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.ToScreenProjector;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.util.Pair;

import java.util.Collection;

/** Interface to be implemented by a map-specific provider of geoitem layers */
public interface IProviderGeoItemLayer<C> {

    /**
     * Called once to initialize the map layer.
     * <br />
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

    /** Optional method which is called after a batch of map layer updates (add/remove). It is intended to
     *  be used to trigger a "global map view update" if this is required by map implementation
      */
    default String onMapChangeBatchEnd(final long processedCount) {
        //default implementation does nothing
        return null;
    }

    /**
     * Hook to destroy object and free all allocated resources
     * Framework guarantees that this method is only called once and that after this call no other method
     * will be called. Framework will dispose all instances of this class after calling "destroy"
     */
    void destroy(Collection<Pair<GeoPrimitive, C>> values);

    /** Provides a function which maps lat-lon-geopoints to actual screen pixel coordinates */
    ToScreenProjector getScreenCoordCalculator();

    default void runCommandChain(final Runnable runnable) {
        AndroidRxUtils.computationScheduler.scheduleDirect(runnable);
    }

    default void runMapChanges(final Runnable runnable) {
        // inspired by http://stackoverflow.com/questions/12850143/android-basics-running-code-in-the-ui-thread/25250494#25250494
        // modifications of google map must be run on main (UI) thread
        //new Handler(Looper.getMainLooper()).post(runnable);
        Log.v("AsyncMapWrapper: request Thread for: " + runnable.getClass().getName());

        AndroidRxUtils.runOnUi(runnable);
    }

    default boolean continueMapChangeExecutions(final long startTime, final  int queueLength) {
        return System.currentTimeMillis() - startTime < 40;
    }
}
