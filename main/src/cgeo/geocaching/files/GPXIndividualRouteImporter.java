package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.routing.RouteItem;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
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

    public static void doImport(final Context context, final File file) {
        final AtomicInteger size = new AtomicInteger(0);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            try {
                size.set(doInBackground(file));
            } catch (final Exception e) {
                //
            }
        }, () -> {
            Toast.makeText(context, size.get() > 0 ? context.getResources().getQuantityString(R.plurals.individual_route_loaded, size.get(), size.get()) : context.getString(R.string.load_individual_route_error), Toast.LENGTH_SHORT).show();
        });
    }

    // returns the length of the parsed route / 0 on empty or error
    private static int doInBackground(final File file) {
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            return 0;
        }
        try {
            GPXIndividualRouteParser parser = new GPXIndividualRouteParser("http://www.topografix.com/GPX/1/1", "1.1");
            ArrayList<RouteItem> routeItems = null;
            try {
                try {
                    routeItems = parser.parse(stream);
                } catch (ParserException e) {
                    // retry with v1.0 format
                    parser = new GPXIndividualRouteParser("http://www.topografix.com/GPX/1/0", "1.0");
                    routeItems = parser.parse(stream);
                }
            } catch (IOException | ParserException e) {
                Log.e(e.getMessage());
            }
            if (null != routeItems && routeItems.size() > 0) {
                DataStore.saveRoute(routeItems);
                return routeItems.size();
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return 0;
    }

}
