/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.openwig.platform.UI;
import java.util.Enumeration;
import java.util.Hashtable;
import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;

/**
 * Wherigo API function library and environment configuration.
 * <p>
 * WherigoLib is an enum that implements all the Wherigo-specific functions
 * that are exposed to Lua scripts. It also maintains the Wherigo environment
 * table (env) which contains platform information, device ID, and other
 * configuration that Lua scripts can query.
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Registers Wherigo functions in the Lua environment (Zone, ZItem, etc.)</li>
 * <li>Maintains the env table with device and platform information</li>
 * <li>Maps Lua constructors to Java classes</li>
 * <li>Provides factory functions for creating game objects from Lua</li>
 * </ul>
 * <p>
 * <strong>Important:</strong> You must set a device ID before using OpenWIG:
 * <pre>
 * WherigoLib.env.put(WherigoLib.DEVICE_ID, "your-device-id");
 * </pre>
 */
public enum WherigoLib implements JavaFunction {
    COMMAND("Command"),
    ZONEPOINT("ZonePoint"),
    DISTANCE("Distance"),
    CARTRIDGE("ZCartridge"),
    MESSAGEBOX("MessageBox"),
    ZONE("Zone"),
    DIALOG("Dialog"),
    ZCHARACTER("ZCharacter"),
    ZITEM("ZItem"),
    ZCOMMAND("ZCommand"),
    ZMEDIA("ZMedia"),
    ZINPUT("ZInput"),
    ZTIMER("ZTimer"),
    ZTASK("ZTask"),
    AUDIO("PlayAudio"),
    GETINPUT("GetInput"),
    NOCASEEQUALS("NoCaseEquals"),
    SHOWSCREEN("ShowScreen"),
    TRANSLATEPOINT("TranslatePoint"),
    SHOWSTATUSTEXT("ShowStatusText"),
    VECTORTOPOINT("VectorToPoint"),
    LOGMESSAGE("LogMessage"),
    MADE("made"),
    GETVALUE("GetValue");

    private final String name;

    WherigoLib(String name) {
        this.name = name;
    }

    public static final Hashtable<String, Object> env = new Hashtable<>(); /* Wherigo's Env table */
    public static final String DEVICE_ID = "DeviceID";
    public static final String PLATFORM = "Platform";
    static {
        env.put("Device", "undefined");
        env.put("DeviceID", "undefined");
        env.put("Platform", "MIDP-2.0/CLDC-1.1");
        env.put("CartFolder", "c:/what/is/it/to/you");
        env.put("SyncFolder", "c:/what/is/it/to/you");
        env.put("LogFolder", "c:/what/is/it/to/you");
        env.put("CartFilename", "cartridge.gwc");
        env.put("PathSep", "/"); // no. you may NOT do file i/o on this device.
        env.put("Version", "2.11-compatible(r"+Engine.VERSION+")");
        env.put("Downloaded", Double.valueOf(0));
    }

    private final Class<?> klass = assignClass();

    private Class<?> assignClass () {
        // because i'm too lazy to type out the break;s in a switch
        return switch (this) {
            case DISTANCE -> Double.class;
            case ZONEPOINT -> ZonePoint.class;
            case ZONE -> Zone.class;
            case ZCHARACTER, ZITEM -> Thing.class;
            case ZCOMMAND -> Action.class;
            case ZMEDIA -> Media.class;
            case ZINPUT -> EventTable.class;
            case ZTIMER -> Timer.class;
            case ZTASK -> Task.class;
            case CARTRIDGE -> Cartridge.class;
            default -> getClass();
        };
    }

    public static void register(Engine engine) {

        if (env.get(DEVICE_ID) == null) throw new IllegalStateException("set your DeviceID! WherigoLib.env.put(WherigoLib.DEVICE_ID, \"some value\")");

        LuaTable environment = engine.luaState.getEnvironment();

        LuaTable wig = new LuaTableImpl();
        environment.rawset("Wherigo", wig);
        for (WherigoLib function : WherigoLib.values()) {
            engine.savegame.addJavafunc(function);
            wig.rawset(function.name, function);
        }

        LuaTable distanceMetatable = new LuaTableImpl();
        distanceMetatable.rawset("__index", distanceMetatable);
        distanceMetatable.rawset("__call", GETVALUE);
        distanceMetatable.rawset(GETVALUE.name, GETVALUE);
        engine.luaState.setClassMetatable(Double.class, distanceMetatable);

        engine.luaState.setClassMetatable(WherigoLib.class, wig);
        wig.rawset("__index", wig);

        wig.rawset("Player", engine.player);
        wig.rawset("INVALID_ZONEPOINT", null);

        // screen constants
        wig.rawset("MAINSCREEN", new Double(UI.MAINSCREEN));
        wig.rawset("DETAILSCREEN", new Double(UI.DETAILSCREEN));
        wig.rawset("ITEMSCREEN", new Double(UI.ITEMSCREEN));
        wig.rawset("INVENTORYSCREEN", new Double(UI.INVENTORYSCREEN));
        wig.rawset("LOCATIONSCREEN", new Double(UI.LOCATIONSCREEN));
        wig.rawset("TASKSCREEN", new Double(UI.TASKSCREEN));

        LuaTable pack = (LuaTable)environment.rawget("package");
        LuaTable loaded = (LuaTable)pack.rawget("loaded");
        loaded.rawset("Wherigo", wig);

        LuaTable envtable = new LuaTableImpl(); /* Wherigo's Env table */
        Enumeration<String> e = env.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            envtable.rawset(key, env.get(key));
        }
        envtable.rawset("Device", engine.gwcfile.device);
        environment.rawset("Env", envtable);

        Cartridge.register();
        Container.register();
        Player.register();
        Timer.register();

        Media.reset();
    }
    
    /** Deprecated method for backward compatibility */
    @Deprecated
    public static void register(LuaState state) {
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            register(currentEngine);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int call(LuaCallFrame callFrame, int nArguments) {
        return switch (this) {
            case MADE -> made(callFrame, nArguments);

            // special constructors:
            case ZONEPOINT -> zonePoint(callFrame, nArguments);
            case DISTANCE -> distance(callFrame, nArguments);

            // generic constructors:
            case ZITEM -> construct(new Thing(false), callFrame, nArguments);
            case ZCHARACTER -> construct(new Thing(true), callFrame, nArguments);
            case CARTRIDGE -> {
                Engine currentEngine = Engine.getCurrentInstance();
                if (currentEngine != null) {
                    currentEngine.cartridge = new Cartridge(currentEngine);
                    yield construct(currentEngine.cartridge, callFrame, nArguments);
                }
                yield 0;
            }
            case ZONE, ZCOMMAND, ZMEDIA, ZINPUT, ZTIMER, ZTASK -> construct(callFrame, nArguments);

            // functions:
            case MESSAGEBOX -> messageBox(callFrame, nArguments);
            case DIALOG -> dialog(callFrame, nArguments);
            case NOCASEEQUALS -> nocaseequals(callFrame, nArguments);
            case GETINPUT -> getinput(callFrame, nArguments);
            case SHOWSCREEN -> showscreen(callFrame, nArguments);
            case TRANSLATEPOINT -> translatePoint(callFrame, nArguments);
            case AUDIO -> playAudio(callFrame, nArguments);
            case VECTORTOPOINT -> vectorToPoint(callFrame, nArguments);
            case COMMAND -> command(callFrame, nArguments);
            case SHOWSTATUSTEXT -> showStatusText(callFrame, nArguments);
            case LOGMESSAGE -> logMessage(callFrame, nArguments);
            case GETVALUE -> distanceGetValue(callFrame, nArguments);
            default -> 0;
        };
    }

    private int construct(LuaCallFrame callFrame, int nArguments) {
        try {
            return construct((EventTable) klass.newInstance(), callFrame, nArguments);
        } catch (InstantiationException e) {
            /* will not happen */
            return 0;
        } catch (IllegalAccessException e) {
            /* will not happen either */
            return 0;
        }
    }

    private int made (LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for object:made");
        try {
            WherigoLib maker = (WherigoLib)callFrame.get(0);
            Object makee = callFrame.get(1);
            return callFrame.push(LuaState.toBoolean(maker.klass == makee.getClass()));
        } catch (ClassCastException e) { throw new IllegalStateException("bad arguments to object:made"); }
    }

    private int construct(EventTable what, LuaCallFrame callFrame, int nArguments) {
        Object param = callFrame.get(0);
        Cartridge c = null;
        if (param instanceof Cartridge cart) {
            c = cart;
        } else if (param instanceof LuaTable lt) {
            c = (Cartridge)lt.rawget("Cartridge");
            what.setTable(lt);
            if (what instanceof Container cont) {
                Container target = (Container)lt.rawget("Container");
                if (target != null)
                    cont.moveTo(target);
            }
        }
        if (c == null) {
            Engine currentEngine = Engine.getCurrentInstance();
            if (currentEngine != null) {
                c = currentEngine.cartridge;
            }
        }
        if (c != null) {
            c.addObject(what);
        }
        return callFrame.push(what);
    }

    private int zonePoint (LuaCallFrame callFrame, int nArguments) {
        if (nArguments == 0) {
            callFrame.push(new ZonePoint());
        } else {
            BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for ZonePoint");
            double a = LuaState.fromDouble(callFrame.get(0));
            double b = LuaState.fromDouble(callFrame.get(1));
            double c = 0;
            if (nArguments > 2) c = LuaState.fromDouble(callFrame.get(2));
            callFrame.push(new ZonePoint(a,b,c));
        }
        return 1;
    }

    /** Fake Distance constructor
     *
     * Called from Lua code: d = Wherigo.Distance(number, unit),
     * converts 'number' from specified unit to metres and returns
     * that as a double.
     */
    private int distance (LuaCallFrame callFrame, int nArguments) {
        double a = LuaState.fromDouble(callFrame.get(0));
        String b = (String)callFrame.get(1);
        double dist = ZonePoint.convertDistanceFrom(a, b);
        callFrame.push(LuaState.toDouble(dist));
        return 1;
    }

    /** Distance object's fake GetValue or __call method
     *
     * Called from Lua code: dist:GetValue("metres") or dist("ft"),
     * where 'dist' is double, converts the number to specified units
     * and returns as double.
     */
    private int distanceGetValue (LuaCallFrame callFrame, int nArguments) {
        double a = LuaState.fromDouble(callFrame.get(0));
        String b = (String)callFrame.get(1);
        double dist = ZonePoint.convertDistanceTo(a, b);
        callFrame.push(LuaState.toDouble(dist));
        return 1;
    }

    private int messageBox (LuaCallFrame callFrame, int nArguments) {
        LuaTable lt = (LuaTable)callFrame.get(0);
        Engine.message(lt);
        return 0;
    }

    private int dialog (LuaCallFrame callFrame, int nArguments) {
        LuaTable lt = (LuaTable)callFrame.get(0);
        int n = lt.len();
        String[] texts = new String[n];
        Media[] media = new Media[n];
        for (int i = 1; i <= n; i++) {
            LuaTable item = (LuaTable)lt.rawget(new Double(i));
            texts[i-1] = Engine.removeHtml((String)item.rawget("Text"));
            media[i-1] = (Media)item.rawget("Media");
        }
        Engine.dialog(texts, media);
        return 0;
    }

    private int nocaseequals (LuaCallFrame callFrame, int nArguments) {
        Object a = callFrame.get(0); Object b = callFrame.get(1);
        String aa = a == null ? null : a.toString();
        String bb = b == null ? null : b.toString();
        boolean result = (aa == bb || (aa != null && aa.equalsIgnoreCase(bb)));
        callFrame.push(LuaState.toBoolean(result));
        return 1;
    }

    private int getinput (LuaCallFrame callFrame, int nArguments) {
        EventTable lt = (EventTable)callFrame.get(0);
        Engine.input(lt);
        return 1;
    }

    private int showscreen (LuaCallFrame callFrame, int nArguments) {
        int screen = (int)LuaState.fromDouble(callFrame.get(0));
        EventTable et = null;
        if (nArguments > 1) {
            Object o = callFrame.get(1);
            if (o instanceof EventTable e) et = e;
        }
        final Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            Engine.log("CALL: ShowScreen("+screen+") " + (et == null ? "" : et.name), Engine.LOG_CALL);
            currentEngine.uiInstance.showScreen(screen, et);
        }
        return 0;
    }

    private int translatePoint (LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 3, "insufficient arguments for TranslatePoint");
        ZonePoint z = (ZonePoint)callFrame.get(0);
        double dist = LuaState.fromDouble(callFrame.get(1));
        double angle = LuaState.fromDouble(callFrame.get(2));
        callFrame.push(z.translate(angle, dist));
        return 1;
    }

    private int vectorToPoint (LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for VectorToPoint");
        ZonePoint a = (ZonePoint)callFrame.get(0);
        ZonePoint b = (ZonePoint)callFrame.get(1);
        double bearing = ZonePoint.angle2azimuth(b.bearing(a));
        double distance = b.distance(a);
        callFrame.push(LuaState.toDouble(distance));
        callFrame.push(LuaState.toDouble(bearing));
        return 2;
    }

    private int playAudio (LuaCallFrame callFrame, int nArguments) {
        Media m = (Media)callFrame.get(0);
        m.play();
        return 0;
    }

    private int showStatusText (LuaCallFrame callFrame, int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for ShowStatusText");
        String text = (String)callFrame.get(0);
        if (text != null && text.length() == 0) text = null;
        final Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            currentEngine.uiInstance.setStatusText(text);
        }
        return 0;
    }

    private int logMessage (LuaCallFrame callFrame, int nArguments) {
        if (nArguments < 1) return 0;
        Object arg = callFrame.get(0);
        String text;
        if (arg instanceof LuaTable lt) {
            text = (String)lt.rawget("Text");
        } else {
            text = arg.toString();
        }
        if (text != null && text.length() == 0) return 0;
        Engine.log("CUST: " + text, Engine.LOG_CALL);
        return 0;
    }

    private int command(LuaCallFrame callFrame, int nArguments) {
      BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for Command");
      String cmd = (String) callFrame.get(0);
      if (cmd != null && cmd.length() == 0)
        cmd = null;
      final Engine currentEngine = Engine.getCurrentInstance();
      if (currentEngine != null) {
          currentEngine.uiInstance.command(cmd);
      }
      return 0;
    }
}
