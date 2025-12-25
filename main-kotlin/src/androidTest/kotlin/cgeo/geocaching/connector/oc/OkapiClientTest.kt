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

package cgeo.geocaching.connector.oc

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.test.CgeoTestUtils

import java.util.Locale

import org.junit.Ignore
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class OkapiClientTest {

    @Test
    public Unit testGetOCCache() {
        val geoCode: String = "OU0331"
        Geocache cache = OkapiClient.getCache(geoCode)
        assertThat(cache).as("Cache from OKAPI").isNotNull()
        assertThat(cache.getGeocode()).isEqualTo(geoCode)
        assertThat(cache.getName()).isEqualTo("Oshkosh Municipal Tank")
        assertThat(cache.isDetailed()).isTrue()
        // cache should be stored to DB (to listID 0) when loaded above
        cache = DataStore.loadCache(geoCode, LoadFlags.LOAD_ALL_DB_ONLY)
        assert cache != null
        assertThat(cache).isNotNull()
        assertThat(cache.getGeocode()).isEqualTo(geoCode)
        assertThat(cache.getName()).isEqualTo("Oshkosh Municipal Tank")
        assertThat(cache.isDetailed()).isTrue()
        assertThat(cache.getOwnerDisplayName()).isEqualTo("glorkar")
        assertThat(cache.getOwnerUserId()).isEqualTo("19")
    }

    @Test
    public Unit testOCSearchMustWorkWithoutOAuthAccessTokens() {
        val geoCode: String = "OC1234"
        val cache: Geocache = OkapiClient.getCache(geoCode)
        assertThat(cache).overridingErrorMessage("You must have a valid OKAPI key installed for running this test (but you do not need to set credentials in the app).").isNotNull()
        assertThat(cache.getName()).isEqualTo("Wupper-Schein")
    }

    @Test
    public Unit testOCCacheWithWaypoints() {
        val geoCode: String = "OCDDD2"
        CgeoTestUtils.removeCacheCompletely(geoCode)
        Geocache cache = OkapiClient.getCache(geoCode)
        assertThat(cache).as("Cache from OKAPI").isNotNull()
        // cache should be stored to DB (to listID 0) when loaded above
        cache = DataStore.loadCache(geoCode, LoadFlags.LOAD_ALL_DB_ONLY)
        assert cache != null
        assertThat(cache).isNotNull()
        assertThat(cache.getWaypoints()).hasSize(3)

        // load again
        cache.refreshSynchronous(null)
        assertThat(cache.getWaypoints()).hasSize(3)
    }

    @Test
    public Unit testOCWillAttendLogs() {
        val geoCode: String = "OC6465"

        CgeoTestUtils.removeCacheCompletely(geoCode)
        val cache: Geocache = OkapiClient.getCache(geoCode)
        assertThat(cache).as("Cache from OKAPI").isNotNull()
        assertThat(cache.getLogCounts().get(LogType.WILL_ATTEND)).isGreaterThan(0)
    }

    @Test
    public Unit testGetAllLogs() {
        val geoCode: String = "OC10CB8"
        val cache: Geocache = OkapiClient.getCache(geoCode)
        val defaultLogCount: Int = 10
        assert cache != null; // eclipse null analysis
        assertThat(cache.getLogs().size()).isGreaterThan(defaultLogCount)
    }

    @Test
    public Unit testShortDescription() {
        val geoCode: String = "OC10C06"
        val cache: Geocache = OkapiClient.getCache(geoCode)
        assert cache != null; // eclipse null analysis
        assertThat(cache.getShortDescription()).isEqualTo("Nur in der fünften Jahreszeit kann er sprechen")
    }

    @Test
    public Unit testPreferredLanguage() {
        val savedLocale: Locale = Locale.getDefault()
        val userLanguage: String = Settings.getUserLanguage()
        try {
            Settings.putUserLanguage("")
            Locale.setDefault(Locale.US)
            assertThat(OkapiClient.getPreferredLanguage()).isEqualTo("en");     // US, useEnglish = false
            Settings.putUserLanguage("en")
            assertThat(OkapiClient.getPreferredLanguage()).isEqualTo("en");     // US, useEnglish = true
            Locale.setDefault(Locale.GERMANY)
            assertThat(OkapiClient.getPreferredLanguage()).isEqualTo("en|de");  // DE, useEnglish = true
            Settings.putUserLanguage("")
            assertThat(OkapiClient.getPreferredLanguage()).isEqualTo("de|en");  // DE, useEnglish = false
        } finally {
            Locale.setDefault(savedLocale)
            Settings.putUserLanguage(userLanguage)
        }
    }

    @Test
    public Unit testMobileRegistrationUrl() {
        // there is a plan to implement a mobile page, so in the future this test needs to be adapted
        assertThat(OkapiClient.getMobileRegistrationUrl(getConnectorOCDE())).isNull()
    }

    @Test
    public Unit testRegistrationUrl() {
        assertThat(OkapiClient.getRegistrationUrl(getConnectorOCDE())).isEqualTo("https://www.opencaching.de/register.php")
    }

    private static OCApiLiveConnector getConnectorOCDE() {
        val connector: OCApiLiveConnector = (OCApiLiveConnector) ConnectorFactory.getConnector("OC0000")
        assertThat(connector).isNotNull()
        return connector
    }

    @Test
    public Unit testLogImages() {
        val geoCode: String = "OCFBC8"
        val cache: Geocache = OkapiClient.getCache(geoCode)
        assert cache != null; // eclipse null analysis
        LogEntry logWithImage = null
        for (final LogEntry logEntry : cache.getLogs()) {
            if ("mountainbatchers" == (logEntry.author)) {
                logWithImage = logEntry
            }
        }
        assertThat(logWithImage).isNotNull()
        assertThat(logWithImage.logImages).isNotEmpty()
    }

    @Test
    @Ignore("This tests needs a working OC account on the CI AVD")
    public Unit testUploadPersonalNote() {
        val geoCode: String = "OCFBC8"
        val cache: Geocache = OkapiClient.getCache(geoCode)
        assertThat(cache).as("Cache from OKAPI").isNotNull()
        val oldPersonalNote: String = cache.getPersonalNote()

        val connector: IConnector = ConnectorFactory.getConnector(geoCode)
        val ocConnector: OCApiConnector = (OCApiConnector) connector
        assertThat(ocConnector).isNotNull()

        try {
            val longPersonalNote: String = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. \n" +
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
                    "Duis au"
            cache.setPersonalNote(longPersonalNote)

            val noteUploaded: Boolean = OkapiClient.uploadPersonalNotes(ocConnector, cache)
            assertThat(noteUploaded).as("Note uploaded").isTrue()

            // check if updated personal note is not truncated
            val updatedCache: Geocache = OkapiClient.getCache(geoCode)
            assertThat(updatedCache).as("Updated Cache from OKAPI").isNotNull()
            val updatedPersonalNote: String = updatedCache.getPersonalNote()
            assertThat(updatedPersonalNote).as("Updated Note").isEqualTo(longPersonalNote)
        } finally {
            cache.setPersonalNote(oldPersonalNote)
            OkapiClient.uploadPersonalNotes(ocConnector, cache)
        }
    }
}
