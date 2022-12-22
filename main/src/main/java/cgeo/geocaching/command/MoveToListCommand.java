package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

public abstract class MoveToListCommand extends AbstractCachesCommand {

    private final int currentListId;
    private int newListId;

    protected MoveToListCommand(@NonNull final Activity context, @NonNull final Collection<Geocache> caches, final int currentListId) {
        super(context, caches, R.string.command_move_caches_progress);
        this.currentListId = currentListId;
    }

    @Override
    public void execute() {
        // as we cannot show the dialog inside the background doCommand, we override execute to ask in the UI thread
        new StoredList.UserInterface(getContext()).promptForListSelection(R.string.cache_menu_move_list, newListId -> {
            MoveToListCommand.this.newListId = newListId;
            final AbstractList list = AbstractList.getListById(newListId);
            if (list != null) {
                final String newListName = list.getTitle();
                setProgressMessage(getContext().getString(R.string.command_move_caches_progress, newListName));
                MoveToListCommand.super.execute();
            }
        }, true, currentListId);
    }

    @Override
    protected void doCommand() {
        DataStore.moveToList(getCaches(), currentListId, newListId);
    }

    @Override
    protected void undoCommand() {
        DataStore.moveToList(getCaches(), newListId, currentListId);
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        final int size = getCaches().size();
        return getContext().getResources().getQuantityString(R.plurals.command_move_caches_result, size, size);
    }

    protected final int getNewListId() {
        return newListId;
    }

}
