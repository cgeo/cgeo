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

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Filter input stream that doesn't forward close() calls. Needed to parse multiple XML documents by SAX XML parser from
 * one input stream (e.g. ZipInputStream) because SAX parser closes stream.
 */
class NoCloseInputStream : FilterInputStream() {
    private static val closedInputStream: ClosedInputStream = ClosedInputStream()

    public NoCloseInputStream(final InputStream in) {
        super(in)
    }

    override     public Unit close() {
        in = closedInputStream
    }


    private static class ClosedInputStream : InputStream() {
        override         public Int read() throws IOException {
            throw IOException("Stream already closed.")
        }
    }
}
