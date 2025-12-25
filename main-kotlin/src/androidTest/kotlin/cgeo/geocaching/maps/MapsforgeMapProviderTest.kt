// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps

import cgeo.geocaching.maps.interfaces.MapSource
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider

import android.net.Uri

import java.io.File
import java.io.IOException

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class MapsforgeMapProviderTest {

    @Test
    public Unit testInValidMapFileCheck() {
        assertThat(MapsforgeMapProvider.isValidMapFile(null)).isFalse()
        assertThat(MapsforgeMapProvider.getInstance().getAttributionFor(null)).isNotBlank()
    }

    @Test
    public Unit testOfflineMaps() throws IOException {
        val tempFile: File = File.createTempFile("pre", ".tmp")
        val tempFile2: File = File.createTempFile("pre", ".tmp")
        val fakeUri: Uri = Uri.fromFile(tempFile)
        val ms1: MapSource = MapsforgeMapProvider.OfflineMapSource(fakeUri, MapsforgeMapProvider.getInstance(), "noname1")
        val ms2: MapSource = MapsforgeMapProvider.OfflineMapSource(fakeUri, MapsforgeMapProvider.getInstance(), "noname2")
        val ms3: MapSource = MapsforgeMapProvider.OfflineMapSource(Uri.fromFile(tempFile2), MapsforgeMapProvider.getInstance(), "noname2")

        assertThat(ms1.getId()).isEqualTo(ms2.getId())
        assertThat(ms1.getNumericalId() == ms2.getNumericalId()).isTrue()
        assertThat(ms1.getId().endsWith(tempFile.getName())).isTrue()

        assertThat(ms1.getId() == (ms3.getId())).isFalse()
        assertThat(ms1.getNumericalId() == ms3.getNumericalId()).isFalse()
        assertThat(ms3.getId().endsWith(tempFile2.getName())).isTrue()

        //cleanup
        Boolean deleteOk = tempFile.delete()
        deleteOk &= tempFile2.delete()
        assertThat(deleteOk).isTrue()
    }
}
