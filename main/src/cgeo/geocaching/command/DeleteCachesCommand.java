package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

import java.util.Collection;

/**
 * delete multiple caches
 */
public abstract class DeleteCachesCommand extends AbstractCachesCommand {
    private int listId = StoredList.TEMPORARY_LIST.id;

    public DeleteCachesCommand(@NonNull final Activity context, @NonNull final Collection<Geocache> caches) {
        super(context, caches, R.string.command_delete_caches_progress);
    }

    @Override
    protected void doCommand() {
        listId = getCaches().iterator().next().getListId();
        DataStore.markDropped(getCaches());
    }

    @Override
    protected void undoCommand() {
        DataStore.moveToList(getCaches(), listId);
    }

    @Override
    @NonNull
    protected String getResultMessage() {
        final int size = getCaches().size();
        return getContext().getResources().getQuantityString(R.plurals.command_delete_caches_result, size, size);
    }

}
