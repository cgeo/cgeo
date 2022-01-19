package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.IOUtils;

/**
 * Imports an GPX file containing an individual route
 * and stores the resulting geoitems to the route table.
 * (Caller needs to trigger reload and screen update.)
 */
public class GPXIndividualRouteImporter {
    private GPXIndividualRouteImporter() {
    }

    public static void doImport(final Context context, final Uri uri) {
        final AtomicInteger size = new AtomicInteger(0);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            try {
                size.set(doInBackground(uri));
            } catch (final Exception e) {
                //
            }
        }, () -> Toast.makeText(context, size.get() > 0 ? context.getResources().getQuantityString(R.plurals.individual_route_loaded, size.get(), size.get()) : context.getString(R.string.load_individual_route_error), Toast.LENGTH_SHORT).show());
    }

    // returns the length of the parsed route / 0 on empty or error
    private static int doInBackground(final Uri uri) {
        BufferedInputStream stream = null;
        try {
            final InputStream is = ContentStorage.get().openForRead(uri);
            if (is == null) {
                return 0;
            }
            stream = new BufferedInputStream(is);

            GPXIndividualRouteParser parser = new GPXIndividualRouteParser("http://www.topografix.com/GPX/1/1", "1.1");
            Route route = null;
            try {
                try {
                    route = parser.parse(stream);
                } catch (ParserException e) {
                    // retry with v1.0 format
                    parser = new GPXIndividualRouteParser("http://www.topografix.com/GPX/1/0", "1.0");
                    route = parser.parse(stream);
                }
            } catch (IOException | ParserException e) {
                Log.e(e.getMessage());
            }
            if (null != route && route.getNumSegments() > 0) {
                DataStore.saveIndividualRoute(route);
                return route.getNumSegments();
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return 0;
    }

}
