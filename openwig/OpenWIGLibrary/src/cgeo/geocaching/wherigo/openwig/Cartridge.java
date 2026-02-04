package cgeo.geocaching.wherigo.openwig;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cgeo.geocaching.wherigo.openwig.kahlua.stdlib.TableLib;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaTableImpl;

public class Cartridge extends EventTable {
    public List<Zone> zones = new ArrayList<>();
    public List<Timer> timers = new ArrayList<>();
    
    public List<Thing> things = new ArrayList<>();
    public List<Action> universalActions = new ArrayList<>();
    
    public List<Task> tasks = new ArrayList<>();
    
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

    protected String luaTostring () {
        return "a ZCartridge instance";
    }
    
    public Cartridge () {
        table.rawset("RequestSync", requestSync);
        table.rawset("AllZObjects", allZObjects);
        TableLib.rawappend(allZObjects, this);
    }
        
    public void walk (ZonePoint zp) {       
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            z.walk(zp);
        }
    }
    
    public void tick () {
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            z.tick();
        }
        for (int i = 0; i < timers.size(); i++) {
            Timer t = timers.get(i);
            t.updateRemaining();
        }

    }
    
    public int visibleZones () {
        int count = 0;
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            if (z.isVisible()) {
                count++;
            }
        }
        return count;
    }
    
    public int visibleThings () {
        int count = 0;
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            count += z.visibleThings();
        }
        return count;
    }
    
    public LuaTable currentThings () {
        LuaTable ret = new LuaTableImpl();
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            z.collectThings(ret);
        }
        return ret;
    }
    
    public int visibleUniversalActions () {
        int count = 0;
        for (int i = 0; i < universalActions.size(); i++) {
            Action a = universalActions.get(i);
            if (a.isEnabled() && a.getActor().visibleToPlayer()) {
                count++;
            }
        }
        return count;
    }
    
    public int visibleTasks () {
        int count = 0;
        for (int i = 0; i < tasks.size(); i++) {
            Task a = tasks.get(i);
            if (a.isVisible()) {
                count++;
            }
        }
        return count;
    }
    
    public void addObject (Object o) {
        TableLib.rawappend(allZObjects, o);
        sortObject(o);
    }

    private void sortObject (Object o) {
        if (o instanceof Task) {
            tasks.add(o);
        } else if (o instanceof Zone) {
            zones.add(o);
        } else if (o instanceof Timer) {
            timers.add(o);
        } else if (o instanceof Thing) {
            things.add(o);
        }
    }

    public void deserialize (DataInputStream in)
    throws IOException {
        super.deserialize(in);
        Engine.instance.cartridge = this;
        allZObjects = (LuaTable) table.rawget("AllZObjects");
        Object next = null;
        while ((next = allZObjects.next(next)) != null) {
            sortObject(allZObjects.rawget(next));
        }
    }
}
