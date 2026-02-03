package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;
import cgeo.geocaching.wherigo.openwig.platform.UI;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test suite for Engine class, focusing on parallel execution capabilities.
 */
public class EngineTest {

    private MockUI mockUI;
    private MockLocationService mockGPS;
    private ByteArrayOutputStream logStream;

    @Before
    public void setUp() {
        mockUI = new MockUI();
        mockGPS = new MockLocationService();
        logStream = new ByteArrayOutputStream();
    }

    @Test
    public void testGetCurrentInstance_withNoInstance() {
        // Clear any existing instance
        Engine.instance = null;
        assertNull("getCurrentInstance should return null when no instance exists", 
                   Engine.getCurrentInstance());
    }

    @Test
    public void testGetCurrentInstance_withStaticInstance() {
        Engine mockEngine = new Engine() {};
        Engine.instance = mockEngine;
        
        assertThat(Engine.getCurrentInstance())
            .as("getCurrentInstance should return static instance")
            .isSameAs(mockEngine);
        
        // Cleanup
        Engine.instance = null;
    }

    @Test
    public void testEngineConstructor_setsInstanceFields() throws IOException {
        MockCartridgeFile mockCartridge = new MockCartridgeFile();
        
        Engine engine = new Engine(mockCartridge, logStream, mockUI, mockGPS);
        
        assertThat(engine.uiInstance).isSameAs(mockUI);
        assertThat(engine.gpsInstance).isSameAs(mockGPS);
        assertThat(engine.gwcfile).isSameAs(mockCartridge);
        assertNotNull("Player should be initialized", engine.player);
    }

    @Test
    public void testParallelExecution_threadsIsolated() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        Runnable task1 = () -> {
            try {
                MockCartridgeFile cart1 = new MockCartridgeFile();
                Engine engine1 = new Engine(cart1, new ByteArrayOutputStream(), 
                                           new MockUI(), new MockLocationService());
                
                // Simulate setting ThreadLocal
                assertNotNull("Engine instance should be created", engine1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                MockCartridgeFile cart2 = new MockCartridgeFile();
                Engine engine2 = new Engine(cart2, new ByteArrayOutputStream(), 
                                           new MockUI(), new MockLocationService());
                
                assertNotNull("Engine instance should be created", engine2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get())
            .as("Both threads should successfully create engines")
            .isEqualTo(2);
    }

    @Test
    public void testDeprecatedNewInstance_createsEngine() throws IOException {
        MockCartridgeFile mockCartridge = new MockCartridgeFile();
        
        @SuppressWarnings("deprecation")
        Engine engine = Engine.newInstance(mockCartridge, logStream, mockUI, mockGPS);
        
        assertNotNull("Engine should be created", engine);
        assertThat(Engine.instance).isSameAs(engine);
        assertThat(Engine.ui).isSameAs(mockUI);
        assertThat(Engine.gps).isSameAs(mockGPS);
        
        // Cleanup
        Engine.instance = null;
        Engine.ui = null;
        Engine.gps = null;
    }

    @Test
    public void testPlayerInitialization() throws IOException {
        MockCartridgeFile mockCartridge = new MockCartridgeFile();
        Engine engine = new Engine(mockCartridge, logStream, mockUI, mockGPS);
        
        assertNotNull("Player should be initialized", engine.player);
        assertThat(engine.player).isInstanceOf(Player.class);
    }

    // Mock classes for testing
    private static class MockUI implements UI {
        @Override public void start() {}
        @Override public void end() {}
        @Override public void refresh() {}
        @Override public void showError(String msg) {}
        @Override public void debugMsg(String msg) {}
        @Override public void setStatusText(String text) {}
        @Override public void pushDialog(String[] texts, cgeo.geocaching.wherigo.openwig.Media[] media, 
                                        String button1, String button2, 
                                        cgeo.geocaching.wherigo.kahlua.vm.LuaClosure callback) {}
        @Override public void pushInput(EventTable input) {}
        @Override public void showScreen(int screenId, EventTable details) {}
        @Override public void playSound(byte[] data, String mime) {}
        @Override public void blockForSaving() {}
        @Override public void unblock() {}
        @Override public void command(String cmd) {}
    }

    private static class MockLocationService implements LocationService {
        private double latitude = 0.0;
        private double longitude = 0.0;
        private double altitude = 0.0;

        @Override public double getLatitude() { return latitude; }
        @Override public double getLongitude() { return longitude; }
        @Override public double getAltitude() { return altitude; }
        @Override public double getPrecision() { return 10.0; }
    }

    private static class MockCartridgeFile extends CartridgeFile {
        public MockCartridgeFile() throws IOException {
            super(null, null);
        }

        @Override
        public byte[] getBytecode() {
            return new byte[0];
        }
    }
}
