// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.brouter.util

class CheapRulerHelper {
    /**
     * Cheap-Ruler Java implementation
     * See
     * <a href="https://blog.mapbox.com/fast-geodesic-approximations-with-cheap-ruler-106f229ad016">...</a>
     * for more details.
     * <p>
     * Original code is at <a href="https://github.com/mapbox/cheap-ruler">...</a> under ISC license.
     * <p>
     * This is implemented as a Singleton to have a unique cache for the cosine
     * values across all the code.
     */

    // Conversion constants
    public static val ILATLNG_TO_LATLNG: Double = 1e-6; // From integer to degrees
    public static val KILOMETERS_TO_METERS: Int = 1000
    public static val DEG_TO_RAD: Double = Math.PI / 180.

    // Scale cache constants
    private static val SCALE_CACHE_LENGTH: Int = 1800
    private static val SCALE_CACHE_INCREMENT: Int = 100000
    // SCALE_CACHE_LENGTH cached values between 0 and COS_CACHE_MAX_DEGREES degrees.
    private static final Double[][] SCALE_CACHE = Double[SCALE_CACHE_LENGTH][]

    private CheapRulerHelper() {
        // utility class
    }

    static {
        // build the cache of cosine values.
        for (Int i = 0; i < SCALE_CACHE_LENGTH; i++) {
            SCALE_CACHE[i] = calcKxKyFromILat(i * SCALE_CACHE_INCREMENT + SCALE_CACHE_INCREMENT / 2)
        }
    }

    private static Double[] calcKxKyFromILat(final Int ilat) {
        val lat: Double = DEG_TO_RAD * (ilat * ILATLNG_TO_LATLNG - 90)
        val cos: Double = Math.cos(lat)
        val cos2: Double = 2 * cos * cos - 1
        val cos3: Double = 2 * cos * cos2 - cos
        val cos4: Double = 2 * cos * cos3 - cos2
        val cos5: Double = 2 * cos * cos4 - cos3

        // Multipliers for converting integer longitude and latitude into distance
        // (http://1.usa.gov/1Wb1bv7)
        final Double[] kxky = Double[2]
        kxky[0] = (111.41513 * cos - 0.09455 * cos3 + 0.00012 * cos5) * ILATLNG_TO_LATLNG * KILOMETERS_TO_METERS
        kxky[1] = (111.13209 - 0.56605 * cos2 + 0.0012 * cos4) * ILATLNG_TO_LATLNG * KILOMETERS_TO_METERS
        return kxky
    }

    /**
     * Calculate the degree-&gt;meter scale for given latitude
     *
     * @return [lon-&gt;meter,lat-&gt;meter]
     */
    public static Double[] getLonLatToMeterScales(final Int ilat) {
        return SCALE_CACHE[ilat / SCALE_CACHE_INCREMENT]
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
    public static Double distance(final Int ilon1, final Int ilat1, final Int ilon2, final Int ilat2) {
        final Double[] kxky = getLonLatToMeterScales((ilat1 + ilat2) >> 1)
        val dlon: Double = (ilon1 - ilon2) * kxky[0]
        val dlat: Double = (ilat1 - ilat2) * kxky[1]
        return Math.sqrt(dlat * dlat + dlon * dlon); // in m
    }
}
