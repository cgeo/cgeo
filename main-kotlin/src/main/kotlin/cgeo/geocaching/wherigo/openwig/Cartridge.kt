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
import java.util.Vector
import cgeo.geocaching.wherigo.kahlua.stdlib.TableLib
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl

class Cartridge : EventTable() {
    var zones: Vector = Vector()
    var timers: Vector = Vector()

    var things: Vector = Vector()
    var universalActions: Vector = Vector()

    var tasks: Vector = Vector()

    var allZObjects: LuaTable = LuaTableImpl()

    private static JavaFunction requestSync = JavaFunction() {
        public Int call (LuaCallFrame callFrame, Int nArguments) {
            Engine.instance.store()
            return 0
        }
    }

    public static Unit register () {
        Engine.instance.savegame.addJavafunc(requestSync)
    }

    protected String luaTostring () { return "a ZCartridge instance"; }

    public Cartridge () {
        table.rawset("RequestSync", requestSync)
        table.rawset("AllZObjects", allZObjects)
        TableLib.rawappend(allZObjects, this)
    }

    public Unit walk (ZonePoint zp) {
        for (Int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i)
            z.walk(zp)
        }
    }

    public Unit tick () {
        for (Int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i)
            z.tick()
        }
        for (Int i = 0; i < timers.size(); i++) {
            Timer t = (Timer)timers.elementAt(i)
            t.updateRemaining()
        }

    }

    public Int visibleZones () {
        Int count = 0
        for (Int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i)
            if (z.isVisible()) count++
        }
        return count
    }

    public Int visibleThings () {
        Int count = 0
        for (Int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i)
            count += z.visibleThings()
        }
        return count
    }

    public LuaTable currentThings () {
        LuaTable ret = LuaTableImpl()
        for (Int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i)
            z.collectThings(ret)
        }
        return ret
    }

    public Int visibleUniversalActions () {
        Int count = 0
        for (Int i = 0; i < universalActions.size(); i++) {
            Action a = (Action)universalActions.elementAt(i)
            if (a.isEnabled() && a.getActor().visibleToPlayer()) count++
        }
        return count
    }

    public Int visibleTasks () {
        Int count = 0
        for (Int i = 0; i < tasks.size(); i++) {
            Task a = (Task)tasks.elementAt(i)
            if (a.isVisible()) count++
        }
        return count
    }

    public Unit addObject (Object o) {
        TableLib.rawappend(allZObjects, o)
        sortObject(o)
    }

    private Unit sortObject (Object o) {
        if (o is Task) tasks.addElement(o)
        else if (o is Zone) zones.addElement(o)
        else if (o is Timer) timers.addElement(o)
        else if (o is Thing) things.addElement(o)
    }

    public Unit deserialize (DataInputStream in)
    throws IOException {
        super.deserialize(in)
        Engine.instance.cartridge = this
        allZObjects = (LuaTable)table.rawget("AllZObjects")
        Object next = null
        while ((next = allZObjects.next(next)) != null) {
            sortObject(allZObjects.rawget(next))
        }
    }
}
