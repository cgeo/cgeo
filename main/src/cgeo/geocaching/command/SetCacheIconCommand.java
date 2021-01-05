package cgeo.geocaching.command;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

public abstract class SetCacheIconCommand extends AbstractCachesCommand {

    private final int newCacheIcon;

    protected SetCacheIconCommand(@NonNull final Activity context, @NonNull final Collection<Geocache> caches, final int newCacheIcon) {
        super(context, caches, R.string.command_set_cache_icons_progress);
        this.newCacheIcon = newCacheIcon;
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
        // no undo available
    }

    @Override
    @Nullable
    protected String getResultMessage() {
        return null;
    }
}
