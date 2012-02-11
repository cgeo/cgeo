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

    /** Text separator used for formatting texts */
    public static final String SEPARATOR = " · ";

}
