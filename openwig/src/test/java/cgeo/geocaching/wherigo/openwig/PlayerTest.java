package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for Player class.
 */
public class PlayerTest {

    private Player player;

    @Before
    public void setUp() {
        player = new Player();
    }

    @Test
    public void testPlayerInitialization() {
        assertNotNull("Player should be initialized", player);
        assertNotNull("Player position should be initialized", player.position);
        assertNotNull("InsideOfZones should be initialized", player.rawget("InsideOfZones"));
    }

    @Test
    public void testPlayerIsCharacter() {
        // Player extends Thing with isCharacter = true
        Object name = player.rawget("Name");
        // Player should have basic Thing properties
        assertThat(player.position).isNotNull();
    }

    @Test
    public void testRefreshLocationFunction() {
        Object refreshFunc = player.rawget("RefreshLocation");
        assertNotNull("RefreshLocation function should be registered", refreshFunc);
    }

    @Test
    public void testPlayerCannotMoveTo() {
        Container container = new Container();
        container.rawset("Name", "TestContainer");
        
        // moveTo should do nothing for Player
        player.moveTo(container);
        
        // Player should not be in the container's inventory
        assertThat(containsInInventory(container, player)).isFalse();
    }

    @Test
    public void testEnterZone() {
        Zone zone = new Zone();
        zone.rawset("Name", "TestZone");
        
        player.enterZone(zone);
        
        assertThat(player.container).isSameAs(zone);
        
        Object insideZones = player.rawget("InsideOfZones");
        assertThat(insideZones).isInstanceOf(cgeo.geocaching.wherigo.kahlua.vm.LuaTable.class);
    }

    @Test
    public void testLeaveZone() {
        Zone zone = new Zone();
        zone.rawset("Name", "TestZone");
        
        player.enterZone(zone);
        player.leaveZone(zone);
        
        // After leaving, player should not be in the zone
        Object insideZones = player.rawget("InsideOfZones");
        cgeo.geocaching.wherigo.kahlua.vm.LuaTable zonesTable = 
            (cgeo.geocaching.wherigo.kahlua.vm.LuaTable) insideZones;
        
        assertThat(zonesTable.len()).isEqualTo(0);
    }

    @Test
    public void testMultipleZones() {
        Zone zone1 = new Zone();
        zone1.rawset("Name", "Zone1");
        Zone zone2 = new Zone();
        zone2.rawset("Name", "Zone2");
        
        player.enterZone(zone1);
        player.enterZone(zone2);
        
        Object insideZones = player.rawget("InsideOfZones");
        cgeo.geocaching.wherigo.kahlua.vm.LuaTable zonesTable = 
            (cgeo.geocaching.wherigo.kahlua.vm.LuaTable) insideZones;
        
        assertThat(zonesTable.len()).isEqualTo(2);
        
        player.leaveZone(zone1);
        assertThat(zonesTable.len()).isEqualTo(1);
        assertThat(player.container).isSameAs(zone2);
    }

    @Test
    public void testVisibleThings() {
        Thing thing1 = new Thing(false);
        thing1.rawset("Name", "VisibleThing");
        thing1.rawset("Visible", true);
        thing1.moveTo(player);
        
        Thing thing2 = new Thing(false);
        thing2.rawset("Name", "InvisibleThing");
        thing2.rawset("Visible", false);
        thing2.moveTo(player);
        
        int visibleCount = player.visibleThings();
        assertThat(visibleCount).isEqualTo(1);
    }

    @Test
    public void testLuaToString() {
        String result = player.luaTostring();
        assertThat(result).isEqualTo("a Player instance");
    }

    @Test
    public void testPlayerPositionInitialization() {
        // Player starts at invalid coordinates (360, 360)
        assertThat(player.position.latitude).isEqualTo(360.0);
        assertThat(player.position.longitude).isEqualTo(360.0);
        assertThat(player.position.altitude).isEqualTo(0.0);
    }

    @Test
    public void testObjectLocationIsReadOnly() {
        ZonePoint newLocation = new ZonePoint(50.0, 10.0, 100.0);
        
        // Setting ObjectLocation should be ignored
        player.rawset("ObjectLocation", newLocation);
        
        // Getting ObjectLocation should return a copy of position
        Object location = player.rawget("ObjectLocation");
        assertThat(location).isInstanceOf(ZonePoint.class);
        
        ZonePoint retrievedLocation = (ZonePoint) location;
        assertThat(retrievedLocation.latitude).isEqualTo(player.position.latitude);
    }

    // Helper method
    private boolean containsInInventory(Container container, Thing thing) {
        Object key = null;
        while ((key = container.inventory.next(key)) != null) {
            if (container.inventory.rawget(key) == thing) {
                return true;
            }
        }
        return false;
    }
}
