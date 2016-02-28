package cgeo.geocaching.maps.interfaces;

public interface MapSource {
    String getName();

    boolean isAvailable();

    int getNumericalId();

    MapProvider getMapProvider();
}
