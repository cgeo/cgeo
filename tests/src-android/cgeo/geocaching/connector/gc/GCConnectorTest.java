package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.connector.trackable.TravelBugConnector;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;

import java.util.Set;

import org.assertj.core.api.AbstractBooleanAssert;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class GCConnectorTest extends AbstractResourceInstrumentationTestCase {

    public static void testGetViewport() {
        // backup user settings
        final CacheType cacheType = Settings.getCacheType();
        try {
            // set up settings required for test
            Settings.setCacheType(CacheType.ALL);
            GCLogin.getInstance().login();

            {
                final Viewport viewport = new Viewport(new Geopoint("N 52° 25.369 E 9° 35.499"), new Geopoint("N 52° 25.600 E 9° 36.200"));
                final SearchResult searchResult = ConnectorFactory.searchByViewport(viewport);
                assertThat(searchResult).isNotNull();
                assertThat(searchResult.isEmpty()).isFalse();
                assertThat(searchResult.getGeocodes()).contains("GC1J1CT");
                assertThat(searchResult.getGeocodes()).doesNotContain("GC4ER5H");
            }

            {
                final Viewport viewport = new Viewport(new Geopoint("N 51° 36.000 E 7° 50.500"), new Geopoint("N 51° 37.350 E7° 51.500"));
                final SearchResult searchResult = ConnectorFactory.searchByViewport(viewport);
                assertThat(searchResult).isNotNull();
                assertThat(searchResult.getGeocodes()).contains("GC75NF6"); // N 51° 37.320 E 007° 50.600

                // redo search with a smaller viewport completely contained in the last one - should lead to an identical searchResult due to caching
                final Viewport viewport2 = new Viewport(new Geopoint("N 51° 36.500 E 7° 51.200"), new Geopoint("N 51° 36.750 E7° 51.400"));
                final SearchResult searchResult2 = ConnectorFactory.searchByViewport(viewport2);
                assertThat(searchResult.equals(searchResult2));

                // redo search with a way bigger viewport - caching does not help here, so a new searchResult should be delivered
                final Viewport viewport3 = new Viewport(new Geopoint("N 51° 35.000 E 7° 50.000"), new Geopoint("N 51° 38.000 E7° 52.000"));
                final SearchResult searchResult3 = ConnectorFactory.searchByViewport(viewport3);
                assertThat(!searchResult.equals(searchResult3));
            }
        } finally {
            // restore user settings
            Settings.setCacheType(cacheType);
        }
    }

    public static void testCanHandle() {
        assertCanHandle("GC2MEGA").isTrue();
        assertCanHandle("OXZZZZZ").isFalse();
        assertCanHandle("gc77").isTrue();
    }

    public static void testGeocodeForbiddenChars() {
        assertCanHandle("GC123").isTrue();
        assertCanHandle("GC123M").isTrue();
        assertCanHandle("GC123L").overridingErrorMessage("L is not allowed in GC codes").isFalse();
    }

    private static AbstractBooleanAssert<?> assertCanHandle(final String geocode) {
        return assertThat(GCConnector.getInstance().canHandle(geocode));
    }

    /**
     * functionality moved to {@link TravelBugConnector}
     */
    public static void testCanNotHandleTrackablesAnymore() {
        assertCanHandle("TB3F651").isFalse();
    }

    /** Tile computation with different zoom levels */
    public static void testTile() {
        // http://coord.info/GC2CT8K = N 52° 30.462 E 013° 27.906
        assertTileAt(8804, 5374, new Tile(new Geopoint(52.5077, 13.4651), 14));

        // (8633, 5381); N 52° 24,516 E 009° 42,592
        assertTileAt(8633, 5381, new Tile(new Geopoint("N 52° 24,516 E 009° 42,592"), 14));

        // Hannover, GC22VTB UKM Memorial Tour
        assertTileAt(2159, 1346, new Tile(new Geopoint("N 52° 22.177 E 009° 45.385"), 12));

        // Seattle, GCK25B Groundspeak Headquarters
        assertTileAt(5248, 11440, new Tile(new Geopoint("N 47° 38.000 W 122° 20.000"), 15));

        // Sydney, GCXT2R Victoria Cross
        assertTileAt(7536, 4915, new Tile(new Geopoint("S 33° 50.326 E 151° 12.426"), 13));
    }

    private static void assertTileAt(final int x, final int y, final Tile tile) {
        assertThat(tile.getX()).isEqualTo(x);
        assertThat(tile.getY()).isEqualTo(y);
    }

    public static void testGetGeocodeFromUrl() {
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("some string")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/GC12ABC")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/GC12ABC")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("https://www.geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau")).isEqualTo("GC12ABC");

        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/TB1234")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/TB1234")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/WM1234")).isNull();

        // uppercase is managed in ConnectorFactory
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/gc77")).isEqualTo("gc77");
    }

    public static void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(GCConnector.getInstance().handledGeocodes(geocodes)).containsOnly("GC1234", "GC5678");
    }

    public static void testIsChallengeCache() {
        assertIsChallengeCache("Some Challenge Cache", CacheType.MYSTERY).isTrue();
        assertIsChallengeCache("Some None Challenge Traditional", CacheType.TRADITIONAL).isFalse();
        assertIsChallengeCache("Some ordinary Mystery", CacheType.MYSTERY).isFalse();
    }

    private static AbstractBooleanAssert<?> assertIsChallengeCache(final String name, final CacheType type) {
        final Geocache geocache = new Geocache();
        geocache.setName(name);
        geocache.setType(type);
        return assertThat(GCConnector.getInstance().isChallengeCache(geocache));
    }
}
