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
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig.formsts
 */
package cgeo.geocaching.wherigo.openwig.formats

import java.io.*
import java.util.Hashtable

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure
import cgeo.geocaching.wherigo.kahlua.vm.LuaPrototype
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl
import cgeo.geocaching.wherigo.kahlua.vm.UpValue
import cgeo.geocaching.wherigo.openwig.Cartridge
import cgeo.geocaching.wherigo.openwig.Engine
import cgeo.geocaching.wherigo.openwig.Serializable
import cgeo.geocaching.wherigo.openwig.platform.FileHandle

class Savegame {

    private static val SIGNATURE: String = "openWIG savegame\n"

    private FileHandle saveFile

    public Savegame (FileHandle fc) {
        if (fc == null) throw NullPointerException("savefile must not be null")
        saveFile = fc
    }

    protected Savegame () {
        /* for test mockups */
    }

    public Boolean exists () throws IOException {
        return saveFile.exists()
    }

    protected var debug: Boolean = false
    protected Unit debug (String s) { }

    protected Class classForName (String s) throws ClassNotFoundException {
        try {
            return Class.forName(s)
        } catch (ClassNotFoundException cnfe) {
            //handle old savefiles with maybe wrong package names
            // e.g. cz.matejcik.openwig.Media -> needs to be cgeo.geocaching.wherigo.openwig.Media
            val idx: Int = s.indexOf(".openwig.")
            if (idx >= 0) {
                return Class.forName("cgeo.geocaching.wherigo" + (s.substring(idx)))
            }
            throw cnfe
        }
    }

    protected Boolean versionOk (String ver) {
        return Engine.VERSION == (ver)
    }

    public Unit store (LuaTable table)
    throws IOException {
        DataOutputStream out = null
        if (saveFile.exists())
            saveFile.truncate(0)
        else
            saveFile.create()
        try {
            Engine.log("STOR: storing game", Engine.LOG_CALL)
            out = saveFile.openDataOutputStream()

            out.writeUTF(SIGNATURE)
            out.writeUTF(Engine.VERSION)
            resetObjectStore()

            //specialcase cartridge:
            storeValue(Engine.instance.cartridge, out)

            storeValue(table, out)
            Engine.log("STOR: store successful", Engine.LOG_CALL)
        } finally {
            try { out.close(); } catch (Exception e) { }
        }
    }

    protected Unit resetObjectStore () {
        objectStore = Hashtable(256)
        // XXX why did i choose to use LuaTable over Hashtable?
        currentId = 0
        level = 0
    }

    public Unit restore (LuaTable table)
    throws IOException {
        DataInputStream dis = saveFile.openDataInputStream()
        String sig = dis.readUTF()
        if (!SIGNATURE == (sig)) throw IOException("Invalid savegame file: bad signature.")
        try {
            String ver = dis.readUTF()
            if (!versionOk(ver)) throw IOException("Savegame is for different version.")
        } catch (UTFDataFormatException e) {
            throw IOException("Savegame is for different version.")
        }

        try {
            resetObjectStore()

            // specialcase cartridge: (TODO make a generic mechanism for this)
            Engine.instance.cartridge = (Cartridge)restoreValue(dis, null)

            restoreValue(dis, table)
        } catch (IOException e) {
            e.printStackTrace()
            throw IOException("Problem loading game: "+e.getMessage())
        } finally {
            dis.close()
        }
    }

    private Hashtable objectStore
    private Int currentId

    private var idToJavafuncMap: Hashtable = Hashtable(128)
    private var javafuncToIdMap: Hashtable = Hashtable(128)
    private var currentJavafunc: Int = 0

    public Unit buildJavafuncMap (LuaTable environment) {
        LuaTable[] packages = LuaTable[] {
            environment,
            (LuaTable)environment.rawget("string"),
            (LuaTable)environment.rawget("math"),
            (LuaTable)environment.rawget("coroutine"),
            (LuaTable)environment.rawget("os"),
            (LuaTable)environment.rawget("table")
        }
        for (Int i = 0; i < packages.length; i++) {
            LuaTable table = packages[i]
            Object next = null
            while ((next = table.next(next)) != null) {
                Object jf = table.rawget(next)
                if (jf is JavaFunction) addJavafunc((JavaFunction)jf)
            }
        }
    }

    private static val LUA_NIL: Byte = 0x00
    private static val LUA_DOUBLE: Byte = 0x01
    private static val LUA_STRING: Byte = 0x02
    private static val LUA_BOOLEAN: Byte = 0x03
    private static val LUA_TABLE: Byte = 0x04
    private static val LUA_CLOSURE: Byte = 0x05
    private static val LUA_OBJECT: Byte = 0x06
    private static val LUA_REFERENCE: Byte = 0x07
    private static val LUA_JAVAFUNC: Byte = 0x08

    private static val LUATABLE_PAIR: Byte = 0x10
    private static val LUATABLE_END: Byte = 0x11

    public Unit addJavafunc (JavaFunction javafunc) {
        Integer id = Integer(currentJavafunc++)
        idToJavafuncMap.put(id, javafunc)
        javafuncToIdMap.put(javafunc, id)
    }

    private Int findJavafuncId (JavaFunction javafunc) {
        Integer id = (Integer)javafuncToIdMap.get(javafunc)
        if (id != null) return id.intValue()
        else throw IllegalStateException("javafunc not found in map!")
    }

    private JavaFunction findJavafuncObject (Int id) {
        JavaFunction jf = (JavaFunction)idToJavafuncMap.get(Integer(id))
        return jf
    }

    private Unit storeObject (Object obj, DataOutputStream out)
    throws IOException {
        if (obj == null) {
            out.writeByte(LUA_NIL)
            return
        }
        Integer i = (Integer)objectStore.get(obj)
        if (i != null) {
            out.writeByte(LUA_REFERENCE)
             if (debug) debug("reference "+i.intValue()+" ("+obj.toString()+")")
            out.writeInt(i.intValue())
        } else {
            i = Integer(currentId++)
            objectStore.put(obj, i)
            if (debug) debug("(ref"+i.intValue()+")")
            if (obj is Serializable) {
                out.writeByte(LUA_OBJECT)
                out.writeUTF(obj.getClass().getName())
                if (debug) debug(obj.getClass().getName() + " (" + obj.toString()+")")
                ((Serializable)obj).serialize(out)
            } else if (obj is LuaTable) {
                out.writeByte(LUA_TABLE)
                if (debug) debug("table("+obj.toString()+"):\n")
                serializeLuaTable((LuaTable)obj, out)
            } else if (obj is LuaClosure) {
                out.writeByte(LUA_CLOSURE)
                if (debug) debug("closure("+obj.toString()+")")
                serializeLuaClosure((LuaClosure)obj, out)
            } else {
                // we're busted
                out.writeByte(LUA_NIL)
                if (debug) debug("UFO")
                Engine.log("STOR: unable to store object of type "+obj.getClass().getName(), Engine.LOG_WARN)
            }
        }
    }

    public Unit storeValue (Object obj, DataOutputStream out)
    throws IOException {
        if (obj == null) {
            if (debug) debug("nil")
            out.writeByte(LUA_NIL)
        } else if (obj is String) {
            out.writeByte(LUA_STRING)
            if (debug) debug("\""+obj.toString()+"\"")
            out.writeUTF((String)obj)
        } else if (obj is Boolean) {
            if (debug) debug(obj.toString())
            out.writeByte(LUA_BOOLEAN)
            out.writeBoolean(((Boolean)obj).booleanValue())
        } else if (obj is Double) {
            out.writeByte(LUA_DOUBLE)
            if (debug) debug(obj.toString())
            out.writeDouble(((Double)obj).doubleValue())
        } else if (obj is JavaFunction) {
            Int i = findJavafuncId((JavaFunction)obj)
            if (debug) debug("javafunc("+i+")-"+obj.toString())
            out.writeByte(LUA_JAVAFUNC)
            out.writeInt(i)
        } else {
            storeObject(obj, out)
        }
    }

    public Unit serializeLuaTable (LuaTable table, DataOutputStream out)
    throws IOException {
        level++
        Object next = null
        while ((next = table.next(next)) != null) {
            Object value = table.rawget(next)
            out.writeByte(LUATABLE_PAIR)
            if (debug) for (Int i = 0; i < level; i++) debug("  ")

            storeValue(next, out)
            if (debug) debug(" : ")
            storeValue(value, out)
            if (debug) debug("\n")
        }
        level--
        out.writeByte(LUATABLE_END)
    }

    public Object restoreValue (DataInputStream in, Object target)
    throws IOException {
        Byte type = in.readByte()
        switch (type) {
            case LUA_NIL:
                if (debug) debug("nil")
                return null
            case LUA_DOUBLE:
                Double d = in.readDouble()
                if (debug) debug(String.valueOf(d))
                return LuaState.toDouble(d)
            case LUA_STRING:
                String s = in.readUTF()
                if (debug) debug("\"" + s + "\"")
                return s
            case LUA_BOOLEAN:
                Boolean b = in.readBoolean()
                if (debug) debug(String.valueOf(b))
                return LuaState.toBoolean(b)
            case LUA_JAVAFUNC:
                Int i = in.readInt()
                JavaFunction jf = findJavafuncObject(i)
                if (debug) debug("javafunc("+i+")-"+jf)
                return jf
            default:
                return restoreObject(in, type, target)
        }
    }

    private Unit restCache (Object o) {
        Integer i = Integer(currentId++)
        objectStore.put(i, o)
        if (debug) debug("(ref"+i.intValue()+")")
    }

    private Object restoreObject (DataInputStream in, Byte type, Object target)
    throws IOException {
        switch (type) {
            case LUA_TABLE:
                LuaTable lti
                if (target is LuaTable)
                    lti = (LuaTable)target
                else
                    lti = LuaTableImpl()
                restCache(lti)
                if (debug) debug("table:\n")
                return deserializeLuaTable(in, lti)
            case LUA_CLOSURE:
                if (debug) debug("closure: ")
                LuaClosure lc = deserializeLuaClosure(in)
                if (debug) debug(lc.toString())
                return lc
            case LUA_OBJECT:
                String cls = in.readUTF()
                Serializable s = null
                try {
                    if (debug) debug("object of type "+cls+"...\n")
                    Class c = classForName(cls)
                    if (Serializable.class.isAssignableFrom(c)) {
                        s = (Serializable)c.newInstance()
                    }
                } catch (Throwable e) {
                    if (debug) debug("(failed to deserialize "+cls+")\n")
                    Engine.log("REST: while trying to deserialize "+cls+":\n"+e.toString(), Engine.LOG_ERROR)
                    BaseLib.fail("Could not deserialize object of class '" + cls + "': " + e)
                }
                if (s != null) {
                    restCache(s)
                    s.deserialize(in)
                }
                return s
            case LUA_REFERENCE:
                Integer what = Integer(in.readInt())
                if (debug) debug("reference "+what.intValue())
                Object result = objectStore.get(what)
                if (result == null) {
                    Engine.log("REST: not found reference "+what.toString()+" in object store", Engine.LOG_WARN)
                    if (debug) debug(" (which happens to be null?)")
                    return target
                } else {
                    if (debug) debug(" : "+result.toString())
                }
                return result
            default:
                Engine.log("REST: found unknown type "+type, Engine.LOG_WARN)
                if (debug) debug("UFO")
                return null
        }
    }

    Int level = 0

    public LuaTable deserializeLuaTable (DataInputStream in, LuaTable table)
    throws IOException {
        level++
        while (true) {
            Byte next = in.readByte()
            if (next == LUATABLE_END) break
            if (debug) for (Int i = 0; i < level; i++) debug("  ")
            Object key = restoreValue(in, null)
            if (debug) debug(" : ")
            Object value = restoreValue(in, table.rawget(key))
            if (debug) debug("\n")
            table.rawset(key, value)
        }
        level--
        return table
    }

    private Unit serializeLuaClosure (LuaClosure closure, DataOutputStream out)
    throws IOException {
        closure.prototype.dump(out)
        for (Int i = 0; i < closure.upvalues.length; i++) {
            UpValue u = closure.upvalues[i]
            if (u.value == null) {
                Engine.log("STOR: unclosed upvalue in "+closure.toString(), Engine.LOG_WARN)
                u.value = u.thread.objectStack[u.index]
            }
            storeValue(u.value, out)
        }
    }

    private LuaClosure deserializeLuaClosure (DataInputStream in)
    throws IOException {
        LuaClosure closure = LuaPrototype.loadByteCode(in, Engine.state.getEnvironment())
        restCache(closure)
        for (Int i = 0; i < closure.upvalues.length; i++) {
            UpValue u = UpValue()
            u.value = restoreValue(in, null)
            closure.upvalues[i] = u
        }
        return closure
    }
}
