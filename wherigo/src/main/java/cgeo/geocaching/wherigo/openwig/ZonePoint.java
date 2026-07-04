/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import org.apache.commons.collections4.IteratorUtils;

import java.io.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.stdlib.MathLib;

public class ZonePoint implements LuaTable, Serializable {
    public double latitude = 0;
    public double longitude = 0;
    public double altitude = 0;

    public static final double LATITUDE_COEF = 110940.00000395167;
    public static final double METRE_COEF = 9.013881377e-6;
    public static final double PI_180 = Math.PI / 180;
    public static final double DEG_PI = 180 / Math.PI;
    public static final double PI_2 = Math.PI / 2;

    public static ZonePoint copy (final ZonePoint z) {
        if (z == null) return null;
        else return new ZonePoint (z);
    }

    public ZonePoint () { }

    public ZonePoint (final ZonePoint z) {
        latitude = z.latitude;
        longitude = z.longitude;
        altitude = z.altitude;
    }

    public ZonePoint (final double lat, final double lon, final double alt) {
        latitude = lat;
        longitude = lon;
        altitude = alt;
    }

    public ZonePoint translate (final double angle, final double dist) {
        final double rad = azimuth2angle(angle);
        final double x = m2lat(dist * Math.sin(rad));
        final double y = m2lon(latitude, dist * Math.cos(rad));
        return new ZonePoint(latitude + x, longitude + y, altitude);
    }

    public void sync (final ZonePoint z) {
        latitude = z.latitude;
        longitude = z.longitude;
    }

    public static double lat2m (final double degrees) {
        return degrees * LATITUDE_COEF;
    }

    public static double lon2m (final double latitude, final double degrees) {
        return degrees * PI_180 * Math.cos(latitude * PI_180) * 6367449;
    }

    public static double m2lat (final double metres) {
        return metres * METRE_COEF;
    }

    public static double m2lon (final double latitude, final double metres) {
        return metres / (PI_180 * Math.cos(latitude * PI_180) * 6367449);
    }

    public double distance (final double lat, final double lon) {
        return distance(lat, lon, latitude, longitude);
    }

    public double distance (final ZonePoint z) {
        return distance(z.latitude, z.longitude, latitude, longitude);
    }

    public static final Map<String, Double> conversions = new HashMap<>(6);
    static {
        conversions.put("feet", Double.valueOf(0.3048));
        conversions.put("ft", Double.valueOf(0.3048));
        conversions.put("miles", Double.valueOf(1609.344));
        conversions.put("meters", Double.valueOf(1));
        conversions.put("kilometers", Double.valueOf(1000));
        conversions.put("nauticalmiles", Double.valueOf(1852));
    }

    public static double convertDistanceTo (final double value, final String unit) {
        if (unit != null && conversions.containsKey(unit)) {
            return value / conversions.get(unit).doubleValue();
        } else {
            return value;
        }
    }

    public static double convertDistanceFrom (final double value, final String unit) {
        if (unit != null && conversions.containsKey(unit)) {
            return value * conversions.get(unit).doubleValue();
        } else {
            return value;
        }
    }

    public static double distance (final double lat1, final double lon1, final double lat2, final double lon2) {
        final double mx = Math.abs(ZonePoint.lat2m(lat1 - lat2));
        final double my = Math.abs(ZonePoint.lon2m(lat2, lon1 - lon2));
        return Math.sqrt(mx * mx + my * my);
    }

    public String friendlyDistance (final double lat, final double lon) {
        return makeFriendlyDistance(distance(lat, lon));
    }

    public static String makeFriendlyDistance (final double dist) {
        final double d; final long part;
        if (dist > 1500) { // abcd.ef km
            part = (long)(dist / 10);
            d = part / 100.0;
            return Double.toString(d)+" km";
        } else if (dist > 100) { // abcd m
            return Double.toString((long)dist)+" m";
        } else { // abcd.ef m
            part = (long)(dist * 100);
            d = part / 100.0;
            return Double.toString(d)+" m";
        }
    }

    public static String makeFriendlyAngle (final double angle) {
        boolean neg = false;
        double a = angle;
        if (a < 0) {
            neg = true;
            a *= -1;
        }
        final int degrees = (int)a;
        a = (a - degrees) * 60;
        String an = String.valueOf(a);
        if (an.indexOf('.') != -1)
            an = an.substring(0, Math.min(an.length(), an.indexOf('.') + 5));
        return (neg ? "- " : "+ ") + String.valueOf(degrees) + "\u00b0 " + an;
    }

    public static String makeFriendlyLatitude (final double angle) {
        return makeFriendlyAngle(angle).replace('+', 'N').replace('-', 'S');
    }

    public static String makeFriendlyLongitude (final double angle) {
        return makeFriendlyAngle(angle).replace('+', 'E').replace('-', 'W');
    }

    public double bearing (final double lat, final double lon) {
        // calculates bearing from specified point to here
        return MathLib.atan2(lat2m(latitude - lat), lon2m(lat, longitude - lon));
    }

    public double bearing (final ZonePoint zp) {
        return bearing(zp.latitude, zp.longitude);
    }

    public static double angle2azimuth (final double angle) {
        double degrees = -((angle - PI_2) * DEG_PI);
        while (degrees < 0) degrees += 360;
        while (degrees >= 360) degrees -= 360;
        return degrees;
    }

    public static double azimuth2angle (final double azim) {
        double ret = -(azim * PI_180) + PI_2;
        while (ret > Math.PI) ret -= Math.PI * 2;
        while (ret <= -Math.PI) ret += Math.PI * 2;
        return ret;
    }

    public void setMetatable (final LuaTable metatable) { }
    public LuaTable getMetatable () { return null; }

    public void rawset (final Object key, final Object value) {
        if (key == null) return;
        final String name = key.toString();
        if ("latitude".equals(name))
            latitude = LuaState.fromDouble(value);
        else if ("longitude".equals(name))
            longitude = LuaState.fromDouble(value);
        else if ("altitude".equals(name)) {
            altitude = LuaState.fromDouble(value);
        }
    }

    public Object rawget (final Object key) {
        if (key == null) return null;
        final String name = key.toString();
        if ("latitude".equals(name)) return LuaState.toDouble(latitude);
        if ("longitude".equals(name)) return LuaState.toDouble(longitude);
        if ("altitude".equals(name)) return LuaState.toDouble(altitude);
        return null;
    }

    public Object next (final Object key) { return null; }
    public int len () { return 3; }

    public Iterator<Object> keys() { return IteratorUtils.arrayIterator(new Object[] { "latitude", "longitude", "altitude"}); }

    public void updateWeakSettings (final boolean weakKeys, final boolean weakValues) { }

    public void serialize (final DataOutputStream out) throws IOException {
        out.writeDouble(latitude);
        out.writeDouble(longitude);
        out.writeDouble(altitude);
    }

    public void deserialize (final DataInputStream in) throws IOException {
        latitude = in.readDouble();
        longitude = in.readDouble();
        altitude = in.readDouble();
    }

    public String toString () {
        return "ZonePoint("+latitude+","+longitude+","+altitude+")" /* + "-" + super.toString()*/;
    }
}
