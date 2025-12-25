// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

import org.apache.commons.io.IOUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.fail


class FileByteReaderTest {

    @Test
    public Unit simple() throws IOException {
        final Byte[] buffer = Byte[10]

        val testFile: File = createTestFile(Byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
        val fbr: FileByteReader = FileByteReader(FileInputStream(testFile))

        fbr.readFully(0, 5, buffer)
        assertThat(buffer).isEqualTo(Byte[]{1, 2, 3, 4, 5, 0, 0, 0, 0, 0})

        fbr.readFully(4, 5, buffer)
        assertThat(buffer).isEqualTo(Byte[]{5, 6, 7, 8, 9, 0, 0, 0, 0, 0})
    }

    @Test
    public Unit overthelimit() throws IOException {
        final Byte[] buffer = Byte[20]

        val testFile: File = createTestFile(Byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
        val fbr: FileByteReader = FileByteReader(FileInputStream(testFile))

        try {
            fbr.readFully(4, 20, buffer)
            fail("Expected IOException because we read more data than available in file")
        } catch (IOException ioe) {
            //expected!
        }
        assertThat(buffer).isEqualTo(Byte[]{5, 6, 7, 8, 9, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0})
    }

    private static File createTestFile(final Byte[] data) throws IOException {
        val newFile: File = File.createTempFile("cgeo-test-" + System.currentTimeMillis(), ".txt")
        IOUtils.write(data, FileOutputStream(newFile, false))
        return newFile
    }
}
