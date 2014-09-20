package cgeo.geocaching.loaders;

import cgeo.geocaching.TrackableLog;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;

import java.util.List;

public abstract class AbstractInventoryLoader extends AsyncTaskLoader<List<TrackableLog>> {

    public AbstractInventoryLoader(final Context context) {
        super(context);
    }

}
