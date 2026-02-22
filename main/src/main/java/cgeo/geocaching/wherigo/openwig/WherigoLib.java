/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;
import cgeo.geocaching.wherigo.openwig.platform.UI;

import java.util.HashMap;
import java.util.Map;

public enum WherigoLib implements JavaFunction {

    // Declaration order defines ordinal() = Lua protocol value (0-23).
    COMMAND("Command", WherigoLib.class),          // ordinal 0  - Wherigo.Command
    ZONEPOINT("ZonePoint", ZonePoint.class),       // ordinal 1
    DISTANCE("Distance", Double.class),            // ordinal 2
    CARTRIDGE("ZCartridge", Cartridge.class),      // ordinal 3
    MESSAGEBOX("MessageBox", WherigoLib.class),    // ordinal 4
    ZONE("Zone", Zone.class),                      // ordinal 5
    DIALOG("Dialog", WherigoLib.class),            // ordinal 6
    ZCHARACTER("ZCharacter", Thing.class),         // ordinal 7
    ZITEM("ZItem", Thing.class),                   // ordinal 8
    ZCOMMAND("ZCommand", Action.class),            // ordinal 9
    ZMEDIA("ZMedia", Media.class),                 // ordinal 10
    ZINPUT("ZInput", EventTable.class),            // ordinal 11
    ZTIMER("ZTimer", Timer.class),                 // ordinal 12
    ZTASK("ZTask", Task.class),                    // ordinal 13
    AUDIO("PlayAudio", WherigoLib.class),          // ordinal 14
    GETINPUT("GetInput", WherigoLib.class),        // ordinal 15
    NOCASEEQUALS("NoCaseEquals", WherigoLib.class),// ordinal 16
    SHOWSCREEN("ShowScreen", WherigoLib.class),    // ordinal 17
    TRANSLATEPOINT("TranslatePoint", WherigoLib.class), // ordinal 18
    SHOWSTATUSTEXT("ShowStatusText", WherigoLib.class), // ordinal 19
    VECTORTOPOINT("VectorToPoint", WherigoLib.class),   // ordinal 20
    LOGMESSAGE("LogMessage", WherigoLib.class),    // ordinal 21
    MADE("made", WherigoLib.class),                // ordinal 22
    GETVALUE("GetValue", WherigoLib.class);        // ordinal 23

    public static final Map<String, Object> env = new HashMap<>(); /* Wherigo's Env table */
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
        env.put("Version", "2.11-compatible(r" + Engine.VERSION + ")");
        env.put("Downloaded", 0d);
    }

    private final String luaName;
    private final Class<?> klass;

    WherigoLib(final String luaName, final Class<?> klass) {
        this.luaName = luaName;
        this.klass = klass;
    }

    public static void register(final LuaState state) {

        if (env.get(DEVICE_ID) == null) throw new IllegalStateException("set your DeviceID! WherigoLib.env.put(WherigoLib.DEVICE_ID, \"some value\")");

        final LuaTable environment = state.getEnvironment();

        final LuaTable wig = new LuaTableImpl();
        environment.rawset("Wherigo", wig);
        for (final WherigoLib f : values()) {
            Engine.instance.savegame.addJavafunc(f);
            wig.rawset(f.luaName, f);
        }

        final LuaTable distanceMetatable = new LuaTableImpl();
        distanceMetatable.rawset("__index", distanceMetatable);
        distanceMetatable.rawset("__call", GETVALUE);
        distanceMetatable.rawset(GETVALUE.luaName, GETVALUE);
        state.setClassMetatable(Double.class, distanceMetatable);

        state.setClassMetatable(WherigoLib.class, wig);
        wig.rawset("__index", wig);

        wig.rawset("Player", Engine.instance.player);
        wig.rawset("INVALID_ZONEPOINT", null);

        // screen constants
        wig.rawset("MAINSCREEN", (double) UI.Screen.MAINSCREEN.ordinal());
        wig.rawset("DETAILSCREEN", (double) UI.Screen.DETAILSCREEN.ordinal());
        wig.rawset("ITEMSCREEN", (double) UI.Screen.ITEMSCREEN.ordinal());
        wig.rawset("INVENTORYSCREEN", (double) UI.Screen.INVENTORYSCREEN.ordinal());
        wig.rawset("LOCATIONSCREEN", (double) UI.Screen.LOCATIONSCREEN.ordinal());
        wig.rawset("TASKSCREEN", (double) UI.Screen.TASKSCREEN.ordinal());

        final LuaTable pack = (LuaTable) environment.rawget("package");
        final LuaTable loaded = (LuaTable) pack.rawget("loaded");
        loaded.rawset("Wherigo", wig);

        final LuaTable envtable = new LuaTableImpl(); /* Wherigo's Env table */
        for (final Map.Entry<String, Object> entry : env.entrySet()) {
            envtable.rawset(entry.getKey(), entry.getValue());
        }
        envtable.rawset("Device", Engine.instance.gwcfile.device);
        environment.rawset("Env", envtable);

        Cartridge.register();
        Container.register();
        Player.register();
        Timer.register();

        Media.reset();
    }

    @Override
    public String toString() {
        return luaName;
    }

    @Override
    public int call(final LuaCallFrame callFrame, final int nArguments) {
        switch (this) {
            case MADE: return made(callFrame, nArguments);

            // special constructors:
            case ZONEPOINT: return zonePoint(callFrame, nArguments);
            case DISTANCE: return distance(callFrame, nArguments);

            // generic constructors:
            case ZITEM: return construct(new Thing(false), callFrame, nArguments);
            case ZCHARACTER: return construct(new Thing(true), callFrame, nArguments);
            case CARTRIDGE: return construct(Engine.instance.cartridge = new Cartridge(), callFrame, nArguments);
            case ZONE: return construct(new Zone(), callFrame, nArguments);
            case ZCOMMAND: return construct(new Action(), callFrame, nArguments);
            case ZMEDIA: return construct(new Media(), callFrame, nArguments);
            case ZINPUT: return construct(new EventTable(), callFrame, nArguments);
            case ZTIMER: return construct(new Timer(), callFrame, nArguments);
            case ZTASK: return construct(new Task(), callFrame, nArguments);

            // functions:
            case MESSAGEBOX: return messageBox(callFrame, nArguments);
            case DIALOG: return dialog(callFrame, nArguments);
            case NOCASEEQUALS: return nocaseequals(callFrame, nArguments);
            case GETINPUT: return getinput(callFrame, nArguments);
            case SHOWSCREEN: return showscreen(callFrame, nArguments);
            case TRANSLATEPOINT: return translatePoint(callFrame, nArguments);
            case AUDIO: return playAudio(callFrame, nArguments);
            case VECTORTOPOINT: return vectorToPoint(callFrame, nArguments);
            case COMMAND: return command(callFrame, nArguments);
            case SHOWSTATUSTEXT: return showStatusText(callFrame, nArguments);
            case LOGMESSAGE: return logMessage(callFrame, nArguments);
            case GETVALUE: return distanceGetValue(callFrame, nArguments);
            default: return 0;
        }
    }

    private int made(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for object:made");
        try {
            final WherigoLib maker = (WherigoLib) callFrame.get(0);
            final Object makee = callFrame.get(1);
            return callFrame.push(LuaState.toBoolean(maker.klass == makee.getClass()));
        } catch (ClassCastException e) { throw new IllegalStateException("bad arguments to object:made"); }
    }

    private int construct(final EventTable what, final LuaCallFrame callFrame, final int nArguments) {
        final Object param = callFrame.get(0);
        Cartridge c = null;
        if (param instanceof Cartridge) {
            c = (Cartridge) param;
        } else if (param instanceof LuaTable) {
            final LuaTable lt = (LuaTable) param;
            c = (Cartridge) lt.rawget("Cartridge");
            what.setTable((LuaTable) param);
            if (what instanceof Container) {
                final Container cont = (Container) what;
                final Container target = (Container) lt.rawget("Container");
                if (target != null) {
                    cont.moveTo(target);
                }
            }
        }
        if (c == null) c = Engine.instance.cartridge;
        c.addObject(what);
        return callFrame.push(what);
    }

    private int zonePoint(final LuaCallFrame callFrame, final int nArguments) {
        if (nArguments == 0) {
            callFrame.push(new ZonePoint());
        } else {
            BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for ZonePoint");
            double a = LuaState.fromDouble(callFrame.get(0));
            double b = LuaState.fromDouble(callFrame.get(1));
            double c = 0;
            if (nArguments > 2) c = LuaState.fromDouble(callFrame.get(2));
            callFrame.push(new ZonePoint(a, b, c));
        }
        return 1;
    }

    /** Fake Distance constructor
     *
     * Called from Lua code: d = Wherigo.Distance(number, unit),
     * converts 'number' from specified unit to metres and returns
     * that as a double.
     */
    private int distance(final LuaCallFrame callFrame, final int nArguments) {
        double a = LuaState.fromDouble(callFrame.get(0));
        String b = (String) callFrame.get(1);
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
    private int distanceGetValue(final LuaCallFrame callFrame, final int nArguments) {
        double a = LuaState.fromDouble(callFrame.get(0));
        String b = (String) callFrame.get(1);
        double dist = ZonePoint.convertDistanceTo(a, b);
        callFrame.push(LuaState.toDouble(dist));
        return 1;
    }

    private int messageBox(final LuaCallFrame callFrame, final int nArguments) {
        final LuaTable lt = (LuaTable) callFrame.get(0);
        Engine.message(lt);
        return 0;
    }

    private int dialog(final LuaCallFrame callFrame, final int nArguments) {
        final LuaTable lt = (LuaTable) callFrame.get(0);
        final int n = lt.len();
        final String[] texts = new String[n];
        final Media[] media = new Media[n];
        for (int i = 1; i <= n; i++) {
            final LuaTable item = (LuaTable) lt.rawget((double) i);
            texts[i - 1] = Engine.removeHtml((String) item.rawget("Text"));
            media[i - 1] = (Media) item.rawget("Media");
        }
        Engine.dialog(texts, media);
        return 0;
    }

    private int nocaseequals(final LuaCallFrame callFrame, final int nArguments) {
        final Object a = callFrame.get(0);
        final Object b = callFrame.get(1);
        final String aa = a == null ? null : a.toString();
        final String bb = b == null ? null : b.toString();
        final boolean result = (aa == bb || (aa != null && aa.equalsIgnoreCase(bb)));
        callFrame.push(LuaState.toBoolean(result));
        return 1;
    }

    private int getinput(final LuaCallFrame callFrame, final int nArguments) {
        final EventTable lt = (EventTable) callFrame.get(0);
        Engine.input(lt);
        return 1;
    }

    private int showscreen(final LuaCallFrame callFrame, final int nArguments) {
        final int screenId = (int) LuaState.fromDouble(callFrame.get(0));
        final UI.Screen screen = UI.Screen.fromId(screenId);
        EventTable et = null;
        if (nArguments > 1) {
            final Object o = callFrame.get(1);
            if (o instanceof EventTable) et = (EventTable) o;
        }
        Engine.log("CALL: ShowScreen(" + screenId + ") " + (et == null ? "" : et.name), Engine.LOG_CALL);
        Engine.ui.showScreen(screen, et);
        return 0;
    }

    private int translatePoint(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 3, "insufficient arguments for TranslatePoint");
        final ZonePoint z = (ZonePoint) callFrame.get(0);
        double dist = LuaState.fromDouble(callFrame.get(1));
        double angle = LuaState.fromDouble(callFrame.get(2));
        callFrame.push(z.translate(angle, dist));
        return 1;
    }

    private int vectorToPoint(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 2, "insufficient arguments for VectorToPoint");
        final ZonePoint a = (ZonePoint) callFrame.get(0);
        final ZonePoint b = (ZonePoint) callFrame.get(1);
        double bearing = ZonePoint.angle2azimuth(b.bearing(a));
        double distance = b.distance(a);
        callFrame.push(LuaState.toDouble(distance));
        callFrame.push(LuaState.toDouble(bearing));
        return 2;
    }

    private int playAudio(final LuaCallFrame callFrame, final int nArguments) {
        final Media m = (Media) callFrame.get(0);
        m.play();
        return 0;
    }

    private int showStatusText(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for ShowStatusText");
        String text = (String) callFrame.get(0);
        if (text != null && text.length() == 0) text = null;
        Engine.ui.setStatusText(text);
        return 0;
    }

    private int logMessage(final LuaCallFrame callFrame, final int nArguments) {
        if (nArguments < 1) return 0;
        final Object arg = callFrame.get(0);
        String text;
        if (arg instanceof LuaTable) {
            final LuaTable lt = (LuaTable) arg;
            text = (String) lt.rawget("Text");
        } else {
            text = arg.toString();
        }
        if (text != null && text.length() == 0) return 0;
        Engine.log("CUST: " + text, Engine.LOG_CALL);
        return 0;
    }

    private int command(final LuaCallFrame callFrame, final int nArguments) {
        BaseLib.luaAssert(nArguments >= 1, "insufficient arguments for Command");
        String cmd = (String) callFrame.get(0);
        if (cmd != null && cmd.length() == 0) {
            cmd = null;
        }
        Engine.ui.command(cmd);
        return 0;
    }
}

