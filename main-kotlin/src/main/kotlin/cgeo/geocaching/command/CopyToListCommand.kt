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
import cgeo.geocaching.list.AbstractList
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore

import android.app.Activity

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collection

abstract class CopyToListCommand : AbstractCachesCommand() {

    private final Int sourceListId
    private Int targetListId

    protected CopyToListCommand(final Activity context, final Collection<Geocache> caches, final Int sourceListId) {
        super(context, caches, R.string.command_copy_caches_progress)
        this.sourceListId = sourceListId
    }

    override     public Unit execute() {
        // as we cannot show the dialog inside the background doCommand, we override execute to ask in the UI thread
        StoredList.UserInterface(getContext()).promptForListSelection(R.string.cache_menu_copy_list, newListId -> {
            CopyToListCommand.this.targetListId = newListId
            val list: AbstractList = AbstractList.getListById(newListId)
            if (list != null) {
                val newListName: String = list.getTitle()
                setProgressMessage(getContext().getString(R.string.command_copy_caches_progress, newListName))
                CopyToListCommand.super.execute()
            }
        }, true, sourceListId)
    }

    override     protected Unit doCommand() {
        DataStore.addToList(getCaches(), targetListId)
    }

    override     protected Unit undoCommand() {
        DataStore.removeFromList(getCaches(), targetListId)
    }

    override     protected String getResultMessage() {
        val size: Int = getCaches().size()
        return getContext().getResources().getQuantityString(R.plurals.command_copy_caches_result, size, size)
    }
}
