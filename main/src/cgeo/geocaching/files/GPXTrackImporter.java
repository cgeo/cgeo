package cgeo.geocaching.files;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TrackUtils;

import android.app.Activity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.IOUtils;

public class GPXTrackImporter {

    private GPXTrackImporter(final Activity activity) {
    }

    public static void doImport(final File file, final Consumer<ArrayList<TrackUtils.Track>> callback) {
        Schedulers.io().createWorker().schedule(() -> {
            try {
                final ArrayList<TrackUtils.Track> value = doInBackground(file);
                AndroidSchedulers.mainThread().createWorker().schedule(() -> {
                    try {
                        callback.accept(value);
                    } catch (final Throwable t) {
                        //
                    }
                });
            } catch (final Exception e) {
                //
            }
        });
    }

    private static ArrayList<TrackUtils.Track> doInBackground(final File file) {
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
