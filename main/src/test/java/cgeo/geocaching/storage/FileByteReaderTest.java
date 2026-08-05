package cgeo.geocaching.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;


public class FileByteReaderTest {

    @Test
    public void simple() throws IOException {
        final byte[] buffer = new byte[10];

        final File testFile = createTestFile(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        final FileByteReader fbr = new FileByteReader(new FileInputStream(testFile));

        fbr.readFully(0, 5, buffer);
        assertThat(buffer).isEqualTo(new byte[]{1, 2, 3, 4, 5, 0, 0, 0, 0, 0});

        fbr.readFully(4, 5, buffer);
        assertThat(buffer).isEqualTo(new byte[]{5, 6, 7, 8, 9, 0, 0, 0, 0, 0});
    }

    @Test
    public void overthelimit() throws IOException {
        final byte[] buffer = new byte[20];

        final File testFile = createTestFile(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        final FileByteReader fbr = new FileByteReader(new FileInputStream(testFile));

        try {
            fbr.readFully(4, 20, buffer);
            fail("Expected IOException because we read more data than available in file");
        } catch (IOException ioe) {
            //expected!
        }
        assertThat(buffer).isEqualTo(new byte[]{5, 6, 7, 8, 9, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    }

    private static File createTestFile(final byte[] data) throws IOException {
        final File newFile = File.createTempFile("cgeo-test-" + System.currentTimeMillis(), ".txt");
        IOUtils.write(data, new FileOutputStream(newFile, false));
        return newFile;
    }
}
