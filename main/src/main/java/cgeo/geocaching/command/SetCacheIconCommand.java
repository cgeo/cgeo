package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;

public abstract class SetCacheIconCommand extends AbstractCachesCommand {

    private final int newCacheIcon;
    private final HashMap<String, Integer> undo;

    protected SetCacheIconCommand(@NonNull final Activity context, @NonNull final Collection<Geocache> caches, final int newCacheIcon) {
        super(context, caches, R.string.command_set_cache_icons_progress);
        this.newCacheIcon = newCacheIcon;
        final int size = caches.size();
        this.undo = new HashMap<>(size);
        for (final Geocache cache : caches) {
            this.undo.put(cache.getGeocode(), cache.getAssignedEmoji());
        }
    }

    @Override
    public void execute() {
        setProgressMessage(getContext().getString(R.string.command_set_cache_icons_progress));
        SetCacheIconCommand.super.execute();
    }

    @Override
    protected void doCommand() {
        DataStore.setCacheIcons(getCaches(), newCacheIcon);
    }

    @Override
    protected void undoCommand() {
        DataStore.setCacheIcons(getCaches(), this.undo);
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        final int size = getCaches().size();
        return getContext().getResources().getQuantityString(R.plurals.command_set_cache_icons_result, size, size);
    }
}
