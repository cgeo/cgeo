package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class RenameListCommand extends AbstractCommand {

    private final int listId;
    private String oldName;

    public RenameListCommand(@NonNull final Activity context, final int listId) {
        super(context);
        this.listId = listId;
    }

    @Override
    public void execute() {
        final StoredList list = DataStore.getList(listId);
        oldName = list.getTitle();
        new StoredList.UserInterface(getContext()).promptForListRename(listId, RenameListCommand.super::execute);
    }

    @Override
    protected void doCommand() {
        // do nothing, has already been handled by input dialog in execute()
    }

    @Override
    protected void undoCommand() {
        DataStore.renameList(listId, oldName);
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        return getContext().getString(R.string.command_rename_list_result);
    }

}
