/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig.formsts
 */
package cgeo.geocaching.wherigo.openwig.formats;

import java.io.*;
import java.util.Hashtable;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.kahlua.vm.LuaPrototype;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.kahlua.vm.UpValue;
import cgeo.geocaching.wherigo.openwig.Cartridge;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.Serializable;
import cgeo.geocaching.wherigo.openwig.platform.FileHandle;

/**
 * Handles saving and loading of Wherigo game state.
 * <p>
 * Savegame manages the serialization and deserialization of all game objects,
 * including the Lua state, to allow games to be saved and resumed. It handles
 * the complex task of converting between Java objects, Lua tables, and binary
 * data streams while maintaining object references and relationships.
 * <p>
 * Key features:
 * <ul>
 * <li>Serializes entire game state including Lua tables and Java objects</li>
 * <li>Preserves object references and circular references</li>
 * <li>Stores Lua closures, prototypes, and upvalues</li>
 * <li>Version checking to prevent loading incompatible saves</li>
 * <li>Handles legacy save files from older package names</li>
 * <li>Maintains registry of JavaFunctions for Lua callbacks</li>
 * </ul>
 * <p>
 * The save file format includes a signature, version number, and serialized
 * game objects. Only one save file is maintained per cartridge.
 */
public class Savegame {

    private static final String SIGNATURE = "openWIG savegame\n";

    private FileHandle saveFile;

    public Savegame (FileHandle fc) {
        if (fc == null) throw new NullPointerException("savefile must not be null");
        saveFile = fc;
    }

    protected Savegame () {
        /* for test mockups */
    }

    public boolean exists () throws IOException {
        return saveFile.exists();
    }

    protected boolean debug = false;
    protected void debug (String s) { }

    protected Class<?> classForName (String s) throws ClassNotFoundException {
        try {
            return Class.forName(s);
        } catch (ClassNotFoundException cnfe) {
            //handle old savefiles with maybe wrong package names
            // e.g. cz.matejcik.openwig.Media -> needs to be cgeo.geocaching.wherigo.openwig.Media
            final int idx = s.indexOf(".openwig.");
            if (idx >= 0) {
                return Class.forName("cgeo.geocaching.wherigo" + (s.substring(idx)));
            }
            throw cnfe;
        }
    }

    protected boolean versionOk (String ver) {
        return Engine.VERSION.equals(ver);
    }

    public void store (LuaTable table)
    throws IOException {
        DataOutputStream out = null;
        if (saveFile.exists())
            saveFile.truncate(0);
        else
            saveFile.create();
        try {
            Engine.log("STOR: storing game", Engine.LOG_CALL);
            out = saveFile.openDataOutputStream();

            out.writeUTF(SIGNATURE);
            out.writeUTF(Engine.VERSION);
            resetObjectStore();

            //specialcase cartridge:
            Engine currentEngine = Engine.getCurrentInstance();
            if (currentEngine != null) {
                storeValue(currentEngine.cartridge, out);
            }

            storeValue(table, out);
            Engine.log("STOR: store successful", Engine.LOG_CALL);
        } finally {
            try { out.close(); } catch (Exception ignored) { }
        }
    }

    protected void resetObjectStore () {
        objectStore = new Hashtable<>(256);
        // XXX why did i choose to use LuaTable over Hashtable?
        currentId = 0;
        level = 0;
    }

    public void restore (LuaTable table)
    throws IOException {
        DataInputStream dis = saveFile.openDataInputStream();
        String sig = dis.readUTF();
        if (!SIGNATURE.equals(sig)) throw new IOException("Invalid savegame file: bad signature.");
        try {
            String ver = dis.readUTF();
            if (!versionOk(ver)) throw new IOException("Savegame is for different version.");
        } catch (UTFDataFormatException e) {
            throw new IOException("Savegame is for different version.");
        }

        try {
            resetObjectStore();

            // specialcase cartridge: (TODO make a generic mechanism for this)
            Engine currentEngine = Engine.getCurrentInstance();
            if (currentEngine != null) {
                currentEngine.cartridge = (Cartridge)restoreValue(dis, null);
            } else {
                restoreValue(dis, null); // still need to read from stream
            }

            restoreValue(dis, table);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Problem loading game: "+e.getMessage());
        } finally {
            dis.close();
        }
    }

    private Hashtable<Object,Object> objectStore;
    private int currentId;

    private Hashtable<Integer, JavaFunction> idToJavafuncMap = new Hashtable<>(128);
    private Hashtable<JavaFunction, Integer> javafuncToIdMap = new Hashtable<>(128);
    private int currentJavafunc = 0;

    public void buildJavafuncMap (LuaTable environment) {
        LuaTable[] packages = new LuaTable[] {
            environment,
            (LuaTable)environment.rawget("string"),
            (LuaTable)environment.rawget("math"),
            (LuaTable)environment.rawget("coroutine"),
            (LuaTable)environment.rawget("os"),
            (LuaTable)environment.rawget("table")
        };
        for (int i = 0; i < packages.length; i++) {
            LuaTable table = packages[i];
            Object next = null;
            while ((next = table.next(next)) != null) {
                Object jf = table.rawget(next);
                if (jf instanceof JavaFunction) addJavafunc((JavaFunction)jf);
            }
        }
    }

    private static final byte LUA_NIL    = 0x00;
    private static final byte LUA_DOUBLE    = 0x01;
    private static final byte LUA_STRING    = 0x02;
    private static final byte LUA_BOOLEAN    = 0x03;
    private static final byte LUA_TABLE    = 0x04;
    private static final byte LUA_CLOSURE    = 0x05;
    private static final byte LUA_OBJECT    = 0x06;
    private static final byte LUA_REFERENCE = 0x07;
    private static final byte LUA_JAVAFUNC    = 0x08;

    private static final byte LUATABLE_PAIR = 0x10;
    private static final byte LUATABLE_END  = 0x11;

    public void addJavafunc (JavaFunction javafunc) {
        Integer id = new Integer(currentJavafunc++);
        idToJavafuncMap.put(id, javafunc);
        javafuncToIdMap.put(javafunc, id);
    }

    private int findJavafuncId (JavaFunction javafunc) {
        Integer id = javafuncToIdMap.get(javafunc);
        if (id != null) return id.intValue();
        else throw new IllegalStateException("javafunc not found in map!");
    }

    private JavaFunction findJavafuncObject (int id) {
        JavaFunction jf = idToJavafuncMap.get(Integer.valueOf(id));
        return jf;
    }

    private void storeObject (Object obj, DataOutputStream out)
    throws IOException {
        if (obj == null) {
            out.writeByte(LUA_NIL);
            return;
        }
        Integer i = (Integer)objectStore.get(obj);
        if (i != null) {
            out.writeByte(LUA_REFERENCE);
             if (debug) debug("reference "+i.intValue()+" ("+obj.toString()+")");
            out.writeInt(i.intValue());
        } else {
            i = new Integer(currentId++);
            objectStore.put(obj, i);
            if (debug) debug("(ref"+i.intValue()+")");
            if (obj instanceof Serializable) {
                out.writeByte(LUA_OBJECT);
                out.writeUTF(obj.getClass().getName());
                if (debug) debug(obj.getClass().getName() + " (" + obj.toString()+")");
                ((Serializable)obj).serialize(out);
            } else if (obj instanceof LuaTable) {
                out.writeByte(LUA_TABLE);
                if (debug) debug("table("+obj.toString()+"):\n");
                serializeLuaTable((LuaTable)obj, out);
            } else if (obj instanceof LuaClosure) {
                out.writeByte(LUA_CLOSURE);
                if (debug) debug("closure("+obj.toString()+")");
                serializeLuaClosure((LuaClosure)obj, out);
            } else {
                // we're busted
                out.writeByte(LUA_NIL);
                if (debug) debug("UFO");
                Engine.log("STOR: unable to store object of type "+obj.getClass().getName(), Engine.LOG_WARN);
            }
        }
    }

    public void storeValue (Object obj, DataOutputStream out)
    throws IOException {
        if (obj == null) {
            if (debug) debug("nil");
            out.writeByte(LUA_NIL);
        } else if (obj instanceof String) {
            out.writeByte(LUA_STRING);
            if (debug) debug("\""+obj.toString()+"\"");
            out.writeUTF((String)obj);
        } else if (obj instanceof Boolean) {
            if (debug) debug(obj.toString());
            out.writeByte(LUA_BOOLEAN);
            out.writeBoolean(((Boolean)obj).booleanValue());
        } else if (obj instanceof Double) {
            out.writeByte(LUA_DOUBLE);
            if (debug) debug(obj.toString());
            out.writeDouble(((Double)obj).doubleValue());
        } else if (obj instanceof JavaFunction) {
            int i = findJavafuncId((JavaFunction)obj);
            if (debug) debug("javafunc("+i+")-"+obj.toString());
            out.writeByte(LUA_JAVAFUNC);
            out.writeInt(i);
        } else {
            storeObject(obj, out);
        }
    }

    public void serializeLuaTable (LuaTable table, DataOutputStream out)
    throws IOException {
        level++;
        Object next = null;
        while ((next = table.next(next)) != null) {
            Object value = table.rawget(next);
            out.writeByte(LUATABLE_PAIR);
            if (debug) for (int i = 0; i < level; i++) debug("  ");

            storeValue(next, out);
            if (debug) debug(" : ");
            storeValue(value, out);
            if (debug) debug("\n");
        }
        level--;
        out.writeByte(LUATABLE_END);
    }

    public Object restoreValue (DataInputStream in, Object target)
    throws IOException {
        byte type = in.readByte();
        return switch (type) {
            case LUA_NIL -> {
                if (debug) debug("nil");
                yield null;
            }
            case LUA_DOUBLE -> {
                double d = in.readDouble();
                if (debug) debug(String.valueOf(d));
                yield LuaState.toDouble(d);
            }
            case LUA_STRING -> {
                String s = in.readUTF();
                if (debug) debug("\"" + s + "\"");
                yield s;
            }
            case LUA_BOOLEAN -> {
                boolean b = in.readBoolean();
                if (debug) debug(String.valueOf(b));
                yield LuaState.toBoolean(b);
            }
            case LUA_JAVAFUNC -> {
                int i = in.readInt();
                JavaFunction jf = findJavafuncObject(i);
                if (debug) debug("javafunc("+i+")-"+jf);
                yield jf;
            }
            default -> restoreObject(in, type, target);
        };
    }

    private void restCache (Object o) {
        Integer i = new Integer(currentId++);
        objectStore.put(i, o);
        if (debug) debug("(ref"+i.intValue()+")");
    }

    private Object restoreObject (DataInputStream in, byte type, Object target)
    throws IOException {
        return switch (type) {
            case LUA_TABLE -> {
                LuaTable lti;
                if (target instanceof LuaTable lt)
                    lti = lt;
                else
                    lti = new LuaTableImpl();
                restCache(lti);
                if (debug) debug("table:\n");
                yield deserializeLuaTable(in, lti);
            }
            case LUA_CLOSURE -> {
                if (debug) debug("closure: ");
                LuaClosure lc = deserializeLuaClosure(in);
                if (debug) debug(lc.toString());
                yield lc;
            }
            case LUA_OBJECT -> {
                String cls = in.readUTF();
                Serializable s = null;
                try {
                    if (debug) debug("object of type "+cls+"...\n");
                    Class<?> c = classForName(cls);
                    if (Serializable.class.isAssignableFrom(c)) {
                        s = (Serializable)c.newInstance();
                    }
                } catch (Throwable e) {
                    if (debug) debug("(failed to deserialize "+cls+")\n");
                    Engine.log("REST: while trying to deserialize "+cls+":\n"+e.toString(), Engine.LOG_ERROR);
                    BaseLib.fail("Could not deserialize object of class '" + cls + "': " + e);
                }
                if (s != null) {
                    restCache(s);
                    s.deserialize(in);
                }
                yield s;
            }
            case LUA_REFERENCE -> {
                Integer what = new Integer(in.readInt());
                if (debug) debug("reference "+what.intValue());
                Object result = objectStore.get(what);
                if (result == null) {
                    Engine.log("REST: not found reference "+what.toString()+" in object store", Engine.LOG_WARN);
                    if (debug) debug(" (which happens to be null?)");
                    yield target;
                } else {
                    if (debug) debug(" : "+result.toString());
                }
                yield result;
            }
            default -> {
                Engine.log("REST: found unknown type "+type, Engine.LOG_WARN);
                if (debug) debug("UFO");
                yield null;
            }
        };
    }

    int level = 0;

    public LuaTable deserializeLuaTable (DataInputStream in, LuaTable table)
    throws IOException {
        level++;
        while (true) {
            byte next = in.readByte();
            if (next == LUATABLE_END) break;
            if (debug) for (int i = 0; i < level; i++) debug("  ");
            Object key = restoreValue(in, null);
            if (debug) debug(" : ");
            Object value = restoreValue(in, table.rawget(key));
            if (debug) debug("\n");
            table.rawset(key, value);
        }
        level--;
        return table;
    }

    private void serializeLuaClosure (LuaClosure closure, DataOutputStream out)
    throws IOException {
        closure.prototype.dump(out);
        for (int i = 0; i < closure.upvalues.length; i++) {
            UpValue u = closure.upvalues[i];
            if (u.value == null) {
                Engine.log("STOR: unclosed upvalue in "+closure.toString(), Engine.LOG_WARN);
                u.value = u.thread.objectStack[u.index];
            }
            storeValue(u.value, out);
        }
    }

    private LuaClosure deserializeLuaClosure (DataInputStream in)
    throws IOException {
        Engine currentEngine = Engine.getCurrentInstance();
        LuaClosure closure = LuaPrototype.loadByteCode(in, 
            currentEngine != null ? currentEngine.luaState.getEnvironment() : null);
        restCache(closure);
        for (int i = 0; i < closure.upvalues.length; i++) {
            UpValue u = new UpValue();
            u.value = restoreValue(in, null);
            closure.upvalues[i] = u;
        }
        return closure;
    }
}
