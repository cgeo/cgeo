package cgeo.geocaching.files;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.test.CgeoTemporaryListRule;
import cgeo.geocaching.test.CgeoTestUtils;
import static cgeo.geocaching.enumerations.CacheSize.MICRO;
import static cgeo.geocaching.enumerations.CacheSize.UNKNOWN;
import static cgeo.geocaching.test.R.raw.gc1bkp3_loc;
import static cgeo.geocaching.test.R.raw.oc5952_loc;
import static cgeo.geocaching.test.R.raw.waymarking_loc;

import androidx.annotation.RawRes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LocParserTest {

    @Rule
    private final CgeoTemporaryListRule tempList = new CgeoTemporaryListRule();

    private List<Geocache> readLoc(@RawRes final int resourceId) throws IOException, ParserException {
        final LocParser parser = new LocParser(tempList.getListId());
        final Collection<Geocache> caches;
        final InputStream instream = CgeoTestUtils.getResourceStream(resourceId);
        try {
            caches = parser.parse(instream, null);
            assertThat(caches).isNotNull();
            assertThat(caches).isNotEmpty();
        } finally {
            IOUtils.closeQuietly(instream);
        }

        return new ArrayList<>(caches);
    }

    @Test
    public void testOCLoc() throws IOException, ParserException {
        final List<Geocache> caches = readLoc(oc5952_loc);
        assertThat(caches).hasSize(1);
        final Geocache cache = caches.get(0);
        assertThat(cache).isNotNull();
        assertThat(cache.getGeocode()).isEqualTo("OC5952");
        assertThat(cache.getName()).isEqualTo("Die Schatzinsel / treasure island");
        assertThat(cache.getOwnerUserId()).isEqualTo("Die unbesiegbaren Geo - Geparden");
        assertThat(cache.getCoords()).isEqualTo(new Geopoint(48.85968, 9.18740));
    }

    @Test
    public void testGCLoc() throws IOException, ParserException {
        final List<Geocache> caches = readLoc(gc1bkp3_loc);
        assertThat(caches).hasSize(1);
        final Geocache cache = caches.get(0);
        assertThat(cache).isNotNull();
        assertThat(cache.getGeocode()).isEqualTo("GC1BKP3");
        assertThat(cache.getName()).isEqualTo("Die Schatzinsel / treasure island");
        assertThat(cache.getOwnerUserId()).isEqualTo("Die unbesiegbaren Geo - Geparden");
        assertThat(cache.getCoords()).isEqualTo(new Geopoint(48.859683, 9.1874));
        assertThat(cache.getDifficulty()).isEqualTo(1.0f);
        assertThat(cache.getTerrain()).isEqualTo(5.0f);
        assertThat(cache.getSize()).isEqualTo(MICRO);
    }

    @Test
    public void testWaymarkingLoc() throws IOException, ParserException {
        final List<Geocache> waymarks = readLoc(waymarking_loc);
        assertThat(waymarks).hasSize(1);
        final Geocache waymark = waymarks.get(0);
        assertThat(waymark).isNotNull();
        assertThat(waymark.getGeocode()).isEqualTo("WM7BK7");
        assertThat(waymark.getName()).isEqualTo("Römerstrasse Kornwestheim");
        assertThat(waymark.getOwnerUserId()).isEqualTo("travelling");
        assertThat(waymark.getCoords()).isEqualTo(new Geopoint(48.856733, 9.197683));
        // links are not yet stored for single caches
        // assertThat(waymark.getUrl()).isEqualTo("http://www.waymarking.com/waymarks/WM7BK7_Rmerstrasse_Kornwestheim");
        assertThat(waymark.getSize()).isEqualTo(UNKNOWN);
        assertThat(waymark.getType()).isEqualTo(CacheType.UNKNOWN);
    }

}
