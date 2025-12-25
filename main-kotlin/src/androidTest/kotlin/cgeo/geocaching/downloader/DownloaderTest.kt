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

package cgeo.geocaching.downloader

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.models.Download
import cgeo.geocaching.network.Network

import android.net.Uri

import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class DownloaderTest {

    private static List<Download> getList(final AbstractDownloader downloader, final String url) {
        val page: String = Network.getResponseData(Network.getRequest(url))
        val list: List<Download> = ArrayList<>()
        if (page != null) {
            downloader.analyzePage(Uri.parse(url), list, page)
        }
        return list
    }

    private static Int count(final List<Download> list, final Boolean isDir) {
        Int i = 0
        for (Download d : list) {
            if (d.isDir() == isDir) {
                i++
            }
        }
        return i
    }

    private static Download findByName(final List<Download> list, final String name) {
        for (Download d : list) {
            if (StringUtils == (d.getName(), name)) {
                return d
            }
        }
        return null
    }

    @Test
    public Unit testMapsforge() {
        val list: List<Download> = getList(MapDownloaderMapsforge.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_mapsforge_downloadurl) + "europe/")

        // europe starting page currently has ... entries (including the "up" entry)
        assertThat(list.size()).isBetween(53, 57)

        // first entry has to be the "up" entry
        assertThat(list.get(0).isDir()).isTrue()

        // number of dirs found
        assertThat(count(list, true)).isGreaterThanOrEqualTo(7)

        // number of non-dirs found
        assertThat(count(list, false)).isBetween(49, 53)

        // check one named entry
        val d: Download = findByName(list, "Portugal")
        assertThat(d).isNotNull()
        val sizeInfoString: String = d.getSizeInfo(); // 261M as of 2023-03-15
        val sizeInfoInt: Int = Integer.parseInt(sizeInfoString.substring(0, sizeInfoString.length() - 1))
        assertThat(sizeInfoInt).isBetween(250, 350)
    }

    @Test
public Unit testOpenAndroMaps() {
        val list: List<Download> = getList(MapDownloaderOpenAndroMaps.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_downloadurl) + "europe/")

        // europe starting page currently has ... entries (including the "up" entry)
        assertThat(list.size()).isBetween(55, 65)

        // first entry has to be the "up" entry
        assertThat(list.get(0).isDir()).isTrue()

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(1)

        // number of non-dirs found
        assertThat(count(list, false)).isBetween(54, 64)

        // check one named entry
        val d: Download = findByName(list, "Scandinavia-SouthWest")
        assertThat(d).isNotNull()
        val sizeInfoString: String = d.getSizeInfo(); // 1.7 GB / 2.1GB as of 5.11.23
        val sizeInfoFloat: Float = Float.parseFloat(sizeInfoString.substring(0, sizeInfoString.length() - 3))
        assertThat(sizeInfoFloat).isBetween(1.6F, 2.5F)
    }

    @Test
    public Unit testOpenAndroMapsThemes() {
        val list: List<Download> = getList(MapDownloaderOpenAndroMapsThemes.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_downloadurl))

        // number of themes
        assertThat(list.size()).isEqualTo(3)

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0)

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(3)

        // check one named entry
        val d: Download = findByName(list, "Elevate")
        assertThat(d).isNotNull()
        assertThat(d.getSizeInfo()).isNotBlank()
    }

    @Test
    public Unit testFreizeitkarte() {
        val list: List<Download> = getList(MapDownloaderFreizeitkarte.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_freizeitkarte_downloadurl))

        // number of maps found
        assertThat(list.size()).isBetween(80, 90)

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0)

        // number of non-dirs found
        assertThat(count(list, false)).isBetween(80, 90)

        // check one named entry
        val d: Download = findByName(list, "Freizeitkarte Hamburg")
        assertThat(d).isNotNull()
        val sizeInfoString: String = d.getSizeInfo(); // 20.11 MB
        val sizeInfoFloat: Float = Float.parseFloat(sizeInfoString.substring(0, sizeInfoString.length() - 3))
        assertThat(sizeInfoFloat).isBetween(17.8F, 25.0F)
    }

    @Test
    public Unit testFreizeitkarteThemes() {
        val list: List<Download> = getList(MapDownloaderFreizeitkarteThemes.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_freizeitkarte_downloadurl))

        // number of themes
        assertThat(list.size()).isGreaterThan(2)

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0)

        // number of non-dirs found
        assertThat(count(list, false)).isGreaterThan(2)

        // check one named entry
        val d: Download = findByName(list, "Outdoor design contrast v5")
        assertThat(d).isNotNull()
        val sizeInfoString: String = d.getSizeInfo(); // 351.6 KB
        val sizeInfoFloat: Float = Float.parseFloat(sizeInfoString.substring(0, sizeInfoString.length() - 3))
        assertThat(sizeInfoFloat).isBetween(350.0F, 360.0F)
    }

    @Test
    public Unit testBRouterTiles() {
        val list: List<Download> = getList(BRouterTileDownloader.getInstance(), CgeoApplication.getInstance().getString(R.string.brouter_downloadurl))

        // number of tiles
        assertThat(list.size()).isBetween(1100, 1300)

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0)

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(list.size())

        // check one named entry
        val d: Download = findByName(list, "E5_N50.rd5")
        assertThat(d).isNotNull()
        val sizeInfoString: String = d.getSizeInfo(); // 178328254 Byte as of 2025-10-07
        val sizeInfoFloat: Float = Float.parseFloat(sizeInfoString.substring(0, sizeInfoString.length() - 3))
        assertThat(sizeInfoFloat).isBetween(120.0F, 250.0F)
    }
}
