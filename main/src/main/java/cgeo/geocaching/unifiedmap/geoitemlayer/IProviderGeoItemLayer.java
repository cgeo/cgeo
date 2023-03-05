package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.models.geoitem.GeoPrimitive;

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
}
