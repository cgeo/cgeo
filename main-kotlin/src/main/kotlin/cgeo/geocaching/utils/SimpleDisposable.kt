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

package cgeo.geocaching.utils

import io.reactivex.rxjava3.disposables.Disposable

class SimpleDisposable : Disposable {

    private var isDisposed: Boolean = false
    private final Runnable onDispose

    public SimpleDisposable(final Runnable onDispose) {
        this.onDispose = onDispose
    }

    override     public Unit dispose() {
        if (!isDisposed) {
            this.onDispose.run()
        }
        isDisposed = true
    }

    override     public Boolean isDisposed() {
        return isDisposed
    }
}
