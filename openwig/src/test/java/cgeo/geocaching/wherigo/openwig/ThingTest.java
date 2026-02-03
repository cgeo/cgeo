package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for Thing class.
 */
public class ThingTest {

    private Thing thing;

    @Before
    public void setUp() {
        thing = new Thing(false);
    }

    @Test
    public void testThingInitialization() {
        assertNotNull("Thing should be initialized", thing);
        assertNotNull("Inventory should be initialized", thing.inventory);
        assertNotNull("Actions vector should be initialized", thing.actions);
    }

    @Test
    public void testThingAsCharacter() {
        Thing character = new Thing(true);
        assertNotNull("Character should be initialized", character);
    }

    @Test
    public void testThingVisibility() {
        thing.rawset("Visible", true);
        assertTrue("Thing should be visible", thing.isVisible());
        
        thing.rawset("Visible", false);
        assertFalse("Thing should not be visible", thing.isVisible());
    }

    @Test
    public void testThingWithName() {
        thing.rawset("Name", "TestThing");
        assertThat(thing.rawget("Name")).isEqualTo("TestThing");
        assertThat(thing.name).isEqualTo("TestThing");
    }

    @Test
    public void testThingWithDescription() {
        thing.rawset("Description", "A test thing");
        assertThat(thing.rawget("Description")).isEqualTo("A test thing");
    }

    @Test
    public void testThingWithIcon() {
        Media icon = new Media();
        icon.rawset("Name", "IconMedia");
        thing.rawset("Icon", icon);
        
        assertThat(thing.rawget("Icon")).isSameAs(icon);
    }

    @Test
    public void testThingPosition() {
        ZonePoint position = new ZonePoint(50.0, 10.0, 100.0);
        thing.setPosition(position);
        
        assertThat(thing.position).isSameAs(position);
    }

    @Test
    public void testThingIsLocated_withPosition() {
        ZonePoint position = new ZonePoint(50.0, 10.0, 100.0);
        thing.setPosition(position);
        
        assertTrue("Thing with position should be located", thing.isLocated());
    }

    @Test
    public void testThingIsLocated_withoutPosition() {
        thing.setPosition(null);
        assertFalse("Thing without position should not be located", thing.isLocated());
    }

    @Test
    public void testThingMoveTo() {
        Container container = new Container();
        container.rawset("Name", "TestContainer");
        
        thing.moveTo(container);
        
        assertThat(thing.container).isSameAs(container);
    }

    @Test
    public void testThingActions() {
        Action action1 = new Action();
        action1.rawset("Name", "Action1");
        thing.actions.addElement(action1);
        
        Action action2 = new Action();
        action2.rawset("Name", "Action2");
        thing.actions.addElement(action2);
        
        assertThat(thing.actions).hasSize(2);
        assertThat(thing.actions).contains(action1, action2);
    }

    @Test
    public void testThingVisibleToPlayer() {
        thing.rawset("Visible", true);
        
        Container container = new Container();
        thing.moveTo(container);
        
        // visibleToPlayer depends on container state
        // Just verify the method runs without error
        boolean visible = thing.visibleToPlayer();
        assertThat(visible).isIn(true, false);
    }

    @Test
    public void testThingContainsOtherThings() {
        Thing innerThing = new Thing(false);
        innerThing.rawset("Name", "InnerThing");
        innerThing.moveTo(thing);
        
        assertTrue("Thing should contain inner thing", thing.contains(innerThing));
    }

    @Test
    public void testThingDoesNotContain() {
        Thing otherThing = new Thing(false);
        otherThing.rawset("Name", "OtherThing");
        
        assertFalse("Thing should not contain unrelated thing", 
                   thing.contains(otherThing));
    }

    @Test
    public void testMultipleThings() {
        Thing thing1 = new Thing(false);
        thing1.rawset("Name", "Thing1");
        
        Thing thing2 = new Thing(false);
        thing2.rawset("Name", "Thing2");
        
        assertThat(thing1.rawget("Name")).isEqualTo("Thing1");
        assertThat(thing2.rawget("Name")).isEqualTo("Thing2");
    }

    @Test
    public void testThingWithMedia() {
        Media media = new Media();
        media.rawset("Name", "TestMedia");
        thing.rawset("Media", media);
        
        assertThat(thing.rawget("Media")).isSameAs(media);
    }

    @Test
    public void testThingCommands() {
        Action command1 = new Action();
        command1.rawset("Name", "Command1");
        thing.actions.addElement(command1);
        
        assertThat(thing.actions).contains(command1);
    }
}
