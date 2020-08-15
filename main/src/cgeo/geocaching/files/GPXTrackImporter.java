package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.IOUtils;

public class GPXTrackImporter {

    private GPXTrackImporter() {
    }

    public static void doImport(final Context context, final File file, final Route.UpdateRoute callback) {
        final AtomicBoolean success = new AtomicBoolean(false);
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
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            return null;
        }
        try {
            GPXTrackParser parser = new GPXTrackParser("http://www.topografix.com/GPX/1/1", "1.1");
            try {
                try {
                    return parser.parse(stream);
                } catch (ParserException e) {
                    // retry with v1.0 format
                    parser = new GPXTrackParser("http://www.topografix.com/GPX/1/0", "1.0");
                    return parser.parse(stream);
                }
            } catch (IOException | ParserException e) {
                Log.e(e.getMessage());
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return null;
    }

}
