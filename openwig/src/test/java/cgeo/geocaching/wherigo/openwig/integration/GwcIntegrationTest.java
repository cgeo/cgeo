package cgeo.geocaching.wherigo.openwig.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.EventTable;
import cgeo.geocaching.wherigo.openwig.Media;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import cgeo.geocaching.wherigo.openwig.platform.FileHandle;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;
import cgeo.geocaching.wherigo.openwig.platform.SeekableFile;
import cgeo.geocaching.wherigo.openwig.platform.UI;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Integration test for loading and initializing GWC (Groundspeak Wherigo Cartridge) files.
 * 
 * This test validates the complete load and initialization workflow:
 * 1. Creating/loading a test GWC file (simulating download)
 * 2. Parsing the GWC file format and extracting metadata
 * 3. Initializing the OpenWig engine with the cartridge
 * 
 * Note: This test focuses on load/initialization rather than full execution because
 * full execution requires stdlib.lbc and Android runtime environment.
 */
public class GwcIntegrationTest {

    private TestUI testUI;
    private TestLocationService testGPS;
    private ByteArrayOutputStream logStream;

    @Before
    public void setUp() {
        testUI = new TestUI();
        testGPS = new TestLocationService();
        logStream = new ByteArrayOutputStream();
    }

    /**
     * Test the workflow of loading a GWC cartridge file.
     * This integration test verifies:
     * - GWC file can be successfully loaded
     * - Cartridge metadata is correctly parsed
     * - OpenWig engine can be initialized with the cartridge
     * - Bytecode can be extracted from the cartridge
     */
    @Test
    public void testLoadGwcCartridge() throws Exception {
        // Create a minimal test GWC cartridge file
        TestSeekableFile testGwcFile = createMinimalTestGwcFile();
        TestFileHandle testSaveFile = new TestFileHandle();
        
        // Load the cartridge file - this tests the download/load workflow
        CartridgeFile cartridgeFile = CartridgeFile.read(testGwcFile, testSaveFile);
        assertNotNull("CartridgeFile should be loaded successfully", cartridgeFile);
        
        // Verify cartridge metadata was correctly parsed
        assertThat(cartridgeFile.name).isEqualTo("Test Cartridge");
        assertThat(cartridgeFile.author).isEqualTo("Test Author");
        assertThat(cartridgeFile.version).isEqualTo("1.0");
        assertThat(cartridgeFile.type).isEqualTo("Tour");
        assertThat(cartridgeFile.description).isEqualTo("Test Description");
        assertThat(cartridgeFile.code).isEqualTo("TESTCODE123");
        assertThat(cartridgeFile.latitude).isEqualTo(47.6062);
        assertThat(cartridgeFile.longitude).isEqualTo(-122.3321);
        
        // Verify bytecode can be extracted
        byte[] bytecode = cartridgeFile.getBytecode();
        assertNotNull("Bytecode should be extracted successfully", bytecode);
        assertThat(bytecode.length).isGreaterThan(0);
        
        // Verify it's valid Lua bytecode by checking the header
        assertThat(bytecode[0]).isEqualTo((byte) 0x1B);
        assertThat(bytecode[1]).isEqualTo((byte) 'L');
        assertThat(bytecode[2]).isEqualTo((byte) 'u');
        assertThat(bytecode[3]).isEqualTo((byte) 'a');
        
        // Create engine with the cartridge - tests initialization workflow
        Engine engine = new Engine(cartridgeFile, logStream, testUI, testGPS);
        assertNotNull("Engine should be created successfully", engine);
        
        // Verify engine properties are set correctly
        assertThat(engine.gwcfile).isSameAs(cartridgeFile);
        assertThat(engine.uiInstance).isSameAs(testUI);
        assertThat(engine.gpsInstance).isSameAs(testGPS);
        assertNotNull("Player should be initialized", engine.player);
        
        // Verify savegame is accessible
        assertNotNull("Savegame should be initialized", engine.savegame);
    }
    
    /**
     * Test multiple cartridges can be loaded independently.
     * This verifies the integration workflow supports loading multiple GWC files.
     */
    @Test
    public void testLoadMultipleGwcCartridges() throws Exception {
        // Create first cartridge
        TestSeekableFile testGwcFile1 = createMinimalTestGwcFile();
        TestFileHandle testSaveFile1 = new TestFileHandle();
        CartridgeFile cartridge1 = CartridgeFile.read(testGwcFile1, testSaveFile1);
        
        // Create second cartridge with different data
        TestSeekableFile testGwcFile2 = createCustomTestGwcFile("Another Cartridge", "Another Author");
        TestFileHandle testSaveFile2 = new TestFileHandle();
        CartridgeFile cartridge2 = CartridgeFile.read(testGwcFile2, testSaveFile2);
        
        // Verify both cartridges are loaded correctly
        assertNotNull("First cartridge should be loaded", cartridge1);
        assertNotNull("Second cartridge should be loaded", cartridge2);
        
        assertThat(cartridge1.name).isEqualTo("Test Cartridge");
        assertThat(cartridge2.name).isEqualTo("Another Cartridge");
        
        assertThat(cartridge1.author).isEqualTo("Test Author");
        assertThat(cartridge2.author).isEqualTo("Another Author");
        
        // Verify both can initialize engines
        Engine engine1 = new Engine(cartridge1, new ByteArrayOutputStream(), new TestUI(), new TestLocationService());
        Engine engine2 = new Engine(cartridge2, new ByteArrayOutputStream(), new TestUI(), new TestLocationService());
        
        assertNotNull("First engine should be created", engine1);
        assertNotNull("Second engine should be created", engine2);
        
        assertThat(engine1.gwcfile).isSameAs(cartridge1);
        assertThat(engine2.gwcfile).isSameAs(cartridge2);
    }

    /**
     * Creates a minimal valid GWC file for testing purposes.
     * 
     * The GWC file format consists of:
     * - 7-byte header signature: 02 0a CART 00
     * - 2-byte file count (number of embedded files)
     * - File table (2-byte id + 4-byte offset for each file)
     * - Cartridge metadata header
     * - Lua bytecode (file 0)
     */
    private TestSeekableFile createMinimalTestGwcFile() throws IOException {
        return createCustomTestGwcFile("Test Cartridge", "Test Author");
    }
    
    /**
     * Creates a custom test GWC file with specified name and author.
     */
    private TestSeekableFile createCustomTestGwcFile(String cartridgeName, String authorName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Write GWC header signature
        baos.write(new byte[] { 0x02, 0x0a, 0x43, 0x41, 0x52, 0x54, 0x00 });
        
        // Write file count (1 file - the bytecode)
        writeShort(baos, (short) 1);
        
        // File table entry for bytecode (id=0)
        // The offset field starts at position: 7 (header) + 2 (file count) + 2 (id) = 11
        writeShort(baos, (short) 0); // File ID (position 9-10)
        int bytecodeOffsetPosition = baos.size(); // Save position before writing offset (position 11)
        writeInt(baos, 0); // Temporary offset placeholder, will update after calculating actual position
        
        // Build metadata header
        ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
        
        // Latitude and longitude
        writeDouble(headerStream, 47.6062);  // Seattle latitude
        writeDouble(headerStream, -122.3321); // Seattle longitude
        
        // 8 bytes of zeroes
        writeDouble(headerStream, 0.0);
        
        // Unknown long values (8 bytes)
        writeLong(headerStream, 0L);
        
        // Splash and icon IDs
        writeShort(headerStream, (short) 0);
        writeShort(headerStream, (short) 0);
        
        // Strings
        writeString(headerStream, "Tour"); // type
        writeString(headerStream, "TestPlayer"); // member
        
        // More unknown values (8 bytes)
        writeLong(headerStream, 0L);
        
        writeString(headerStream, cartridgeName); // name
        writeString(headerStream, "{12345678-1234-1234-1234-123456789012}"); // GUID
        writeString(headerStream, "Test Description"); // description
        writeString(headerStream, "Test Start Description"); // startdesc
        writeString(headerStream, "1.0"); // version
        writeString(headerStream, authorName); // author
        writeString(headerStream, "http://test.example.com"); // url
        writeString(headerStream, "TestDevice"); // device
        
        // Unknown value (4 bytes)
        writeInt(headerStream, 0);
        
        writeString(headerStream, "TESTCODE123"); // completion code
        
        byte[] headerData = headerStream.toByteArray();
        
        // Write header length
        writeInt(baos, headerData.length);
        
        // Write header data
        baos.write(headerData);
        
        // Now write the bytecode file
        int bytecodeOffset = baos.size();
        
        // Create minimal Lua bytecode
        byte[] bytecode = createMinimalLuaBytecode();
        
        // Write bytecode length and data
        writeInt(baos, bytecode.length);
        baos.write(bytecode);
        
        // Get final data
        byte[] gwcData = baos.toByteArray();
        
        // Update bytecode offset in file table
        ByteBuffer buffer = ByteBuffer.wrap(gwcData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(bytecodeOffsetPosition, bytecodeOffset);
        
        return new TestSeekableFile(gwcData);
    }

    /**
     * Creates minimal Lua bytecode that does nothing but is valid.
     * This is a simple empty function that returns immediately.
     */
    private byte[] createMinimalLuaBytecode() {
        // This is a minimal valid Lua 5.1 bytecode chunk
        // It represents an empty function: function() end
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            // Lua signature
            baos.write(0x1B);
            baos.write('L');
            baos.write('u');
            baos.write('a');
            
            // Version (5.1)
            baos.write(0x51);
            
            // Format version
            baos.write(0x00);
            
            // Endianness (little)
            baos.write(0x01);
            
            // Size of int
            baos.write(0x04);
            
            // Size of size_t
            baos.write(0x04);
            
            // Size of instruction
            baos.write(0x04);
            
            // Size of lua_Number
            baos.write(0x08);
            
            // Integral flag
            baos.write(0x00);
            
            // Function header
            writeLuaString(baos, ""); // source name
            writeInt(baos, 0); // line defined
            writeInt(baos, 0); // last line defined
            baos.write(0); // number of upvalues
            baos.write(0); // number of parameters
            baos.write(0); // is vararg
            baos.write(2); // max stack size
            
            // Code section - just a RETURN instruction
            writeInt(baos, 1); // number of instructions
            writeInt(baos, 0x00000026); // RETURN 0 0 (opcode 0x26)
            
            // Constants section
            writeInt(baos, 0); // no constants
            
            // Functions section
            writeInt(baos, 0); // no functions
            
            // Source line positions
            writeInt(baos, 0); // no line info
            
            // Locals
            writeInt(baos, 0); // no locals
            
            // Upvalues
            writeInt(baos, 0); // no upvalues
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Lua bytecode", e);
        }
        
        return baos.toByteArray();
    }

    // Helper methods for writing binary data in little-endian format
    
    private void writeShort(ByteArrayOutputStream out, short value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }
    
    private void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }
    
    private void writeLong(ByteArrayOutputStream out, long value) {
        writeInt(out, (int) (value & 0xFFFFFFFFL));
        writeInt(out, (int) ((value >> 32) & 0xFFFFFFFFL));
    }
    
    private void writeDouble(ByteArrayOutputStream out, double value) {
        long bits = Double.doubleToLongBits(value);
        writeLong(out, bits);
    }
    
    private void writeString(ByteArrayOutputStream out, String str) {
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
        out.write(0); // null terminator
    }
    
    /**
     * Writes a string in Lua bytecode format (size-prefixed).
     * For Lua 5.1, strings are size_t length followed by characters (no null terminator).
     */
    private void writeLuaString(ByteArrayOutputStream out, String str) {
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // For empty string, Lua uses size 0
        // For non-empty strings, Lua uses size + 1 (includes implicit null)
        writeInt(out, bytes.length == 0 ? 0 : bytes.length + 1);
        if (bytes.length > 0) {
            out.write(bytes, 0, bytes.length);
        }
    }

    // Test implementation classes
    
    /**
     * Test implementation of SeekableFile that reads from a byte array.
     */
    private static class TestSeekableFile implements SeekableFile {
        private final byte[] data;
        private int position;
        
        public TestSeekableFile(byte[] data) {
            this.data = data;
            this.position = 0;
        }
        
        @Override
        public void seek(long pos) throws IOException {
            if (pos < 0 || pos > data.length) {
                throw new IOException("Invalid seek position: " + pos);
            }
            this.position = (int) pos;
        }
        
        @Override
        public long position() throws IOException {
            return position;
        }
        
        @Override
        public long skip(long what) throws IOException {
            long newPos = position + what;
            if (newPos > data.length) {
                newPos = data.length;
            }
            long skipped = newPos - position;
            position = (int) newPos;
            return skipped;
        }
        
        @Override
        public short readShort() throws IOException {
            if (position + 2 > data.length) {
                throw new IOException("Not enough data for short");
            }
            int b1 = data[position++] & 0xFF;
            int b2 = data[position++] & 0xFF;
            return (short) (b1 | (b2 << 8));
        }
        
        @Override
        public int readInt() throws IOException {
            if (position + 4 > data.length) {
                throw new IOException("Not enough data for int");
            }
            int b1 = data[position++] & 0xFF;
            int b2 = data[position++] & 0xFF;
            int b3 = data[position++] & 0xFF;
            int b4 = data[position++] & 0xFF;
            return b1 | (b2 << 8) | (b3 << 16) | (b4 << 24);
        }
        
        @Override
        public double readDouble() throws IOException {
            long bits = readLong();
            return Double.longBitsToDouble(bits);
        }
        
        @Override
        public long readLong() throws IOException {
            long low = readInt() & 0xFFFFFFFFL;
            long high = readInt() & 0xFFFFFFFFL;
            return low | (high << 32);
        }
        
        @Override
        public void readFully(byte[] buf) throws IOException {
            if (position + buf.length > data.length) {
                throw new IOException("Not enough data to fill buffer");
            }
            System.arraycopy(data, position, buf, 0, buf.length);
            position += buf.length;
        }
        
        @Override
        public String readString() throws IOException {
            int start = position;
            while (position < data.length && data[position] != 0) {
                position++;
            }
            if (position >= data.length) {
                throw new IOException("String not null-terminated");
            }
            String result = new String(data, start, position - start, java.nio.charset.StandardCharsets.UTF_8);
            position++; // skip null terminator
            return result;
        }
        
        @Override
        public int read() throws IOException {
            if (position >= data.length) {
                return -1;
            }
            return data[position++] & 0xFF;
        }
    }
    
    /**
     * Test implementation of FileHandle for save game files.
     * Uses a custom DataOutputStream wrapper that writes to an internal buffer.
     */
    private static class TestFileHandle implements FileHandle {
        private byte[] data;
        private boolean exists;
        
        public TestFileHandle() {
            this.data = new byte[0];
            this.exists = false;
        }
        
        @Override
        public DataInputStream openDataInputStream() throws IOException {
            if (!exists) {
                throw new IOException("File does not exist");
            }
            return new DataInputStream(new ByteArrayInputStream(data));
        }
        
        @Override
        public DataOutputStream openDataOutputStream() throws IOException {
            // Create a ByteArrayOutputStream that captures data when closed
            ByteArrayOutputStream baos = new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    super.close();
                    // Capture the data when the stream is closed
                    TestFileHandle.this.data = this.toByteArray();
                }
            };
            return new DataOutputStream(baos);
        }
        
        @Override
        public boolean exists() throws IOException {
            return exists;
        }
        
        @Override
        public void create() throws IOException {
            if (exists) {
                throw new IOException("File already exists");
            }
            exists = true;
            data = new byte[0];
        }
        
        @Override
        public void delete() throws IOException {
            if (!exists) {
                throw new IOException("File does not exist");
            }
            exists = false;
            data = new byte[0];
        }
        
        @Override
        public void truncate(long len) throws IOException {
            if (!exists) {
                throw new IOException("File does not exist");
            }
            // For testing purposes, truncate to empty
            data = new byte[0];
        }
    }
    
    /**
     * Test implementation of UI interface that tracks method calls.
     */
    private static class TestUI implements UI {
        private final AtomicBoolean started = new AtomicBoolean(false);
        private final AtomicBoolean ended = new AtomicBoolean(false);
        private final AtomicBoolean hasErrors = new AtomicBoolean(false);
        private Runnable onStart;
        private Runnable onEnd;
        
        public void setOnStart(Runnable callback) {
            this.onStart = callback;
        }
        
        public void setOnEnd(Runnable callback) {
            this.onEnd = callback;
        }
        
        public boolean wasStarted() {
            return started.get();
        }
        
        public boolean wasEnded() {
            return ended.get();
        }
        
        public boolean hasErrors() {
            return hasErrors.get();
        }
        
        @Override
        public void start() {
            started.set(true);
            if (onStart != null) {
                onStart.run();
            }
        }
        
        @Override
        public void end() {
            ended.set(true);
            if (onEnd != null) {
                onEnd.run();
            }
        }
        
        @Override
        public void refresh() {
            // No-op for testing
        }
        
        @Override
        public void showError(String msg) {
            hasErrors.set(true);
            System.err.println("UI Error: " + msg);
        }
        
        @Override
        public void debugMsg(String msg) {
            System.out.println("UI Debug: " + msg);
        }
        
        @Override
        public void setStatusText(String text) {
            // No-op for testing
        }
        
        @Override
        public void pushDialog(String[] texts, Media[] media, String button1, 
                              String button2, LuaClosure callback) {
            // No-op for testing
        }
        
        @Override
        public void pushInput(EventTable input) {
            // No-op for testing
        }
        
        @Override
        public void showScreen(int screenId, EventTable details) {
            // No-op for testing
        }
        
        @Override
        public void playSound(byte[] data, String mime) {
            // No-op for testing
        }
        
        @Override
        public void blockForSaving() {
            // No-op for testing
        }
        
        @Override
        public void unblock() {
            // No-op for testing
        }
        
        @Override
        public void command(String cmd) {
            // No-op for testing
        }
    }
    
    /**
     * Test implementation of LocationService with fixed coordinates.
     */
    private static class TestLocationService implements LocationService {
        private double latitude = 47.6062;   // Seattle
        private double longitude = -122.3321;
        private double altitude = 0.0;
        
        @Override
        public double getLatitude() {
            return latitude;
        }
        
        @Override
        public double getLongitude() {
            return longitude;
        }
        
        @Override
        public double getAltitude() {
            return altitude;
        }
        
        @Override
        public double getPrecision() {
            return 10.0;
        }
    }
}
