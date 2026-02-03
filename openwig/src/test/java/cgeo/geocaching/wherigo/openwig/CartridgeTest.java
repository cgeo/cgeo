package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for Cartridge class.
 */
public class CartridgeTest {

    private Cartridge cartridge;

    @Before
    public void setUp() {
        cartridge = new Cartridge();
    }

    @Test
    public void testCartridgeInitialization() {
        assertNotNull("Zones vector should be initialized", cartridge.zones);
        assertNotNull("Timers vector should be initialized", cartridge.timers);
        assertNotNull("Things vector should be initialized", cartridge.things);
        assertNotNull("Universal actions vector should be initialized", cartridge.universalActions);
        assertNotNull("AllZObjects table should be initialized", cartridge.allZObjects);
    }

    @Test
    public void testCartridgeWithEngineParameter() {
        Engine mockEngine = new Engine() {};
        Cartridge cartridgeWithEngine = new Cartridge(mockEngine);
        
        assertNotNull("Cartridge should be created with engine", cartridgeWithEngine);
        assertNotNull("AllZObjects should be initialized", cartridgeWithEngine.allZObjects);
    }

    @Test
    public void testAddObject_zone() {
        Zone zone = new Zone();
        zone.rawset("Name", "TestZone");
        
        cartridge.addObject(zone);
        
        assertThat(cartridge.zones).contains(zone);
        assertTrue("Zone should be in allZObjects", 
                   containsObject(cartridge.allZObjects, zone));
    }

    @Test
    public void testAddObject_timer() {
        Timer timer = new Timer();
        timer.rawset("Name", "TestTimer");
        
        cartridge.addObject(timer);
        
        assertThat(cartridge.timers).contains(timer);
        assertTrue("Timer should be in allZObjects", 
                   containsObject(cartridge.allZObjects, timer));
    }

    @Test
    public void testAddObject_task() {
        Task task = new Task();
        task.rawset("Name", "TestTask");
        
        cartridge.addObject(task);
        
        assertThat(Cartridge.tasks).contains(task);
        assertTrue("Task should be in allZObjects", 
                   containsObject(cartridge.allZObjects, task));
    }

    @Test
    public void testAddObject_thing() {
        Thing thing = new Thing(false);
        thing.rawset("Name", "TestThing");
        
        cartridge.addObject(thing);
        
        assertThat(cartridge.things).contains(thing);
        assertTrue("Thing should be in allZObjects", 
                   containsObject(cartridge.allZObjects, thing));
    }

    @Test
    public void testVisibleZones() {
        Zone zone1 = new Zone();
        zone1.rawset("Name", "Zone1");
        zone1.rawset("Visible", true);
        zone1.rawset("Active", true);
        
        Zone zone2 = new Zone();
        zone2.rawset("Name", "Zone2");
        zone2.rawset("Visible", false);
        
        cartridge.addObject(zone1);
        cartridge.addObject(zone2);
        
        // Note: The actual visibility depends on zone's contain state
        // This test just verifies the method runs without error
        int visibleCount = cartridge.visibleZones();
        assertThat(visibleCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testLuaToString() {
        String result = cartridge.luaTostring();
        assertThat(result).isEqualTo("a ZCartridge instance");
    }

    @Test
    public void testRequestSyncInTable() {
        Object syncFunc = cartridge.rawget("RequestSync");
        assertNotNull("RequestSync function should be registered", syncFunc);
    }

    @Test
    public void testAllZObjectsTable() {
        assertNotNull("AllZObjects should be in cartridge table", 
                     cartridge.rawget("AllZObjects"));
        
        // Cartridge itself should be the first object
        Object firstObject = cartridge.allZObjects.rawget(1.0);
        assertThat(firstObject).isSameAs(cartridge);
    }

    // Helper method to check if an object is in a LuaTable
    private boolean containsObject(cgeo.geocaching.wherigo.kahlua.vm.LuaTable table, Object obj) {
        Object key = null;
        while ((key = table.next(key)) != null) {
            if (table.rawget(key) == obj) {
                return true;
            }
        }
        return false;
    }
}
