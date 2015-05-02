package cgeo.geocaching.command;

import cgeo.geocaching.Geocache;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

import java.util.Collection;

abstract class AbstractCachesCommand extends AbstractCommand {

    @NonNull private final Collection<Geocache> caches;

    public AbstractCachesCommand(final @NonNull Activity context, final @NonNull Collection<Geocache> caches, final int progressMessage) {
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
