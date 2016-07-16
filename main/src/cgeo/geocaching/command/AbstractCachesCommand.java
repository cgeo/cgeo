package cgeo.geocaching.command;

import cgeo.geocaching.models.Geocache;

import android.support.annotation.NonNull;

import android.app.Activity;

import java.util.Collection;

public abstract class AbstractCachesCommand extends AbstractCommand {

    @NonNull private final Collection<Geocache> caches;

    public AbstractCachesCommand(@NonNull final Activity context, @NonNull final Collection<Geocache> caches, final int progressMessage) {
        super(context, progressMessage);
        this.caches = caches;
    }

    @Override
    protected boolean canExecute() {
        return !caches.isEmpty();
    }

    @NonNull
    protected Collection<Geocache> getCaches() {
        return caches;
    }

}
