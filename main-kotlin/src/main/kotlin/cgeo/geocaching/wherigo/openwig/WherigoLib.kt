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

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl
import cgeo.geocaching.wherigo.openwig.platform.UI
import java.util.Enumeration
import java.util.Hashtable
import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib

class WherigoLib : JavaFunction {

    private static val COMMAND: Int = 0; // Wherigo.Command
    private static val ZONEPOINT: Int = 1
    private static val DISTANCE: Int = 2
    private static val CARTRIDGE: Int = 3
    private static val MESSAGEBOX: Int = 4
    private static val ZONE: Int = 5
    private static val DIALOG: Int = 6
    private static val ZCHARACTER: Int = 7
    private static val ZITEM: Int = 8
    private static val ZCOMMAND: Int = 9
    private static val ZMEDIA: Int = 10
    private static val ZINPUT: Int = 11
    private static val ZTIMER: Int = 12
    private static val ZTASK: Int = 13
    private static val AUDIO: Int = 14
    private static val GETINPUT: Int = 15
    private static val NOCASEEQUALS: Int = 16
    private static val SHOWSCREEN: Int = 17
    private static val TRANSLATEPOINT: Int = 18
    private static val SHOWSTATUSTEXT: Int = 19
    private static val VECTORTOPOINT: Int = 20
    private static val LOGMESSAGE: Int = 21
    private static val MADE: Int = 22
    private static val GETVALUE: Int = 23

    private static val NUM_FUNCTIONS: Int = 24

    private static final String[] names
    static {
        names = String[NUM_FUNCTIONS]
        names[ZONEPOINT] = "ZonePoint"
        names[DISTANCE] = "Distance"
        names[CARTRIDGE] = "ZCartridge"
        names[MESSAGEBOX] = "MessageBox"
        names[ZONE] = "Zone"
        names[DIALOG] = "Dialog"
        names[ZCHARACTER] = "ZCharacter"
        names[ZITEM] = "ZItem"
        names[ZCOMMAND] = "ZCommand"
        names[ZMEDIA] = "ZMedia"
        names[ZINPUT] = "ZInput"
        names[ZTIMER] = "ZTimer"
        names[ZTASK] = "ZTask"
        names[AUDIO] = "PlayAudio"
        names[GETINPUT] = "GetInput"
        names[NOCASEEQUALS] = "NoCaseEquals"
        names[SHOWSCREEN] = "ShowScreen"
        names[TRANSLATEPOINT] = "TranslatePoint"
        names[SHOWSTATUSTEXT] = "ShowStatusText"
        names[VECTORTOPOINT] = "VectorToPoint"
        names[COMMAND] = "Command"
        names[LOGMESSAGE] = "LogMessage"
        names[MADE] = "made"
        names[GETVALUE] = "GetValue"
    }

    public static val env: Hashtable = Hashtable(); /* Wherigo's Env table */
    public static val DEVICE_ID: String = "DeviceID"
    public static val PLATFORM: String = "Platform"
    static {
        env.put("Device", "undefined")
        env.put("DeviceID", "undefined")
        env.put("Platform", "MIDP-2.0/CLDC-1.1")
        env.put("CartFolder", "c:/what/is/it/to/you")
        env.put("SyncFolder", "c:/what/is/it/to/you")
        env.put("LogFolder", "c:/what/is/it/to/you")
        env.put("CartFilename", "cartridge.gwc")
        env.put("PathSep", "/"); // no. you may NOT do file i/o on this device.
        env.put("Version", "2.11-compatible(r"+Engine.VERSION+")")
        env.put("Downloaded", Double(0))
    }

    private Int index
    private Class klass

    private static WherigoLib[] functions
    static {
        functions = WherigoLib[NUM_FUNCTIONS]
        for (Int i = 0; i < NUM_FUNCTIONS; i++) {
            functions[i] = WherigoLib(i)
        }
    }

    private Class assignClass () {
        // because i'm too lazy to type out the break;s in a switch
        switch (index) {
            case DISTANCE:
                return Double.class
            case ZONEPOINT:
                return ZonePoint.class
            case ZONE:
                return Zone.class
            case ZCHARACTER: case ZITEM:
                return Thing.class
            case ZCOMMAND:
                return Action.class
            case ZMEDIA:
                return Media.class
            case ZINPUT:
                return EventTable.class
            case ZTIMER:
                return Timer.class
            case ZTASK:
                return Task.class
            case CARTRIDGE:
                return Cartridge.class
            default:
                return getClass()
        }
    }

    public WherigoLib(Int index) {
        this.index = index
        this.klass = assignClass()
    }

    public static Unit register(LuaState state) {

        if (env.get(DEVICE_ID) == null) throw IllegalStateException("set your DeviceID! WherigoLib.env.put(WherigoLib.DEVICE_ID, \"some value\")")

        LuaTable environment = state.getEnvironment()

        LuaTable wig = LuaTableImpl()
        environment.rawset("Wherigo", wig)
        for (Int i = 0; i < NUM_FUNCTIONS; i++) {
            Engine.instance.savegame.addJavafunc(functions[i])
            wig.rawset(names[i], functions[i])
        }

        LuaTable distanceMetatable = LuaTableImpl()
        distanceMetatable.rawset("__index", distanceMetatable)
        distanceMetatable.rawset("__call", functions[GETVALUE])
        distanceMetatable.rawset(names[GETVALUE], functions[GETVALUE])
        state.setClassMetatable(Double.class, distanceMetatable)

        state.setClassMetatable(WherigoLib.class, wig)
        wig.rawset("__index", wig)

        wig.rawset("Player", Engine.instance.player)
        wig.rawset("INVALID_ZONEPOINT", null)

        // screen constants
        wig.rawset("MAINSCREEN", Double(UI.MAINSCREEN))
        wig.rawset("DETAILSCREEN", Double(UI.DETAILSCREEN))
        wig.rawset("ITEMSCREEN", Double(UI.ITEMSCREEN))
        wig.rawset("INVENTORYSCREEN", Double(UI.INVENTORYSCREEN))
        wig.rawset("LOCATIONSCREEN", Double(UI.LOCATIONSCREEN))
        wig.rawset("TASKSCREEN", Double(UI.TASKSCREEN))

        LuaTable pack = (LuaTable)environment.rawget("package")
        LuaTable loaded = (LuaTable)pack.rawget("loaded")
        loaded.rawset("Wherigo", wig)

        LuaTable envtable = LuaTableImpl(); /* Wherigo's Env table */
        Enumeration e = env.keys()
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement()
            envtable.rawset(key, env.get(key))
        }
        envtable.rawset("Device", Engine.instance.gwcfile.device)
        environment.rawset("Env", envtable)

        Cartridge.register()
        Container.register()
        Player.register()
        Timer.register()

        Media.reset()
    }

    public String toString() {
        return names[index]
    }


    public Int call(LuaCallFrame callFrame, Int nArguments) {
        switch (index) {
            case MADE: return made(callFrame, nArguments)

            // special constructors:
            case ZONEPOINT: return zonePoint(callFrame, nArguments)
            case DISTANCE: return distance(callFrame, nArguments)

            // generic constructors:
            case ZITEM: return construct(Thing(false), callFrame, nArguments)
            case ZCHARACTER: return construct(Thing(true), callFrame, nArguments)
            case CARTRIDGE: return construct(Engine.instance.cartridge = Cartridge(), callFrame, nArguments)
            case ZONE:
            case ZCOMMAND:
            case ZMEDIA:
            case ZINPUT:
            case ZTIMER:
            case ZTASK:
                try {
                    return construct((EventTable)klass.newInstance(), callFrame, nArguments)
                } catch (InstantiationException e) {
                    /* will not happen */
                    return 0
                } catch (IllegalAccessException e) {
                    /* will not happen either */
                    return 0
                }

            // functions:
            case MESSAGEBOX: return messageBox(callFrame, nArguments)
            case DIALOG: return dialog(callFrame, nArguments)
            case NOCASEEQUALS: return nocaseequals(callFrame, nArguments)
            case GETINPUT: return getinput(callFrame, nArguments)
            case SHOWSCREEN: return showscreen(callFrame, nArguments)
            case TRANSLATEPOINT: return translatePoint(callFrame, nArguments)
            case AUDIO: return playAudio(callFrame, nArguments)
            case VECTORTOPOINT: return vectorToPoint(callFrame, nArguments)
            case COMMAND: return command(callFrame, nArguments)
            case SHOWSTATUSTEXT: return showStatusText(callFrame, nArguments)
            case LOGMESSAGE: return logMessage(callFrame, nArguments)
            case GETVALUE: return distanceGetValue(callFrame, nArguments)
            default: return 0
        }
    }

    private Int made (LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for object:made")
        try {
            WherigoLib maker = (WherigoLib)callFrame.get(0)
            Object makee = callFrame.get(1)
            return callFrame.push(LuaState.toBoolean(maker.klass == makee.getClass()))
        } catch (ClassCastException e) { throw IllegalStateException("bad arguments to object:made"); }
    }

    private Int construct(EventTable what, LuaCallFrame callFrame, Int nArguments) {
        Object param = callFrame.get(0)
        Cartridge c = null
        if (param is Cartridge) {
            c = (Cartridge)param
        } else if (param is LuaTable) {
            LuaTable lt = (LuaTable)param
            c = (Cartridge)lt.rawget("Cartridge")
            what.setTable((LuaTable)param)
            if (what is Container) {
                Container cont = (Container)what
                Container target = (Container)lt.rawget("Container")
                if (target != null)
                    cont.moveTo(target)
            }
        }
        if (c == null) c = Engine.instance.cartridge
        c.addObject(what)
        return callFrame.push(what)
    }

    private Int zonePoint (LuaCallFrame callFrame, Int nArguments) {
        if (nArguments == 0) {
            callFrame.push(ZonePoint())
        } else {
            BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for ZonePoint")
            Double a = LuaState.fromDouble(callFrame.get(0))
            Double b = LuaState.fromDouble(callFrame.get(1))
            Double c = 0
            if (nArguments > 2) c = LuaState.fromDouble(callFrame.get(2))
            callFrame.push(ZonePoint(a,b,c))
        }
        return 1
    }

    /** Fake Distance constructor
     *
     * Called from Lua code: d = Wherigo.Distance(number, unit),
     * converts 'number' from specified unit to metres and returns
     * that as a Double.
     */
    private Int distance (LuaCallFrame callFrame, Int nArguments) {
        Double a = LuaState.fromDouble(callFrame.get(0))
        String b = (String)callFrame.get(1)
        Double dist = ZonePoint.convertDistanceFrom(a, b)
        callFrame.push(LuaState.toDouble(dist))
        return 1
    }

    /** Distance object's fake GetValue or __call method
     *
     * Called from Lua code: dist:GetValue("metres") or dist("ft"),
     * where 'dist' is Double, converts the number to specified units
     * and returns as Double.
     */
    private Int distanceGetValue (LuaCallFrame callFrame, Int nArguments) {
        Double a = LuaState.fromDouble(callFrame.get(0))
        String b = (String)callFrame.get(1)
        Double dist = ZonePoint.convertDistanceTo(a, b)
        callFrame.push(LuaState.toDouble(dist))
        return 1
    }

    private Int messageBox (LuaCallFrame callFrame, Int nArguments) {
        LuaTable lt = (LuaTable)callFrame.get(0)
        Engine.message(lt)
        return 0
    }

    private Int dialog (LuaCallFrame callFrame, Int nArguments) {
        LuaTable lt = (LuaTable)callFrame.get(0)
        Int n = lt.len()
        String[] texts = String[n]
        Media[] media = Media[n]
        for (Int i = 1; i <= n; i++) {
            LuaTable item = (LuaTable)lt.rawget(Double(i))
            texts[i-1] = Engine.removeHtml((String)item.rawget("Text"))
            media[i-1] = (Media)item.rawget("Media")
        }
        Engine.dialog(texts, media)
        return 0
    }

    private Int nocaseequals (LuaCallFrame callFrame, Int nArguments) {
        Object a = callFrame.get(0); Object b = callFrame.get(1)
        String aa = a == null ? null : a.toString()
        String bb = b == null ? null : b.toString()
        Boolean result = (aa == bb || (aa != null && aa.equalsIgnoreCase(bb)))
        callFrame.push(LuaState.toBoolean(result))
        return 1
    }

    private Int getinput (LuaCallFrame callFrame, Int nArguments) {
        EventTable lt = (EventTable)callFrame.get(0)
        Engine.input(lt)
        return 1
    }

    private Int showscreen (LuaCallFrame callFrame, Int nArguments) {
        Int screen = (Int)LuaState.fromDouble(callFrame.get(0))
        EventTable et = null
        if (nArguments > 1) {
            Object o = callFrame.get(1)
            if (o is EventTable) et = (EventTable)o
        }
        Engine.log("CALL: ShowScreen("+screen+") " + (et == null ? "" : et.name), Engine.LOG_CALL)
        Engine.ui.showScreen(screen, et)
        return 0
    }

    private Int translatePoint (LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 3, "insufficient arguments for TranslatePoint")
        ZonePoint z = (ZonePoint)callFrame.get(0)
        Double dist = LuaState.fromDouble(callFrame.get(1))
        Double angle = LuaState.fromDouble(callFrame.get(2))
        callFrame.push(z.translate(angle, dist))
        return 1
    }

    private Int vectorToPoint (LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for VectorToPoint")
        ZonePoint a = (ZonePoint)callFrame.get(0)
        ZonePoint b = (ZonePoint)callFrame.get(1)
        Double bearing = ZonePoint.angle2azimuth(b.bearing(a))
        Double distance = b.distance(a)
        callFrame.push(LuaState.toDouble(distance))
        callFrame.push(LuaState.toDouble(bearing))
        return 2
    }

    private Int playAudio (LuaCallFrame callFrame, Int nArguments) {
        Media m = (Media)callFrame.get(0)
        m.play()
        return 0
    }

    private Int showStatusText (LuaCallFrame callFrame, Int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for ShowStatusText")
        String text = (String)callFrame.get(0)
        if (text != null && text.length() == 0) text = null
        Engine.ui.setStatusText(text)
        return 0
    }

    private Int logMessage (LuaCallFrame callFrame, Int nArguments) {
        if (nArguments < 1) return 0
        Object arg = callFrame.get(0)
        String text
        if (arg is LuaTable) {
            LuaTable lt = (LuaTable)arg
            text = (String)lt.rawget("Text")
        } else {
            text = arg.toString()
        }
        if (text != null && text.length() == 0) return 0
        Engine.log("CUST: " + text, Engine.LOG_CALL)
        return 0
    }

    private Int command(LuaCallFrame callFrame, Int nArguments) {
      BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for Command")
      String cmd = (String) callFrame.get(0)
      if (cmd != null && cmd.length() == 0)
        cmd = null
      Engine.ui.command(cmd)
      return 0
    }
}
