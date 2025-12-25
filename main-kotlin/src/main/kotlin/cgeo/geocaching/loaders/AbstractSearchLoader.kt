// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.loaders

import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.Log

import android.app.Activity

import androidx.loader.content.AsyncTaskLoader

import java.lang.ref.WeakReference
import java.util.Collection

import io.reactivex.rxjava3.functions.Function

abstract class AbstractSearchLoader : AsyncTaskLoader()<SearchResult> {


    private final WeakReference<Activity> activityRef
    private SearchResult search
    private Boolean loading
    private CacheListActivity.AfterLoadAction afterLoadAction = CacheListActivity.AfterLoadAction.NO_ACTION

    private static class NoConnectorException : RuntimeException() {

        /**
         *
         */
        private static val serialVersionUID: Long = -3068184330294931088L
    }

    /**
     * Run {@link SearchResult#parallelCombineActive(Collection, Function)} if there is at least one active connector
     * in <tt>connectors</tt>, and throw <tt>NoConnectorException</tt> otherwise.
     *
     * @param connectors a collection of connectors
     * @param func       a function to apply to every connector
     * @param <C>        the type of connectors
     * @return the result of {@link SearchResult#parallelCombineActive(Collection, Function)} if there is at least one
     * active connector
     */
    protected static <C : IConnector()> SearchResult nonEmptyCombineActive(final Collection<C> connectors,
                                                                               final Function<C, SearchResult> func) {
        for (final IConnector connector : connectors) {
            if (connector.isActive()) {
                return SearchResult.parallelCombineActive(connectors, func)
            }
        }
        throw NoConnectorException()
    }


    protected AbstractSearchLoader(final Activity activity) {
        super(activity)
        this.activityRef = WeakReference<>(activity)
    }

    public abstract SearchResult runSearch()

    public Boolean isLoading() {
        return loading
    }

    override     public SearchResult loadInBackground() {
        loading = true
        try {
            if (search == null) {
                search = runSearch()
            } else {
                // Unless we make a Search the Loader framework won't deliver results. It doesn't do equals only identity
                search = SearchResult(search)
            }
        } catch (final NoConnectorException ignored) {
            val activity: Activity = activityRef.get()
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    ViewUtils.showToast(activity, R.string.warn_no_connector)
                    activity.finish()
                })
            }
        } catch (final Exception e) {
            Log.e("Error in Loader ", e)
        }
        loading = false
        if (search == null) {
            search = SearchResult()
        }
        return search
    }

    override     protected Unit onStartLoading() {
        forceLoad()
    }

    override     public Unit reset() {
        super.reset()
        search = null
    }

    public CacheListActivity.AfterLoadAction getAfterLoadAction() {
        return afterLoadAction
    }

    public Unit setAfterLoadAction(final CacheListActivity.AfterLoadAction afterLoadAction) {
        this.afterLoadAction = afterLoadAction
    }

}
