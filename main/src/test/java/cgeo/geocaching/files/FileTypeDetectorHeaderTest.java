package cgeo.geocaching.files;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class FileTypeDetectorHeaderTest {

    @Test
    public void detectsTypesFromHeader() throws IOException {
        assertThat(detect("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.0\">\n")).isEqualTo(FileType.GPX);
        assertThat(detect("<?xml version=\"1.0\" encoding=\"UTF-8\"?><loc version=\"1.0\">")).isEqualTo(FileType.LOC);
        assertThat(detect("PK\u0003\u0004rest of a zip file")).isEqualTo(FileType.ZIP);
        assertThat(detect("mapsforge binary OSM and more")).isEqualTo(FileType.MAP);
        assertThat(detect("\u0002\nCART and more")).isEqualTo(FileType.WHERIGO);
        assertThat(detect("<html>\n<body>\n</body>\n</html>\n")).isEqualTo(FileType.UNKNOWN);
    }

    @Test
    public void readsOnlyStartOfLargeFileWithoutLineBreaks() throws IOException {
        // e.g. a large image: before, the first "line" (the whole file) was read into memory
        final byte[] content = new byte[10 * 1024 * 1024];
        Arrays.fill(content, (byte) 'x');
        final CountingInputStream is = new CountingInputStream(content);

        assertThat(FileTypeDetector.detectFileType(is)).isEqualTo(FileType.UNKNOWN);
        assertThat(is.bytesRead).isEqualTo(FileTypeDetector.MAX_HEADER_BYTES);
    }

    private static FileType detect(final String content) throws IOException {
        return FileTypeDetector.detectFileType(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static class CountingInputStream extends ByteArrayInputStream {
        private int bytesRead = 0;

        CountingInputStream(final byte[] buf) {
            super(buf);
        }

        @Override
        public synchronized int read() {
            final int result = super.read();
            if (result >= 0) {
                bytesRead++;
            }
            return result;
        }

        @Override
        public synchronized int read(final byte[] b, final int off, final int len) {
            final int result = super.read(b, off, len);
            if (result > 0) {
                bytesRead += result;
            }
            return result;
        }
    }
}
