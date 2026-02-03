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
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

/**
 * Represents the player character in a Wherigo game.
 * <p>
 * Player extends Thing to provide special handling for the human player.
 * Unlike other Things, the player cannot be moved to containers and has
 * special location tracking that syncs with the GPS position.
 * <p>
 * Key features:
 * <ul>
 * <li>Position automatically synchronized with GPS via RefreshLocation()</li>
 * <li>Tracks zones the player is currently inside (InsideOfZones)</li>
 * <li>Cannot be moved via MoveTo() - location tied to GPS</li>
 * <li>Acts as a container for items in player's inventory</li>
 * <li>Provides access to position accuracy from GPS</li>
 * <li>Automatically triggers zone events as player moves</li>
 * </ul>
 * <p>
 * There is always exactly one Player instance per game, accessible via
 * Engine.getCurrentInstance().player.
 */
public class Player extends Thing {

    private LuaTableImpl insideOfZones = new LuaTableImpl();

    private final JavaFunction refreshLocation = (callFrame, nArguments) -> {
        refreshLocation();
        return 0;
    };

    public static void register () {
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            currentEngine.savegame.addJavafunc(new JavaFunction() {
                @Override
                public int call(LuaCallFrame callFrame, int nArguments) {
                    Engine engine = Engine.getCurrentInstance();
                    if (engine != null) {
                        engine.player.refreshLocation();
                    }
                    return 0;
                }
            });
        }
    }

    public Player() {
        super(true);
        rawset("RefreshLocation", refreshLocation);
        rawset("InsideOfZones", insideOfZones);
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
            container = (Container)insideOfZones.rawget(new Double(insideOfZones.len()));
        //TableLib.removeItem(z.inventory, this);
    }

    protected String luaTostring () { return "a Player instance"; }

    @Override
    public void deserialize (DataInputStream in)
    throws IOException {
        super.deserialize(in);
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            currentEngine.player = this;
        }
        //setPosition(new ZonePoint(360,360,0));
    }

    public int visibleThings() {
        int count = 0;
        Object key = null;
        while ((key = inventory.next(key)) != null) {
            Object o = inventory.rawget(key);
            if (o instanceof Thing thing && thing.isVisible()) count++;
        }
        return count;
    }

    public void refreshLocation() {
        Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            position.latitude = currentEngine.gpsInstance.getLatitude();
            position.longitude = currentEngine.gpsInstance.getLongitude();
            position.altitude = currentEngine.gpsInstance.getAltitude();
            rawset("PositionAccuracy", LuaState.toDouble(currentEngine.gpsInstance.getPrecision()));
            currentEngine.cartridge.walk(position);
        }
    }

    @Override
    public void setItem (final String key, final Object value) {
        if ("ObjectLocation".equals(key)) {
            return;
        }
        super.setItem(key, value);
    }

    @Override
    public Object getItem (final String key) {
        if ("ObjectLocation".equals(key)) {
            return ZonePoint.copy(position);
        }
        return super.getItem(key);
    }
}
