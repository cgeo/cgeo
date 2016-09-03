package cgeo.geocaching.loaders;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.RecaptchaHandler;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import rx.functions.Func1;

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
        NEXT_PAGE;

        public int getLoaderId() {
            return ordinal();
        }
    }

    private final WeakReference<Activity> activityRef;
    private Handler recaptchaHandler = null;
    private String recaptchaChallenge = null;
    private String recaptchaKey = null;
    private String recaptchaText = null;
    private SearchResult search;
    private boolean loading;
    private final CountDownLatch latch = new CountDownLatch(1);
    private CacheListActivity.AfterLoadAction afterLoadAction = CacheListActivity.AfterLoadAction.NO_ACTION;

    private static class NoConnectorException extends RuntimeException {

        /**
         *
         */
        private static final long serialVersionUID = -3068184330294931088L;
    }

    /**
     * Run {@link SearchResult#parallelCombineActive(Collection, Func1)} if there is at least one active connector
     * in <tt>connectors</tt>, and throw <tt>NoConnectorException</tt> otherwise.
     *
     * @param connectors
     *            a collection of connectors
     * @param func
     *            a function to apply to every connector
     * @param <C>
     *            the type of connectors
     * @return the result of {@link SearchResult#parallelCombineActive(Collection, Func1)} if there is at least one
     *         active connector
     */
    protected static <C extends IConnector> SearchResult nonEmptyCombineActive(final Collection<C> connectors,
                                                                        final Func1<C, SearchResult> func) {
        for (final IConnector connector : connectors) {
            if (connector.isActive()) {
                return SearchResult.parallelCombineActive(connectors, func);
            }
        }
        throw new NoConnectorException();
    }


    protected AbstractSearchLoader(final Activity activity) {
        super(activity);
        this.activityRef = new WeakReference<>(activity);
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
                // Unless we make a new Search the Loader framework won't deliver results. It doesn't do equals only identity
                search = new SearchResult(search);
            }
        } catch (final NoConnectorException ignored) {
            final Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, R.string.warn_no_connector, Toast.LENGTH_LONG).show();
                        activity.finish();
                    }
                });
            }
        } catch (final Exception e) {
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
        } catch (final InterruptedException ignored) {
            Log.w("searchThread is not waiting for user…");
        }
    }

    @Override
    public void setKey(final String key) {
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
    public void setText(final String text) {
        recaptchaText = text;
        latch.countDown();
    }

    @Override
    public String getText() {
        return recaptchaText;
    }

    @Override
    public void reset() {
        super.reset();
        search = null;
    }

    public CacheListActivity.AfterLoadAction getAfterLoadAction() {
        return afterLoadAction;
    }

    public void setAfterLoadAction(final CacheListActivity.AfterLoadAction afterLoadAction) {
        this.afterLoadAction = afterLoadAction;
    }
}
