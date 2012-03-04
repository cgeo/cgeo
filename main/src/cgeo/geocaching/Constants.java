package cgeo.geocaching;

/**
 * Various constant values used all over in c:geo
 *
 * @author blafoo
 */

public final class Constants {

    /** Number of days (as ms) after temporarily saved caches are deleted */
    public static long DAYS_AFTER_CACHE_IS_DELETED = 3 * 24 * 60 * 60 * 1000;

    /** Number of logs to retrieve from GC.com */
    public final static int NUMBER_OF_LOGS = 35;

    /** User agent id */
    public final static String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1";

    /** Text separator used for formatting texts */
    public static final String SEPARATOR = " · ";

    /**
     * Factor used to calculate distance from meters to foot;
     * <p>
     * ft = m * M2FT;
     */
    public static final double M2FT = 3.2808399d;

}
