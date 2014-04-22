package cgeo.geocaching.files;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

public class ProgressInputStreamTest extends TestCase {

    public static void testRead() throws Exception {
        ProgressInputStream stream = new ProgressInputStream(IOUtils.toInputStream("test"));
        assertThat(stream.getProgress()).isEqualTo(0);

        int bytesRead = 0;
        while (stream.read() >= 0 && bytesRead < 10000) {
            bytesRead++;
        }
        assertThat(bytesRead).isEqualTo(4);
        assertThat(stream.getProgress()).isEqualTo(4);
        IOUtils.closeQuietly(stream);
    }

}
