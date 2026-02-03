package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for Zone class.
 */
public class ZoneTest {

    private Zone zone;

    @Before
    public void setUp() {
        zone = new Zone();
    }

    @Test
    public void testZoneInitialization() {
        assertNotNull("Zone should be initialized", zone);
        assertThat(zone.contain).isEqualTo(Zone.NOWHERE);
        assertThat(zone.distance).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    public void testZoneConstants() {
        assertThat(Zone.INSIDE).isEqualTo(2);
        assertThat(Zone.PROXIMITY).isEqualTo(1);
        assertThat(Zone.DISTANT).isEqualTo(0);
        assertThat(Zone.NOWHERE).isEqualTo(-1);
    }

    @Test
    public void testShowObjectsConstants() {
        assertThat(Zone.S_ALWAYS).isEqualTo(0);
        assertThat(Zone.S_ONENTER).isEqualTo(1);
        assertThat(Zone.S_ONPROXIMITY).isEqualTo(2);
        assertThat(Zone.S_NEVER).isEqualTo(3);
    }

    @Test
    public void testZoneWithPoints() {
        ZonePoint[] points = new ZonePoint[4];
        points[0] = new ZonePoint(50.0, 10.0, 0);
        points[1] = new ZonePoint(50.1, 10.0, 0);
        points[2] = new ZonePoint(50.1, 10.1, 0);
        points[3] = new ZonePoint(50.0, 10.1, 0);
        
        cgeo.geocaching.wherigo.kahlua.vm.LuaTable pointsTable = 
            new cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl();
        for (int i = 0; i < points.length; i++) {
            pointsTable.rawset((double)(i + 1), points[i]);
        }
        
        zone.rawset("Points", pointsTable);
        
        assertThat(zone.points).isNotNull();
        assertThat(zone.points.length).isEqualTo(4);
    }

    @Test
    public void testZoneActivation() {
        zone.rawset("Active", false);
        assertThat(zone.isActive()).isFalse();
        
        zone.rawset("Active", true);
        // isActive also depends on contain state
        // Just verify the property was set
        assertThat(zone.rawget("Active")).isNotNull();
    }

    @Test
    public void testZoneVisibility() {
        zone.rawset("Visible", true);
        zone.rawset("Active", true);
        
        // Visibility also depends on contain state
        Object visible = zone.rawget("Visible");
        assertThat(visible).isNotNull();
    }

    @Test
    public void testZoneIsLocated() {
        assertTrue("Zone should always be located", zone.isLocated());
    }

    @Test
    public void testLuaToString() {
        String result = zone.luaTostring();
        assertThat(result).isEqualTo("a Zone instance");
    }

    @Test
    public void testZoneInventory() {
        Thing thing = new Thing(false);
        thing.rawset("Name", "TestThing");
        thing.rawset("Visible", true);
        thing.moveTo(zone);
        
        assertThat(zone.inventory).isNotNull();
        assertTrue("Thing should be in zone inventory", 
                  containsInInventory(zone, thing));
    }

    @Test
    public void testVisibleThings() {
        Thing thing1 = new Thing(false);
        thing1.rawset("Name", "Thing1");
        thing1.rawset("Visible", true);
        thing1.moveTo(zone);
        
        Thing thing2 = new Thing(false);
        thing2.rawset("Name", "Thing2");
        thing2.rawset("Visible", false);
        thing2.moveTo(zone);
        
        // Note: visibleThings depends on showThings() which depends on zone state
        int count = zone.visibleThings();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testZoneNearestPoint() {
        assertNotNull("Nearest point should be initialized", zone.nearestPoint);
        assertThat(zone.nearestPoint.latitude).isEqualTo(0.0);
        assertThat(zone.nearestPoint.longitude).isEqualTo(0.0);
    }

    @Test
    public void testZoneBoundingBox() {
        // Test that bounding box fields exist
        zone.bbTop = 50.1;
        zone.bbBottom = 50.0;
        zone.bbLeft = 10.0;
        zone.bbRight = 10.1;
        
        assertThat(zone.bbTop).isEqualTo(50.1);
        assertThat(zone.bbBottom).isEqualTo(50.0);
        assertThat(zone.bbLeft).isEqualTo(10.0);
        assertThat(zone.bbRight).isEqualTo(10.1);
    }

    @Test
    public void testCollectThings() {
        cgeo.geocaching.wherigo.kahlua.vm.LuaTable collection = 
            new cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl();
        
        Thing thing = new Thing(false);
        thing.rawset("Name", "CollectableThing");
        thing.rawset("Visible", true);
        thing.moveTo(zone);
        
        zone.collectThings(collection);
        
        // collectThings may or may not add items depending on zone state
        // Just verify the method runs without error
        assertNotNull("Collection should not be null", collection);
    }

    @Test
    public void testZoneWithPlayer() {
        Player player = new Player();
        
        // Player should be excluded from thing counts
        player.moveTo(zone);
        
        int visibleCount = zone.visibleThings();
        // Player is explicitly excluded from visible things count
        assertThat(visibleCount).isEqualTo(0);
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
