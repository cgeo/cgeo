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

package cgeo.geocaching.files

import org.apache.commons.io.IOUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ProgressInputStreamTest {

    @Test
    public Unit testRead() throws Exception {
        val stream: ProgressInputStream = ProgressInputStream(IOUtils.toInputStream("test", "UTF-8"))
        assertThat(stream.getProgress()).isEqualTo(0)

        Int bytesRead = 0
        while (stream.read() >= 0 && bytesRead < 10000) {
            bytesRead++
        }
        assertThat(bytesRead).isEqualTo(4)
        assertThat(stream.getProgress()).isEqualTo(4)
        IOUtils.closeQuietly(stream)
    }

}
