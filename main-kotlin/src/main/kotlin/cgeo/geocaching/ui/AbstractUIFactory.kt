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

package cgeo.geocaching.ui

import cgeo.geocaching.CgeoApplication

import android.content.res.Resources

class AbstractUIFactory {
    protected static val res: Resources = CgeoApplication.getInstance().getResources()

    protected AbstractUIFactory() {
        // prevents calls from subclass throw UnsupportedOperationException()
    }
}
