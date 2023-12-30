package cgeo.geocaching.connector.oc;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.test.CgeoTestUtils;

import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class OkapiClientTest {

    @Test
    public void testGetOCCache() {
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

    @Test
    public void testOCSearchMustWorkWithoutOAuthAccessTokens() {
        final String geoCode = "OC1234";
        final Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).overridingErrorMessage("You must have a valid OKAPI key installed for running this test (but you do not need to set credentials in the app).").isNotNull();
        assertThat(cache.getName()).isEqualTo("Wupper-Schein");
    }

    @Test
    public void testOCCacheWithWaypoints() {
        final String geoCode = "OCDDD2";
        CgeoTestUtils.removeCacheCompletely(geoCode);
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

    @Test
    public void testOCWillAttendLogs() {
        final String geoCode = "OC6465";

        CgeoTestUtils.removeCacheCompletely(geoCode);
        final Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).as("Cache from OKAPI").isNotNull();
        assertThat(cache.getLogCounts().get(LogType.WILL_ATTEND)).isGreaterThan(0);
    }

    @Test
    public void testGetAllLogs() {
        final String geoCode = "OC10CB8";
        final Geocache cache = OkapiClient.getCache(geoCode);
        final int defaultLogCount = 10;
        assert cache != null; // eclipse null analysis
        assertThat(cache.getLogs().size()).isGreaterThan(defaultLogCount);
    }

    @Test
    public void testShortDescription() {
        final String geoCode = "OC10C06";
        final Geocache cache = OkapiClient.getCache(geoCode);
        assert cache != null; // eclipse null analysis
        assertThat(cache.getShortDescription()).isEqualTo("Nur in der fünften Jahreszeit kann er sprechen");
    }

    @Test
    public void testPreferredLanguage() {
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

    @Test
    public void testMobileRegistrationUrl() {
        // there is a plan to implement a mobile page, so in the future this test needs to be adapted
        assertThat(OkapiClient.getMobileRegistrationUrl(getConnectorOCDE())).isNull();
    }

    @Test
    public void testRegistrationUrl() {
        assertThat(OkapiClient.getRegistrationUrl(getConnectorOCDE())).isEqualTo("https://www.opencaching.de/register.php");
    }

    private static OCApiLiveConnector getConnectorOCDE() {
        final OCApiLiveConnector connector = (OCApiLiveConnector) ConnectorFactory.getConnector("OC0000");
        assertThat(connector).isNotNull();
        return connector;
    }

    @Test
    public void testLogImages() {
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

    @Test
    @Ignore("This tests needs a working OC account on the CI AVD")
    public void testUploadPersonalNote() {
        final String geoCode = "OCFBC8";
        final Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).as("Cache from OKAPI").isNotNull();
        final String oldPersonalNote = cache.getPersonalNote();

        final IConnector connector = ConnectorFactory.getConnector(geoCode);
        final OCApiConnector ocConnector = (OCApiConnector) connector;
        assertThat(ocConnector).isNotNull();

        try {
            final String longPersonalNote = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. \n" +
                    "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. \n" +
                    "Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. \n" +
                    "Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer possim assum. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. \n" +
                    "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis. \n" +
                    "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, At accusam aliquyam diam diam dolore dolores duo eirmod eos erat, et nonumy sed tempor et et invidunt justo labore Stet clita ea et gubergren, kasd magna no rebum. sanctus sea sed takimata ut vero voluptua. est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat. \n" +
                    "Consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. \n" +
                    "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. \n" +
                    "Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. \n" +
                    "Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer possim assum. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. \n" +
                    "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis. \n" +
                    "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, At accusam aliquyam diam diam dolore dolores duo eirmod eos erat, et nonumy sed tempor et et invidunt justo labore Stet clita ea et gubergren, kasd magna no rebum. sanctus sea sed takimata ut vero voluptua. est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat. \n" +
                    "Consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. \n" +
                    "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. \n" +
                    "Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. \n" +
                    "Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer possim assum. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. \n" +
                    "Duis au";
            cache.setPersonalNote(longPersonalNote);

            final Boolean noteUploaded = OkapiClient.uploadPersonalNotes(ocConnector, cache);
            assertThat(noteUploaded).as("Note uploaded").isTrue();

            // check if updated personal note is not truncated
            final Geocache updatedCache = OkapiClient.getCache(geoCode);
            assertThat(updatedCache).as("Updated Cache from OKAPI").isNotNull();
            final String updatedPersonalNote = updatedCache.getPersonalNote();
            assertThat(updatedPersonalNote).as("Updated Note").isEqualTo(longPersonalNote);
        } finally {
            cache.setPersonalNote(oldPersonalNote);
            OkapiClient.uploadPersonalNotes(ocConnector, cache);
        }
    }
}
