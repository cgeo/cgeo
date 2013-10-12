package cgeo.geocaching.files;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

public class ProgressInputStreamTest extends TestCase {

    public static void testRead() throws Exception {
        ProgressInputStream stream = new ProgressInputStream(IOUtils.toInputStream("test"));
        assertEquals(0, stream.getProgress());

        int bytesRead = 0;
        while (stream.read() >= 0 && bytesRead < 10000) {
            bytesRead++;
        }
        assertEquals(4, bytesRead);
        assertEquals(4, stream.getProgress());
        IOUtils.closeQuietly(stream);
    }

}
