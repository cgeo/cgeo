package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.IgnoreListUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Set;

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
                setProgressMessage(LocalizationUtils.getString(R.string.command_move_caches_progress, newListName));
                MoveToListCommand.super.execute();
            }
        }, true, currentListId);
    }

    @Override
    protected void doCommand() {
        final Set<String> wasOnIgnoreList = IgnoreListUtils.snapshotIgnoreListMembership(getCaches());
        DataStore.moveToList(getCaches(), currentListId, newListId);
        IgnoreListUtils.reflectMembershipChange(getCaches(), wasOnIgnoreList);
    }

    @Override
    protected void undoCommand() {
        final Set<String> wasOnIgnoreList = IgnoreListUtils.snapshotIgnoreListMembership(getCaches());
        DataStore.moveToList(getCaches(), newListId, currentListId);
        IgnoreListUtils.reflectMembershipChange(getCaches(), wasOnIgnoreList);
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        final int size = getCaches().size();
        return LocalizationUtils.getPlural(R.plurals.command_move_caches_result, size);
    }

    protected final int getNewListId() {
        return newListId;
    }

}
