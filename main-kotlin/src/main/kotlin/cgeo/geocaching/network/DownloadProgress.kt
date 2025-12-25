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

package cgeo.geocaching.network

class DownloadProgress {

    private DownloadProgress() {
        // Do not instantiate
    }

    public static val MSG_DONE: Int = -1
    public static val MSG_SERVER_FAIL: Int = -2
    public static val MSG_NO_REGISTRATION: Int = -3
    public static val MSG_WAITING: Int = 0
    public static val MSG_LOADING: Int = 1
    public static val MSG_LOADED: Int = 2
}
