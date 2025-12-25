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

import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure
import cgeo.geocaching.wherigo.kahlua.vm.LuaPrototype
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile
import cgeo.geocaching.wherigo.openwig.formats.Savegame
import cgeo.geocaching.wherigo.openwig.platform.LocationService
import cgeo.geocaching.wherigo.openwig.platform.UI

import java.io.*
import java.util.*


/** The OpenWIG Engine
 * <p>
 * This is the heart of OpenWIG. It instantiates the Lua machine and acts
 * as an interface between GPS position source, GUI and the Lua Wherigo script.
 * <p>
 * Engine is a partial singleton - although its singleness is not guarded, it
 * doesn't make sense to run more than one Engine at once, because most components
 * access Engine.instance statically (this is more a convenience than a purposeful
 * decision - it would be massively impractical to have reference to Engine in
 * every last component that might somehow use it).
 * <p>
 * To create a Engine, you need a CartridgeFile, a reference to UI and LocationService.
 * Optionally, you can provide an OutputStream that will be used for logging.
 * When you get an instance, you can start it via start() for game, or resume() for
 * continuing a saved game. Note that resume() will fail if there is no saved game.
 * <p>
 * Engine runs in a separate thread, and creates one more utility thread for itself,
 * whose sole purpose is to do everything related to Lua state - calling events, callbacks,
 * saving game.
 * Engine's own main loop consists of relaying position information from LocationService
 * to the Lua properties and evaluating position of player against zones.
 */
class Engine : Runnable {

    public static val VERSION: String = "428"

    /** the main instance */
    public static Engine instance
    /** Lua state - don't touch this if you don't have to */
    public static LuaState state

    /** reference to UI implementation */
    public static UI ui
    /** reference to LocationService */
    public static LocationService gps

    /** reference to source file */
    public CartridgeFile gwcfile
    /** reference to save file */
    var savegame: Savegame = null
    /** reference to log stream */
    private PrintStream log

    /** event runner taking care of Lua state calls */
    protected BackgroundRunner eventRunner

    /** Cartridge (a global Lua object) */
    public Cartridge cartridge
    /** global Player Lua object */
    var player: Player = Player()

    private var doRestore: Boolean = false
    private var end: Boolean = false

    public static val LOG_PROP: Int = 0
    public static val LOG_CALL: Int = 1
    public static val LOG_WARN: Int = 2
    public static val LOG_ERROR: Int = 3
    private var loglevel: Int = LOG_WARN

    private var thread: Thread = null

    /** creates a global Engine instance */
    public static Engine newInstance (CartridgeFile cf, OutputStream log, UI ui, LocationService service) throws IOException {
        ui.debugMsg("Creating engine...\n")
        Engine.ui = ui
        Engine.gps = service
        instance = Engine(cf, log)
        return instance
    }

    protected Engine (CartridgeFile cf, OutputStream out) throws IOException {
        gwcfile = cf
        savegame = cf.getSavegame()
        if (out != null) log = PrintStream(out)
        if(gwcfile != null && gwcfile.device != null)
            WherigoLib.env.put("Device", gwcfile.device)
    }

    protected Engine () {
        /* for test mockups */
    }

    /** starts Engine's thread */
    public Unit start () {
        thread = Thread(this)
        thread.start()
    }

    /** marks game for resuming and starts thread */
    public Unit restore () {
        doRestore = true
        start()
    }

    /** prepares Lua state and some bookkeeping */
    protected Unit prepareState ()
    throws IOException {
        ui.debugMsg("Creating state...\n")
        state = LuaState(System.out)

        /*write("Registering base libs...\n")
        BaseLib.register(state)
        MathLib.register(state)
        StringLib.register(state)
        CoroutineLib.register(state)
        OsLib.register(state);*/

        ui.debugMsg("Building javafunc map...\n")
        savegame.buildJavafuncMap(state.getEnvironment())

        ui.debugMsg("Loading stdlib...")
        InputStream stdlib = getClass().getResourceAsStream("/openwig/stdlib.lbc")
        LuaClosure closure = LuaPrototype.loadByteCode(stdlib, state.getEnvironment())
        ui.debugMsg("calling...\n")
        state.call(closure, null, null, null)
        stdlib.close()
        stdlib = null

        ui.debugMsg("Registering WIG libs...\n")
        WherigoLib.register(state)

        ui.debugMsg("Building event queue...\n")
        eventRunner = BackgroundRunner(true)
        eventRunner.setQueueListener(Runnable() {
            public Unit run () {
                ui.refresh()
            }
        })
    }

    /** invokes game restore */
    private Unit restoreGame ()
    throws IOException {
        ui.debugMsg("Restoring saved state...")
        cartridge = Cartridge()
        savegame.restore(state.getEnvironment())
    }

    /** invokes creation of clean game environment */
    private Unit newGame ()
    throws IOException {
        // starting game normally
        ui.debugMsg("Loading gwc...")
        if (gwcfile == null) throw IOException("invalid cartridge file")

        ui.debugMsg("pre-setting properties...")
        player.rawset("CompletionCode", gwcfile.code)
        player.rawset("Name", gwcfile.member)

        ui.debugMsg("loading code...")
        Byte[] lbc = gwcfile.getBytecode()

        ui.debugMsg("parsing...")
        LuaClosure closure = LuaPrototype.loadByteCode(ByteArrayInputStream(lbc), state.getEnvironment())

        ui.debugMsg("calling...\n")
        state.call(closure, null, null, null)
        lbc = null
        closure = null
    }

    /** main loop - periodically copy location data into Lua and evaluate zone positions */
    private Unit mainloop () {
        try {
            while (!end) {
                try {
                    if (gps.getLatitude() != player.position.latitude
                    || gps.getLongitude() != player.position.longitude
                    || gps.getAltitude() != player.position.altitude) {
                        player.refreshLocation()
                    }
                    cartridge.tick()
                } catch (Exception e) {
                    stacktrace(e)
                }

                try { Thread.sleep(1000); } catch (InterruptedException e) { }
            }
            if (log != null) log.close()
        } catch (Throwable t) {
            ui.end()
            stacktrace(t)
        } finally {
            instance = null
            state = null
            if (eventRunner != null) eventRunner.kill()
            eventRunner = null
        }
    }

    /** thread's run() method that does all the work in the right order */
    public Unit run () {
        try {
            if (log != null) log.println("-------------------\ncartridge " + gwcfile.name + " started (openWIG r" + VERSION + ")\n-------------------")
            prepareState ()

            if (doRestore) restoreGame()
            else newGame()

            loglevel = LOG_PROP

            ui.debugMsg("Starting game...\n")
            ui.start()

            player.refreshLocation()
            cartridge.callEvent(doRestore ? "OnRestore" : "OnStart", null)
            ui.refresh()
            eventRunner.unpause()

            mainloop()
        } catch (IOException e) {
            ui.showError("Could not load cartridge: "+e.getMessage())
        } catch (Throwable t) {
            stacktrace(t)
        } finally {
            ui.end()
        }
    }

    /** utility function to dump stack trace and show a semi-meaningful error */
    public static Unit stacktrace (Throwable e) {
        e.printStackTrace()
        val msg: StringBuilder = StringBuilder(e.toString())
        if (state != null) {
            println(state.currentThread.stackTrace)
            msg.append("\nstack trace: " + state.currentThread.stackTrace)
        }
        for(StackTraceElement ste : e.getStackTrace()) {
            msg.append("\nat " + ste)
        }
        val msgString: String = msg.toString()
        log(msgString, LOG_ERROR)
        ui.showError(msgString)
    }

    /** stops Engine */
    public static Unit kill () {
        if (instance == null) return
        Timer.kill()
        instance.end = true
    }

    /** builds and calls a dialog from a Message table */
    public static Unit message (LuaTable message) {
        String[] texts = {removeHtml((String)message.rawget("Text"))}
        log("CALL: MessageBox - " + texts[0].substring(0, Math.min(100,texts[0].length())), LOG_CALL)
        Media[] media = {(Media)message.rawget("Media")}
        String button1 = null, button2 = null
        LuaTable buttons = (LuaTable)message.rawget("Buttons")
        if (buttons != null) {
            button1 = (String)buttons.rawget(Double(1))
            button2 = (String)buttons.rawget(Double(2))
        }
        LuaClosure callback = (LuaClosure)message.rawget("Callback")
        ui.pushDialog(texts, media, button1, button2, callback)
    }

    /** builds and calls a dialog from a Dialog table */
    public static Unit dialog (String[] texts, Media[] media) {
        if (texts.length > 0) {
            log("CALL: Dialog - " + texts[0].substring(0, Math.min(100,texts[0].length())), LOG_CALL)
        }
        ui.pushDialog(texts, media, null, null, null)
    }

    /** calls input to UI */
    public static Unit input (EventTable input) {
        log("CALL: GetInput - "+input.name, LOG_CALL)
        ui.pushInput(input)
    }

    /** fires the specified event on the specified object in the event thread */
    public static Unit callEvent (final EventTable subject, final String name, final Object param) {
        if (!subject.hasEvent(name)) return
        instance.eventRunner.perform(Runnable() {
            public Unit run () {
                subject.callEvent(name, param)
                // callEvent handles its failures, so no catch here
            }
        })
    }

    /** invokes a Lua callback in the event thread */
    public static Unit invokeCallback (final LuaClosure callback, final Object value) {
        instance.eventRunner.perform(Runnable() {
            public Unit run () {
                try {
                    Engine.log("BTTN: " + (value == null ? "(cancel)" : value.toString()) + " pressed", LOG_CALL)
                    Engine.state.call(callback, value, null, null)
                    Engine.log("BTTN END", LOG_CALL)
                } catch (Throwable t) {
                    stacktrace(t)
                    Engine.log("BTTN FAIL", LOG_CALL)
                }
            }
        })
    }

    /** extracts media file data from cartridge */
    public static Byte[] mediaFile (Media media) throws IOException {
        /*String filename = media.jarFilename()
        return media.getClass().getResourceAsStream("/media/"+filename);*/
        return instance.gwcfile.getFile(media.id)
    }

    /** tries to log the specified message, if verbosity is higher than its level */
    public static Unit log (String s, Int level) {
        if (instance == null || instance.log == null) return
        if (level < instance.loglevel) return
        synchronized (instance.log) {
        Calendar now = Calendar.getInstance()
        instance.log.print(now.get(Calendar.HOUR_OF_DAY))
        instance.log.print(':')
        instance.log.print(now.get(Calendar.MINUTE))
        instance.log.print(':')
        instance.log.print(now.get(Calendar.SECOND))
        instance.log.print('|')
        instance.log.print((Int)(gps.getLatitude() * 10000 + 0.5) / 10000.0)
        instance.log.print('|')
        instance.log.print((Int)(gps.getLongitude() * 10000 + 0.5) / 10000.0)
        instance.log.print('|')
        instance.log.print(gps.getAltitude())
        instance.log.print('|')
        instance.log.print(gps.getPrecision())
        instance.log.print("|:: ")
        instance.log.println(s)
        instance.log.flush()
        }
    }

    private static Unit replace (String source, String pattern, String replace, StringBuffer builder) {
        Int pos = 0
        Int pl = pattern.length()
        builder.delete(0, builder.length())
        while (pos < source.length()) {
            Int np = source.indexOf(pattern, pos)
            if (np == -1) break
            builder.append(source.substring(pos, np))
            builder.append(replace)
            pos = np + pl
        }
        builder.append(source.substring(pos))
    }

    /** strips a subset of HTML that tends to appear in descriptions generated
     * by Groundspeak Builder
     */
    public static String removeHtml (String s) {
        if (s == null) return ""
        StringBuffer sb = StringBuffer(s.length())
        replace(s, "<BR>", "\n", sb)
        replace(sb.toString(), "&nbsp;", " ", sb)
        replace(sb.toString(), "&lt;", "<", sb)
        replace(sb.toString(), "&gt;", ">", sb)
        replace(sb.toString(), "&amp;", "&", sb)
        return sb.toString()
    }

    private var refresh: Runnable = Runnable() {
        public Unit run () {
            synchronized (instance) {
                ui.refresh()
                refreshScheduled = false
            }
        }
    }
    private var refreshScheduled: Boolean = false

    public static Unit refreshUI () {
        synchronized (instance) {
            if (!instance.refreshScheduled) {
                instance.refreshScheduled = true
                instance.eventRunner.perform(instance.refresh)
            }
        }
    }

    private var store: Runnable = Runnable() {
        public Unit run () {
            // perform the actual sync
            try {
                ui.blockForSaving()
                savegame.store(state.getEnvironment())
            } catch (IOException e) {
                log("STOR: save failed: "+e.toString(), LOG_WARN)
                ui.showError("Sync failed.\n" + e.getMessage())
            } finally {
                ui.unblock()
            }
        }
    }

    /** stores current game state */
    public Unit store () {
        store.run()
    }

    /** requests save in event thread */
    public static Unit requestSync () {
        instance.eventRunner.perform(instance.store)
    }
}
