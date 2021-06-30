package cgeo.geocaching.brouter.util;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.Log;

import android.net.Uri;

import androidx.annotation.RawRes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class DefaultFilesUtils {

    private enum DefaultFiles {
        CAR_ECO(R.raw.routing_car_eco, "car-eco.brf"),
        CAR_FAST(R.raw.routing_car_fast, "car-fast.brf"),
        FASTBIKE(R.raw.routing_fastbike, "fastbike.brf"),
        MOPED(R.raw.routing_moped, "moped.brf"),
        SHORTEST(R.raw.routing_shortest, "shortest.brf"),
        TREKKING(R.raw.routing_trekking, "trekking.brf"),
        LOOKUPS(R.raw.routing_lookups, "lookups.dat");

        @RawRes
        public final int resId;
        public final String filename;

        DefaultFiles(final @RawRes int resId, final String filename) {
            this.resId = resId;
            this.filename = filename;
        }
    }

    private DefaultFilesUtils() {
        // utility class
    }

    public static void checkDefaultFiles() {
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.ROUTING_BASE);
        for (DefaultFiles file : DefaultFiles.values()) {
            boolean found = false;
            for (ContentStorage.FileInformation fi : files) {
                if (fi.name.equals(file.filename)) {
                    found = true;
                    break;
                }
            }
            // try to recreate missing file
            if (!found) {
                Log.i("Recreating routing default file " + file.filename);
                final Uri newFile = ContentStorage.get().create(PersistableFolder.ROUTING_BASE.getFolder(), file.filename);
                if (newFile == null) {
                    Log.w("Couldn't create file '" + file.filename + "' in '" + PersistableFolder.ROUTING_BASE.getFolder() + "'");
                } else {
                    try (OutputStream os = ContentStorage.get().openForWrite(newFile); InputStream in = CgeoApplication.getInstance().getResources().openRawResource(file.resId)) {
                        final byte[] buff = new byte[1024];
                        int read;
                        while ((read = in.read(buff)) > 0) {
                            os.write(buff, 0, read);
                        }
                    } catch (IOException e) {
                        Log.w("error creating default file " + newFile + " ' in '" + PersistableFolder.ROUTING_BASE.getFolder() + "'", e);
                    }
                }
            }
        }
    }
}
