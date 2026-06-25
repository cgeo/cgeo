/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.stdlib.TableLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Cartridge extends EventTable {
    public List<Zone> zones = new ArrayList<>();
    public List<Timer> timers = new ArrayList<>();

    public List<Thing> things = new ArrayList<>();
    public List<Action> universalActions = new ArrayList<>();

    public List<Task> tasks = new ArrayList<>();

    public LuaTable allZObjects = new LuaTableImpl();

    private static JavaFunction requestSync = (callFrame, nArguments) -> {
        Engine.instance.store();
        return 0;
    };

    public static void register () {
        Engine.instance.savegame.addJavafunc(requestSync);
    }

    protected String luaTostring () { return "a ZCartridge instance"; }

    public Cartridge () {
        table.rawset("RequestSync", requestSync);
        table.rawset("AllZObjects", allZObjects);
        TableLib.rawappend(allZObjects, this);
    }

    public void walk (ZonePoint zp) {
        for (final Zone z : zones) {
            z.walk(zp);
        }
    }

    public void tick () {
        for (final Zone z : zones) {
            z.tick();
        }
        for (final Timer t : timers) {
            t.updateRemaining();
        }

    }

    public int visibleZones () {
        int count = 0;
        for (final Zone z : zones) {
            if (z.isVisible()) count++;
        }
        return count;
    }

    public int visibleThings () {
        int count = 0;
        for (final Zone z : zones) {
            count += z.visibleThings();
        }
        return count;
    }

    public LuaTable currentThings () {
        LuaTable ret = new LuaTableImpl();
        for (final Zone z : zones) {
            z.collectThings(ret);
        }
        return ret;
    }

    public int visibleUniversalActions () {
        int count = 0;
        for (final Action a : universalActions) {
            if (a.isEnabled() && a.getActor().visibleToPlayer()) count++;
        }
        return count;
    }

    public int visibleTasks () {
        int count = 0;
        for (final Task a : tasks) {
            if (a.isVisible()) count++;
        }
        return count;
    }

    public void addObject (Object o) {
        TableLib.rawappend(allZObjects, o);
        sortObject(o);
    }

    private void sortObject (Object o) {
        if (o instanceof Task) tasks.add((Task) o);
        else if (o instanceof Zone) zones.add((Zone) o);
        else if (o instanceof Timer) timers.add((Timer) o);
        else if (o instanceof Thing) things.add((Thing) o);
    }

    public void deserialize (DataInputStream in)
    throws IOException {
        super.deserialize(in);
        Engine.instance.cartridge = this;
        allZObjects = (LuaTable)table.rawget("AllZObjects");
        Object next = null;
        while ((next = allZObjects.next(next)) != null) {
            sortObject(allZObjects.rawget(next));
        }
    }
}
