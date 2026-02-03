/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import androidx.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Extended LuaTable implementation that adds game object functionality for Wherigo cartridges.
 *
 * <p>EventTable extends the basic Lua table with:</p>
 * <ul>
 *   <li>Event handling system for Lua callbacks (OnTick, OnStart, OnStop, etc.)</li>
 *   <li>Common game object properties (name, description, position, visibility)</li>
 *   <li>Serialization support for save game persistence</li>
 *   <li>Property interception via setItem/getItem hooks</li>
 *   <li>Media and icon management</li>
 * </ul>
 *
 * <p>This class serves as the base for all Wherigo game objects including Timer, Task,
 * Media, Container, Thing, Zone, and Player.</p>
 *
 * <h3>Thread Safety:</h3>
 * This class is thread-safe. Critical fields are volatile and methods that access shared
 * state are synchronized where necessary. Inherits thread-safety from {@link LuaTableImpl}.
 *
 * <h3>Event System:</h3>
 * Subclasses can respond to Lua events by storing LuaClosure callbacks in the table.
 * Use {@link #callEvent(String, Object)} to invoke these callbacks.
 *
 * <h3>Property Interception:</h3>
 * Override {@link #setItem(String, Object)} and {@link #getItem(String)} to intercept
 * specific property assignments and provide computed properties.
 *
 * @see LuaTableImpl
 */
public class EventTable extends LuaTableImpl implements Serializable {

    private volatile boolean isDeserializing = false;

    private static class TostringJavaFunc implements JavaFunction {

        public EventTable parent;

        public TostringJavaFunc (EventTable parent) {
            this.parent = parent;
        }

        public int call (LuaCallFrame callFrame, int nArguments) {
            callFrame.push(parent.luaTostring()); //it is ESSENTIAL not to call toString() here!
            return 1;
        }
    }

    protected String luaTostring () { return "a ZObject instance"; }

    public EventTable() {
        // Initialize metatable to support __tostring
        LuaTable metatable = new LuaTableImpl();
        metatable.rawset("__tostring", new TostringJavaFunc(this));
        super.setMetatable(metatable);
    }

    @Override
    public void serialize (DataOutputStream out) throws IOException {
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            currentEngine.savegame.storeValue(this, out);
        }
    }

    @Override
    public void deserialize (DataInputStream in) throws IOException {
        isDeserializing = true;
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            currentEngine.savegame.restoreValue(in, this);
        }
        isDeserializing = false;
        //setTable(table);
    }

    public volatile String name, description;
    public volatile ZonePoint position = null;
    protected volatile boolean visible = false;

    public volatile Media media, icon;

    public byte[] getMedia () throws IOException {
        return Engine.mediaFile(media);
    }

    public byte[] getIcon () throws IOException {
        return Engine.mediaFile(icon);
    }

    public synchronized boolean isVisible() { return visible; }

    public synchronized void setPosition(ZonePoint location) {
        position = location;
        this.rawset("ObjectLocation", location);
    }

    public synchronized boolean isLocated() {
        return position != null;
    }

    /**
     * Hook method called when a property is set via rawset.
     * Subclasses override this to intercept specific property assignments and
     * implement custom behavior (e.g., triggering events, validation, type conversion).
     *
     * <p>The default implementation handles common properties:</p>
     * <ul>
     *   <li>Name - Sets the object's name</li>
     *   <li>Description - Sets the description (with HTML removal)</li>
     *   <li>Visible - Sets visibility flag</li>
     *   <li>ObjectLocation - Sets the position</li>
     *   <li>Media - Sets the media resource</li>
     *   <li>Icon - Sets the icon resource</li>
     * </ul>
     *
     * @param key The property name
     * @param value The value being set
     */
    protected void setItem(String key, Object value) {
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

    /**
     * Hook method called when a property is retrieved.
     * Subclasses override this to provide computed properties that don't exist in the table
     * (e.g., CurrentDistance, CurrentBearing calculated from position).
     *
     * <p>The default implementation provides computed distance and bearing properties:</p>
     * <ul>
     *   <li>CurrentDistance - Distance from player to this object in meters</li>
     *   <li>CurrentBearing - Bearing from player to this object in degrees (0-360)</li>
     * </ul>
     *
     * @param key The property name
     * @return The property value, or the raw table value if no special handling
     */
    protected Object getItem (String key) {
        if ("CurrentDistance".equals(key)) {
            Engine currentEngine = Engine.getCurrentInstance();
            if (isLocated() && currentEngine != null && currentEngine.player != null) 
                return LuaState.toDouble(position.distance(currentEngine.player.position));
            else return LuaState.toDouble(-1);
        } else if ("CurrentBearing".equals(key)) {
            Engine currentEngine = Engine.getCurrentInstance();
            if (isLocated() && currentEngine != null && currentEngine.player != null)
                return LuaState.toDouble(ZonePoint.angle2azimuth(position.bearing(currentEngine.player.position)));
            else return LuaState.toDouble(0);
        } else return this.rawget(key);
    }

    public void setTable (LuaTable table) {
        Object n = null;
        while ((n = table.next(n)) != null) {
            Object val = table.rawget(n);
            rawset(n, val);
            //if (n instanceof String) setItem((String)n, val);
        }
    }

    /**
     * Calls a Lua event callback if one is defined for the given event name.
     *
     * <p>This method looks up the event name in the table. If the value is a LuaClosure,
     * it invokes the closure with this object as the first parameter and the provided
     * param as the second parameter.</p>
     *
     * <p>Note: This method is suppressed during deserialization to avoid calling events
     * before the object is fully initialized. See
     * <a href="https://github.com/cgeo/openWIG/issues/8#issuecomment-612182631">Issue #8</a>
     * for details.</p>
     *
     * @param name The event name (e.g., "OnTick", "OnStart", "OnStop")
     * @param param Optional parameter to pass to the event callback, or null
     */
    public void callEvent(String name, Object param) {
        /*
         workaround: suppress RuntimeException if callEvent() is called at deserialiation
         @see https://github.com/cgeo/openWIG/issues/8#issuecomment-612182631
         TODO: fix EventTable and ALL of its subclasses as described in the link
        */
        if (isDeserializing) {
            return;
        }

        try {
            Object o = this.rawget(name);
            if (o instanceof LuaClosure) {
                Engine.log("EVNT: " + toString() + "." + name + (param!=null ? " (" + param.toString() + ")" : ""), Engine.LOG_CALL);
                LuaClosure event = (LuaClosure) o;
                Engine currentEngine = Engine.getCurrentInstance();
                if (currentEngine != null) {
                    currentEngine.luaState.call(event, this, param, null);
                }
                Engine.log("EEND: " + toString() + "." + name, Engine.LOG_CALL);
            }
        } catch (Throwable t) {
            Engine.stacktrace(t);
        }
    }

    public boolean hasEvent(String name) {
        return (this.rawget(name)) instanceof LuaClosure;
    }

    @NonNull
    @Override
    public String toString() {
        return baseToString(this) + BaseLib.luaTableToString(this, value ->
            value instanceof EventTable ? baseToString((EventTable) value) : null);
    }

    private static String baseToString(final EventTable et) {
        return "[" + et.getClass().getSimpleName() + "]" +
            (et.name == null ? "(unnamed)" : et.name);
    }

    @Override
    public void rawset(Object key, Object value) {
        // TODO unify rawset/setItem
        if (key instanceof String) {
            setItem((String) key, value);
        }
        super.rawset(key, value);
        Engine.log("PROP: " + toString() + "." + key + " is set to " + (value == null ? "nil" : value.toString()), Engine.LOG_PROP);
    }
}
