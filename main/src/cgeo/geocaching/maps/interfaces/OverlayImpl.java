package cgeo.geocaching.maps.interfaces;

/**
 * Marker interface of the provider-specific
 * Overlay implementations
 */
public interface OverlayImpl {

    public enum OverlayType {
        PositionOverlay,
        ScaleOverlay
    }

    void lock();

    void unlock();

    MapViewImpl getMapViewImpl();
}
