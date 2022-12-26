package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.connector.trackable.TravelBugConnector;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;

import java.util.Set;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class GCConnectorTest  {

    @Test
    public void testGetViewport() {

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
            assertThat(searchResult.equals(searchResult2)).isTrue();

            // redo search with a way bigger viewport - caching does not help here, so a new searchResult should be delivered
            final Viewport viewport3 = new Viewport(new Geopoint("N 51° 35.000 E 7° 50.000"), new Geopoint("N 51° 38.000 E7° 52.000"));
            final SearchResult searchResult3 = ConnectorFactory.searchByViewport(viewport3);
            assertThat(!searchResult.equals(searchResult3)).isTrue();
        }
    }

    @Test
    public void testCanHandle() {
        assertCanHandle("GC2MEGA").isTrue();
        assertCanHandle("GCAAAAAAAAAAAAA").isFalse();
        assertCanHandle("OXZZZZZ").isFalse();
        assertCanHandle("gc77").isTrue();
    }

    @Test
    public void testGeocodeForbiddenChars() {
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
    @Test
    public void testCanNotHandleTrackablesAnymore() {
        assertCanHandle("TB3F651").isFalse();
    }

    @Test
    public void testGetGeocodeFromUrl() {
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("some string")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/GC12ABC")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/GC12ABC?test")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/GC12ABC")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("https://coord.info/GC123_tset")).isEqualTo("GC123");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("https://www.geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://geocaching.com/geocache/GC12ABC")).isEqualTo("GC12ABC");

        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/TB1234")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/TB1234")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/WM1234")).isNull();

        // uppercase is managed in ConnectorFactory
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/gc77")).isEqualTo("gc77");
    }

    @Test
    public void testGetGeocodeFromText() {
        // Matching a geocode in text
        assertThat(GCConnector.getInstance().getGeocodeFromText("https://coord.info/GC123 tset")).isEqualTo("GC123");
        assertThat(GCConnector.getInstance().getGeocodeFromText("GC123asddd")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromText("Text GC123")).isEqualTo("GC123");
        assertThat(GCConnector.getInstance().getGeocodeFromText("GC123")).isEqualTo("GC123");
        assertThat(GCConnector.getInstance().getGeocodeFromText("GC123 Text")).isEqualTo("GC123");
        assertThat(GCConnector.getInstance().getGeocodeFromText("asdf GC123 asdf")).isEqualTo("GC123");
        assertThat(GCConnector.getInstance().getGeocodeFromText("Bla GC123tset")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromText("I can't find GC123 do you have a hint?")).isEqualTo("GC123");
        assertThat(GCConnector.getInstance().getGeocodeFromText("Check out GC123.")).isEqualTo("GC123");
        assertThat(GCConnector.getInstance().getGeocodeFromText("CheckoutGC123.")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromText(">GC123<")).isEqualTo("GC123");
        assertThat(GCConnector.getInstance().getGeocodeFromText("Do you have a hint for GC123?")).isEqualTo("GC123");
    }

    @Test
    public void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(GCConnector.getInstance().handledGeocodes(geocodes)).containsOnly("GC1234", "GC5678");
    }

    @Test
    public void testIsChallengeCache() {
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
