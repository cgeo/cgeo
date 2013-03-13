package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;

public abstract class AbstractSearchLoader extends AsyncTaskLoader<SearchResult> implements RecaptchaReceiver {

    public enum CacheListLoaderType {
        OFFLINE,
        HISTORY,
        NEAREST,
        COORDINATE,
        KEYWORD,
        ADDRESS,
        USERNAME,
        OWNER,
        MAP,
        REMOVE_FROM_HISTORY;
    }

    private Handler recaptchaHandler = null;
    private String recaptchaChallenge = null;
    private String recaptchaText = null;
    private SearchResult search;
    private boolean loading;

    public AbstractSearchLoader(Context context) {
        super(context);
    }

    public abstract SearchResult runSearch();

    public boolean isLoading() {
        return loading;
    }

    @Override
    public SearchResult loadInBackground() {
        loading = true;
        if (search == null) {
            search = runSearch();
        } else {
            // Unless we make a new Search the Loader framework won't deliver results. It does't do equals only identity
            search = GCParser.searchByNextPage(new SearchResult(search), Settings.isShowCaptcha(), this);
        }
        loading = false;
        return search;
    }

    @Override
    public boolean takeContentChanged() {
        return super.takeContentChanged();
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    public void setRecaptchaHandler(Handler recaptchaHandlerIn) {
        recaptchaHandler = recaptchaHandlerIn;
    }

    @Override
    public void notifyNeed() {
        if (recaptchaHandler != null) {
            recaptchaHandler.sendEmptyMessage(1);
        }
    }

    @Override
    public synchronized void waitForUser() {
        try {
            wait();
        } catch (InterruptedException e) {
            Log.w("searchThread is not waiting for user…");
        }
    }

    @Override
    public void setChallenge(String challenge) {
        recaptchaChallenge = challenge;
    }

    @Override
    public String getChallenge() {
        return recaptchaChallenge;
    }

    @Override
    public synchronized void setText(String text) {
        recaptchaText = text;

        notify();
    }

    @Override
    public synchronized String getText() {
        return recaptchaText;
    }


    @Override
    public void reset() {
        super.reset();
        search = null;
    }
}
