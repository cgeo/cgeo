package cgeo.geocaching.enumerations;

public enum Loaders {
    LOGGING_GEOCHACHING,
    CACHE_INVENTORY_GEOKRETY;

    public int getLoaderId() {
        return ordinal();
    }
}