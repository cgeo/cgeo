package cgeo.geocaching.downloader;

import android.net.Uri;

import org.apache.commons.lang3.StringUtils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class DownloaderTest extends AbstractResourceInstrumentationTestCase {

    private static List<Download> getList(AbstractDownloader downloader, final String url) {
        final String page = Network.getResponseData(Network.getRequest(url));
        final List<Download> list = new ArrayList<>();
        downloader.analyzePage(Uri.parse(url), list, page);
        return list;
    }

    private static int count(final List<Download> list, final boolean isDir) {
        int i = 0;
        for (Download d : list) {
            if (d.getIsDir() == isDir) {
                i++;
            }
        }
        return i;
    }

    public static void testMapsforge() {
        final List<Download> list = getList(MapDownloaderMapsforge.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_mapsforge_downloadurl) + "europe/");

        // europe starting page currently has ... entries (including the "up" entry)
        assertThat(list.size()).isEqualTo(54);

        // first entry has to be the "up" entry
        assertThat(list.get(0).getIsDir()).isTrue();

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(5);

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(49);
    }

    public static void testOpenAndroMaps() {
        final List<Download> list = getList(MapDownloaderOpenAndroMaps.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_downloadurl) + "europe/");

        // europe starting page currently has ... entries (including the "up" entry)
        assertThat(list.size()).isEqualTo(59);

        // first entry has to be the "up" entry
        assertThat(list.get(0).getIsDir()).isTrue();

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(1);

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(58);
    }

    public static void testOpenAndroMapsThemes() {
        final List<Download> list = getList(MapDownloaderOpenAndroMapsThemes.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_updatecheckurl));

        // number of themes
        assertThat(list.size()).isEqualTo(1);

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0);

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(1);
    }

    public static void testFreizeitkarte() {
        final List<Download> list = getList(MapDownloaderFreizeitkarte.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_freizeitkarte_downloadurl));

        // number of maps found
        assertThat(list.size()).isEqualTo(85);

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0);

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(85);
    }

    public static void testFreizeitkarteThemes() {
        final List<Download> list = getList(MapDownloaderFreizeitkarteThemes.getInstance(), CgeoApplication.getInstance().getString(R.string.mapserver_freizeitkarte_downloadurl));

        // number of themes
        assertThat(list.size()).isEqualTo(3);

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0);

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(3);
    }

    public static void testBRouterTiles() {
        final List<Download> list = getList(BRouterTileDownloader.getInstance(), CgeoApplication.getInstance().getString(R.string.brouter_downloadurl));

        // number of tiles
        assertThat(list.size()).isEqualTo(1120);

        // number of dirs found
        assertThat(count(list, true)).isEqualTo(0);

        // number of non-dirs found
        assertThat(count(list, false)).isEqualTo(1120);
    }

}
