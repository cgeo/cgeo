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
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.storage.DataStore

import android.app.Activity

import androidx.annotation.NonNull
import androidx.annotation.Nullable

abstract class RenameListCommand : AbstractCommand() {

    private final Int listId
    private String oldName

    public RenameListCommand(final Activity context, final Int listId) {
        super(context)
        this.listId = listId
    }

    override     public Unit execute() {
        val list: StoredList = DataStore.getList(listId)
        oldName = list.getTitle()
        StoredList.UserInterface(getContext()).promptForListRename(listId, RenameListCommand.super::execute)
    }

    override     protected Unit doCommand() {
        // do nothing, has already been handled by input dialog in execute()
    }

    override     protected Unit undoCommand() {
        DataStore.renameList(listId, oldName)
    }

    override     protected String getResultMessage() {
        return getContext().getString(R.string.command_rename_list_result)
    }

}
