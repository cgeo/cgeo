package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;

import android.net.Uri;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class MapsforgeMapProviderTest {

    @Test
    public void testInValidMapFileCheck() {
        assertThat(MapsforgeMapProvider.isValidMapFile(null)).isFalse();
        assertThat(MapsforgeMapProvider.getInstance().getAttributionFor(null)).isNotBlank();
    }

    @Test
    public void testOfflineMaps() throws IOException {
        final File tempFile = File.createTempFile("pre", ".tmp");
        final File tempFile2 = File.createTempFile("pre", ".tmp");
        final Uri fakeUri = Uri.fromFile(tempFile);
        final MapSource ms1 = new MapsforgeMapProvider.OfflineMapSource(fakeUri, MapsforgeMapProvider.getInstance(), "noname1");
        final MapSource ms2 = new MapsforgeMapProvider.OfflineMapSource(fakeUri, MapsforgeMapProvider.getInstance(), "noname2");
        final MapSource ms3 = new MapsforgeMapProvider.OfflineMapSource(Uri.fromFile(tempFile2), MapsforgeMapProvider.getInstance(), "noname2");

        assertThat(ms1.getId()).isEqualTo(ms2.getId());
        assertThat(ms1.getNumericalId() == ms2.getNumericalId()).isTrue();
        assertThat(ms1.getId().endsWith(tempFile.getName())).isTrue();

        assertThat(ms1.getId().equals(ms3.getId())).isFalse();
        assertThat(ms1.getNumericalId() == ms3.getNumericalId()).isFalse();
        assertThat(ms3.getId().endsWith(tempFile2.getName())).isTrue();

        //cleanup
        boolean deleteOk = tempFile.delete();
        deleteOk &= tempFile2.delete();
        assertThat(deleteOk).isTrue();
    }
}
