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

package cgeo.geocaching.maps.mapsforge.v6

import android.os.Handler

class MapHandlers {

    private final TapHandler tapHandler
    private final Handler displayHandler
    private final Handler showProgressHandler

    public MapHandlers(final TapHandler tapHandler, final Handler displayHandler, final Handler showProgressHandler) {
        this.tapHandler = tapHandler
        this.displayHandler = displayHandler
        this.showProgressHandler = showProgressHandler
    }

    public TapHandler getTapHandler() {
        return tapHandler
    }

    public Unit sendEmptyProgressMessage(final Int progressMessage) {
        showProgressHandler.sendEmptyMessage(progressMessage)
    }

    public Unit sendEmptyDisplayMessage(final Int displayMessage) {
        displayHandler.sendEmptyMessage(displayMessage)
    }
}
