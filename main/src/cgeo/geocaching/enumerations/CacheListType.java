package cgeo.geocaching.enumerations;

public enum CacheListType {
    OFFLINE(true),
    HISTORY(true),
    NEAREST(false),
    COORDINATE(false),
    KEYWORD(false),
    ADDRESS(false),
    USERNAME(false),
    OWNER(false),
    MAP(false);

    /**
     * whether or not this list allows switching to another list
     */
    public final boolean canSwitch;

    private CacheListType(final boolean canSwitch) {
        this.canSwitch = canSwitch;
    }
}
