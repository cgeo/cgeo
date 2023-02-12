package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.location.GeoObjectList;
import cgeo.geocaching.location.IGeoDataProvider;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.GeoJsonUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

public class GPXTrackOrRouteImporter {

    private GPXTrackOrRouteImporter() {
    }

    public static void doImport(final Context context, final Uri uri, final Route.UpdateRoute callback) {
        final AtomicBoolean success = new AtomicBoolean(false);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            try {
                final IGeoDataProvider value = doInBackground(uri);
                success.set(null != value && value.hasData());
                if (success.get()) {
                    AndroidSchedulers.mainThread().createWorker().schedule(() -> {
                        try {
                            callback.updateRoute(value);
                        } catch (final Throwable t) {
                            Log.w("Error on track/route import: " + t.getMessage());
                        }
                    });
                }
            } catch (final Exception e) {
                //
            }
        }, () -> {
            if (!success.get()) {
                Toast.makeText(context, R.string.load_track_error, Toast.LENGTH_SHORT).show();
                callback.updateRoute(null);
            }
        });
    }

    private static IGeoDataProvider doInBackground(final Uri uri) {
        try {
            // default: import properly formatted routes or tracks
            Route route = parse(new GPXTrackParser("http://www.topografix.com/GPX/1/1", "1.1"), uri);
            if (null == route) {
                route = parse(new GPXRouteParser("http://www.topografix.com/GPX/1/1", "1.1"), uri);
            }
            if (null == route) {
                route = parse(new GPXTrackParser("http://www.topografix.com/GPX/1/0", "1.0"), uri);
            }
            if (null == route) {
                route = parse(new GPXRouteParser("http://www.topografix.com/GPX/1/0", "1.0"), uri);
            }
            // import waypoints as tracks
            if (null == route) {
                route = parse(new GPXWptAsTrackParser("http://www.topografix.com/GPX/1/1", "1.1"), uri);
            }
            if (null == route) {
                route = parse(new GPXWptAsTrackParser("http://www.topografix.com/GPX/1/0", "1.0"), uri);
            }
            // as last resort ignore missing namespace identifier
            if (null == route) {
                route = parse(new GPXTrackParser("", "1.0"), uri);
            }
            if (null != route) {
                route.calculateNavigationRoute();
            }
            if (null == route) {
                return parseAsGeoJson(uri);
            }
            return route;
        } catch (IOException e) {
            Log.w("Problem accessing GPX Track file '" + uri + "'. Maybe file was removed or renamed by user?", e);
            return null;
        }
    }

    private static Route parse(@NonNull final AbstractTrackOrRouteParser.RouteParse parser, @NonNull final Uri uri) throws IOException {
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(ContentStorage.get().openForRead(uri));
            if (stream == null) {
                return null;
            }
            final Route route = parser.parse(stream);
            return route.getNumSegments() < 1 ? null : route;
        } catch (ParserException e) {
            Log.w("Problem parsing GPX Track file '" + uri + "': " + e);
            return null;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    private static IGeoDataProvider parseAsGeoJson(final Uri uri) throws IOException {
        try (InputStream is = ContentStorage.get().openForRead(uri)) {
            if (is == null) {
                return null;
            }
            final List<GeoPrimitive> gos = GeoJsonUtils.parseGeoJson(is);
            final GeoObjectList gg = new GeoObjectList();
            gg.addAll(gos);
            return gg;
        } catch (JSONException e) {
            Log.w("Problem parsing GeoJson file '" + uri + "': " + e);
            return null;
        }
    }

}
