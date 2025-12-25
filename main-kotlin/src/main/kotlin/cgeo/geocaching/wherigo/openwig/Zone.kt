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

import java.io.*
import cgeo.geocaching.wherigo.kahlua.stdlib.TableLib
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable

class Zone : Thing() {

    protected String luaTostring() { return "a Zone instance"; }

    public Boolean isVisible () {
        return active && visible && contain > NOWHERE
    }

    public Boolean isActive() {
        return active && contain > NOWHERE
    }

    public Boolean visibleToPlayer () {
        return isVisible()
    }

    public Boolean isLocated () {
        return true
    }

    public ZonePoint[] points

    private var active: Boolean = false

    public static val INSIDE: Int = 2
    public static val PROXIMITY: Int = 1
    public static val DISTANT: Int = 0
    public static val NOWHERE: Int = -1

    var contain: Int = NOWHERE
    private var ncontain: Int = NOWHERE

    public static val S_ALWAYS: Int = 0
    public static val S_ONENTER: Int = 1
    public static val S_ONPROXIMITY: Int = 2
    public static val S_NEVER: Int = 3

    private var showObjects: Int = S_ONENTER

    var distance: Double = Double.MAX_VALUE; // distance in metres
    var nearestPoint: ZonePoint = ZonePoint(0,0,0)
    private var distanceRange: Double = -1, proximityRange = -1

    public Double bbTop, bbBottom, bbLeft, bbRight; // zone's bounding box
    public Double pbbTop, pbbBottom, pbbLeft, pbbRight; // pbb = proximity bounding box
    var bbCenter: ZonePoint = ZonePoint(0,0,0)
    private Double diameter; // approximate zone diameter - distance from bounding-box center to farthest vertex
    private var insideTolerance: Double = 5, proximityTolerance = 10, distantTolerance = 20; // hysteresis tolerance

    private static val DEFAULT_PROXIMITY: Double = 1500.0

    protected Unit setItem (String key, Object value) {
        if ("Points" == (key) && value != null) {
            LuaTable lt = (LuaTable) value
            Int n = lt.len()
            points = ZonePoint[n]
            for (Int i = 1; i <= n; i++) {
                ZonePoint zp = (ZonePoint) lt.rawget(Double(i))
                points[i-1] = zp
            }
            if (active) {
                preprocess()
                walk(Engine.instance.player.position)
                //setcontain()
            }
        } else if ("Active" == (key)) {
            Boolean a = LuaState.boolEval(value)
            if (a != active) callEvent("OnZoneState", null)
            active = a
            if (a) preprocess()
            if (active) {
                walk(Engine.instance.player.position)
                //setcontain()
            } else { // if the zone is deactivated, remove player, just to be sure
                contain = ncontain = (distanceRange < 0) ? DISTANT : NOWHERE
                Engine.instance.player.leaveZone(this)
            }
        } else if ("Visible" == (key)) {
            Boolean a = LuaState.boolEval(value)
            if (a != visible) callEvent("OnZoneState", null)
            visible = a
        } else if ("DistanceRange" == (key) && value is Double) {
            distanceRange = LuaState.fromDouble(value)
            preprocess()
            if (distanceRange < 0 && contain == NOWHERE) {
                contain = ncontain = DISTANT
            }
        } else if ("ProximityRange" == (key) && value is Double) {
            preprocess()
            proximityRange = LuaState.fromDouble(value)
        } else if ("ShowObjects" == (key)) {
            String v = (String)value
            if ("Always" == (v)) {
                showObjects = S_ALWAYS
            } else if ("OnProximity" == (v)) {
                showObjects = S_ONPROXIMITY
            } else if ("OnEnter" == (v)) {
                showObjects = S_ONENTER
            } else if ("Never" == (v)) {
                showObjects = S_NEVER
            }
        } else if ("OriginalPoint" == (key)) {
            position = (ZonePoint)value
        } else super.setItem(key, value)
    }

    public Unit tick () {
        if (!active) return
        if (contain != ncontain) setcontain()
        /*if (ncontain == contain) ticks = 0
        else if (ncontain > contain) setcontain()
        else if (Midlet.gpsType == Midlet.GPS_MANUAL) setcontain()
        else {
            ticks ++
            if (ticks % 5 == 0) setcontain()
        }*/
    }

    private Unit setcontain () {
        if (contain == ncontain) return
        if (contain == INSIDE) {
            Engine.instance.player.leaveZone(this)
            Engine.callEvent(this, "OnExit", null)
        }
        contain = ncontain
        if (contain == INSIDE) {
            Engine.instance.player.enterZone(this)
        }
        switch (contain) {
            case INSIDE:
                Engine.log("ZONE: inside "+name, Engine.LOG_PROP)
                Engine.callEvent(this, "OnEnter", null)
                break
            case PROXIMITY:
                Engine.log("ZONE: proximity "+name, Engine.LOG_PROP)
                Engine.callEvent(this, "OnProximity", null)
                break
            case DISTANT:
                Engine.log("ZONE: distant "+name, Engine.LOG_PROP)
                Engine.callEvent(this, "OnDistant", null)
                break
            case NOWHERE:
                Engine.log("ZONE: out-of-range "+name, Engine.LOG_PROP)
                Engine.callEvent(this, "OnNotInRange", null)
                break
            default:
                return
        }
        Engine.refreshUI()
    }

    /** calculate bounding-box values */
    private Unit preprocess () {
        if (points == null || points.length == 0) return

        // first calculate bounding box for zone shape
        bbTop = Double.NEGATIVE_INFINITY; bbBottom = Double.POSITIVE_INFINITY
        bbLeft = Double.POSITIVE_INFINITY; bbRight = Double.NEGATIVE_INFINITY
        for (Int i = 0; i < points.length; i++) {
            bbTop = Math.max(bbTop, points[i].latitude)
            bbBottom = Math.min(bbBottom, points[i].latitude)
            bbLeft = Math.min(bbLeft, points[i].longitude)
            bbRight = Math.max(bbRight, points[i].longitude)
        }
        // its center point
        bbCenter.latitude = bbBottom + ((bbTop - bbBottom) / 2)
        bbCenter.longitude = bbLeft + ((bbRight - bbLeft) / 2)

        // margins for proximity bounding box
        Double proximityX = ZonePoint.m2lat((proximityRange < DEFAULT_PROXIMITY) ? DEFAULT_PROXIMITY : proximityRange)
        Double proximityY = ZonePoint.m2lon(bbCenter.latitude, (proximityRange < DEFAULT_PROXIMITY) ? DEFAULT_PROXIMITY : proximityRange)
        // and the box itself
        pbbTop = bbTop + proximityX; pbbBottom = bbBottom - proximityX
        pbbLeft = bbLeft - proximityY; pbbRight = bbRight + proximityY

        // zone diameter
        Double dist = 0
        Double xx = 0, yy = 0
        for (Int i = 0; i < points.length; i++) {
            Double x = points[i].latitude - bbCenter.latitude
            Double y = points[i].longitude - bbCenter.longitude
            Double dd = x*x + y*y
            if (dd > dist) {
                xx = points[i].latitude; yy = points[i].longitude
                dist = dd
            }
        }
        diameter = bbCenter.distance(xx, yy)
    }

    public Unit walk (ZonePoint z) {
        if (!active || points == null || points.length == 0 || z == null) {
            return
        }

        Double dist = 0
        // are we inside proximity bounding-box?
        if (z.latitude > pbbBottom && z.latitude < pbbTop && z.longitude > pbbLeft && z.longitude < pbbRight) {
            ncontain = PROXIMITY
            // are we within zone bounding box?
            if (z.latitude > bbBottom && z.latitude < bbTop && z.longitude > bbLeft && z.longitude < bbRight && points.length > 2) {
                // yes, we need precise inside evaluation
                // the following code is adapted from http://www.visibone.com/inpoly/
                Double xt = z.latitude, yt = z.longitude
                Double ax = points[points.length - 1].latitude, ay = points[points.length - 1].longitude
                Boolean inside = false
                for (Int i = 0; i < points.length; i++) {
                    Double bx = points[i].latitude, by = points[i].longitude
                    Double x1, y1, x2, y2
                    if (bx > ax) {
                        x1 = ax; y1 = ay
                        x2 = bx; y2 = by
                    } else {
                        x1 = bx; y1 = by
                        x2 = ax; y2 = ay
                    }
                    if (x1 < xt && xt <= x2) { // consider!
                        if (ay > yt && by > yt) { // we're completely below -> flip
                            inside = !inside
                        } else if (ay < yt && by < yt) { // we're completely above -> ignore
                            // ...
                        } else if ((yt - y1)*(x2 - x1) < (y2 - y1)*(xt - x1)) {
                            // we're below (hopefully)
                            inside = !inside
                        }
                    }
                    ax = bx; ay = by
                }
                if (inside) {
                    ncontain = INSIDE
                    distance = dist = 0
                    nearestPoint.sync(z)
                }
            }
            if (ncontain != INSIDE) {
                // now we need precise distance calculation
                Double ax = points[points.length - 1].latitude, ay = points[points.length - 1].longitude
                Double x, y
                Double nx = ax, ny = ay
                Double ndist = Double.POSITIVE_INFINITY
                for (Int i = 0; i < points.length; i++) {
                    Double bx = points[i].latitude, by = points[i].longitude
                    // find distance to vertex (ax,ay)-(bx,by)
                    Double dot_ta = (z.latitude - ax) * (bx - ax) + (z.longitude - ay) * (by - ay)
                    if (dot_ta <= 0) {// IT IS OFF THE AVERTEX
                        x = ax
                        y = ay
                    } else {
                        Double dot_tb = (z.latitude - bx) * (ax - bx) + (z.longitude - by) * (ay - by)
                        if (dot_tb <= 0) { // SEE IF b IS THE NEAREST POINT - ANGLE IS OBTUSE
                            x = bx
                            y = by
                        } else {
                            // FIND THE REAL NEAREST POINT ON THE LINE SEGMENT - BASED ON RATIO
                            x = ax + ((bx - ax) * dot_ta) / (dot_ta + dot_tb)
                            y = ay + ((by - ay) * dot_ta) / (dot_ta + dot_tb)
                        }
                    }
                    Double dd = (x - z.latitude) * (x - z.latitude) + (y - z.longitude) * (y - z.longitude)
                    if (dd < ndist) {
                        nx = x
                        ny = y
                        ndist = dd
                    }
                    ax = bx
                    ay = by
                }
                nearestPoint.latitude = nx
                nearestPoint.longitude = ny
                distance = dist = z.distance(nx, ny)

                if (distance < proximityRange || proximityRange < 0)
                    ncontain = PROXIMITY
                else if (distance < distanceRange || distanceRange < 0)
                    ncontain = DISTANT
                else
                    ncontain = NOWHERE
            }
        } else {
            // only need to calculate distance to center + diameter
            distance = bbCenter.distance(z); // display distance, definitely bigger than precise distance
            dist = distance - diameter; // calc distance approximation
            nearestPoint.sync(bbCenter)
            if (dist < distanceRange || distanceRange < 0)
                ncontain = DISTANT
            else
                ncontain = NOWHERE
            // account for tolerances
        }

        // account for tolerances (notice no breaks)
        if (ncontain < contain) switch (contain) {
            case DISTANT:
                if (dist - distantTolerance < distanceRange) ncontain = DISTANT
            case PROXIMITY:
                if (dist - proximityTolerance < proximityRange) ncontain = PROXIMITY
            case INSIDE:
                if (dist - insideTolerance < 0)    ncontain = INSIDE
        }
    }

    public Boolean showThings () {
        if (!active) return false
        switch (showObjects) {
            case S_ALWAYS: return true
            case S_ONPROXIMITY: return contain >= PROXIMITY
            case S_ONENTER: return contain == INSIDE
            case S_NEVER: return false
            default: return false
        }
    }

    public Int visibleThings() {
        if (!showThings()) return 0
        Int count = 0
        Object key = null
        while ((key = inventory.next(key)) != null) {
            Object o = inventory.rawget(key)
            if (o is Player) continue
            if (!(o is Thing)) continue
            if (((Thing)o).isVisible()) count++
        }
        return count
    }

    public Unit collectThings (LuaTable c) {
        // XXX does this have to be a LuaTable? maybe it does...
        if (!showThings()) return
        Object key = null
        while ((key = inventory.next(key)) != null) {
            Object z = inventory.rawget(key)
            if (z is Thing && ((Thing)z).isVisible())
                TableLib.rawappend(c, z)
        }
    }

    public Boolean contains (Thing t) {
        if (t == Engine.instance.player) {
            return contain == INSIDE
        } else return super.contains(t)
    }

    public Unit serialize (DataOutputStream out) throws IOException {
        out.writeInt(contain)
        out.writeInt(ncontain)
        super.serialize(out)
    }

    public Unit deserialize (DataInputStream in) throws IOException {
        contain = in.readInt()
        ncontain = in.readInt()
        super.deserialize(in)
    }
}
