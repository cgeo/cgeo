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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes the caches of the selected list from all other lists.
 */
public abstract class MakeListUniqueCommand extends AbstractCommand {

    private final int listId;
    @NonNull
    private final Map<String, Set<Integer>> oldLists = new HashMap<>();

    public MakeListUniqueCommand(@NonNull final Activity context, final int listId) {
        super(context);
        this.listId = listId;
    }

    @Override
    protected void doCommand() {
        final SearchResult search = DataStore.getBatchOfStoredCaches(null, listId);
        final Set<Geocache> caches = DataStore.loadCaches(search.getGeocodes(), LoadFlags.LOAD_CACHE_OR_DB);

        for (final Geocache geocache : caches) {
            final HashSet<Integer> backupOfLists = new HashSet<>(geocache.getLists());
            final String geocode = geocache.getGeocode();
            // remove the current list, since that will ease the undo operation
            backupOfLists.remove(listId);
            oldLists.put(geocode, backupOfLists);
        }

        final List<StoredList> lists = DataStore.getLists();
        for (final StoredList list : lists) {
            if (list.id == listId) {
                continue;
            }
            DataStore.removeFromList(caches, list.id);
        }
    }

    @Override
    protected void undoCommand() {
        final Set<Geocache> caches = DataStore.loadCaches(oldLists.keySet(), LoadFlags.LOAD_CACHE_OR_DB);
        DataStore.addToLists(caches, oldLists);
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        return getContext().getString(R.string.command_unique_list_result);
    }

}
