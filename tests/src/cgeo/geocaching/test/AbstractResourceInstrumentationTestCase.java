package cgeo.geocaching.test;

import cgeo.geocaching.cgData;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;

import android.content.res.Resources;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Scanner;

public abstract class AbstractResourceInstrumentationTestCase extends InstrumentationTestCase {
    protected static void removeCacheCompletely(final String geocode) {
        final EnumSet<RemoveFlag> flags = EnumSet.copyOf(LoadFlags.REMOVE_ALL);
        flags.add(RemoveFlag.REMOVE_OWN_WAYPOINTS_ONLY_FOR_TESTING);
        cgData.removeCache(geocode, flags);
    }

    protected InputStream getResourceStream(int resourceId) {
        final Resources res = getInstrumentation().getContext().getResources();
        return res.openRawResource(resourceId);
    }

    protected String getFileContent(int resourceId) {
        final InputStream ins = getResourceStream(resourceId);
        final String result = new Scanner(ins).useDelimiter("\\A").next();
        try {
            ins.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    protected void copyResourceToFile(int resourceId, File file) throws IOException {
        final InputStream is = getResourceStream(resourceId);
        final FileOutputStream os = new FileOutputStream(file);

        try {
            final byte[] buffer = new byte[4096];
            int byteCount;
            while ((byteCount = is.read(buffer)) >= 0) {
                os.write(buffer, 0, byteCount);
            }
        } finally {
            os.close();
            is.close();
        }
    }
}
