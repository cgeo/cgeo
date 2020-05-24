package cgeo.geocaching.files;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TrackUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.IOUtils;

public class GPXTrackImporter {

    private GPXTrackImporter() {
    }

    public static void doImport(final File file, final TrackUtils.TrackUpdaterMulti callback) {
        Schedulers.io().createWorker().schedule(() -> {
            try {
                final TrackUtils.Tracks value = doInBackground(file);
                AndroidSchedulers.mainThread().createWorker().schedule(() -> {
                    try {
                        callback.updateTracks(value);
                    } catch (final Throwable t) {
                        //
                    }
                });
            } catch (final Exception e) {
                //
            }
        });
    }

    private static TrackUtils.Tracks doInBackground(final File file) {
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
