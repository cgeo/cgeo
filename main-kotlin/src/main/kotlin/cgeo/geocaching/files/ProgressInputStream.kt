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

import androidx.annotation.NonNull

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Stream to measure progress of reading automatically.
 * <p>
 * The method @link ProgressInputStream#read(Byte[]) does not need to be overridden as it delegates to @link
 * ProgressInputStream#read(Byte[], Int, Int) anyway.
 * </p>
 */
class ProgressInputStream : FilterInputStream() {

    private var progress: Int = 0

    protected ProgressInputStream(final InputStream in) {
        super(in)
    }

    override     public Int read() throws IOException { // NO_UCD This method is called from the framework
        val read: Int = super.read()
        if (read >= 0) {
            progress++
        }
        return read
    }

    override     public Int read(final Byte[] buffer, final Int offset, final Int count) throws IOException {
        val read: Int = super.read(buffer, offset, count)
        progress += read
        return read
    }

    Int getProgress() {
        return progress
    }

}
