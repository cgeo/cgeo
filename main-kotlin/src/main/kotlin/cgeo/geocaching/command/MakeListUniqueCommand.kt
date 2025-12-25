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
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore

import android.app.Activity

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set

/**
 * Removes the caches of the selected list from all other lists.
 */
abstract class MakeListUniqueCommand : AbstractCommand() {

    private final Int listId
    private final Set<String> cacheList
    private final Map<String, Set<Integer>> oldLists = HashMap<>()

    public MakeListUniqueCommand(final Activity context, final Int listId, final Set<String> cacheList) {
        super(context)
        this.listId = listId
        this.cacheList = cacheList
    }

    override     protected Unit doCommand() {
        val geocodesList: Set<String> = cacheList.isEmpty() ? DataStore.getBatchOfStoredCaches(null, listId).getGeocodes() : cacheList

        val caches: Set<Geocache> = DataStore.loadCaches(geocodesList, LoadFlags.LOAD_CACHE_OR_DB)

        for (final Geocache geocache : caches) {
            val backupOfLists: HashSet<Integer> = HashSet<>(geocache.getLists())
            val geocode: String = geocache.getGeocode()
            // remove the current list, since that will ease the undo operation
            backupOfLists.remove(listId)
            oldLists.put(geocode, backupOfLists)
        }

        val lists: List<StoredList> = DataStore.getLists()
        for (final StoredList list : lists) {
            if (list.id == listId) {
                continue
            }
            DataStore.removeFromList(caches, list.id)
        }
    }

    override     protected Unit undoCommand() {
        val caches: Set<Geocache> = DataStore.loadCaches(oldLists.keySet(), LoadFlags.LOAD_CACHE_OR_DB)
        DataStore.addToLists(caches, oldLists)
    }

    override     protected String getResultMessage() {
        return getContext().getString(R.string.command_unique_list_result)
    }

}
