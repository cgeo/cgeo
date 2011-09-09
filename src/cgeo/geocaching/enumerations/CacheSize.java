package cgeo.geocaching.enumerations;

import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;

/**
 * Enum listing cache sizes
 *  
 * @author koem
 */
public enum CacheSize {
    MICRO      ("micro",      1, PointGeocachingData.CACHE_SIZE_MICRO),
    SMALL      ("small",      2, PointGeocachingData.CACHE_SIZE_SMALL),
    REGULAR    ("regular",    3, PointGeocachingData.CACHE_SIZE_REGULAR),
    LARGE      ("large",      4, PointGeocachingData.CACHE_SIZE_LARGE),
    NOT_CHOSEN ("not chosen", 0, PointGeocachingData.CACHE_SIZE_NOT_CHOSEN),
    OTHER      ("other",      0, PointGeocachingData.CACHE_SIZE_OTHER);
    
    public final String cgeoId;
    public final int comparable;
    public final int locusId;

    private CacheSize(String cgeoId, int comparable, int locusId) {
        this.cgeoId = cgeoId;
        this.comparable = comparable;
        this.locusId = locusId;
    }
}
