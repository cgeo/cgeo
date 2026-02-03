/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import java.io.*;
import cgeo.geocaching.wherigo.kahlua.stdlib.TableLib;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

/**
 * Represents a geographic zone in a Wherigo game.
 * <p>
 * Zone extends Thing to provide location-based gameplay. A zone is a geographic
 * area defined by a set of points that forms a polygon. The engine continuously
 * monitors the player's position relative to zones to trigger events when the
 * player enters, exits, or approaches zones.
 * <p>
 * Key features:
 * <ul>
 * <li>Defined by a set of geographic points forming a polygon</li>
 * <li>Tracks player state: INSIDE, PROXIMITY, DISTANT, or NOWHERE</li>
 * <li>Can be active/inactive and visible/invisible</li>
 * <li>Triggers events on state changes (OnEnter, OnExit, OnProximity)</li>
 * <li>Calculates distance to player and nearest point</li>
 * <li>Supports proximity detection with configurable ranges</li>
 * <li>Can contain items that are shown when player enters the zone</li>
 * </ul>
 * <p>
 * Zones use bounding boxes and distance calculations to efficiently determine
 * player proximity without requiring complex polygon tests every frame.
 */
public class Zone extends Thing {

    protected String luaTostring() { return "a Zone instance"; }

    public boolean isVisible () {
        return active && visible && contain > NOWHERE;
    }

    public boolean isActive() {
        return active && contain > NOWHERE;
    }

    public boolean visibleToPlayer () {
        return isVisible();
    }

    public boolean isLocated () {
        return true;
    }

    public ZonePoint[] points;

    private boolean active = false;

    public static final int INSIDE = 2;
    public static final int PROXIMITY = 1;
    public static final int DISTANT = 0;
    public static final int NOWHERE = -1;

    public int contain = NOWHERE;
    private int ncontain = NOWHERE;

    public static final int S_ALWAYS = 0;
    public static final int S_ONENTER = 1;
    public static final int S_ONPROXIMITY = 2;
    public static final int S_NEVER = 3;

    private int showObjects = S_ONENTER;

    public double distance = Double.MAX_VALUE; // distance in metres
    public ZonePoint nearestPoint = new ZonePoint(0,0,0);
    private double distanceRange = -1, proximityRange = -1;

    public double bbTop, bbBottom, bbLeft, bbRight; // zone's bounding box
    public double pbbTop, pbbBottom, pbbLeft, pbbRight; // pbb = proximity bounding box
    public ZonePoint bbCenter = new ZonePoint(0,0,0);
    private double diameter; // approximate zone diameter - distance from bounding-box center to farthest vertex
    private double insideTolerance = 5, proximityTolerance = 10, distantTolerance = 20; // hysteresis tolerance

    private static final double DEFAULT_PROXIMITY = 1500.0;

    protected void setItem (String key, Object value) {
        if ("Points".equals(key) && value != null) {
            LuaTable lt = (LuaTable) value;
            int n = lt.len();
            points = new ZonePoint[n];
            for (int i = 1; i <= n; i++) {
                ZonePoint zp = (ZonePoint) lt.rawget(new Double(i));
                points[i-1] = zp;
            }
            if (active) {
                preprocess();
                Engine currentEngine = Engine.getCurrentInstance();
                if (currentEngine != null && currentEngine.player != null) {
                    walk(currentEngine.player.position);
                }
                //setcontain();
            }
        } else if ("Active".equals(key)) {
            boolean a = LuaState.boolEval(value);
            if (a != active) callEvent("OnZoneState", null);
            active = a;
            if (a) preprocess();
            if (active) {
                Engine currentEngine = Engine.getCurrentInstance();
                if (currentEngine != null && currentEngine.player != null) {
                    walk(currentEngine.player.position);
                }
                //setcontain();
            } else { // if the zone is deactivated, remove player, just to be sure
                contain = ncontain = (distanceRange < 0) ? DISTANT : NOWHERE;
                Engine currentEngine = Engine.getCurrentInstance();
                if (currentEngine != null && currentEngine.player != null) {
                    currentEngine.player.leaveZone(this);
                }
            }
        } else if ("Visible".equals(key)) {
            boolean a = LuaState.boolEval(value);
            if (a != visible) callEvent("OnZoneState", null);
            visible = a;
        } else if ("DistanceRange".equals(key) && value instanceof Double) {
            distanceRange = LuaState.fromDouble(value);
            preprocess();
            if (distanceRange < 0 && contain == NOWHERE) {
                contain = ncontain = DISTANT;
            }
        } else if ("ProximityRange".equals(key) && value instanceof Double) {
            preprocess();
            proximityRange = LuaState.fromDouble(value);
        } else if ("ShowObjects".equals(key)) {
            String v = (String)value;
            if ("Always".equals(v)) {
                showObjects = S_ALWAYS;
            } else if ("OnProximity".equals(v)) {
                showObjects = S_ONPROXIMITY;
            } else if ("OnEnter".equals(v)) {
                showObjects = S_ONENTER;
            } else if ("Never".equals(v)) {
                showObjects = S_NEVER;
            }
        } else if ("OriginalPoint".equals(key)) {
            position = (ZonePoint)value;
        } else super.setItem(key, value);
    }

    public void tick () {
        if (!active) return;
        if (contain != ncontain) setcontain();
        /*if (ncontain == contain) ticks = 0;
        else if (ncontain > contain) setcontain();
        else if (Midlet.gpsType == Midlet.GPS_MANUAL) setcontain();
        else {
            ticks ++;
            if (ticks % 5 == 0) setcontain();
        }*/
    }

    private void setcontain () {
        if (contain == ncontain) return;
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine == null || currentEngine.player == null) return;
        
        if (contain == INSIDE) {
            currentEngine.player.leaveZone(this);
            Engine.callEvent(this, "OnExit", null);
        }
        contain = ncontain;
        if (contain == INSIDE) {
            currentEngine.player.enterZone(this);
        }
        switch (contain) {
            case INSIDE -> {
                Engine.log("ZONE: inside "+name, Engine.LOG_PROP);
                Engine.callEvent(this, "OnEnter", null);
            }
            case PROXIMITY -> {
                Engine.log("ZONE: proximity "+name, Engine.LOG_PROP);
                Engine.callEvent(this, "OnProximity", null);
            }
            case DISTANT -> {
                Engine.log("ZONE: distant "+name, Engine.LOG_PROP);
                Engine.callEvent(this, "OnDistant", null);
            }
            case NOWHERE -> {
                Engine.log("ZONE: out-of-range "+name, Engine.LOG_PROP);
                Engine.callEvent(this, "OnNotInRange", null);
            }
            default -> {
                return;
            }
        }
        Engine.refreshUI();
    }

    /** calculate bounding-box values */
    private void preprocess () {
        if (points == null || points.length == 0) return;

        // first calculate bounding box for zone shape
        bbTop = Double.NEGATIVE_INFINITY; bbBottom = Double.POSITIVE_INFINITY;
        bbLeft = Double.POSITIVE_INFINITY; bbRight = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.length; i++) {
            bbTop = Math.max(bbTop, points[i].latitude);
            bbBottom = Math.min(bbBottom, points[i].latitude);
            bbLeft = Math.min(bbLeft, points[i].longitude);
            bbRight = Math.max(bbRight, points[i].longitude);
        }
        // its center point
        bbCenter.latitude = bbBottom + ((bbTop - bbBottom) / 2);
        bbCenter.longitude = bbLeft + ((bbRight - bbLeft) / 2);

        // margins for proximity bounding box
        double proximityX = ZonePoint.m2lat((proximityRange < DEFAULT_PROXIMITY) ? DEFAULT_PROXIMITY : proximityRange);
        double proximityY = ZonePoint.m2lon(bbCenter.latitude, (proximityRange < DEFAULT_PROXIMITY) ? DEFAULT_PROXIMITY : proximityRange);
        // and the box itself
        pbbTop = bbTop + proximityX; pbbBottom = bbBottom - proximityX;
        pbbLeft = bbLeft - proximityY; pbbRight = bbRight + proximityY;

        // zone diameter
        double dist = 0;
        double xx = 0, yy = 0;
        for (int i = 0; i < points.length; i++) {
            double x = points[i].latitude - bbCenter.latitude;
            double y = points[i].longitude - bbCenter.longitude;
            double dd = x*x + y*y;
            if (dd > dist) {
                xx = points[i].latitude; yy = points[i].longitude;
                dist = dd;
            }
        }
        diameter = bbCenter.distance(xx, yy);
    }

    public void walk (ZonePoint z) {
        if (!active || points == null || points.length == 0 || z == null) {
            return;
        }

        double dist = 0;
        // are we inside proximity bounding-box?
        if (z.latitude > pbbBottom && z.latitude < pbbTop && z.longitude > pbbLeft && z.longitude < pbbRight) {
            ncontain = PROXIMITY;
            // are we within zone bounding box?
            if (z.latitude > bbBottom && z.latitude < bbTop && z.longitude > bbLeft && z.longitude < bbRight && points.length > 2) {
                // yes, we need precise inside evaluation
                // the following code is adapted from http://www.visibone.com/inpoly/
                double xt = z.latitude, yt = z.longitude;
                double ax = points[points.length - 1].latitude, ay = points[points.length - 1].longitude;
                boolean inside = false;
                for (int i = 0; i < points.length; i++) {
                    double bx = points[i].latitude, by = points[i].longitude;
                    double x1, y1, x2, y2;
                    if (bx > ax) {
                        x1 = ax; y1 = ay;
                        x2 = bx; y2 = by;
                    } else {
                        x1 = bx; y1 = by;
                        x2 = ax; y2 = ay;
                    }
                    if (x1 < xt && xt <= x2) { // consider!
                        if (ay > yt && by > yt) { // we're completely below -> flip
                            inside = !inside;
                        } else if (ay < yt && by < yt) { // we're completely above -> ignore
                            // ...
                        } else if ((yt - y1)*(x2 - x1) < (y2 - y1)*(xt - x1)) {
                            // we're below (hopefully)
                            inside = !inside;
                        }
                    }
                    ax = bx; ay = by;
                }
                if (inside) {
                    ncontain = INSIDE;
                    distance = dist = 0;
                    nearestPoint.sync(z);
                }
            }
            if (ncontain != INSIDE) {
                // now we need precise distance calculation
                double ax = points[points.length - 1].latitude, ay = points[points.length - 1].longitude;
                double x, y;
                double nx = ax, ny = ay;
                double ndist = Double.POSITIVE_INFINITY;
                for (int i = 0; i < points.length; i++) {
                    double bx = points[i].latitude, by = points[i].longitude;
                    // find distance to vertex (ax,ay)-(bx,by)
                    double dot_ta = (z.latitude - ax) * (bx - ax) + (z.longitude - ay) * (by - ay);
                    if (dot_ta <= 0) {// IT IS OFF THE AVERTEX
                        x = ax;
                        y = ay;
                    } else {
                        double dot_tb = (z.latitude - bx) * (ax - bx) + (z.longitude - by) * (ay - by);
                        if (dot_tb <= 0) { // SEE IF b IS THE NEAREST POINT - ANGLE IS OBTUSE
                            x = bx;
                            y = by;
                        } else {
                            // FIND THE REAL NEAREST POINT ON THE LINE SEGMENT - BASED ON RATIO
                            x = ax + ((bx - ax) * dot_ta) / (dot_ta + dot_tb);
                            y = ay + ((by - ay) * dot_ta) / (dot_ta + dot_tb);
                        }
                    }
                    double dd = (x - z.latitude) * (x - z.latitude) + (y - z.longitude) * (y - z.longitude);
                    if (dd < ndist) {
                        nx = x;
                        ny = y;
                        ndist = dd;
                    }
                    ax = bx;
                    ay = by;
                }
                nearestPoint.latitude = nx;
                nearestPoint.longitude = ny;
                distance = dist = z.distance(nx, ny);

                if (distance < proximityRange || proximityRange < 0)
                    ncontain = PROXIMITY;
                else if (distance < distanceRange || distanceRange < 0)
                    ncontain = DISTANT;
                else
                    ncontain = NOWHERE;
            }
        } else {
            // only need to calculate distance to center + diameter
            distance = bbCenter.distance(z); // display distance, definitely bigger than precise distance
            dist = distance - diameter; // calc distance approximation
            nearestPoint.sync(bbCenter);
            if (dist < distanceRange || distanceRange < 0)
                ncontain = DISTANT;
            else
                ncontain = NOWHERE;
            // account for tolerances
        }

        // account for tolerances (notice no breaks)
        if (ncontain < contain) switch (contain) {
            case DISTANT:
                if (dist - distantTolerance < distanceRange) ncontain = DISTANT;
            case PROXIMITY:
                if (dist - proximityTolerance < proximityRange) ncontain = PROXIMITY;
            case INSIDE:
                if (dist - insideTolerance < 0)    ncontain = INSIDE;
        }
    }

    public boolean showThings () {
        if (!active) return false;
        return switch (showObjects) {
            case S_ALWAYS -> true;
            case S_ONPROXIMITY -> contain >= PROXIMITY;
            case S_ONENTER -> contain == INSIDE;
            case S_NEVER -> false;
            default -> false;
        };
    }

    public int visibleThings() {
        if (!showThings()) return 0;
        int count = 0;
        Object key = null;
        while ((key = inventory.next(key)) != null) {
            Object o = inventory.rawget(key);
            if (o instanceof Player) continue;
            if (!(o instanceof Thing thing)) continue;
            if (thing.isVisible()) count++;
        }
        return count;
    }

    public void collectThings (LuaTable c) {
        // XXX does this have to be a LuaTable? maybe it does...
        if (!showThings()) return;
        Object key = null;
        while ((key = inventory.next(key)) != null) {
            Object z = inventory.rawget(key);
            if (z instanceof Thing thing && thing.isVisible())
                TableLib.rawappend(c, z);
        }
    }

    public boolean contains (Thing t) {
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null && t == currentEngine.player) {
            return contain == INSIDE;
        } else return super.contains(t);
    }

    @Override
    public void serialize (DataOutputStream out) throws IOException {
        out.writeInt(contain);
        out.writeInt(ncontain);
        super.serialize(out);
    }

    @Override
    public void deserialize (DataInputStream in) throws IOException {
        contain = in.readInt();
        ncontain = in.readInt();
        super.deserialize(in);
    }
}
