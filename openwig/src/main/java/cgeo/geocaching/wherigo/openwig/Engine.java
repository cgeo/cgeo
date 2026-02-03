/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.kahlua.vm.LuaPrototype;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import cgeo.geocaching.wherigo.openwig.formats.Savegame;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;
import cgeo.geocaching.wherigo.openwig.platform.UI;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;


/** The OpenWIG Engine
 * <p>
 * This is the heart of OpenWIG. It instantiates the Lua machine and acts
 * as an interface between GPS position source, GUI and the Lua Wherigo script.
 * <p>
 * Engine uses ThreadLocal storage to support parallel cartridge execution.
 * Each thread can have its own Engine instance, accessible via getCurrentInstance().
 * This allows multiple cartridges to run simultaneously on different threads.
 * <p>
 * To create a new Engine, you need a CartridgeFile, a reference to UI and LocationService.
 * Optionally, you can provide an OutputStream that will be used for logging.
 * When you get an instance, you can start it via start() for new game, or resume() for
 * continuing a saved game. Note that resume() will fail if there is no saved game.
 * <p>
 * Engine runs in a separate thread, and creates one more utility thread for itself,
 * whose sole purpose is to do everything related to Lua state - calling events, callbacks,
 * saving game.
 * Engine's own main loop consists of relaying position information from LocationService
 * to the Lua properties and evaluating position of player against zones.
 */
public class Engine implements Runnable {

    public static final String VERSION = "428";

    /** ThreadLocal storage for engine instances to support parallel cartridge execution */
    private static final ThreadLocal<Engine> threadLocalInstance = new ThreadLocal<>();

    /** Instance-specific Lua state for this engine */
    public LuaState luaState;
    /** Instance-specific UI implementation for this engine */
    public UI uiInstance;
    /** Instance-specific LocationService for this engine */
    public LocationService gpsInstance;

    /** reference to source file */
    public CartridgeFile gwcfile;
    /** reference to save file */
    public Savegame savegame = null;
    /** reference to log stream */
    private PrintStream log;

    /** event runner taking care of Lua state calls */
    protected BackgroundRunner eventRunner;

    /** Cartridge (a global Lua object) */
    public Cartridge cartridge;
    /** global Player Lua object */
    public Player player = new Player();

    private boolean doRestore = false;
    private boolean end = false;

    public static final int LOG_PROP = 0;
    public static final int LOG_CALL = 1;
    public static final int LOG_WARN = 2;
    public static final int LOG_ERROR = 3;
    private int loglevel = LOG_WARN;

    private Thread thread = null;

    /**
     * Gets the Engine instance for the current thread.
     * This allows multiple engines to run in parallel on different threads.
     * 
     * @return the Engine instance for the current thread, or null if none exists
     */
    public static Engine getCurrentInstance() {
        return threadLocalInstance.get();
    }

    /** Creates a new Engine instance for running a cartridge
     * @param cf CartridgeFile to run
     * @param out OutputStream for logging (can be null)
     * @param ui UI implementation for this engine
     * @param service LocationService implementation for this engine
     * @throws IOException if cartridge file cannot be read
     */
    public Engine (CartridgeFile cf, OutputStream out, UI ui, LocationService service) throws IOException {
        gwcfile = cf;
        savegame = cf.getSavegame();
        if (out != null) log = new PrintStream(out);
        this.uiInstance = ui;
        this.gpsInstance = service;
        if(gwcfile != null && gwcfile.device != null)
            WherigoLib.env.put("Device", gwcfile.device);
    }

    /** Deprecated constructor for backward compatibility */
    @Deprecated
    protected Engine (CartridgeFile cf, OutputStream out) throws IOException {
        this(cf, out, null, null);
    }

    protected Engine () {
        /* for test mockups */
    }

    /** starts Engine's thread */
    public void start () {
        thread = new Thread(this);
        thread.start();
    }

    /** marks game for resuming and starts thread */
    public void restore () {
        doRestore = true;
        start();
    }

    /** prepares Lua state and some bookkeeping */
    protected void prepareState ()
    throws IOException {
        uiInstance.debugMsg("Creating state...\n");
        luaState = new LuaState(System.out);
        
        // Set ThreadLocal instance for this thread
        threadLocalInstance.set(this);

        /*write("Registering base libs...\n");
        BaseLib.register(luaState);
        MathLib.register(luaState);
        StringLib.register(luaState);
        CoroutineLib.register(luaState);
        OsLib.register(luaState);*/

        uiInstance.debugMsg("Building javafunc map...\n");
        savegame.buildJavafuncMap(luaState.getEnvironment());

        uiInstance.debugMsg("Loading stdlib...");
        InputStream stdlib = getClass().getResourceAsStream("/openwig/stdlib.lbc");
        LuaClosure closure = LuaPrototype.loadByteCode(stdlib, luaState.getEnvironment());
        uiInstance.debugMsg("calling...\n");
        luaState.call(closure, null, null, null);
        stdlib.close();
        stdlib = null;

        uiInstance.debugMsg("Registering WIG libs...\n");
        WherigoLib.register(this);

        uiInstance.debugMsg("Building event queue...\n");
        eventRunner = new BackgroundRunner(true);
        eventRunner.setQueueListener(new Runnable() {
            public void run () {
                uiInstance.refresh();
            }
        });
    }

    /** invokes game restore */
    private void restoreGame ()
    throws IOException {
        uiInstance.debugMsg("Restoring saved state...");
        cartridge = new Cartridge(this);
        savegame.restore(luaState.getEnvironment());
    }

    /** invokes creation of clean new game environment */
    private void newGame ()
    throws IOException {
        // starting game normally
        uiInstance.debugMsg("Loading gwc...");
        if (gwcfile == null) throw new IOException("invalid cartridge file");

        uiInstance.debugMsg("pre-setting properties...");
        player.rawset("CompletionCode", gwcfile.code);
        player.rawset("Name", gwcfile.member);

        uiInstance.debugMsg("loading code...");
        byte[] lbc = gwcfile.getBytecode();

        uiInstance.debugMsg("parsing...");
        LuaClosure closure = LuaPrototype.loadByteCode(new ByteArrayInputStream(lbc), luaState.getEnvironment());

        uiInstance.debugMsg("calling...\n");
        luaState.call(closure, null, null, null);
        lbc = null;
        closure = null;
    }

    /** main loop - periodically copy location data into Lua and evaluate zone positions */
    private void mainloop () {
        try {
            while (!end) {
                try {
                    if (gpsInstance.getLatitude() != player.position.latitude
                    || gpsInstance.getLongitude() != player.position.longitude
                    || gpsInstance.getAltitude() != player.position.altitude) {
                        player.refreshLocation();
                    }
                    cartridge.tick();
                } catch (Exception e) {
                    stacktrace(e);
                }

                try { Thread.sleep(1000); } catch (InterruptedException e) { }
            }
            if (log != null) log.close();
        } catch (Throwable t) {
            uiInstance.end();
            stacktrace(t);
        } finally {
            // Clear ThreadLocal for this thread
            threadLocalInstance.remove();
            
            if (eventRunner != null) eventRunner.kill();
            eventRunner = null;
        }
    }

    /** thread's run() method that does all the work in the right order */
    public void run () {
        try {
            if (log != null) log.println("-------------------\ncartridge " + gwcfile.name + " started (openWIG r" + VERSION + ")\n-------------------");
            prepareState ();

            if (doRestore) restoreGame();
            else newGame();

            loglevel = LOG_PROP;

            uiInstance.debugMsg("Starting game...\n");
            uiInstance.start();

            player.refreshLocation();
            cartridge.callEvent(doRestore ? "OnRestore" : "OnStart", null);
            uiInstance.refresh();
            eventRunner.unpause();

            mainloop();
        } catch (IOException e) {
            uiInstance.showError("Could not load cartridge: "+e.getMessage());
        } catch (Throwable t) {
            stacktrace(t);
        } finally {
            uiInstance.end();
        }
    }

    /** utility function to dump stack trace and show a semi-meaningful error */
    public static void stacktrace (final Throwable e) {
        final Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) {
            e.printStackTrace();
            return;
        }
        
        e.printStackTrace();
        final StringBuilder msg = new StringBuilder(e.toString());
        if (currentEngine.luaState != null) {
            System.out.println(currentEngine.luaState.currentThread.stackTrace);
            msg.append("\nstack trace: " + currentEngine.luaState.currentThread.stackTrace);
        }
        for (final StackTraceElement ste : e.getStackTrace()) {
            msg.append("\nat " + ste);
        }
        final String msgString = msg.toString();
        currentEngine.log(msgString, LOG_ERROR);
        currentEngine.uiInstance.showError(msgString);
    }

    /** stops Engine */
    public static void kill () {
        Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) return;
        Timer.kill();
        currentEngine.end = true;
    }

    /** builds and calls a dialog from a Message table */
    public static void message (LuaTable message) {
        Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) return;
        String[] texts = {removeHtml((String)message.rawget("Text"))};
        log("CALL: MessageBox - " + texts[0].substring(0, Math.min(100,texts[0].length())), LOG_CALL);
        Media[] media = {(Media)message.rawget("Media")};
        String button1 = null, button2 = null;
        LuaTable buttons = (LuaTable)message.rawget("Buttons");
        if (buttons != null) {
            button1 = (String)buttons.rawget(new Double(1));
            button2 = (String)buttons.rawget(new Double(2));
        }
        LuaClosure callback = (LuaClosure)message.rawget("Callback");
        currentEngine.uiInstance.pushDialog(texts, media, button1, button2, callback);
    }

    /** builds and calls a dialog from a Dialog table */
    public static void dialog (String[] texts, Media[] media) {
        Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) return;
        if (texts.length > 0) {
            log("CALL: Dialog - " + texts[0].substring(0, Math.min(100,texts[0].length())), LOG_CALL);
        }
        currentEngine.uiInstance.pushDialog(texts, media, null, null, null);
    }

    /** calls input to UI */
    public static void input (EventTable input) {
        Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) return;
        log("CALL: GetInput - "+input.name, LOG_CALL);
        currentEngine.uiInstance.pushInput(input);
    }

    /** fires the specified event on the specified object in the event thread */
    public static void callEvent (final EventTable subject, final String name, final Object param) {
        if (!subject.hasEvent(name)) return;
        final Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) return;
        currentEngine.eventRunner.perform(new Runnable() {
            public void run () {
                subject.callEvent(name, param);
                // callEvent handles its failures, so no catch here
            }
        });
    }

    /** invokes a Lua callback in the event thread */
    public static void invokeCallback (final LuaClosure callback, final Object value) {
        final Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) return;
        currentEngine.eventRunner.perform(new Runnable() {
            public void run () {
                try {
                    Engine.log("BTTN: " + (value == null ? "(cancel)" : value.toString()) + " pressed", LOG_CALL);
                    currentEngine.luaState.call(callback, value, null, null);
                    Engine.log("BTTN END", LOG_CALL);
                } catch (Throwable t) {
                    stacktrace(t);
                    Engine.log("BTTN FAIL", LOG_CALL);
                }
            }
        });
    }

    /** extracts media file data from cartridge */
    public static byte[] mediaFile (Media media) throws IOException {
        /*String filename = media.jarFilename();
        return media.getClass().getResourceAsStream("/media/"+filename);*/
        Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) throw new IOException("No engine instance available");
        return currentEngine.gwcfile.getFile(media.id);
    }

    /** tries to log the specified message, if verbosity is higher than its level */
    public static void log (String s, int level) {
        Engine currentEngine = getCurrentInstance();
        if (currentEngine == null || currentEngine.log == null) return;
        if (level < currentEngine.loglevel) return;
        synchronized (currentEngine.log) {
        LocalDateTime now = LocalDateTime.now();
        currentEngine.log.print(now.getHour());
        currentEngine.log.print(':');
        currentEngine.log.print(now.getMinute());
        currentEngine.log.print(':');
        currentEngine.log.print(now.getSecond());
        currentEngine.log.print('|');
        currentEngine.log.print((int)(currentEngine.gpsInstance.getLatitude() * 10000 + 0.5) / 10000.0);
        currentEngine.log.print('|');
        currentEngine.log.print((int)(currentEngine.gpsInstance.getLongitude() * 10000 + 0.5) / 10000.0);
        currentEngine.log.print('|');
        currentEngine.log.print(currentEngine.gpsInstance.getAltitude());
        currentEngine.log.print('|');
        currentEngine.log.print(currentEngine.gpsInstance.getPrecision());
        currentEngine.log.print("|:: ");
        currentEngine.log.println(s);
        currentEngine.log.flush();
        }
    }

    private static void replace (String source, String pattern, String replace, StringBuilder builder) {
        int pos = 0;
        int pl = pattern.length();
        builder.delete(0, builder.length());
        while (pos < source.length()) {
            int np = source.indexOf(pattern, pos);
            if (np == -1) break;
            builder.append(source.substring(pos, np));
            builder.append(replace);
            pos = np + pl;
        }
        builder.append(source.substring(pos));
    }

    /** strips a subset of HTML that tends to appear in descriptions generated
     * by Groundspeak Builder
     */
    public static String removeHtml (String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        replace(s, "<BR>", "\n", sb);
        replace(sb.toString(), "&nbsp;", " ", sb);
        replace(sb.toString(), "&lt;", "<", sb);
        replace(sb.toString(), "&gt;", ">", sb);
        replace(sb.toString(), "&amp;", "&", sb);
        return sb.toString();
    }

    private Runnable refresh = new Runnable() {
        public void run () {
            synchronized (Engine.this) {
                uiInstance.refresh();
                refreshScheduled = false;
            }
        }
    };
    private volatile boolean refreshScheduled = false;

    public static void refreshUI () {
        Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) return;
        synchronized (currentEngine) {
            if (!currentEngine.refreshScheduled) {
                currentEngine.refreshScheduled = true;
                currentEngine.eventRunner.perform(currentEngine.refresh);
            }
        }
    }

    private Runnable store = new Runnable() {
        public void run () {
            // perform the actual sync
            try {
                uiInstance.blockForSaving();
                savegame.store(luaState.getEnvironment());
            } catch (IOException e) {
                log("STOR: save failed: "+e.toString(), LOG_WARN);
                uiInstance.showError("Sync failed.\n" + e.getMessage());
            } finally {
                uiInstance.unblock();
            }
        }
    };

    /** stores current game state */
    public void store () {
        store.run();
    }

    /** requests save in event thread */
    public static void requestSync () {
        Engine currentEngine = getCurrentInstance();
        if (currentEngine == null) return;
        currentEngine.eventRunner.perform(currentEngine.store);
    }
}
