package cgeo.geocaching.wherigo.openwig.formats;

import cgeo.geocaching.wherigo.openwig.platform.SeekableFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link CartridgeFile}'s binary GWC parsing/extraction against a synthetic cartridge
 * built by {@link GwcFixture}, embedding real sample image/sound/text resources (see
 * src/test/resources/media) to confirm that resource bytes round-trip byte-for-byte.
 */
public class CartridgeFileTest {

    private static byte[] loadSample(final String name) throws IOException {
        try (InputStream in = CartridgeFileTest.class.getResourceAsStream("/media/" + name)) {
            if (in == null) {
                throw new IOException("test resource not found: " + name);
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static CartridgeFile readFixture(final GwcFixture fixture) throws IOException {
        final SeekableFile seekable = fixture.toSeekableFile();
        return CartridgeFile.read(seekable, new NoopFileHandle());
    }

    @Test
    public void headerFieldsRoundTripThroughBinaryFormat() throws IOException {
        final GwcFixture fixture = new GwcFixture();
        fixture.latitude = 48.2082;
        fixture.longitude = 16.3738;
        fixture.splashId = 1;
        fixture.iconId = 2;
        fixture.type = "tarn";
        fixture.member = "Basic Member";
        fixture.name = "Test Cartridge";
        fixture.guid = "12345678-1234-1234-1234-123456789012";
        fixture.description = "A cartridge used only by unit tests.";
        fixture.startdesc = "Start here.";
        fixture.version = "1.0";
        fixture.author = "c:geo tests";
        fixture.url = "https://example.invalid/cartridge";
        fixture.device = "Generic";
        fixture.code = "ABC123";
        fixture.withBytecode(new byte[]{0x1b, 'L', 'u', 'a', 0x51});

        final CartridgeFile cf = readFixture(fixture);

        assertThat(cf.latitude).isEqualTo(48.2082);
        assertThat(cf.longitude).isEqualTo(16.3738);
        assertThat(cf.splashId).isEqualTo(1);
        assertThat(cf.iconId).isEqualTo(2);
        assertThat(cf.type).isEqualTo("tarn");
        assertThat(cf.member).isEqualTo("Basic Member");
        assertThat(cf.name).isEqualTo("Test Cartridge");
        assertThat(cf.description).isEqualTo("A cartridge used only by unit tests.");
        assertThat(cf.startdesc).isEqualTo("Start here.");
        assertThat(cf.version).isEqualTo("1.0");
        assertThat(cf.author).isEqualTo("c:geo tests");
        assertThat(cf.url).isEqualTo("https://example.invalid/cartridge");
        assertThat(cf.device).isEqualTo("Generic");
        assertThat(cf.code).isEqualTo("ABC123");
    }

    @Test
    public void emptyStringHeaderFieldsRoundTrip() throws IOException {
        // strings are null-terminated with no length prefix, so the empty string (an immediate
        // terminator byte) must parse just as well as a populated one
        final CartridgeFile cf = readFixture(new GwcFixture().withBytecode(new byte[]{0x01}));

        assertThat(cf.name).isEmpty();
        assertThat(cf.author).isEmpty();
        assertThat(cf.description).isEmpty();
    }

    @Test
    public void bytecodeIsReturnedByteForByte() throws IOException {
        final byte[] bytecode = new byte[]{0x1b, 'L', 'u', 'a', 'Q', 0x00, (byte) 0xff, 0x42};
        final GwcFixture fixture = new GwcFixture().withBytecode(bytecode);

        final CartridgeFile cf = readFixture(fixture);

        assertThat(cf.getBytecode()).isEqualTo(bytecode);
    }

    @Test
    public void imageResourceRoundTripsByteForByte() throws IOException {
        final byte[] png = loadSample("sample.png");
        final GwcFixture fixture = new GwcFixture()
            .withBytecode(new byte[]{0x01})
            .withResource(1, png);

        final CartridgeFile cf = readFixture(fixture);

        assertThat(cf.getFile(1)).isEqualTo(png);
    }

    @Test
    public void soundResourceRoundTripsByteForByte() throws IOException {
        final byte[] wav = loadSample("sample.wav");
        final GwcFixture fixture = new GwcFixture()
            .withBytecode(new byte[]{0x01})
            .withResource(1, wav);

        final CartridgeFile cf = readFixture(fixture);

        assertThat(cf.getFile(1)).isEqualTo(wav);
    }

    @Test
    public void textResourceRoundTripsByteForByte() throws IOException {
        final byte[] txt = loadSample("sample.txt");
        final GwcFixture fixture = new GwcFixture()
            .withBytecode(new byte[]{0x01})
            .withResource(1, txt);

        final CartridgeFile cf = readFixture(fixture);

        assertThat(cf.getFile(1)).isEqualTo(txt);
        assertThat(new String(cf.getFile(1), StandardCharsets.UTF_8))
            .contains("sample Wherigo cartridge text resource");
    }

    @Test
    public void multipleResourcesAreDistinguishedById() throws IOException {
        final byte[] png = loadSample("sample.png");
        final byte[] wav = loadSample("sample.wav");
        final byte[] txt = loadSample("sample.txt");
        final GwcFixture fixture = new GwcFixture()
            .withBytecode(new byte[]{0x01})
            .withResource(1, png)
            .withResource(2, wav)
            .withResource(3, txt);

        final CartridgeFile cf = readFixture(fixture);

        assertThat(cf.getFile(1)).isEqualTo(png);
        assertThat(cf.getFile(2)).isEqualTo(wav);
        assertThat(cf.getFile(3)).isEqualTo(txt);
    }

    @Test
    public void unknownResourceIdReturnsNull() throws IOException {
        final GwcFixture fixture = new GwcFixture()
            .withBytecode(new byte[]{0x01})
            .withResource(1, loadSample("sample.txt"));

        final CartridgeFile cf = readFixture(fixture);

        assertThat(cf.getFile(999)).isNull();
    }

    @Test
    public void nonPositiveResourceIdReturnsNull() throws IOException {
        final CartridgeFile cf = readFixture(new GwcFixture().withBytecode(new byte[]{0x01}));

        assertThat(cf.getFile(0)).isNull();
    }

    @Test
    public void invalidSignatureIsRejected() {
        final SeekableFile bogus = GwcFixture.fromRawBytes(
            new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06});

        assertThatThrownBy(() -> CartridgeFile.read(bogus, new NoopFileHandle()))
            .isInstanceOf(IOException.class);
    }
}
