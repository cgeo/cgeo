package cgeo.geocaching.files;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;

import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

public abstract class FileParser {
    protected static StringBuilder readFile(File file)
            throws FileNotFoundException, IOException {
        final StringBuilder buffer = new StringBuilder();
        final BufferedReader input = new BufferedReader(new FileReader(file));
        try {
            String line = null;
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
        } finally {
            input.close();
        }
        return buffer;
    }

    protected static void showCountMessage(final Handler handler, final int count) {
        if (handler != null) {
            final Message msg = new Message();
            msg.obj = count;
            handler.sendMessage(msg);
        }
    }

    protected static void fixCache(cgCache cache) {
        cache.latitudeString = cgBase.formatLatitude(cache.coords.getLatitude(), true);
        cache.longitudeString = cgBase.formatLongitude(cache.coords.getLongitude(), true);
        if (cache.inventory != null) {
            cache.inventoryItems = cache.inventory.size();
        } else {
            cache.inventoryItems = 0;
        }
        cache.updated = new Date().getTime();
        cache.detailedUpdate = new Date().getTime();
    }

}
