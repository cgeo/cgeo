package cgeo.geocaching.command;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class MoveToListAndRemoveFromOthersCommand extends MoveToListCommand {

    private final Map<String, Set<Integer>> oldLists = new HashMap<>();

    protected MoveToListAndRemoveFromOthersCommand(@NonNull final Activity context, @NonNull final Geocache cache) {
        this(context, Collections.singleton(cache));
    }

    protected MoveToListAndRemoveFromOthersCommand(@NonNull final Activity context, @NonNull final Collection<Geocache> caches) {
        super(context, caches, -1);
    }

    @Override
    protected void doCommand() {
        for (final Geocache cache : getCaches()) {
            oldLists.put(cache.getGeocode(), new HashSet<>(cache.getLists()));
        }
        DataStore.saveLists(getCaches(), Collections.singleton(getNewListId()));
    }

    @Override
    protected void undoCommand() {
        for (final Geocache cache : getCaches()) {
            final Set<Integer> listIds = oldLists.get(cache.getGeocode());
            if (listIds != null) {
                DataStore.saveLists(getCaches(), listIds);
            }
        }
    }

}
