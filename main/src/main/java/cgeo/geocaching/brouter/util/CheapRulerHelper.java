package cgeo.geocaching.brouter.util;

public final class CheapRulerHelper {
    /**
     * Cheap-Ruler Java implementation
     * See
     * https://blog.mapbox.com/fast-geodesic-approximations-with-cheap-ruler-106f229ad016
     * for more details.
     * <p>
     * Original code is at https://github.com/mapbox/cheap-ruler under ISC license.
     * <p>
     * This is implemented as a Singleton to have a unique cache for the cosine
     * values across all the code.
     */

    // Conversion constants
    public static final double ILATLNG_TO_LATLNG = 1e-6; // From integer to degrees
    public static final int KILOMETERS_TO_METERS = 1000;
    public static final double DEG_TO_RAD = Math.PI / 180.;

    // Scale cache constants
    private static final int SCALE_CACHE_LENGTH = 1800;
    private static final int SCALE_CACHE_INCREMENT = 100000;
    // SCALE_CACHE_LENGTH cached values between 0 and COS_CACHE_MAX_DEGREES degrees.
    private static final double[][] SCALE_CACHE = new double[SCALE_CACHE_LENGTH][];

    private CheapRulerHelper() {
        // utility class
    }

    /**
     * build the cache of cosine values.
     */
    static {
        for (int i = 0; i < SCALE_CACHE_LENGTH; i++) {
            SCALE_CACHE[i] = calcKxKyFromILat(i * SCALE_CACHE_INCREMENT + SCALE_CACHE_INCREMENT / 2);
        }
    }

    private static double[] calcKxKyFromILat(final int ilat) {
        final double lat = DEG_TO_RAD * (ilat * ILATLNG_TO_LATLNG - 90);
        final double cos = Math.cos(lat);
        final double cos2 = 2 * cos * cos - 1;
        final double cos3 = 2 * cos * cos2 - cos;
        final double cos4 = 2 * cos * cos3 - cos2;
        final double cos5 = 2 * cos * cos4 - cos3;

        // Multipliers for converting integer longitude and latitude into distance
        // (http://1.usa.gov/1Wb1bv7)
        final double[] kxky = new double[2];
        kxky[0] = (111.41513 * cos - 0.09455 * cos3 + 0.00012 * cos5) * ILATLNG_TO_LATLNG * KILOMETERS_TO_METERS;
        kxky[1] = (111.13209 - 0.56605 * cos2 + 0.0012 * cos4) * ILATLNG_TO_LATLNG * KILOMETERS_TO_METERS;
        return kxky;
    }

    /**
     * Calculate the degree-&gt;meter scale for given latitude
     *
     * @return [lon-&gt;meter,lat-&gt;meter]
     */
    public static double[] getLonLatToMeterScales(final int ilat) {
        return SCALE_CACHE[ilat / SCALE_CACHE_INCREMENT];
    }

    /**
     * Compute the distance (in meters) between two points represented by their
     * (integer) latitude and longitude.
     *
     * @param ilon1 Integer longitude for the start point. this is (longitude in degrees + 180) * 1e6.
     * @param ilat1 Integer latitude for the start point, this is (latitude + 90) * 1e6.
     * @param ilon2 Integer longitude for the end point, this is (longitude + 180) * 1e6.
     * @param ilat2 Integer latitude for the end point, this is (latitude + 90) * 1e6.
     * @return The distance between the two points, in meters.
     * <p>
     * Note:
     * Integer longitude is ((longitude in degrees) + 180) * 1e6.
     * Integer latitude is ((latitude in degrees) + 90) * 1e6.
     */
    public static double distance(final int ilon1, final int ilat1, final int ilon2, final int ilat2) {
        final double[] kxky = getLonLatToMeterScales((ilat1 + ilat2) >> 1);
        final double dlon = (ilon1 - ilon2) * kxky[0];
        final double dlat = (ilat1 - ilat2) * kxky[1];
        return Math.sqrt(dlat * dlat + dlon * dlon); // in m
    }
}
