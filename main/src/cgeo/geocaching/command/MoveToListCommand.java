package cgeo.geocaching.command;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.list.StoredList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.functions.Action1;

import android.app.Activity;

import java.util.Collection;

public abstract class MoveToListCommand extends AbstractCachesCommand {

    private final int currentListId;
    private int newListId;

    protected MoveToListCommand(final Activity context, @NonNull final Collection<Geocache> caches, final int currentListId) {
        super(context, caches, R.string.command_move_caches_progress);
        this.currentListId = currentListId;
    }

    @Override
    public void execute() {
        // as we cannot show the dialog inside the background doCommand, we override execute to ask in the UI thread
        new StoredList.UserInterface(getContext()).promptForListSelection(R.string.cache_menu_move_list, new Action1<Integer>() {


            @Override
            public void call(final Integer newListId) {
                MoveToListCommand.this.newListId = newListId;
                MoveToListCommand.super.execute();
            }
        }, true, currentListId);
    }

    @Override
    protected void doCommand() {
        DataStore.moveToList(getCaches(), newListId);
    }

    @Override
    protected void undoCommand() {
        DataStore.moveToList(getCaches(), currentListId);
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        final int size = getCaches().size();
        return getContext().getResources().getQuantityString(R.plurals.command_move_caches_result, size, size);
    }
}
