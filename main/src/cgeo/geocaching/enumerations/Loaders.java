package cgeo.geocaching.enumerations;

public enum Loaders {
    LOGGING_GEOCHACHING;

    public int getLoaderId() {
        return ordinal();
    }
}