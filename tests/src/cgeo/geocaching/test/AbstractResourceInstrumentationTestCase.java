package cgeo.geocaching.test;

import android.content.res.Resources;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public abstract class AbstractResourceInstrumentationTestCase extends InstrumentationTestCase {
    protected InputStream getResourceStream(int resourceId) {
        final Resources res = getInstrumentation().getContext().getResources();
        return res.openRawResource(resourceId);
    }

    protected String getFileContent(int resourceId) {
        InputStream ins = getInstrumentation().getContext().getResources().openRawResource(resourceId);
        return new Scanner(ins).useDelimiter("\\A").next();
    }

    protected void copyResourceToFile(int resourceId, File file) throws IOException {
        final InputStream is = getResourceStream(resourceId);
        final FileOutputStream os = new FileOutputStream(file);
    
        try {
            byte[] buffer = new byte[4096];
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
