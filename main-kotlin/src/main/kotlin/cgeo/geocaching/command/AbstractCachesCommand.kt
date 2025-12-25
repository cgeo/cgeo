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

package cgeo.geocaching.command

import cgeo.geocaching.models.Geocache

import android.app.Activity

import androidx.annotation.NonNull

import java.util.Collection

abstract class AbstractCachesCommand : AbstractCommand() {

    private final Collection<Geocache> caches

    public AbstractCachesCommand(final Activity context, final Collection<Geocache> caches, final Int progressMessage) {
        super(context, progressMessage)
        this.caches = caches
    }

    override     protected Boolean canExecute() {
        return !caches.isEmpty()
    }

    protected Collection<Geocache> getCaches() {
        return caches
    }

}
