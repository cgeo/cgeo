package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;

import android.net.Uri;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class DownloaderTest {

    private static List<Download> getList(final AbstractDownloader downloader, final String url) {
        final String page = Network.getResponseData(Network.getRequest(url));
        final List<Download> list = new ArrayList<>();
        if (page != null) {
            downloader.analyzePage(Uri.parse(url), list, page);
        }
        return list;
    }

    private static int count(final List<Download> list, final boolean isDir) {
        int i = 0;
        for (Download d : list) {
            if (d.isDir() == isDir) {
                i++;
            }
        }
        return i;
    }

    @Nullable
    private static Download findByName(final List<Download> list, final String name) {
        for (Download d : list) {
            if (StringUtils.equals(d.getName(), name)) {
                return d;
            }
        }
        return null;
    }

    @Test
    public void testMapsforge() {
        final List<Download> list = getList(MapDownloaderMapsforge.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_mapsforge_downloadurl) + "europe/");

        // europe starting page currently has ... entries (including the "up" entry)
        assertThat(list.size()).isBetween(53, 57);

        // first entry has to be the "up" entry
        assertThat(list.get(0).isDir()).isTrue();

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(5);

        // number of non-dirs found
        assertThat(count(list, false)).isBetween(49, 53);

        // check one named entry
        final Download d = findByName(list, "Portugal");
        assertThat(d).isNotNull();
        final String sizeInfoString = d.getSizeInfo(); // 261M as of 2023-03-15
        final int sizeInfoInt = Integer.parseInt(sizeInfoString.substring(0, sizeInfoString.length() - 1));
        assertThat(sizeInfoInt).isBetween(250, 350);
    }

    @Test
public void testOpenAndroMaps() {
        final List<Download> list = getList(MapDownloaderOpenAndroMaps.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_downloadurl) + "europe/");

        // europe starting page currently has ... entries (including the "up" entry)
        assertThat(list.size()).isBetween(55, 65);

        // first entry has to be the "up" entry
        assertThat(list.get(0).isDir()).isTrue();

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(1);

        // number of non-dirs found
        assertThat(count(list, false)).isBetween(54, 64);

        // check one named entry
        final Download d = findByName(list, "Scandinavia-SouthWest");
        assertThat(d).isNotNull();
        final String sizeInfoString = d.getSizeInfo(); // 1.7 GB / 2.1GB as of 5.11.23
        final float sizeInfoFloat = Float.parseFloat(sizeInfoString.substring(0, sizeInfoString.length() - 3));
        assertThat(sizeInfoFloat).isBetween(1.6F, 2.5F);
    }

    @Test
    public void testOpenAndroMapsThemes() {
        final List<Download> list = getList(MapDownloaderOpenAndroMapsThemes.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_updatecheckurl));

        // number of themes
        assertThat(list.size()).isEqualTo(2);

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0);

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(2);

        // check one named entry
        final Download d = findByName(list, "Elevate");
        assertThat(d).isNotNull();
        assertThat(d.getSizeInfo()).isBlank(); // no size info available
    }

    @Test
    public void testFreizeitkarte() {
        final List<Download> list = getList(MapDownloaderFreizeitkarte.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_freizeitkarte_downloadurl));

        // number of maps found
        assertThat(list.size()).isBetween(80, 90);

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0);

        // number of non-dirs found
        assertThat(count(list, false)).isBetween(80, 90);

        // check one named entry
        final Download d = findByName(list, "Freizeitkarte Hamburg");
        assertThat(d).isNotNull();
        final String sizeInfoString = d.getSizeInfo(); // 18.64 MB
        final float sizeInfoFloat = Float.parseFloat(sizeInfoString.substring(0, sizeInfoString.length() - 3));
        assertThat(sizeInfoFloat).isBetween(17.8F, 20.0F);
    }

    @Test
    public void testFreizeitkarteThemes() {
        final List<Download> list = getList(MapDownloaderFreizeitkarteThemes.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_freizeitkarte_downloadurl));

        // number of themes
        assertThat(list.size()).isGreaterThan(2);

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0);

        // number of non-dirs found
        assertThat(count(list, false)).isGreaterThan(2);

        // check one named entry
        final Download d = findByName(list, "Outdoor design contrast v5");
        assertThat(d).isNotNull();
        final String sizeInfoString = d.getSizeInfo(); // 351.6 KB
        final float sizeInfoFloat = Float.parseFloat(sizeInfoString.substring(0, sizeInfoString.length() - 3));
        assertThat(sizeInfoFloat).isBetween(350.0F, 360.0F);
    }

    @Test
    public void testBRouterTiles() {
        final List<Download> list = getList(BRouterTileDownloader.getInstance(), CgeoApplication.getInstance().getString(R.string.brouter_downloadurl));

        // number of tiles
        assertThat(list.size()).isBetween(1100, 1300);

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0);

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(list.size());

        // check one named entry
        final Download d = findByName(list, "E5_N50.rd5");
        assertThat(d).isNotNull();
        final String sizeInfoString = d.getSizeInfo(); // 154.8 MB as of 2023-06-05
        final float sizeInfoFloat = Float.parseFloat(sizeInfoString.substring(0, sizeInfoString.length() - 3));
        assertThat(sizeInfoFloat).isBetween(120.0F, 170.0F);
    }
}
