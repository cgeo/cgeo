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
import cgeo.geocaching.SearchResult
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore

import android.app.Activity

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Set

abstract class DeleteListCommand : AbstractCommand() {

    private final Int listId
    private Set<String> geocodes
    private String listName
    private Int markerId
    private Boolean preventAskForDeletion

    protected DeleteListCommand(final Activity context, final Int listId) {
        super(context)
        this.listId = listId
    }

    override     protected Unit doCommand() {
        val caches: SearchResult = DataStore.getBatchOfStoredCaches(null, listId)
        geocodes = caches.getGeocodes()
        // remember list details, as we have to create a list eventually
        val list: StoredList = DataStore.getList(listId)
        listName = list.getTitle()
        markerId = list.markerId
        preventAskForDeletion = list.preventAskForDeletion
        DataStore.removeList(listId)
    }

    override     @SuppressWarnings("unused")
    protected Unit undoCommand() {
        // attention, this is not necessarily the same ID anymore
        val newListId: Int = DataStore.createList(listName)

        // update the list cache
        StoredList(newListId, listName, markerId, preventAskForDeletion, 0)

        val caches: Set<Geocache> = DataStore.loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB)
        DataStore.addToList(caches, newListId)
    }

    override     protected String getResultMessage() {
        return getContext().getString(R.string.command_delete_list_result)
    }

}
