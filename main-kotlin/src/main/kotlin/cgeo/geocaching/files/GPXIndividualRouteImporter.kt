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

package cgeo.geocaching.files

import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.models.Route
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import android.content.Context
import android.net.Uri

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.io.IOUtils

/**
 * Imports an GPX file containing an individual route
 * and stores the resulting geoitems to the route table.
 * (Caller needs to trigger reload and screen update.)
 */
class GPXIndividualRouteImporter {
    private GPXIndividualRouteImporter() {
    }

    public static Unit doImport(final Context context, final Uri uri) {
        val size: AtomicInteger = AtomicInteger(0)
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            try {
                size.set(doInBackground(uri))
            } catch (final Exception e) {
                //
            }
        }, () -> {
            ViewUtils.showShortToast(context, size.get() > 0 ? LocalizationUtils.getPlural(R.plurals.individual_route_loaded, size.get()) : LocalizationUtils.getString(R.string.load_individual_route_error))
            LifecycleAwareBroadcastReceiver.sendBroadcast(context, Intents.ACTION_INDIVIDUALROUTE_CHANGED)
        })
    }

    // returns the length of the parsed route / 0 on empty or error
    private static Int doInBackground(final Uri uri) {
        BufferedInputStream stream = null
        try {
            val is: InputStream = ContentStorage.get().openForRead(uri)
            if (is == null) {
                return 0
            }
            stream = BufferedInputStream(is)

            GPXIndividualRouteParser parser = GPXIndividualRouteParser("http://www.topografix.com/GPX/1/1", "1.1")
            Route route = null
            try {
                try {
                    route = parser.parse(stream)
                } catch (ParserException e) {
                    // retry with v1.0 format
                    parser = GPXIndividualRouteParser("http://www.topografix.com/GPX/1/0", "1.0")
                    route = parser.parse(stream)
                }
            } catch (IOException | ParserException e) {
                Log.e(e.getMessage())
            }
            if (null != route && route.getNumSegments() > 0) {
                DataStore.saveIndividualRoute(route)
                return route.getNumSegments()
            }
        } finally {
            IOUtils.closeQuietly(stream)
        }
        return 0
    }

}
