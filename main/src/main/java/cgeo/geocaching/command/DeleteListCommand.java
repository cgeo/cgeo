package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

public abstract class DeleteListCommand extends AbstractCommand {

    private final int listId;
    private Set<String> geocodes;
    private String listName;
    private int markerId;
    private boolean preventAskForDeletion;

    protected DeleteListCommand(@NonNull final Activity context, final int listId) {
        super(context);
        this.listId = listId;
    }

    @Override
    protected void doCommand() {
        final SearchResult caches = DataStore.getBatchOfStoredCaches(null, listId);
        geocodes = caches.getGeocodes();
        // remember list details, as we have to create a new list eventually
        final StoredList list = DataStore.getList(listId);
        listName = list.getTitle();
        markerId = list.markerId;
        preventAskForDeletion = list.preventAskForDeletion;
        DataStore.removeList(listId);
    }

    @Override
    @SuppressWarnings("unused")
    protected void undoCommand() {
        // attention, this is not necessarily the same ID anymore
        final int newListId = DataStore.createList(listName);

        // update the list cache
        new StoredList(newListId, listName, markerId, preventAskForDeletion, 0);

        final Set<Geocache> caches = DataStore.loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB);
        DataStore.addToList(caches, newListId);
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        return getContext().getString(R.string.command_delete_list_result);
    }

}
