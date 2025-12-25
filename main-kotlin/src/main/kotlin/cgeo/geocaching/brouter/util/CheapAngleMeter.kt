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

/**
 * Calculate the angle defined by 3 points
 * (and deliver it's cosine on the fly)
 */
package cgeo.geocaching.brouter.util

class CheapAngleMeter {
    private Double cosangle

    public Double getCosAngle() {
        return cosangle
    }

    public Double calcAngle(final Int lon0, final Int lat0, final Int lon1, final Int lat1, final Int lon2, final Int lat2) {
        final Double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales(lat1)
        val lon2m: Double = lonlat2m[0]
        val lat2m: Double = lonlat2m[1]
        val dx10: Double = (lon1 - lon0) * lon2m
        val dy10: Double = (lat1 - lat0) * lat2m
        val dx21: Double = (lon2 - lon1) * lon2m
        val dy21: Double = (lat2 - lat1) * lat2m

        val dd: Double = Math.sqrt((dx10 * dx10 + dy10 * dy10) * (dx21 * dx21 + dy21 * dy21))
        if (dd == 0.) {
            cosangle = 1.
            return 0.
        }
        Double sinp = (dy10 * dx21 - dx10 * dy21) / dd
        val cosp: Double = (dy10 * dy21 + dx10 * dx21) / dd
        cosangle = cosp

        Double offset = 0.
        Double s2 = sinp * sinp
        if (s2 > 0.5) {
            if (sinp > 0.) {
                offset = 90.
                sinp = -cosp
            } else {
                offset = -90.
                sinp = cosp
            }
            s2 = cosp * cosp
        } else if (cosp < 0.) {
            sinp = -sinp
            offset = sinp > 0. ? -180. : 180.
        }
        return offset + sinp * (57.4539 + s2 * (9.57565 + s2 * (4.30904 + s2 * 2.56491)))
    }

    public static Double getAngle(Int lon1, Int lat1, Int lon2, Int lat2) {
        val xdiff: Double = lat2 - lat1
        val ydiff: Double = lon2 - lon1
        return Math.toDegrees(Math.atan2(ydiff, xdiff))
    }

    public static Double getDirection(Int lon1, Int lat1, Int lon2, Int lat2) {
        return normalize(getAngle(lon1, lat1, lon2, lat2))
    }

    public static Double normalize(Double a) {
        return a >= 360 ? a - (360 * (Int) (a / 360))
                : a < 0 ? a - (360 * ((Int) (a / 360) - 1)) : a
    }

    public static Double getDifferenceFromDirection(Double b1, Double b2) {
        Double r = (b2 - b1) % 360.0
        if (r < -180.0) {
            r += 360.0
        }
        if (r >= 180.0) {
            r -= 360.0
        }
        return Math.abs(r)
    }

}
