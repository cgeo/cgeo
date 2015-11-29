package cgeo.geocaching.files;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgressInputStreamTest extends TestCase {

    @Test
    public void testRead() throws Exception {
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
