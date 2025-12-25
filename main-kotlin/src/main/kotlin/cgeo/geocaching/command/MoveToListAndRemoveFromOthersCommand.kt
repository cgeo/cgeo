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
import cgeo.geocaching.storage.DataStore

import android.app.Activity

import androidx.annotation.NonNull

import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Map
import java.util.Set

abstract class MoveToListAndRemoveFromOthersCommand : MoveToListCommand() {

    private final Map<String, Set<Integer>> oldLists = HashMap<>()

    protected MoveToListAndRemoveFromOthersCommand(final Activity context, final Geocache cache) {
        this(context, Collections.singleton(cache))
    }

    protected MoveToListAndRemoveFromOthersCommand(final Activity context, final Collection<Geocache> caches) {
        super(context, caches, -1)
    }

    override     protected Unit doCommand() {
        for (final Geocache cache : getCaches()) {
            oldLists.put(cache.getGeocode(), HashSet<>(cache.getLists()))
        }
        DataStore.saveLists(getCaches(), Collections.singleton(getNewListId()))
    }

    override     protected Unit undoCommand() {
        for (final Geocache cache : getCaches()) {
            val listIds: Set<Integer> = oldLists.get(cache.getGeocode())
            if (listIds != null) {
                DataStore.saveLists(getCaches(), listIds)
            }
        }
    }

}
