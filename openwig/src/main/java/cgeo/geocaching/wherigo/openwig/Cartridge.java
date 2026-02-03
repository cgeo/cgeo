/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import java.io.*;
import java.util.Vector;

import cgeo.geocaching.wherigo.kahlua.stdlib.TableLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

/**
 * Represents the cartridge (game) itself in a Wherigo game.
 * <p>
 * Cartridge extends EventTable to act as the root container for all game
 * objects. It maintains collections of zones, timers, tasks, things, and
 * actions that make up the game.
 * <p>
 * Key features:
 * <ul>
 * <li>Central registry for all game objects (zones, timers, tasks, things)</li>
 * <li>Maintains AllZObjects table - collection of all game objects</li>
 * <li>Coordinates zone proximity checking via walk()</li>
 * <li>Updates timers and zones via tick()</li>
 * <li>Provides RequestSync() to trigger game saves</li>
 * <li>Tracks universal actions available from any context</li>
 * </ul>
 * <p>
 * There is always exactly one Cartridge instance per game, accessible via
 * Engine.getCurrentInstance().cartridge. It represents the loaded .gwc cartridge file.
 */
public class Cartridge extends EventTable {
    private Engine engine; // Reference to the engine instance
    
    public Vector<Zone> zones = new Vector<>();
    public Vector<Timer> timers = new Vector<>();

    public Vector<Thing> things = new Vector<>();
    public Vector<Action>universalActions = new Vector<>();

    public static Vector<Task> tasks = new Vector<>();

    public LuaTable allZObjects = new LuaTableImpl();

    private final JavaFunction requestSync = new JavaFunction() {
        public int call (LuaCallFrame callFrame, int nArguments) {
            if (engine != null) {
                engine.store();
            } else {
                Engine.getCurrentInstance().store();
            }
            return 0;
        }
    };

    public static void register () {
        Engine.getCurrentInstance().savegame.addJavafunc(new JavaFunction() {
            public int call (LuaCallFrame callFrame, int nArguments) {
                Engine.getCurrentInstance().store();
                return 0;
            }
        });
    }

    protected String luaTostring () { return "a ZCartridge instance"; }

    public Cartridge () {
        this(null);
    }
    
    public Cartridge (Engine engine) {
        this.engine = engine;
        rawset("RequestSync", requestSync);
        rawset("AllZObjects", allZObjects);
        TableLib.rawappend(allZObjects, this);
    }

    public void walk (ZonePoint zp) {
        for (int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i);
            z.walk(zp);
        }
    }

    public void tick () {
        for (int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i);
            z.tick();
        }
        for (int i = 0; i < timers.size(); i++) {
            Timer t = (Timer)timers.elementAt(i);
            t.updateRemaining();
        }

    }

    public int visibleZones () {
        int count = 0;
        for (int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i);
            if (z.isVisible()) count++;
        }
        return count;
    }

    public int visibleThings () {
        int count = 0;
        for (int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i);
            count += z.visibleThings();
        }
        return count;
    }

    public LuaTable currentThings () {
        LuaTable ret = new LuaTableImpl();
        for (int i = 0; i < zones.size(); i++) {
            Zone z = (Zone)zones.elementAt(i);
            z.collectThings(ret);
        }
        return ret;
    }

    public int visibleUniversalActions () {
        int count = 0;
        for (int i = 0; i < universalActions.size(); i++) {
            Action a = (Action)universalActions.elementAt(i);
            if (a.isEnabled() && a.getActor().visibleToPlayer()) count++;
        }
        return count;
    }

    public int visibleTasks () {
        int count = 0;
        for (int i = 0; i < tasks.size(); i++) {
            Task a = (Task)tasks.elementAt(i);
            if (a.isVisible()) count++;
        }
        return count;
    }

    public void addObject (Object o) {
        TableLib.rawappend(allZObjects, o);
        sortObject(o);
    }

    private void sortObject (Object o) {

        if (o instanceof Task t) tasks.addElement(t);
        else if (o instanceof Zone z) zones.addElement(z);
        else if (o instanceof Timer t) timers.addElement(t);
        else if (o instanceof Thing t) things.addElement(t);
    }

    @Override
    public void deserialize (DataInputStream in)
    throws IOException {
        super.deserialize(in);
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            currentEngine.cartridge = this;
            this.engine = currentEngine;
        }
        allZObjects = rawget("AllZObjects");
        Object next = null;
        while ((next = allZObjects.next(next)) != null) {
            sortObject(allZObjects.rawget(next));
        }
    }
}
