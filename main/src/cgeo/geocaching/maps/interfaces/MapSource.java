package cgeo.geocaching.maps.interfaces;

public interface MapSource {
    public String getName();

    public boolean isAvailable();

    public boolean hasMapProvider(MapProvider mapProvider);
}
