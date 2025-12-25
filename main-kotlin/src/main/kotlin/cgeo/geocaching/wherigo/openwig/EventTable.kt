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

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl

import androidx.annotation.NonNull

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.Iterator

class EventTable : LuaTable, Serializable {

    var table: LuaTable = LuaTableImpl()

    private var metatable: LuaTable = LuaTableImpl()

    private var isDeserializing: Boolean = false

    private static class TostringJavaFunc : JavaFunction {

        public EventTable parent

        public TostringJavaFunc (EventTable parent) {
            this.parent = parent
        }

        public Int call (LuaCallFrame callFrame, Int nArguments) {
            callFrame.push(parent.luaTostring()); //it is ESSENTIAL not to call toString() here!
            return 1
        }
    }

    protected String luaTostring () { return "a ZObject instance"; }

    public EventTable() {
        metatable.rawset("__tostring", TostringJavaFunc(this))
    }

    public Unit serialize (DataOutputStream out) throws IOException {
        Engine.instance.savegame.storeValue(table, out)
    }

    public Unit deserialize (DataInputStream in) throws IOException {
        isDeserializing = true
        Engine.instance.savegame.restoreValue(in, this)
        isDeserializing = false
        //setTable(table)
    }

    public String name, description
    var position: ZonePoint = null
    protected var visible: Boolean = false

    public Media media, icon

    public Byte[] getMedia () throws IOException {
        return Engine.mediaFile(media)
    }

    public Byte[] getIcon () throws IOException {
        return Engine.mediaFile(icon)
    }

    public Boolean isVisible() { return visible; }

    public Unit setPosition(ZonePoint location) {
        position = location
        table.rawset("ObjectLocation", location)
    }

    public Boolean isLocated() {
        return position != null
    }

    protected Unit setItem(String key, Object value) {
        if ("Name" == (key)) {
            name = BaseLib.rawTostring(value)
        } else if ("Description" == (key)) {
            description = Engine.removeHtml(BaseLib.rawTostring(value))
        } else if ("Visible" == (key)) {
            visible = LuaState.boolEval(value)
        } else if ("ObjectLocation" == (key)) {
            //setPosition(ZonePoint.copy((ZonePoint)value))
            // i know there was need to copy. but why? it is messing up deserialization
            position = (ZonePoint)value
        } else if ("Media" == (key)) {
            media = (Media)value
        } else if ("Icon" == (key)) {
            icon = (Media)value
        }
    }

    protected Object getItem (String key) {
        if ("CurrentDistance" == (key)) {
            if (isLocated()) return LuaState.toDouble(position.distance(Engine.instance.player.position))
            else return LuaState.toDouble(-1)
        } else if ("CurrentBearing" == (key)) {
            if (isLocated())
                return LuaState.toDouble(ZonePoint.angle2azimuth(position.bearing(Engine.instance.player.position)))
            else return LuaState.toDouble(0)
        } else return table.rawget(key)
    }

    public Unit setTable (LuaTable table) {
        Object n = null
        while ((n = table.next(n)) != null) {
            Object val = table.rawget(n)
            rawset(n, val)
            //if (n is String) setItem((String)n, val)
        }
    }

    public Unit callEvent(String name, Object param) {
        /*
         workaround: suppress RuntimeException if callEvent() is called at deserialiation
         @see https://github.com/cgeo/openWIG/issues/8#issuecomment-612182631
         TODO: fix EventTable and ALL of its subclasses as described in the link
        */
        if (isDeserializing) {
            return
        }

        try {
            Object o = table.rawget(name)
            if (o is LuaClosure) {
                Engine.log("EVNT: " + toString() + "." + name + (param!=null ? " (" + param.toString() + ")" : ""), Engine.LOG_CALL)
                LuaClosure event = (LuaClosure) o
                Engine.state.call(event, this, param, null)
                Engine.log("EEND: " + toString() + "." + name, Engine.LOG_CALL)
            }
        } catch (Throwable t) {
            Engine.stacktrace(t)
        }
    }

    public Boolean hasEvent(String name) {
        return (table.rawget(name)) is LuaClosure
    }

    public String toString()  {
        return baseToString(this) + BaseLib.luaTableToString(table, value ->
            value is EventTable ? baseToString((EventTable) value) : null)
    }

    private static String baseToString(final EventTable et) {
        return "[" + et.getClass().getSimpleName() + "]" +
            (et.name == null ? "(unnamed)" : et.name)
    }

    public Unit rawset(Object key, Object value) {
        // TODO unify rawset/setItem
        if (key is String) {
            setItem((String) key, value)
        }
        table.rawset(key, value)
        Engine.log("PROP: " + toString() + "." + key + " is set to " + (value == null ? "nil" : value.toString()), Engine.LOG_PROP)
    }

    public Unit setMetatable (LuaTable metatable) { }

    public LuaTable getMetatable () { return metatable; }

    public Object rawget (Object key) {
        // TODO unify rawget/getItem
        if (key is String)
            return getItem((String)key)
        else
            return table.rawget(key)
    }

    public Object next (Object key) { return table.next(key); }

    public Int len () { return table.len(); }

    public Iterator<Object> keys() { return table.keys(); }


}
