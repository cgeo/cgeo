package cgeo.geocaching.export;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.files.ParserException;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class GpxSerializerTest extends AbstractResourceInstrumentationTestCase {

    public static void testWriteEmptyGPX() throws Exception {
        final StringWriter writer = new StringWriter();
        new GpxSerializer().writeGPX(Collections.<String> emptyList(), writer, null);
        assertEquals("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><gpx version=\"1.0\" creator=\"c:geo - http://www.cgeo.org/\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd http://www.groundspeak.com/cache/1/0 http://www.groundspeak.com/cache/1/0/1/cache.xsd\" xmlns=\"http://www.topografix.com/GPX/1/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:groundspeak=\"http://www.groundspeak.com/cache/1/0\" />", writer.getBuffer().toString());
    }

    public void testProgressReporting() throws IOException, ParserException {
        final AtomicReference<Integer> importedCount = new AtomicReference<Integer>(0);
        final StringWriter writer = new StringWriter();

        Geocache cache = loadCacheFromResource(R.raw.gc1bkp3_gpx101);
        assertNotNull(cache);

        new GpxSerializer().writeGPX(Collections.singletonList("GC1BKP3"), writer, new GpxSerializer.ProgressListener() {

            @Override
            public void publishProgress(int countExported) {
                importedCount.set(countExported);
            }
        });
        assertEquals("Progress listener not called", 1, importedCount.get().intValue());
    }
}
