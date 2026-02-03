package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for Container class.
 */
public class ContainerTest {

    private Container container;

    @Before
    public void setUp() {
        container = new Container();
    }

    @Test
    public void testContainerInitialization() {
        assertNotNull("Container should be initialized", container);
        assertNotNull("Inventory should be initialized", container.inventory);
        assertNotNull("MoveTo function should be registered", container.rawget("MoveTo"));
        assertNotNull("Contains function should be registered", container.rawget("Contains"));
    }

    @Test
    public void testMoveTo() {
        Container targetContainer = new Container();
        targetContainer.rawset("Name", "Target");
        
        Thing thing = new Thing(false);
        thing.rawset("Name", "TestThing");
        
        thing.moveTo(targetContainer);
        
        assertThat(thing.container).isSameAs(targetContainer);
        assertTrue("Thing should be in target inventory", 
                  containsInInventory(targetContainer, thing));
    }

    @Test
    public void testMoveToNull() {
        Thing thing = new Thing(false);
        thing.rawset("Name", "TestThing");
        
        Container tempContainer = new Container();
        thing.moveTo(tempContainer);
        thing.moveTo(null);
        
        assertThat(thing.container).isNull();
    }

    @Test
    public void testContains_directChild() {
        Thing thing = new Thing(false);
        thing.rawset("Name", "TestThing");
        thing.moveTo(container);
        
        assertTrue("Container should contain the thing", container.contains(thing));
    }

    @Test
    public void testContains_nested() {
        Container innerContainer = new Container();
        innerContainer.rawset("Name", "Inner");
        innerContainer.moveTo(container);
        
        Thing thing = new Thing(false);
        thing.rawset("Name", "NestedThing");
        thing.moveTo(innerContainer);
        
        assertTrue("Container should contain nested thing", container.contains(thing));
    }

    @Test
    public void testContains_notPresent() {
        Thing thing = new Thing(false);
        thing.rawset("Name", "TestThing");
        
        assertFalse("Container should not contain thing not in inventory", 
                   container.contains(thing));
    }

    @Test
    public void testVisibleToPlayer_notVisible() {
        container.rawset("Visible", false);
        assertFalse("Invisible container should not be visible to player", 
                   container.visibleToPlayer());
    }

    @Test
    public void testMultipleThingsInInventory() {
        Thing thing1 = new Thing(false);
        thing1.rawset("Name", "Thing1");
        thing1.moveTo(container);
        
        Thing thing2 = new Thing(false);
        thing2.rawset("Name", "Thing2");
        thing2.moveTo(container);
        
        assertTrue("Container should contain thing1", container.contains(thing1));
        assertTrue("Container should contain thing2", container.contains(thing2));
        
        assertThat(container.inventory.len()).isEqualTo(2);
    }

    @Test
    public void testMoveFromOneContainerToAnother() {
        Container container1 = new Container();
        container1.rawset("Name", "Container1");
        
        Container container2 = new Container();
        container2.rawset("Name", "Container2");
        
        Thing thing = new Thing(false);
        thing.rawset("Name", "MovableThing");
        
        thing.moveTo(container1);
        assertTrue("Thing should be in container1", container1.contains(thing));
        
        thing.moveTo(container2);
        assertFalse("Thing should not be in container1", container1.contains(thing));
        assertTrue("Thing should be in container2", container2.contains(thing));
    }

    @Test
    public void testInventoryProperty() {
        Object inventoryObj = container.rawget("Inventory");
        assertNotNull("Inventory should be accessible as property", inventoryObj);
        assertThat(inventoryObj).isSameAs(container.inventory);
    }

    @Test
    public void testContainerProperty() {
        Container parent = new Container();
        parent.rawset("Name", "Parent");
        
        container.moveTo(parent);
        
        Object containerProp = container.rawget("Container");
        assertThat(containerProp).isSameAs(parent);
    }

    @Test
    public void testEmptyInventory() {
        assertThat(container.inventory.len()).isEqualTo(0);
        
        Thing thing = new Thing(false);
        assertFalse("Empty container should not contain thing", 
                   container.contains(thing));
    }

    @Test
    public void testNestedContainers() {
        Container level1 = new Container();
        level1.rawset("Name", "Level1");
        
        Container level2 = new Container();
        level2.rawset("Name", "Level2");
        level2.moveTo(level1);
        
        Container level3 = new Container();
        level3.rawset("Name", "Level3");
        level3.moveTo(level2);
        
        Thing thing = new Thing(false);
        thing.rawset("Name", "DeepThing");
        thing.moveTo(level3);
        
        assertTrue("Level1 should contain deeply nested thing", level1.contains(thing));
        assertTrue("Level2 should contain nested thing", level2.contains(thing));
        assertTrue("Level3 should contain thing", level3.contains(thing));
    }

    // Helper method
    private boolean containsInInventory(Container cont, Thing thing) {
        Object key = null;
        while ((key = cont.inventory.next(key)) != null) {
            if (cont.inventory.rawget(key) == thing) {
                return true;
            }
        }
        return false;
    }
}
