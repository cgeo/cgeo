package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for Timer class.
 */
public class TimerTest {

    private Timer timer;

    @Before
    public void setUp() {
        timer = new Timer();
    }

    @Test
    public void testTimerInitialization() {
        assertNotNull("Timer should be initialized", timer);
        assertNotNull("Start function should be registered", timer.rawget("Start"));
        assertNotNull("Stop function should be registered", timer.rawget("Stop"));
        assertNotNull("Tick function should be registered", timer.rawget("Tick"));
    }

    @Test
    public void testLuaToString() {
        String result = timer.luaTostring();
        assertThat(result).isEqualTo("a ZTimer instance");
    }

    @Test
    public void testTimerType_countdown() {
        timer.rawset("Type", "Countdown");
        Object type = timer.rawget("Type");
        assertThat(type).isEqualTo("Countdown");
    }

    @Test
    public void testTimerType_interval() {
        timer.rawset("Type", "Interval");
        Object type = timer.rawget("Type");
        assertThat(type).isEqualTo("Interval");
    }

    @Test
    public void testTimerDuration() {
        timer.rawset("Duration", 60.0); // 60 seconds
        
        Object remaining = timer.rawget("Remaining");
        assertNotNull("Remaining should be set when Duration is set", remaining);
    }

    @Test
    public void testTimerName() {
        timer.rawset("Name", "TestTimer");
        assertThat(timer.rawget("Name")).isEqualTo("TestTimer");
    }

    @Test
    public void testTimerDescription() {
        timer.rawset("Description", "Test timer description");
        assertThat(timer.rawget("Description")).isEqualTo("Test timer description");
    }

    @Test
    public void testMultipleTimers() {
        Timer timer1 = new Timer();
        timer1.rawset("Name", "Timer1");
        
        Timer timer2 = new Timer();
        timer2.rawset("Name", "Timer2");
        
        assertThat(timer1.rawget("Name")).isEqualTo("Timer1");
        assertThat(timer2.rawget("Name")).isEqualTo("Timer2");
    }

    @Test
    public void testTimerWithZeroRemaining() {
        timer.rawset("Duration", 0.0);
        Object remaining = timer.rawget("Remaining");
        assertNotNull("Remaining should be set even for zero duration", remaining);
    }
}
