package cgeo.geocaching.command;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.support.annotation.NonNull;

import android.app.Activity;

import java.util.Collections;
import java.util.HashSet;

public abstract class MoveToListAndRemoveFromOthersCommand extends MoveToListCommand {

    private final Geocache cache;
    private HashSet<Integer> oldLists;

    protected MoveToListAndRemoveFromOthersCommand(@NonNull final Activity context, @NonNull final Geocache cache) {
        super(context, Collections.singleton(cache), -1);
        this.cache = cache;
    }

    @Override
    protected void doCommand() {
        oldLists = new HashSet<>(cache.getLists());
        DataStore.saveLists(getCaches(), Collections.singleton(getNewListId()));
    }

    @Override
    protected void undoCommand() {
        DataStore.saveLists(getCaches(), oldLists);
    }

}
