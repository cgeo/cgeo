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

public class Cartridge extends EventTable {
    public Vector zones = new Vector();
    public Vector timers = new Vector();

    public Vector things = new Vector();
    public Vector universalActions = new Vector();

    public Vector tasks = new Vector();

    public LuaTable allZObjects = new LuaTableImpl();

    private static JavaFunction requestSync = new JavaFunction() {
        public int call (LuaCallFrame callFrame, int nArguments) {
            Engine.instance.store();
            return 0;
        }
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
        if (o instanceof Task) tasks.addElement(o);
        else if (o instanceof Zone) zones.addElement(o);
        else if (o instanceof Timer) timers.addElement(o);
        else if (o instanceof Thing) things.addElement(o);
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
