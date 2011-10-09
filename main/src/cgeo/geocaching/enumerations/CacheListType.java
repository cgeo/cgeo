package cgeo.geocaching.enumerations;

public enum CacheListType {
    OFFLINE,
    HISTORY,
    NEAREST,
    COORDINATE,
    KEYWORD,
    ADDRESS,
    USERNAME,
    OWNER;

    public static CacheListType fromOrdinal(int ordinal) {
        for (CacheListType cacheListType : CacheListType.values()) {
            if (cacheListType.ordinal() == ordinal) {
                return cacheListType;
            }
        }
        return null;
    }
}
