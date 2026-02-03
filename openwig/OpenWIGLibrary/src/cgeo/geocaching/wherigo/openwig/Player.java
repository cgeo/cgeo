package cgeo.geocaching.wherigo.openwig;

import java.io.DataInputStream;
import java.io.IOException;

import cgeo.geocaching.wherigo.openwig.kahlua.stdlib.TableLib;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.*;

public class Player extends Thing {

    private LuaTableImpl insideOfZones = new LuaTableImpl();
    
    private static JavaFunction refreshLocation = new JavaFunction() {
        public int call (LuaCallFrame callFrame, int nArguments) {
            Engine.instance.player.refreshLocation();
            return 0;
        }
    };

    public static void register () {
        Engine.instance.savegame.addJavafunc(refreshLocation);
    }
    
    public Player() {
        super(true);
        table.rawset("RefreshLocation", refreshLocation);
        table.rawset("InsideOfZones", insideOfZones);
        setPosition(new ZonePoint(360,360,0));
    }

    public void moveTo (Container c) {
        // do nothing
    }

    public void enterZone (Zone z) {
        container = z;
        if (!TableLib.contains(insideOfZones, z)) {
            TableLib.rawappend(insideOfZones, z);
        }
        // Player should not go to inventory
        /*if (!TableLib.contains(z.inventory, this)) {
            TableLib.rawappend(z.inventory, this);
        }*/
    }

    public void leaveZone (Zone z) {
        TableLib.removeItem(insideOfZones, z);
        if (insideOfZones.len() > 0)
            container = (Container) insideOfZones.rawget(new Double(insideOfZones.len()));
        //TableLib.removeItem(z.inventory, this);
    }

    protected String luaTostring () { return "a Player instance"; }

    public void deserialize (DataInputStream in)
    throws IOException {
        super.deserialize(in);
        Engine.instance.player = this;
        //setPosition(new ZonePoint(360,360,0));
    }
    
    public int visibleThings() {
        int count = 0;
        Object key = null;
        while ((key = inventory.next(key)) != null) {
            Object o = inventory.rawget(key);
            if (o instanceof Thing && ((Thing) o).isVisible()) count++;
        }
        return count;
    }

    public void refreshLocation() {
        position.latitude = Engine.gps.getLatitude();
        position.longitude = Engine.gps.getLongitude();
        position.altitude = Engine.gps.getAltitude();
        table.rawset("PositionAccuracy", LuaState.toDouble(Engine.gps.getPrecision()));
        Engine.instance.cartridge.walk(position);
    }

    public void rawset (Object key, Object value) {
        if ("ObjectLocation".equals(key)) return;
        super.rawset(key, value);
    }

    public Object rawget (Object key) {
        if ("ObjectLocation".equals(key)) return ZonePoint.copy(position);
        return super.rawget(key);
    }
}
