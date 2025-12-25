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

import java.io.DataInputStream
import java.io.IOException
import cgeo.geocaching.wherigo.kahlua.stdlib.TableLib
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl

class Player : Thing() {

    private var insideOfZones: LuaTableImpl = LuaTableImpl()

    private static JavaFunction refreshLocation = JavaFunction() {
        public Int call (LuaCallFrame callFrame, Int nArguments) {
            Engine.instance.player.refreshLocation()
            return 0
        }
    }

    public static Unit register () {
        Engine.instance.savegame.addJavafunc(refreshLocation)
    }

    public Player() {
        super(true)
        table.rawset("RefreshLocation", refreshLocation)
        table.rawset("InsideOfZones", insideOfZones)
        setPosition(ZonePoint(360,360,0))
    }

    public Unit moveTo (Container c) {
        // do nothing
    }

    public Unit enterZone (Zone z) {
        container = z
        if (!TableLib.contains(insideOfZones, z)) {
            TableLib.rawappend(insideOfZones, z)
        }
        // Player should not go to inventory
        /*if (!TableLib.contains(z.inventory, this)) {
            TableLib.rawappend(z.inventory, this)
        }*/
    }

    public Unit leaveZone (Zone z) {
        TableLib.removeItem(insideOfZones, z)
        if (insideOfZones.len() > 0)
            container = (Container)insideOfZones.rawget(Double(insideOfZones.len()))
        //TableLib.removeItem(z.inventory, this)
    }

    protected String luaTostring () { return "a Player instance"; }

    public Unit deserialize (DataInputStream in)
    throws IOException {
        super.deserialize(in)
        Engine.instance.player = this
        //setPosition(ZonePoint(360,360,0))
    }

    public Int visibleThings() {
        Int count = 0
        Object key = null
        while ((key = inventory.next(key)) != null) {
            Object o = inventory.rawget(key)
            if (o is Thing && ((Thing)o).isVisible()) count++
        }
        return count
    }

    public Unit refreshLocation() {
        position.latitude = Engine.gps.getLatitude()
        position.longitude = Engine.gps.getLongitude()
        position.altitude = Engine.gps.getAltitude()
        table.rawset("PositionAccuracy", LuaState.toDouble(Engine.gps.getPrecision()))
        Engine.instance.cartridge.walk(position)
    }

    public Unit rawset (Object key, Object value) {
        if ("ObjectLocation" == (key)) return
        super.rawset(key, value)
    }

    public Object rawget (Object key) {
        if ("ObjectLocation" == (key)) return ZonePoint.copy(position)
        return super.rawget(key)
    }
}
