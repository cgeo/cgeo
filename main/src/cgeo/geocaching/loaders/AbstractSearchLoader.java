package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.RecaptchaHandler;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;

import java.util.concurrent.CountDownLatch;

public abstract class AbstractSearchLoader extends AsyncTaskLoader<SearchResult> implements RecaptchaReceiver {

    public enum CacheListLoaderType {
        OFFLINE,
        POCKET,
        HISTORY,
        NEAREST,
        COORDINATE,
        KEYWORD,
        ADDRESS,
        FINDER,
        OWNER,
        MAP,
        REMOVE_FROM_HISTORY,
        NEXT_PAGE;

        public int getLoaderId() {
            return ordinal();
        }
    }

    private Handler recaptchaHandler = null;
    private String recaptchaChallenge = null;
    private String recaptchaKey = null;
    private String recaptchaText = null;
    private SearchResult search;
    private boolean loading;
    private CountDownLatch latch = new CountDownLatch(1);

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
        try {
            if (search == null) {
                search = runSearch();
            } else {
                // Unless we make a new Search the Loader framework won't deliver results. It does't do equals only identity
                search = new SearchResult(search);
            }
        } catch (Exception e) {
            Log.e("Error in Loader ", e);
        }
        loading = false;
        if (search == null) {
            search = new SearchResult();
        }
        return search;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    public void setRecaptchaHandler(final Handler recaptchaHandler) {
        this.recaptchaHandler = recaptchaHandler;
    }

    @Override
    public void notifyNeed() {
        if (recaptchaHandler != null) {
            recaptchaHandler.sendEmptyMessage(RecaptchaHandler.SHOW_CAPTCHA);
        }
    }

    @Override
    public void waitForUser() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.w("searchThread is not waiting for user…");
        }
    }

    @Override
    public void setKey(String key) {
        recaptchaKey = key;
    }

    @Override
    public void fetchChallenge() {
        recaptchaChallenge = null;

        if (StringUtils.isNotEmpty(recaptchaKey)) {
            final Parameters params = new Parameters("k", recaptchaKey);
            final String recaptchaJs = Network.getResponseData(Network.getRequest("http://www.google.com/recaptcha/api/challenge", params));

            if (StringUtils.isNotBlank(recaptchaJs)) {
                recaptchaChallenge = TextUtils.getMatch(recaptchaJs, GCConstants.PATTERN_SEARCH_RECAPTCHACHALLENGE, true, 1, null, true);
            }
        }
    }

    @Override
    public String getChallenge() {
        return recaptchaChallenge;
    }

    @Override
    public void setText(String text) {
        recaptchaText = text;
        latch.countDown();
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
