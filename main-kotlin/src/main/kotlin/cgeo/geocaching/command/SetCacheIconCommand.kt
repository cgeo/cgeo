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

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore

import android.app.Activity

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collection
import java.util.HashMap

abstract class SetCacheIconCommand : AbstractCachesCommand() {

    private final Int newCacheIcon
    private final HashMap<String, Integer> undo

    protected SetCacheIconCommand(final Activity context, final Collection<Geocache> caches, final Int newCacheIcon) {
        super(context, caches, R.string.command_set_cache_icons_progress)
        this.newCacheIcon = newCacheIcon
        val size: Int = caches.size()
        this.undo = HashMap<>(size)
        for (final Geocache cache : caches) {
            this.undo.put(cache.getGeocode(), cache.getAssignedEmoji())
        }
    }

    override     public Unit execute() {
        setProgressMessage(getContext().getString(R.string.command_set_cache_icons_progress))
        SetCacheIconCommand.super.execute()
    }

    override     protected Unit doCommand() {
        DataStore.setCacheIcons(getCaches(), newCacheIcon)
    }

    override     protected Unit undoCommand() {
        DataStore.setCacheIcons(getCaches(), this.undo)
    }

    override     protected String getResultMessage() {
        val size: Int = getCaches().size()
        return getContext().getResources().getQuantityString(R.plurals.command_set_cache_icons_result, size, size)
    }
}
