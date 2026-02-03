/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import java.io.DataInputStream;
import java.io.IOException;
import cgeo.geocaching.wherigo.kahlua.stdlib.TableLib;
import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

/**
 * Represents a container that can hold items in a Wherigo game.
 * <p>
 * Container extends EventTable to provide inventory management capabilities.
 * Items (Things) can be moved into or out of containers. The player is also
 * a container, as are zones and characters that can hold items.
 * <p>
 * Key features:
 * <ul>
 * <li>Maintains an inventory of contained items</li>
 * <li>Supports moving items between containers via MoveTo()</li>
 * <li>Can check if it contains a specific item via Contains()</li>
 * <li>Tracks its own container (nested containment)</li>
 * </ul>
 */
public class Container extends EventTable {

    public LuaTable inventory = new LuaTableImpl();
    public Container container = null;

    private static JavaFunction moveTo = new JavaFunction() {
        public int call (LuaCallFrame callFrame, int nArguments) {
            Container subject = (Container) callFrame.get(0);
            Container target = (Container) callFrame.get(1);
            subject.moveTo(target);
            return 0;
        }
    };

    private static JavaFunction contains = new JavaFunction() {
        public int call (LuaCallFrame callFrame, int nArguments) {
            Container p = (Container) callFrame.get(0);
            Thing t = (Thing) callFrame.get(1);
            callFrame.push(LuaState.toBoolean(p.contains(t)));
            return 1;
        }
    };

    public static void register () {
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            currentEngine.savegame.addJavafunc(moveTo);
            currentEngine.savegame.addJavafunc(contains);
        }
    }

    public Container() {
        rawset("MoveTo", moveTo);
        rawset("Contains", contains);
        rawset("Inventory", inventory);
        rawset("Container", container); // fix issues 181, 191
    }

    public void moveTo(Container c) {
        Engine currentEngine = Engine.getCurrentInstance();
        String cn = c == null ? "(nowhere)" : c.name;
        Engine.log("MOVE: "+name+" to "+cn, Engine.LOG_CALL);
        if (container != null) TableLib.removeItem(container.inventory, this);
        // location.things.removeElement(this);
        if (c != null) {
            TableLib.rawappend(c.inventory, this);
            if (currentEngine != null && c == currentEngine.player) setPosition(null);
            else if (position != null) setPosition(c.position);
            else if (currentEngine != null && container == currentEngine.player) setPosition(ZonePoint.copy(currentEngine.player.position));
            container = c;
        } else {
            container = null;
            rawset("ObjectLocation", null);
        }
        rawset("Container", container); // fix issues 181, 191
    }

    public boolean contains (Thing t) {
        Object key = null;
        while ((key = inventory.next(key)) != null) {
            Object value = inventory.rawget(key);
            if (value instanceof Thing thing) {
                if (value == t) return true;
                if (thing.contains(t)) return true;
            }
        }
        return false;
    }

    public boolean visibleToPlayer () {
        if (!isVisible()) return false;
        final Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null && container == currentEngine.player) return true;
        if (container instanceof Zone z) {
            return z.showThings();
        }
        return false;
    }

    @Override
    public Object getItem (final String key) {
        if ("Container".equals(key)) {
            return container;
        }
        return super.getItem(key);
    }

    @Override
    public void deserialize (final DataInputStream in)
    throws IOException {
        super.deserialize(in);
        inventory = (LuaTable) super.rawget("Inventory");
        final Object o = super.rawget("Container");
        if (o instanceof Container c) {
            container = c;
        } else {
            container = null;
        }
    }
}
