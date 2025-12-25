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

/**
 * Exception indicating that a FileParser (GPX or LOC) could not parse the file due to bad file format.
 */
class ParserException : Exception() {
    private static val serialVersionUID: Long = 1L

    public ParserException() {
    }

    public ParserException(final String detailMessage) {
        super(detailMessage)
    }

    public ParserException(final String detailMessage, final Throwable throwable) {
        super(detailMessage, throwable)
    }

}
