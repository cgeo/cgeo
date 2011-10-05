package cgeo.geocaching.files;

import cgeo.geocaching.cgCache;

import android.os.Handler;

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

    protected static void showCountMessage(final Handler handler, final int msgId, final int count) {
        if (handler != null && (count <= 1 || count % 10 == 0)) {
            handler.sendMessage(handler.obtainMessage(0, msgId, count));
        }
    }

    protected static void fixCache(cgCache cache) {
        if (cache.inventory != null) {
            cache.inventoryItems = cache.inventory.size();
        } else {
            cache.inventoryItems = 0;
        }
        cache.updated = new Date().getTime();
        cache.detailedUpdate = new Date().getTime();
    }

}
