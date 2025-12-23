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

public class WherigoLib implements JavaFunction {

    private static final int COMMAND = 0; // Wherigo.Command
    private static final int ZONEPOINT = 1;
    private static final int DISTANCE = 2;
    private static final int CARTRIDGE = 3;
    private static final int MESSAGEBOX = 4;
    private static final int ZONE = 5;
    private static final int DIALOG = 6;
    private static final int ZCHARACTER = 7;
    private static final int ZITEM = 8;
    private static final int ZCOMMAND = 9;
    private static final int ZMEDIA = 10;
    private static final int ZINPUT = 11;
    private static final int ZTIMER = 12;
    private static final int ZTASK = 13;
    private static final int AUDIO = 14;
    private static final int GETINPUT = 15;
    private static final int NOCASEEQUALS = 16;
    private static final int SHOWSCREEN = 17;
    private static final int TRANSLATEPOINT = 18;
    private static final int SHOWSTATUSTEXT = 19;
    private static final int VECTORTOPOINT = 20;
    private static final int LOGMESSAGE = 21;
    private static final int MADE = 22;
    private static final int GETVALUE = 23;

    private static final int NUM_FUNCTIONS = 24;

    private static final String[] names;
    static {
        names = new String[NUM_FUNCTIONS];
        names[ZONEPOINT] = "ZonePoint";
        names[DISTANCE] = "Distance";
        names[CARTRIDGE] = "ZCartridge";
        names[MESSAGEBOX] = "MessageBox";
        names[ZONE] = "Zone";
        names[DIALOG] = "Dialog";
        names[ZCHARACTER] = "ZCharacter";
        names[ZITEM] = "ZItem";
        names[ZCOMMAND] = "ZCommand";
        names[ZMEDIA] = "ZMedia";
        names[ZINPUT] = "ZInput";
        names[ZTIMER] = "ZTimer";
        names[ZTASK] = "ZTask";
        names[AUDIO] = "PlayAudio";
        names[GETINPUT] = "GetInput";
        names[NOCASEEQUALS] = "NoCaseEquals";
        names[SHOWSCREEN] = "ShowScreen";
        names[TRANSLATEPOINT] = "TranslatePoint";
        names[SHOWSTATUSTEXT] = "ShowStatusText";
        names[VECTORTOPOINT] = "VectorToPoint";
        names[COMMAND] = "Command";
        names[LOGMESSAGE] = "LogMessage";
        names[MADE] = "made";
        names[GETVALUE] = "GetValue";
    }

    public static final Hashtable env = new Hashtable(); /* Wherigo's Env table */
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
        env.put("Downloaded", new Double(0));
    }

    private int index;
    private Class klass;

    private static WherigoLib[] functions;
    static {
        functions = new WherigoLib[NUM_FUNCTIONS];
        for (int i = 0; i < NUM_FUNCTIONS; i++) {
            functions[i] = new WherigoLib(i);
        }
    }

    private Class<?> assignClass () {
        // because i'm too lazy to type out the break;s in a switch
        return switch (index) {
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

    public WherigoLib(int index) {
        this.index = index;
        this.klass = assignClass();
    }

    public static void register(LuaState state) {

        if (env.get(DEVICE_ID) == null) throw new IllegalStateException("set your DeviceID! WherigoLib.env.put(WherigoLib.DEVICE_ID, \"some value\")");

        LuaTable environment = state.getEnvironment();

        LuaTable wig = new LuaTableImpl();
        environment.rawset("Wherigo", wig);
        for (int i = 0; i < NUM_FUNCTIONS; i++) {
            Engine.instance.savegame.addJavafunc(functions[i]);
            wig.rawset(names[i], functions[i]);
        }

        LuaTable distanceMetatable = new LuaTableImpl();
        distanceMetatable.rawset("__index", distanceMetatable);
        distanceMetatable.rawset("__call", functions[GETVALUE]);
        distanceMetatable.rawset(names[GETVALUE], functions[GETVALUE]);
        state.setClassMetatable(Double.class, distanceMetatable);

        state.setClassMetatable(WherigoLib.class, wig);
        wig.rawset("__index", wig);

        wig.rawset("Player", Engine.instance.player);
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
        Enumeration e = env.keys();
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            envtable.rawset(key, env.get(key));
        }
        envtable.rawset("Device", Engine.instance.gwcfile.device);
        environment.rawset("Env", envtable);

        Cartridge.register();
        Container.register();
        Player.register();
        Timer.register();

        Media.reset();
    }

    public String toString() {
        return names[index];
    }


    public int call(LuaCallFrame callFrame, int nArguments) {
        return switch (index) {
            case MADE -> made(callFrame, nArguments);

            // special constructors:
            case ZONEPOINT -> zonePoint(callFrame, nArguments);
            case DISTANCE -> distance(callFrame, nArguments);

            // generic constructors:
            case ZITEM -> construct(new Thing(false), callFrame, nArguments);
            case ZCHARACTER -> construct(new Thing(true), callFrame, nArguments);
            case CARTRIDGE ->
                    construct(Engine.instance.cartridge = new Cartridge(), callFrame, nArguments);
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
        if (param instanceof Cartridge) {
            c = (Cartridge)param;
        } else if (param instanceof LuaTable) {
            LuaTable lt = (LuaTable)param;
            c = (Cartridge)lt.rawget("Cartridge");
            what.setTable((LuaTable)param);
            if (what instanceof Container) {
                Container cont = (Container)what;
                Container target = (Container)lt.rawget("Container");
                if (target != null)
                    cont.moveTo(target);
            }
        }
        if (c == null) c = Engine.instance.cartridge;
        c.addObject(what);
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
            if (o instanceof EventTable) et = (EventTable)o;
        }
        Engine.log("CALL: ShowScreen("+screen+") " + (et == null ? "" : et.name), Engine.LOG_CALL);
        Engine.ui.showScreen(screen, et);
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
        Engine.ui.setStatusText(text);
        return 0;
    }

    private int logMessage (LuaCallFrame callFrame, int nArguments) {
        if (nArguments < 1) return 0;
        Object arg = callFrame.get(0);
        String text;
        if (arg instanceof LuaTable) {
            LuaTable lt = (LuaTable)arg;
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
      Engine.ui.command(cmd);
      return 0;
    }
}
