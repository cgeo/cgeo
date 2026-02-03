package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for Media class.
 */
public class MediaTest {

    private Media media;

    @Before
    public void setUp() {
        media = new Media();
    }

    @Test
    public void testMediaInitialization() {
        assertNotNull("Media should be initialized", media);
    }

    @Test
    public void testLuaToString() {
        String result = media.luaTostring();
        assertThat(result).isEqualTo("a ZMedia instance");
    }

    @Test
    public void testMediaWithName() {
        media.rawset("Name", "TestMedia");
        assertThat(media.rawget("Name")).isEqualTo("TestMedia");
    }

    @Test
    public void testMediaWithDescription() {
        media.rawset("Description", "A test media file");
        assertThat(media.rawget("Description")).isEqualTo("A test media file");
    }

    @Test
    public void testMediaType() {
        media.rawset("Type", "image");
        assertThat(media.type).isEqualTo("image");
        
        media.rawset("Type", "audio");
        assertThat(media.type).isEqualTo("audio");
    }

    @Test
    public void testMediaId() {
        media.id = 42;
        assertThat(media.id).isEqualTo(42);
    }

    @Test
    public void testMediaWithFilename() {
        media.rawset("Filename", "image.jpg");
        assertThat(media.rawget("Filename")).isEqualTo("image.jpg");
    }

    @Test
    public void testMediaResources() {
        // Test common media types
        String[] mediaTypes = {"jpg", "png", "gif", "mp3", "wav"};
        
        for (String type : mediaTypes) {
            Media m = new Media();
            m.rawset("Type", type);
            assertThat(m.type).isEqualTo(type);
        }
    }

    @Test
    public void testMultipleMedia() {
        Media media1 = new Media();
        media1.rawset("Name", "Media1");
        media1.id = 1;
        
        Media media2 = new Media();
        media2.rawset("Name", "Media2");
        media2.id = 2;
        
        assertThat(media1.rawget("Name")).isEqualTo("Media1");
        assertThat(media2.rawget("Name")).isEqualTo("Media2");
        assertThat(media1.id).isNotEqualTo(media2.id);
    }

    @Test
    public void testMediaAltText() {
        media.rawset("AltText", "Alternative text for media");
        assertThat(media.rawget("AltText")).isEqualTo("Alternative text for media");
    }
}
