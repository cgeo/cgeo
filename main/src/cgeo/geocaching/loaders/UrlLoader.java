package cgeo.geocaching.loaders;

import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import android.content.Context;

import androidx.loader.content.AsyncTaskLoader;

public class UrlLoader extends AsyncTaskLoader<String> {

    private final String url;
    private final Parameters params;

    public UrlLoader(final Context context, final String url, final Parameters params) {
        super(context);
        this.url = url;
        this.params = params;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public String loadInBackground() {
        try {
            return Network.getResponseData(Network.getRequest(url, params));
        } catch (final Exception e) {
            Log.w("UrlLoader.loadInBackground", e);
            return null;
        }
    }
}
