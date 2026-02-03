package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for Task class.
 */
public class TaskTest {

    private Task task;

    @Before
    public void setUp() {
        task = new Task();
    }

    @Test
    public void testTaskInitialization() {
        assertNotNull("Task should be initialized", task);
    }

    @Test
    public void testLuaToString() {
        String result = task.luaTostring();
        assertThat(result).isEqualTo("a ZTask instance");
    }

    @Test
    public void testTaskWithName() {
        task.rawset("Name", "Find the treasure");
        assertThat(task.rawget("Name")).isEqualTo("Find the treasure");
    }

    @Test
    public void testTaskWithDescription() {
        task.rawset("Description", "You must find the hidden treasure");
        assertThat(task.rawget("Description")).isEqualTo("You must find the hidden treasure");
    }

    @Test
    public void testTaskVisibility() {
        task.rawset("Visible", true);
        assertTrue("Task should be visible", task.isVisible());
        
        task.rawset("Visible", false);
        assertFalse("Task should not be visible", task.isVisible());
    }

    @Test
    public void testTaskComplete() {
        task.rawset("Complete", true);
        assertThat(task.rawget("Complete")).isEqualTo(true);
        
        task.rawset("Complete", false);
        assertThat(task.rawget("Complete")).isEqualTo(false);
    }

    @Test
    public void testTaskCorrectState() {
        task.rawset("CorrectState", "Completed");
        assertThat(task.rawget("CorrectState")).isEqualTo("Completed");
    }

    @Test
    public void testTaskWithIcon() {
        Media icon = new Media();
        icon.rawset("Name", "TaskIcon");
        task.rawset("Icon", icon);
        
        assertThat(task.rawget("Icon")).isSameAs(icon);
    }

    @Test
    public void testTaskWithMedia() {
        Media media = new Media();
        media.rawset("Name", "TaskMedia");
        task.rawset("Media", media);
        
        assertThat(task.rawget("Media")).isSameAs(media);
    }

    @Test
    public void testMultipleTasks() {
        Task task1 = new Task();
        task1.rawset("Name", "Task1");
        
        Task task2 = new Task();
        task2.rawset("Name", "Task2");
        
        assertThat(task1.rawget("Name")).isEqualTo("Task1");
        assertThat(task2.rawget("Name")).isEqualTo("Task2");
    }

    @Test
    public void testTaskStates() {
        task.rawset("CorrectState", "Found");
        assertThat(task.rawget("CorrectState")).isEqualTo("Found");
        
        task.rawset("CorrectState", "NotFound");
        assertThat(task.rawget("CorrectState")).isEqualTo("NotFound");
    }
}
