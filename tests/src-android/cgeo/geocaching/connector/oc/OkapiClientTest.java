package cgeo.geocaching.connector.oc;

import cgeo.CGeoTestCase;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;

import java.util.Locale;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class OkapiClientTest extends CGeoTestCase {

    public static void testGetOCCache() {
        final String geoCode = "OU0331";
        Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).as("Cache from OKAPI").isNotNull();
        assertThat(cache.getGeocode()).isEqualTo(geoCode);
        assertThat(cache.getName()).isEqualTo("Oshkosh Municipal Tank");
        assertThat(cache.isDetailed()).isTrue();
        // cache should be stored to DB (to listID 0) when loaded above
        cache = DataStore.loadCache(geoCode, LoadFlags.LOAD_ALL_DB_ONLY);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertThat(cache.getGeocode()).isEqualTo(geoCode);
        assertThat(cache.getName()).isEqualTo("Oshkosh Municipal Tank");
        assertThat(cache.isDetailed()).isTrue();
        assertThat(cache.getOwnerDisplayName()).isEqualTo("glorkar");
        assertThat(cache.getOwnerUserId()).isEqualTo("19");
    }

    public static void testOCSearchMustWorkWithoutOAuthAccessTokens() {
        final String geoCode = "OC1234";
        final Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).overridingErrorMessage("You must have a valid OKAPI key installed for running this test (but you do not need to set credentials in the app).").isNotNull();
        assertThat(cache.getName()).isEqualTo("Wupper-Schein");
    }

    public static void testOCCacheWithWaypoints() {
        final String geoCode = "OCDDD2";
        removeCacheCompletely(geoCode);
        Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).as("Cache from OKAPI").isNotNull();
        // cache should be stored to DB (to listID 0) when loaded above
        cache = DataStore.loadCache(geoCode, LoadFlags.LOAD_ALL_DB_ONLY);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertThat(cache.getWaypoints()).hasSize(3);

        // load again
        cache.refreshSynchronous(null);
        assertThat(cache.getWaypoints()).hasSize(3);
    }

    public static void testOCWillAttendLogs() {
        final String geoCode = "OC6465";

        removeCacheCompletely(geoCode);
        final Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).as("Cache from OKAPI").isNotNull();
        assertThat(cache.getLogCounts().get(LogType.WILL_ATTEND)).isGreaterThan(0);
    }

    public static void testGetAllLogs() {
        final String geoCode = "OC10CB8";
        final Geocache cache = OkapiClient.getCache(geoCode);
        final int defaultLogCount = 10;
        assert cache != null; // eclipse null analysis
        assertThat(cache.getLogs().size()).isGreaterThan(defaultLogCount);
    }

    public static void testShortDescription() {
        final String geoCode = "OC10C06";
        final Geocache cache = OkapiClient.getCache(geoCode);
        assert cache != null; // eclipse null analysis
        assertThat(cache.getShortDescription()).isEqualTo("Nur in der fünften Jahreszeit kann er sprechen");
    }

    public static void testPreferredLanguage() {
        final Locale savedLocale = Locale.getDefault();
        final String userLanguage = Settings.getUserLanguage();
        try {
            Settings.putUserLanguage("");
            Locale.setDefault(Locale.US);
            assertThat(OkapiClient.getPreferredLanguage()).isEqualTo("en");     // US, useEnglish = false
            Settings.putUserLanguage("en");
            assertThat(OkapiClient.getPreferredLanguage()).isEqualTo("en");     // US, useEnglish = true
            Locale.setDefault(Locale.GERMANY);
            assertThat(OkapiClient.getPreferredLanguage()).isEqualTo("en|de");  // DE, useEnglish = true
            Settings.putUserLanguage("");
            assertThat(OkapiClient.getPreferredLanguage()).isEqualTo("de|en");  // DE, useEnglish = false
        } finally {
            Locale.setDefault(savedLocale);
            Settings.putUserLanguage(userLanguage);
        }
    }

    public static void testMobileRegistrationUrl() {
        // there is a plan to implement a mobile page, so in the future this test needs to be adapted
        assertThat(OkapiClient.getMobileRegistrationUrl(getConnectorOCDE())).isNull();
    }

    public static void testRegistrationUrl() {
        assertThat(OkapiClient.getRegistrationUrl(getConnectorOCDE())).isEqualTo("https://www.opencaching.de/register.php");
    }

    private static OCApiLiveConnector getConnectorOCDE() {
        final OCApiLiveConnector connector = (OCApiLiveConnector) ConnectorFactory.getConnector("OC0000");
        assertThat(connector).isNotNull();
        return connector;
    }

    public static void testLogImages() {
        final String geoCode = "OCFBC8";
        final Geocache cache = OkapiClient.getCache(geoCode);
        assert cache != null; // eclipse null analysis
        LogEntry logWithImage = null;
        for (final LogEntry logEntry : cache.getLogs()) {
            if ("mountainbatchers".equals(logEntry.author)) {
                logWithImage = logEntry;
            }
        }
        assertThat(logWithImage).isNotNull();
        assertThat(logWithImage.logImages).isNotEmpty();
    }

}
