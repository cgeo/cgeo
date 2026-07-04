/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import androidx.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class EventTable implements LuaTable, Serializable {

    public LuaTable table = new LuaTableImpl();

    private static final JavaFunction TOSTRING = (callFrame, nArguments) -> {
        final Object obj = callFrame.get(0);
        //it is ESSENTIAL not to call toString() here!
        callFrame.push(obj instanceof EventTable parent ? parent.luaTostring() : "nil");
        return 1;
    };

    private static final LuaTable METATABLE = new LuaTableImpl();
    static {
        METATABLE.rawset("__tostring", TOSTRING);
    }

    private boolean isDeserializing = false;

    protected String luaTostring () { return "a ZObject instance"; }

    public void serialize (final DataOutputStream out) throws IOException {
        Engine.instance.savegame.storeValue(table, out);
    }

    public void deserialize (final DataInputStream in) throws IOException {
        isDeserializing = true;
        Engine.instance.savegame.restoreValue(in, this);
        isDeserializing = false;
        //setTable(table);
    }

    public String name, description;
    public ZonePoint position = null;
    protected boolean visible = false;

    public Media media, icon;

    public byte[] getMedia () throws IOException {
        return Engine.mediaFile(media);
    }

    public byte[] getIcon () throws IOException {
        return Engine.mediaFile(icon);
    }

    public boolean isVisible() { return visible; }

    public void setPosition(final ZonePoint location) {
        position = location;
        table.rawset("ObjectLocation", location);
    }

    public boolean isLocated() {
        return position != null;
    }

    protected void setItem(final String key, final Object value) {
        if ("Name".equals(key)) {
            name = BaseLib.rawTostring(value);
        } else if ("Description".equals(key)) {
            description = Engine.removeHtml(BaseLib.rawTostring(value));
        } else if ("Visible".equals(key)) {
            visible = LuaState.boolEval(value);
        } else if ("ObjectLocation".equals(key)) {
            //setPosition(ZonePoint.copy((ZonePoint)value));
            // i know there was need to copy. but why? it is messing up deserialization
            position = (ZonePoint)value;
        } else if ("Media".equals(key)) {
            media = (Media)value;
        } else if ("Icon".equals(key)) {
            icon = (Media)value;
        }
    }

    protected Object getItem (final String key) {
        if ("CurrentDistance".equals(key)) {
            if (isLocated()) return LuaState.toDouble(position.distance(Engine.instance.player.position));
            else return LuaState.toDouble(-1);
        } else if ("CurrentBearing".equals(key)) {
            if (isLocated())
                return LuaState.toDouble(ZonePoint.angle2azimuth(position.bearing(Engine.instance.player.position)));
            else return LuaState.toDouble(0);
        } else return table.rawget(key);
    }

    public void setTable (final LuaTable table) {
        Object n = null;
        while ((n = table.next(n)) != null) {
            final Object val = table.rawget(n);
            rawset(n, val);
            //if (n instanceof String) setItem((String)n, val);
        }
    }

    public void callEvent(final String name, final Object param) {
        /*
         workaround: suppress RuntimeException if callEvent() is called at deserialiation
         @see https://github.com/cgeo/openWIG/issues/8#issuecomment-612182631
         TODO: fix EventTable and ALL of its subclasses as described in the link
        */
        if (isDeserializing) {
            return;
        }

        try {
            final Object o = table.rawget(name);
            if (o instanceof LuaClosure) {
                Engine.log("EVNT: " + toString() + "." + name + (param!=null ? " (" + param.toString() + ")" : ""), Engine.LOG_CALL);
                final LuaClosure event = (LuaClosure) o;
                Engine.state.call(event, this, param, null);
                Engine.log("EEND: " + toString() + "." + name, Engine.LOG_CALL);
            }
        } catch (final Throwable t) {
            Engine.stacktrace(t);
        }
    }

    public boolean hasEvent(final String name) {
        return (table.rawget(name)) instanceof LuaClosure;
    }

    @NonNull
    public String toString()  {
        return baseToString(this) + BaseLib.luaTableToString(table, value ->
            value instanceof EventTable ? baseToString((EventTable) value) : null);
    }

    private static String baseToString(final EventTable et) {
        return "[" + et.getClass().getSimpleName() + "]" +
            (et.name == null ? "(unnamed)" : et.name);
    }

    public void rawset(final Object key, final Object value) {
        // TODO unify rawset/setItem
        if (key instanceof String) {
            setItem((String) key, value);
        }
        table.rawset(key, value);
        Engine.log("PROP: " + toString() + "." + key + " is set to " + (value == null ? "nil" : value.toString()), Engine.LOG_PROP);
    }

    public void setMetatable (final LuaTable metatable) { }

    public LuaTable getMetatable () { return METATABLE; }

    public Object rawget (final Object key) {
        // TODO unify rawget/getItem
        if (key instanceof String)
            return getItem((String)key);
        else
            return table.rawget(key);
    }

    public Object next (final Object key) { return table.next(key); }

    public int len () { return table.len(); }

    public Iterator<Object> keys() { return table.keys(); }


}
