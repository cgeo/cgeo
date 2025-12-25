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

package cgeo.geocaching.unifiedmap.geoitemlayer

import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.ToScreenProjector
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log

import android.util.Pair

import java.util.Collection

/** Interface to be implemented by a map-specific provider of geoitem layers */
interface IProviderGeoItemLayer<C> {

    /**
     * Called once to initialize the map layer.
     * <br />
     * Implementor is expected to set up necessary initialization within its concrete map viewer implementation.
     * Framework guarantees that this method is called only once, before any call to other methods of this interface */
    Unit init(Int zLevel)

    /**
     * adds the given item to the map viewer layer. Note that this method expects different objects to
     * be created on the map even if it is called multiple times with two equal items!
     * Implementor should create and return a map-specific context object with which it can identify
     * created objects in follow-up methods.
     */
    C add(GeoPrimitive item)

    /** Removes a previously created item on the map, identified by the given context.
     * For convenience, the implementor also receives the original item from which the object was created from.
     */
    Unit remove(GeoPrimitive item, C context)

    /** Optional method to handle replacements of one object with another. */
    default C replace(GeoPrimitive oldItem, C oldContext, GeoPrimitive newItem) {
        remove(oldItem, oldContext)
        return add(newItem)
    }

    /** Optional method which is called after a batch of map layer updates (add/remove). It is intended to
     *  be used to trigger a "global map view update" if this is required by map implementation
      */
    default String onMapChangeBatchEnd(final Long processedCount) {
        //default implementation does nothing
        return null
    }

    /**
     * Hook to destroy object and free all allocated resources
     * Framework guarantees that this method is only called once and that after this call no other method
     * will be called. Framework will dispose all instances of this class after calling "destroy"
     */
    Unit destroy(Collection<Pair<GeoPrimitive, C>> values)

    /** Provides a function which maps lat-lon-geopoints to actual screen pixel coordinates */
    ToScreenProjector getScreenCoordCalculator()

    default Unit runCommandChain(final Runnable runnable) {
        AndroidRxUtils.computationScheduler.scheduleDirect(runnable)
    }

    default Unit runMapChanges(final Runnable runnable) {
        // inspired by http://stackoverflow.com/questions/12850143/android-basics-running-code-in-the-ui-thread/25250494#25250494
        // modifications of google map must be run on main (UI) thread
        //Handler(Looper.getMainLooper()).post(runnable)
        Log.v("AsyncMapWrapper: request Thread for: " + runnable.getClass().getName())

        AndroidRxUtils.runOnUi(runnable)
    }

    default Boolean continueMapChangeExecutions(final Long startTime, final  Int queueLength) {
        return System.currentTimeMillis() - startTime < 40
    }
}
