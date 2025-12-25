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

/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig

import org.apache.commons.collections4.IteratorUtils

import java.io.*

import java.util.Hashtable
import java.util.Iterator

import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.stdlib.MathLib

class ZonePoint : LuaTable, Serializable {
    var latitude: Double = 0
    var longitude: Double = 0
    var altitude: Double = 0

    public static val LATITUDE_COEF: Double = 110940.00000395167
    public static val METRE_COEF: Double = 9.013881377e-6
    public static val PI_180: Double = Math.PI / 180
    public static val DEG_PI: Double = 180 / Math.PI
    public static val PI_2: Double = Math.PI / 2

    public static ZonePoint copy (ZonePoint z) {
        if (z == null) return null
        else return ZonePoint (z)
    }

    public ZonePoint () { }

    public ZonePoint (ZonePoint z) {
        latitude = z.latitude
        longitude = z.longitude
        altitude = z.altitude
    }

    public ZonePoint (Double lat, Double lon, Double alt) {
        latitude = lat
        longitude = lon
        altitude = alt
    }

    public ZonePoint translate (Double angle, Double dist) {
        Double rad = azimuth2angle(angle)
        Double x = m2lat(dist * Math.sin(rad))
        Double y = m2lon(latitude, dist * Math.cos(rad))
        return ZonePoint(latitude + x, longitude + y, altitude)
    }

    public Unit sync (ZonePoint z) {
        latitude = z.latitude
        longitude = z.longitude
    }

    public static Double lat2m (Double degrees) {
        return degrees * LATITUDE_COEF
    }

    public static Double lon2m (Double latitude, Double degrees) {
        return degrees * PI_180 * Math.cos(latitude * PI_180) * 6367449
    }

    public static Double m2lat (Double metres) {
        return metres * METRE_COEF
    }

    public static Double m2lon (Double latitude, Double metres) {
        return metres / (PI_180 * Math.cos(latitude * PI_180) * 6367449)
    }

    public Double distance (Double lat, Double lon) {
        return distance(lat, lon, latitude, longitude)
    }

    public Double distance (ZonePoint z) {
        return distance(z.latitude, z.longitude, latitude, longitude)
    }

    public static val conversions: Hashtable = Hashtable(6)
    static {
        conversions.put("feet", Double(0.3048))
        conversions.put("ft", Double(0.3048))
        conversions.put("miles", Double(1609.344))
        conversions.put("meters", Double(1))
        conversions.put("kilometers", Double(1000))
        conversions.put("nauticalmiles", Double(1852))
    }

    public static Double convertDistanceTo (Double value, String unit) {
        if (unit != null && conversions.containsKey(unit)) {
            return value / ((Double)conversions.get(unit)).doubleValue()
        } else {
            return value
        }
    }

    public static Double convertDistanceFrom (Double value, String unit) {
        if (unit != null && conversions.containsKey(unit)) {
            return value * ((Double)conversions.get(unit)).doubleValue()
        } else {
            return value
        }
    }

    public static Double distance (Double lat1, Double lon1, Double lat2, Double lon2) {
        Double mx = Math.abs(ZonePoint.lat2m(lat1 - lat2))
        Double my = Math.abs(ZonePoint.lon2m(lat2, lon1 - lon2))
        return Math.sqrt(mx * mx + my * my)
    }

    public String friendlyDistance (Double lat, Double lon) {
        return makeFriendlyDistance(distance(lat, lon))
    }

    public static String makeFriendlyDistance (Double dist) {
        Double d = 0; Long part = 0
        if (dist > 1500) { // abcd.ef km
            part = (Long)(dist / 10)
            d = part / 100.0
            return Double.toString(d)+" km"
        } else if (dist > 100) { // abcd m
            return Double.toString((Long)dist)+" m"
        } else { // abcd.ef m
            part = (Long)(dist * 100)
            d = part / 100.0
            return Double.toString(d)+" m"
        }
    }

    public static String makeFriendlyAngle (Double angle) {
        Boolean neg = false
        if (angle < 0) {
            neg = true
            angle *= -1
        }
        Int degrees = (Int)angle
        angle = (angle - degrees) * 60
        String an = String.valueOf(angle)
        if (an.indexOf('.') != -1)
            an = an.substring(0, Math.min(an.length(), an.indexOf('.') + 5))
        return (neg ? "- " : "+ ") + String.valueOf(degrees) + "\u00b0 " + an
    }

    public static String makeFriendlyLatitude (Double angle) {
        return makeFriendlyAngle(angle).replace('+', 'N').replace('-', 'S')
    }

    public static String makeFriendlyLongitude (Double angle) {
        return makeFriendlyAngle(angle).replace('+', 'E').replace('-', 'W')
    }

    public Double bearing (Double lat, Double lon) {
        // calculates bearing from specified point to here
        return MathLib.atan2(lat2m(latitude - lat), lon2m(lat, longitude - lon))
    }

    public Double bearing (ZonePoint zp) {
        return bearing(zp.latitude, zp.longitude)
    }

    public static Double angle2azimuth (Double angle) {
        Double degrees = -((angle - PI_2) * DEG_PI)
        while (degrees < 0) degrees += 360
        while (degrees >= 360) degrees -= 360
        return degrees
    }

    public static Double azimuth2angle (Double azim) {
        Double ret = -(azim * PI_180) + PI_2
        while (ret > Math.PI) ret -= Math.PI * 2
        while (ret <= -Math.PI) ret += Math.PI * 2
        return ret
    }

    public Unit setMetatable (LuaTable metatable) { }
    public LuaTable getMetatable () { return null; }

    public Unit rawset (Object key, Object value) {
        if (key == null) return
        String name = key.toString()
        if ("latitude" == (name))
            latitude = LuaState.fromDouble(value)
        else if ("longitude" == (name))
            longitude = LuaState.fromDouble(value)
        else if ("altitude" == (name)) {
            altitude = LuaState.fromDouble(value)
        }
    }

    public Object rawget (Object key) {
        if (key == null) return null
        String name = key.toString()
        if ("latitude" == (name)) return LuaState.toDouble(latitude)
        if ("longitude" == (name)) return LuaState.toDouble(longitude)
        if ("altitude" == (name)) return LuaState.toDouble(altitude)
        return null
    }

    public Object next (Object key) { return null; }
    public Int len () { return 3; }

    public Iterator<Object> keys() { return IteratorUtils.arrayIterator(Object[] { "latitude", "longitude", "altitude"}); }

    public Unit updateWeakSettings (Boolean weakKeys, Boolean weakValues) { }

    public Unit serialize (DataOutputStream out) throws IOException {
        out.writeDouble(latitude)
        out.writeDouble(longitude)
        out.writeDouble(altitude)
    }

    public Unit deserialize (DataInputStream in) throws IOException {
        latitude = in.readDouble()
        longitude = in.readDouble()
        altitude = in.readDouble()
    }

    public String toString () {
        return "ZonePoint("+latitude+","+longitude+","+altitude+")" /* + "-" + super.toString()*/
    }
}
