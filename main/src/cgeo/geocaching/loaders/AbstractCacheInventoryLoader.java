package cgeo.geocaching.loaders;

import cgeo.geocaching.Trackable;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.util.List;

public abstract class AbstractCacheInventoryLoader extends AsyncTaskLoader<List<Trackable>> {

    public AbstractCacheInventoryLoader(final Context context) {
        super(context);
    }

}
