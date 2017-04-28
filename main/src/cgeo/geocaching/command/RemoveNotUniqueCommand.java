package cgeo.geocaching.command;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

/**
 * Removes the caches of the selected list from all other lists.
 */
public abstract class RemoveNotUniqueCommand extends AbstractCommand {

    private final int listId;
    @NonNull
    private final Set<Geocache> removedCaches = new HashSet<>();

    protected RemoveNotUniqueCommand(@NonNull final Activity context, final int listId) {
        super(context);
        this.listId = listId;
    }

    @Override
    protected void doCommand() {
        final SearchResult search = DataStore.getBatchOfStoredCaches(null, CacheType.ALL, listId);
        final Set<Geocache> caches = DataStore.loadCaches(search.getGeocodes(), LoadFlags.LOAD_CACHE_OR_DB);

        for (final Geocache geocache : caches) {
            if (geocache.getLists().size() > 1) {
                // stored on more than this list
                removedCaches.add(geocache); // remember deleted geocaches
            }
        }
        DataStore.removeFromList(removedCaches, listId); // remove from this one
    }

    @Override
    protected void undoCommand() {
        DataStore.addToList(removedCaches, listId);
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        return getContext().getString(R.string.command_remove_not_unique_result);
    }
}
