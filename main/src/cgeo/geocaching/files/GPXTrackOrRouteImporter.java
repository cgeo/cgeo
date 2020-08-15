package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.IOUtils;

public class GPXTrackOrRouteImporter {

    private GPXTrackOrRouteImporter() {
    }

    public static void doImport(final Context context, final File file, final Route.UpdateRoute callback) {
        final AtomicBoolean success = new AtomicBoolean(false);
        Toast.makeText(context, R.string.map_load_track_wait, Toast.LENGTH_SHORT).show();
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            try {
                final Route value = doInBackground(file);
                success.set(null != value && value.getNumSegments() > 0);
                if (success.get()) {
                    AndroidSchedulers.mainThread().createWorker().schedule(() -> {
                        try {
                            callback.updateRoute(value);
                        } catch (final Throwable t) {
                            //
                        }
                    });
                }
            } catch (final Exception e) {
                //
            }
        }, () -> {
            if (!success.get()) {
                Toast.makeText(context, R.string.load_track_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static Route doInBackground(final File file) {
        Route route = parse(new GPXTrackParser("http://www.topografix.com/GPX/1/1", "1.1"), file);
        if (null == route) {
            route = parse(new GPXRouteParser("http://www.topografix.com/GPX/1/1", "1.1"), file);
        }
        if (null == route) {
            route = parse(new GPXTrackParser("http://www.topografix.com/GPX/1/0", "1.0"), file);
        }
        if (null == route) {
            route = parse(new GPXRouteParser("http://www.topografix.com/GPX/1/0", "1.0"), file);
        }
        return route;
    }

    private static Route parse(@NonNull final AbstractTrackOrRouteParser.RouteParse parser, @NonNull final File file) {
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            return null;
        }
        try {
            final Route route = parser.parse(stream);
            return route.getNumSegments() < 1 ? null : route;
        } catch (IOException | ParserException e) {
            Log.d(e.getMessage());
            return null;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

}
