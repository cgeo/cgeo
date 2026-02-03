package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

/**
 * Test suite for Action class.
 */
public class ActionTest {

    private Action action;

    @Before
    public void setUp() {
        action = new Action();
    }

    @Test
    public void testActionInitialization() {
        assertNotNull("Action should be initialized", action);
        assertNotNull("Targets vector should be initialized", action.targets);
    }

    @Test
    public void testLuaToString() {
        String result = action.luaTostring();
        assertThat(result).isEqualTo("a ZCommand instance");
    }

    @Test
    public void testActionWithName() {
        action.rawset("Name", "TestAction");
        assertThat(action.rawget("Name")).isEqualTo("TestAction");
    }

    @Test
    public void testActionEnabled() {
        action.rawset("Enabled", true);
        assertTrue("Action should be enabled", action.isEnabled());
        
        action.rawset("Enabled", false);
        assertFalse("Action should be disabled", action.isEnabled());
    }

    @Test
    public void testActionWithActor() {
        Thing actor = new Thing(true); // Character
        actor.rawset("Name", "Actor");
        action.rawset("Actor", actor);
        
        assertThat(action.getActor()).isSameAs(actor);
    }

    @Test
    public void testActionTargets() {
        Thing target1 = new Thing(false);
        target1.rawset("Name", "Target1");
        
        Thing target2 = new Thing(false);
        target2.rawset("Name", "Target2");
        
        action.targets.addElement(target1);
        action.targets.addElement(target2);
        
        assertThat(action.targets).hasSize(2);
        assertThat(action.targets).contains(target1, target2);
    }

    @Test
    public void testActionWithWork() {
        action.rawset("Work", "This is the action work description");
        assertThat(action.rawget("Work")).isEqualTo("This is the action work description");
    }

    @Test
    public void testActionWithEmptyTargets() {
        assertThat(action.targets).isEmpty();
    }

    @Test
    public void testMultipleActions() {
        Action action1 = new Action();
        action1.rawset("Name", "Action1");
        
        Action action2 = new Action();
        action2.rawset("Name", "Action2");
        
        assertThat(action1.rawget("Name")).isEqualTo("Action1");
        assertThat(action2.rawget("Name")).isEqualTo("Action2");
    }

    @Test
    public void testActionVisibleTargets() {
        Container container = new Container();
        
        Thing target1 = new Thing(false);
        target1.rawset("Name", "VisibleTarget");
        target1.rawset("Visible", true);
        target1.moveTo(container);
        
        Thing target2 = new Thing(false);
        target2.rawset("Name", "InvisibleTarget");
        target2.rawset("Visible", false);
        target2.moveTo(container);
        
        action.targets.addElement(target1);
        action.targets.addElement(target2);
        
        int visibleCount = action.visibleTargets(container);
        assertThat(visibleCount).isEqualTo(1);
    }

    @Test
    public void testActionWithTable() {
        cgeo.geocaching.wherigo.kahlua.vm.LuaTable table = 
            new cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl();
        table.rawset("Name", "TableAction");
        table.rawset("Enabled", true);
        
        Action actionFromTable = new Action(table);
        
        assertNotNull("Action should be created from table", actionFromTable);
    }

    @Test
    public void testActionIsUniversal() {
        action.rawset("Universal", true);
        assertTrue("Action should be universal", action.isUniversal());
        
        action.rawset("Universal", false);
        assertFalse("Action should not be universal", action.isUniversal());
    }

    @Test
    public void testActionHasParameter() {
        action.targets.addElement(new Thing(false));
        assertTrue("Action with targets should have parameter", action.hasParameter());
        
        Action emptyAction = new Action();
        assertFalse("Action without targets should not have parameter", 
                   emptyAction.hasParameter());
    }
}
