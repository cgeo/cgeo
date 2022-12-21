package cgeo.geocaching.enumerations;

public enum Loaders {
    LOGGING_GEOCHACHING,
    LOGGING_TRAVELBUG,
    LOGGING_GEOKRETY,
    INVENTORY_GEOKRETY,
    CACHE_INVENTORY_GEOKRETY;

    public int getLoaderId() {
        return ordinal();
    }
}
