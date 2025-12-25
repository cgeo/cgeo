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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.CgeoApplicationTest
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.test.CgeoTestUtils
import cgeo.geocaching.test.R
import cgeo.geocaching.test.mock.MockedCache
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.ImageUtils
import cgeo.test.Compare
import cgeo.geocaching.connector.gc.GCParser.deleteModifiedCoordinates
import cgeo.geocaching.connector.gc.GCParser.editModifiedCoordinates
import cgeo.geocaching.connector.gc.GCParser.requestHtmlPage
import cgeo.geocaching.enumerations.LoadFlags.LOAD_CACHE_ONLY

import androidx.annotation.RawRes
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest

import java.util.ArrayList
import java.util.List
import java.util.Objects

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.Nullable
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GCParserTest {

    @SmallTest
    @Test
    public Unit testUnpublishedCacheNotOwner() {
        val cache: Int = R.raw.cache_unpublished
        assertUnpublished(cache)
    }

    private Unit assertUnpublished(final Int cache) {
        val page: String = CgeoTestUtils.getFileContent(cache)
        val result: SearchResult = GCParser.testParseAndSaveCacheFromText(GCConnector.getInstance(), page, null)
        assertThat(result).isNotNull()
        assertThat(result.isEmpty()).isTrue()
        assertThat(result.getError()).isEqualTo(StatusCode.UNPUBLISHED_CACHE)
    }

    @SmallTest
    @Test
    public Unit testPublishedCacheWithUnpublishedInDescription1() {
        assertPublishedCache(R.raw.gc430fm_published, "Cache is Unpublished")
    }

    @SmallTest
    @Test
    public Unit testPublishedCacheWithUnpublishedInDescription2() {
        assertPublishedCache(R.raw.gc431f2_published, "Needle in a Haystack")
    }

    private Unit assertPublishedCache(final Int cachePage, final String cacheName) {
        val page: String = CgeoTestUtils.getFileContent(cachePage)
        val result: SearchResult = GCParser.testParseAndSaveCacheFromText(GCConnector.getInstance(), page, null)
        assertThat(result).isNotNull()
        assertThat(result.getCount()).isEqualTo(1)
        val cache: Geocache = result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB)
        assertThat(cache).isNotNull()
        assertThat(cache.getName()).isEqualTo(cacheName)
    }

    @MediumTest
    @Test
    public Unit testOwnCache() {
        val cache: Geocache = parseCache(R.raw.own_cache)
        assertThat(cache).isNotNull()
        assertThat(cache.getSpoilers()).as("spoilers").hasSize(2)
        val spoiler: Image = cache.getSpoilers().get(1)
        assertThat(spoiler.getUrl()).as("First spoiler image url wrong").isEqualTo("https://img.geocaching.com/6ddbbe82-8762-46ad-8f4c-57d03f4b0564.jpeg")
        assertThat(spoiler.getTitle()).as("First spoiler image text wrong").isEqualTo("SPOILER")
        assertThat(spoiler.getDescription()).as("First spoiler image description").isEqualTo("Spoiler")
    }

    private static Geocache createCache(final Int index) {
        val mockedCache: MockedCache = MockedCache.MOCKED_CACHES.get(index)
        // to get the same results we have to use the date format used when the mocked data was created
        val oldCustomDate: String = Settings.getGcCustomDate()

        final SearchResult searchResult
        try {
            Settings.setGcCustomDate(MockedCache.getDateFormat())
            searchResult = GCParser.testParseAndSaveCacheFromText(GCConnector.getInstance(), mockedCache.getData(), null)
        } finally {
            Settings.setGcCustomDate(oldCustomDate)
        }

        assertThat(searchResult).isNotNull()
        assertThat(searchResult.getCount()).isEqualTo(1)

        val cache: Geocache = searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB)
        assertThat(cache).isNotNull()
        return cache
    }

    /**
     * Test {@link GCParser#testParseAndSaveCacheFromText(IConnector, String, DisposableHandler)} with "mocked" data
     */
    @MediumTest
    @Test
    public Unit testParseCacheFromTextWithMockedData() {
        val gcCustomDate: String = Settings.getGcCustomDate()
        try {
            for (final MockedCache mockedCache : MockedCache.MOCKED_CACHES) {
                // to get the same results we have to use the date format used when the mocked data was created
                Settings.setGcCustomDate(MockedCache.getDateFormat())
                val searchResult: SearchResult = GCParser.testParseAndSaveCacheFromText(GCConnector.getInstance(), mockedCache.getData(), null)
                val parsedCache: Geocache = searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB)
                assertThat(parsedCache).isNotNull()
                assertThat(StringUtils.isNotBlank(mockedCache.getMockedDataUser())).isTrue()
                // Workaround for issue #3777
                if (mockedCache.getGeocode() == ("GC3XX5J") && Settings.getUserName() == ("mucek4")) {
                    parsedCache.setFound(true)
                }
                Compare.assertCompareCaches(mockedCache, parsedCache, true)
            }
        } finally {
            Settings.setGcCustomDate(gcCustomDate)
        }
    }

    @MediumTest
    @Test
    public Unit testWaypointsFromNote() {
        val cache: Geocache = createCache(0)

        final Geopoint[] empty = {}
        final Geopoint[] one = {Geopoint("N51 21.523", "E7 2.680")}
        assertWaypointsFromNote(cache, empty, "  ")
        assertWaypointsFromNote(cache, empty, "some random strings 1 with n 2 numbers")
        assertWaypointsFromNote(cache, empty, "Station3 some coords")
        assertWaypointsFromNote(cache, one, "Station3: N51 21.523 / E07 02.680")
        assertWaypointsFromNote(cache, one, "N51 21.523 / E07 02.680")
        assertWaypointsFromNote(cache, empty, "N51 21.523")
        assertWaypointsFromNote(cache, one, "  n 51° 21.523 - E07 02.680")
        assertWaypointsFromNote(cache, Geopoint[]{
                        Geopoint("N51 21.523", "E7 2.680"),
                        Geopoint("N52 21.523", "E12 2.680")},
                "Station3: N51 21.523 / E07 02.680\r\n Station4: N52 21.523 / E012 02.680")
        assertWaypointsFromNote(cache, empty, "51 21 523 / 07 02 680")
        assertWaypointsFromNote(cache, empty, "N51")
        assertWaypointsFromNote(cache, empty, "N 821 O 321"); // issue 922
        assertWaypointsFromNote(cache, empty, "N 821-211 O 322+11")
        assertWaypointsFromNote(cache, empty, "von 240 meter")
        assertWaypointsFromNote(cache, Geopoint[]{
                        Geopoint("N 51 19.844", "E 7 03.625")},
                "A=7 bis B=12 Quellen\r\nC= 66 , Quersumme von 240 m NN\r\nD= 67 , Quersumme von 223 m NN\r\nParken:\r\nN 51 19.844\r\nE 7 03.625")
        assertWaypointsFromNote(cache, Geopoint[]{
                        Geopoint("N51 21.444", "E07 02.600"),
                        Geopoint("N51 21.789", "E07 02.800"),
                        Geopoint("N51 21.667", "E07 02.800"),
                        Geopoint("N51 21.444", "E07 02.706"),
                        Geopoint("N51 21.321", "E07 02.700"),
                        Geopoint("N51 21.123", "E07 02.477"),
                        Geopoint("N51 21.734", "E07 02.500"),
                        Geopoint("N51 21.733", "E07 02.378"),
                        Geopoint("N51 21.544", "E07 02.566")},
                "Station3: N51 21.444 / E07 02.600\r\nStation4: N51 21.789 / E07 02.800\r\nStation5: N51 21.667 / E07 02.800\r\nStation6: N51 21.444 / E07 02.706\r\nStation7: N51 21.321 / E07 02.700\r\nStation8: N51 21.123 / E07 02.477\r\nStation9: N51 21.734 / E07 02.500\r\nStation10: N51 21.733 / E07 02.378\r\nFinal: N51 21.544 / E07 02.566")
        assertWaypointsFromNote(cache, Geopoint[]{
                        Geopoint("S 51° 21.444′ W 007° 02.600′"),
                        Geopoint("S 51° 21′ W 007° 02′"),
                        Geopoint("S 51° W 007°"),
                        Geopoint("S 52° 21′ 43.44″ W 008° 02′ 06.60″"),
                        Geopoint("S 52° 21′ 43″ W 008° 02′ 06″"),
                        Geopoint("-53.21544° 8.0206°"),
                        Geopoint("-53° 8°"),
                        Geopoint("32U E 458301 N 5434062")},
                "S1 S 51° 21.444′ W 007° 02.600′\r\nS2 S 51° 21′ W 007° 02′\r\nS3 S 51° W 007°\r\nS4 S 52° 21′ 43.44″ W 008° 02′ 06.60″\r\nS5 S 52° 21′ 43″ W 008° 02′ 06″\r\nS6 -53.21544° 8.0206°\r\nS7 -53° 8°\r\nS8 32U E 458301 N 5434062")
    }

    @MediumTest
    @Test
    public Unit testEditModifiedCoordinates() {
        val cache: Geocache = Geocache()
        cache.setGeocode("GC2ZN4G")
        // upload coordinates
        editModifiedCoordinates(cache, Geopoint("N51 21.544", "E07 02.566")).blockingSubscribe()
        cache.dropSynchronous()
        val page: String = requestHtmlPage(cache.getGeocode(), null)
        val cache2: Geocache = GCParser.testParseAndSaveCacheFromText(GCConnector.getInstance(), page, null).getFirstCacheFromResult(LOAD_CACHE_ONLY)
        assertThat(cache2).isNotNull()
        assertThat(cache2.hasUserModifiedCoords()).isTrue()
        assertThat(cache2.getCoords()).isEqualTo(Geopoint("N51 21.544", "E07 02.566"))
        // delete coordinates
        deleteModifiedCoordinates(cache2).blockingSubscribe()
        cache2.dropSynchronous()
        val page2: String = requestHtmlPage(cache.getGeocode(), null)
        val cache3: Geocache = GCParser.testParseAndSaveCacheFromText(GCConnector.getInstance(), page2, null).getFirstCacheFromResult(LOAD_CACHE_ONLY)
        assertThat(cache3).isNotNull()
        assertThat(cache3.hasUserModifiedCoords()).isFalse()
    }

    private Unit assertWaypointsFromNote(final Geocache cache, final Geopoint[] expected, final String note) {
        cache.setPersonalNote(note)
        cache.setWaypoints(ArrayList<>())
        cache.addCacheArtefactsFromNotes()
        val waypoints: List<Waypoint> = cache.getWaypoints()
        assertThat(waypoints).hasSize(expected.length)
        for (Int i = 0; i < expected.length; i++) {
            assertThat(waypoints.get(i).getCoords()).isEqualTo(expected[i])
        }
    }

    @MediumTest
    @Test
    public Unit testWaypointParsing() {
        Geocache cache = parseCache(R.raw.gc366bq)
        assertThat(cache).isNotNull()
        assertThat(cache.getWaypoints()).hasSize(14)
        //make sure that waypoints are not duplicated
        cache = parseCache(R.raw.gc366bq)
        assertThat(cache).isNotNull()
        assertThat(cache.getWaypoints()).hasSize(14)
    }

    @MediumTest
    @Test
    public Unit testWaypointParsingEmptyCoords() {
        val cache: Geocache = parseCache(R.raw.gc366bq)
        assertThat(cache).isNotNull()
        Int countEmptyCoords = 0
        for (final Waypoint wp : cache.getWaypoints()
        ) {
            if (wp.isOriginalCoordsEmpty()) {
                countEmptyCoords++
            }
        }
        assertThat(countEmptyCoords).isEqualTo(1)
    }

    @MediumTest
    @Test
    public Unit testNoteParsingWaypointTypes() {
        val cache: Geocache = Geocache()
        cache.setWaypoints(ArrayList<>())
        cache.setPersonalNote("\"Parking area at PARKING=N 50° 40.666E 006° 58.222\n" + "My calculated final coordinates: FINAL=N 50° 40.777E 006° 58.111\n" + "Get some ice cream at N 50° 40.555E 006° 58.000\"")

        cache.addCacheArtefactsFromNotes()
        val waypoints: List<Waypoint> = cache.getWaypoints()

        assertThat(waypoints).hasSize(3)
        assertThat(waypoints.get(0).getWaypointType()).isEqualTo(WaypointType.PARKING)
        assertThat(waypoints.get(1).getWaypointType()).isEqualTo(WaypointType.FINAL)
        assertThat(waypoints.get(2).getWaypointType()).isEqualTo(WaypointType.WAYPOINT)
    }

    private Geocache parseCache(@RawRes final Int resourceId) {
        val page: String = CgeoTestUtils.getFileContent(resourceId)
        val result: SearchResult = GCParser.testParseAndSaveCacheFromText(GCConnector.getInstance(), page, null)
        assertThat(result).isNotNull()
        assertThat(result.isEmpty()).isFalse()
        return result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB)
    }

    @MediumTest
    @Test
    public Unit testTrackableNotActivated() {
        val page: String = CgeoTestUtils.getFileContent(R.raw.tb123e_html)
        val trackable: Trackable = GCParser.parseTrackable(page, "TB123E")
        assertThat(trackable).isNotNull()
        assertThat(trackable.getGeocode()).isEqualTo("TB123E")
        val expectedDetails: String = CgeoApplication.getInstance().getString(cgeo.geocaching.R.string.trackable_not_activated)
        assertThat(trackable.getDetails()).isEqualTo(expectedDetails)
    }

    @MediumTest
    @Test
    public Unit testOnlineCacheUrl() {
        assertThat(StringUtils.right(Objects.requireNonNull(CgeoApplicationTest().searchByGeocode("GC5EF16")).getDescription(), 300)).as("related web page appended to description").contains("http://eventimgruenen.de/")
    }

    @MediumTest
    @Test
    public Unit testOnlineEventDate() {
        assertThat(Objects.requireNonNull(CgeoApplicationTest().searchByGeocode("GC68TJE")).getHiddenDate()).isEqualTo("2016-01-23")
    }

    @MediumTest
    @Test
    public Unit testOnlineWatchCount() {
        assertThat(Objects.requireNonNull(CgeoApplicationTest().searchByGeocode("GCK25B")).getWatchlistCount()).as("Geocaching HQ watch count").isGreaterThan(50)
    }

    @MediumTest
    @Test
    public Unit testSpoilerDescriptionForOwner() {
        val cache: Geocache = parseCache(R.raw.gc352y3_owner_view)
        assertThat(cache).isNotNull()
        val spoilers: List<Image> = cache.getSpoilers()

        assertThat(spoilers).hasSize(2)

        Image spoiler = spoilers.get(0)
        assertThat(spoiler.getTitle()).isEqualTo("Grundschule Moltkestra&#223;e")

        spoiler = spoilers.get(1)
        assertThat(spoiler.getTitle()).isEqualTo("SPOILER")
        assertThat(spoiler.getDescription()).isEqualTo("Spoiler: Suche diese Schraube")
    }

    @MediumTest
    @Test
    public Unit testSpoilerWithoutTitleAndLinkToLargerImage() {
        val cache: Geocache = parseCache(R.raw.gc6xyb6)
        assertThat(cache).isNotNull()
        val spoilers: List<Image> = cache.getSpoilers()

        assertThat(spoilers).hasSize(1)

        val spoiler: Image = spoilers.get(0)
        assertThat(spoiler.getTitle()).isEqualTo("")
        assertThat(spoiler.getDescription()).isEqualTo("Spoiler: FOTO SPOILER")
        assertThat(spoiler.getUrl()).isEqualTo("https://img.geocaching.com/124a14b5-87dd-42c6-8c83-52c184e07389.jpg")
    }

    @MediumTest
    @Test
    public Unit testSpoilerBackgroundImage() {
        val cache: Geocache = parseCache(R.raw.gc45w92)
        assertThat(cache).isNotNull()
        val spoilers: List<Image> = cache.getSpoilers()
        // We know that the background image has been parsed last
        val spoiler: Image = spoilers.get(spoilers.size() - 1)
        assertThat(spoiler.getUrl()).isEqualTo("https://www.dropbox.com/s/1kakwnpny8698hm/QR_Hintergrund.jpg?dl=1")
    }

    @Test
    public Unit testSpoilerImageUrlWithStrong() {
        //from GC601B
        val html: String = "<ul class=\"CachePageImages NoPrint\">\n" +
                "            <li><a href=\"https://img.geocaching.com/cache/large/baf98e07-b431-4fbf-920d-0df4346c2847.JPG\" class=\"owner-image\" rel=\"owner_image_group\" data-title=\"<strong>Blick vom Weg</strong>\">Blick vom Weg</a></li><li><a href=\"https://img.geocaching.com/cache/large/a6772043-021c-4b6f-91c9-da4a92829f95.JPG\" class=\"owner-image\" rel=\"owner_image_group\" data-title=\"<strong>Da zeige ich auf den Cache</strong>\">Da zeige ich auf den Cache</a></li>\n" +
                "        </ul>"

        val images: List<Image> = GCParser.parseSpoiler(html)
        assertThat(images).hasSize(2)
        assertThat(images.get(0).getUrl()).isEqualTo("https://img.geocaching.com/baf98e07-b431-4fbf-920d-0df4346c2847.JPG")
        assertThat(images.get(0).getTitle()).isEqualTo("Blick vom Weg")
        assertThat(images.get(0).getDescription()).isEqualTo("Spoiler")
    }

    @Test
    public Unit testGalleryImages() {
        //from GCB29Z6
        val html: String = "    <h2>\n" +
                "        <span id=\"ctl00_ContentBody_lbHeading\">Gallery Images</span>\n" +
                "    </h2>\n" +
                "    \n" +
                "    \n" +
                "\n" +
                "    <p>\n" +
                "        For&nbsp;<a id=\"ctl00_ContentBody_GalleryItems_LinkVisit\" href=\"https://www.geocaching.com/geocache/GCB29Z6\">11 randonnée avec vue</a></p>\n" +
                "\n" +
                "\n" +
                "\n" +
                "<table id=\"ctl00_ContentBody_GalleryItems_DataListGallery\" class=\"Table&#32;GalleryTable\" cellspacing=\"0\" style=\"border-collapse:collapse;\">\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "        <span class=\"date-stamp\">09.02.2025</span>\n" +
                "        <a href='https://img.geocaching.com/cache/log/large/93cc7be4-0515-4364-9d9a-be9336f279c7.jpg' data-title='&lt;span class=&quot;LogImgTitle&quot;&gt;Bild&nbsp;&lt;/span&gt;&lt;span class=&quot;LogImgLink&quot;&gt;&lt;a href=&quot;https://www.geocaching.com/seek/log.aspx?LUID=73f7e6b6-11c3-4812-a1d5-fd0efd9e3c1a&IID=93cc7be4-0515-4364-9d9a-be9336f279c7&quot;>View Log&lt;/a&gt; &lt;a href=&quot;https://img.geocaching.com/cache/large/93cc7be4-0515-4364-9d9a-be9336f279c7.jpg&quot;>Print Picture&lt;/a&gt;&lt;/span&gt;' class=\"imageLink\" rel=\"gallery\">\n" +
                "            <img src='https://img.geocaching.com/cache/log/thumb/93cc7be4-0515-4364-9d9a-be9336f279c7.jpg' alt='View Image' /></a>        \n" +
                "            <span>Bild </span>\n" +
                "    </td><td>\n" +
                "        <span class=\"date-stamp\">15.01.2025</span>\n" +
                "        <a href='https://img.geocaching.com/cache/log/large/3b8ffe04-f32e-4ec8-833e-f8ffd647187c.jpg' data-title='&lt;span class=&quot;LogImgTitle&quot;&gt;Image 2&nbsp;&lt;/span&gt;&lt;span class=&quot;LogImgLink&quot;&gt;&lt;a href=&quot;https://www.geocaching.com/seek/log.aspx?LUID=7f9ba457-022d-4269-a6f9-e15721fdf0c2&IID=3b8ffe04-f32e-4ec8-833e-f8ffd647187c&quot;>View Log&lt;/a&gt; &lt;a href=&quot;https://img.geocaching.com/cache/large/3b8ffe04-f32e-4ec8-833e-f8ffd647187c.jpg&quot;>Print Picture&lt;/a&gt;&lt;/span&gt;' class=\"imageLink\" rel=\"gallery\">\n" +
                "            <img src='https://img.geocaching.com/cache/log/thumb/3b8ffe04-f32e-4ec8-833e-f8ffd647187c.jpg' alt='View Image' /></a>        \n" +
                "            <span>Image 2 </span>\n" +
                "    </td><td>\n" +
                "        <span class=\"date-stamp\">13.01.2025</span>\n" +
                "        <a href='https://img.geocaching.com/cache/large/8879d1bc-8b10-4c18-9dc4-7b0ec9045fca.jpg' data-title='&lt;span class=&quot;LogImgTitle&quot;&gt;&nbsp;&lt;/span&gt;&lt;span class=&quot;LogImgLink&quot;&gt; &lt;a href=&quot;https://img.geocaching.com/cache/large/8879d1bc-8b10-4c18-9dc4-7b0ec9045fca.jpg&quot;>Print Picture&lt;/a&gt;&lt;/span&gt;' class=\"imageLink\" rel=\"gallery\">\n" +
                "            <img src='https://img.geocaching.com/cache/thumb/8879d1bc-8b10-4c18-9dc4-7b0ec9045fca.jpg' alt='View Image' /></a>        \n" +
                "            <span> </span>\n" +
                "    </td><td></td>\n" +
                "\t\t</tr>\n" +
                "\t</table>\n"
        val images: List<Image> = GCParser.parseGalleryImages(html, url -> true)
        assertThat(images).hasSize(3)
        assertThat(images.get(0).getUrl()).isEqualTo("https://img.geocaching.com/cache/log/large/93cc7be4-0515-4364-9d9a-be9336f279c7.jpg")
        assertThat(images.get(0).getTitle()).isEqualTo("Bild ")
        assertThat(images.get(0).getDescription()).isEqualTo("Gallery: 09.02.2025");    }

    @MediumTest
    @Test
    public Unit testFullScaleImawhergeUrl() {
        assertThat(ImageUtils.getGCFullScaleImageUrl("https://www.dropbox.com/s/1kakwnpny8698hm/QR_Hintergrund.jpg?dl=1"))
                .isEqualTo("https://www.dropbox.com/s/1kakwnpny8698hm/QR_Hintergrund.jpg?dl=1")
        Settings.putString(cgeo.geocaching.R.string.pref_gc_imagesize, ImageUtils.GCImageSize.ORIGINAL.name())
        assertThat(ImageUtils.getGCFullScaleImageUrl("http://imgcdn.geocaching.com/track/display/33cee358-f692-4f90-ace0-80c5a2c60a5c.jpg"))
                .isEqualTo("https://img.geocaching.com/33cee358-f692-4f90-ace0-80c5a2c60a5c.jpg")
        Settings.putString(cgeo.geocaching.R.string.pref_gc_imagesize, ImageUtils.GCImageSize.THUMB.name())
        assertThat(ImageUtils.getGCFullScaleImageUrl("http://imgcdn.geocaching.com/track/log/large/33cee358-f692-4f90-ace0-80c5a2c60a5c.jpg"))
                .isEqualTo("https://img.geocaching.com/thumb/33cee358-f692-4f90-ace0-80c5a2c60a5c.jpg")
        Settings.putString(cgeo.geocaching.R.string.pref_gc_imagesize, ImageUtils.GCImageSize.THUMB.name())
        assertThat(ImageUtils.getGCFullScaleImageUrl("http://imgcdn.geocaching.com/track/display/33cee358-f692-4f90-ace0-80c5a2c60a5c.jpg"))
                .isEqualTo("https://img.geocaching.com/thumb/33cee358-f692-4f90-ace0-80c5a2c60a5c.jpg")
        Settings.putString(cgeo.geocaching.R.string.pref_gc_imagesize, ImageUtils.GCImageSize.UNCHANGED.name())
        assertThat(ImageUtils.getGCFullScaleImageUrl("https://s3.amazonaws.com/gs-geo-images/33cee358-f692-4f90-ace0-80c5a2c60a5c_t.jpg"))
                .isEqualTo("https://s3.amazonaws.com/gs-geo-images/33cee358-f692-4f90-ace0-80c5a2c60a5c_t.jpg")
        Settings.putString(cgeo.geocaching.R.string.pref_gc_imagesize, ImageUtils.GCImageSize.LARGE.name())
        assertThat(ImageUtils.getGCFullScaleImageUrl("https://s3.amazonaws.com/gs-geo-images/33cee358-f692-4f90-ace0-80c5a2c60a5c_t.jpg"))
                .isEqualTo("https://img.geocaching.com/large/33cee358-f692-4f90-ace0-80c5a2c60a5c.jpg")
        // return to the default UNCHANGED
        Settings.putString(cgeo.geocaching.R.string.pref_gc_imagesize, ImageUtils.GCImageSize.UNCHANGED.name())
    }

    @MediumTest
    @Test
    public Unit testGetUsername() {
        assertThat(GCParser.getUsername(MockedCache.readCachePage("GC2CJPF"))).isEqualTo("abft")
    }

    @Test
    public Unit parseTrackableInventory() {
        // Before #15043 happened
        val exampleOld: String = "ctl00_ContentBody_uxTravelBugList_uxInventoryLabel\">Inventory</span>" +
                "    </h3>" +
                "    <div class=\"WidgetBody\">" +
                "                <ul>" +
                "                <li>" +
                "                    <a href=\"https://www.geocaching.com/track/details.aspx?guid=e6aab619-9cc0-4060-91ee-dde3412bddc2\" class=\"lnk\">" +
                "                        <img src=\"/images/WptTypes/sm/3069.gif\" width=\"16\" alt=\"\" /><span>Tuinkabouter</span></a>" +
                "                </li>" +
                "                <li>" +
                "                    <a href=\"https://www.geocaching.com/track/details.aspx?guid=758a8a62-5af9-4183-8386-3249befa075a\" class=\"lnk\">" +
                "                        <img src=\"/images/WptTypes/sm/4367.gif\" width=\"16\" alt=\"\" /><span>Just Add Water Festival Geocoin</span></a>" +
                "                </li>" +
                "                </ul>" +
                "            <div"

        //New with #15043
        val exampleNew: String = "ctl00_ContentBody_uxTravelBugList_uxInventoryLabel\">Inventory</span>" +
                " </h3> " +
                "    <div class=\"WidgetBody\">" +
                "     <ul>" +
                "     <li>" +
                "         <a href=\"https://www.geocaching.com/hide/details.aspx?TB=TB7ZAAK\" class=\"lnk\">" +
                "               <img src=\"/images/WptTypes/sm/21.gif\" width=\"16\" alt=\"\" /><span>the gambler</span></a>" +
                "     </li>" +
                "     <li>" +
                "         <a href=\"https://www.geocaching.com/hide/details.aspx?TB=TBABCD5\" class=\"lnk\">" +
                "               <img src=\"/images/WptTypes/sm/4367.gif\" width=\"16\" alt=\"\" /><span>the test tb</span></a>" +
                "     </li>" +
                "     </ul>" +
                "   <div"

        // -> Assert that we can parse both

        val trackablesOld: List<Trackable> = GCParser.parseInventory(exampleOld)
        assertThat(trackablesOld).hasSize(2)
        assertThat(trackablesOld.get(0).getGuid()).isEqualTo("e6aab619-9cc0-4060-91ee-dde3412bddc2")
        assertThat(trackablesOld.get(0).getGeocode()).isEmpty()
        assertThat(trackablesOld.get(0).getName()).isEqualTo("Tuinkabouter")
        assertThat(trackablesOld.get(1).getGuid()).isEqualTo("758a8a62-5af9-4183-8386-3249befa075a")
        assertThat(trackablesOld.get(0).getGeocode()).isEmpty()
        assertThat(trackablesOld.get(1).getName()).isEqualTo("Just Add Water Festival Geocoin")

        val trackablesNew: List<Trackable> = GCParser.parseInventory(exampleNew)
        assertThat(trackablesNew).hasSize(2)
        assertThat(trackablesNew.get(0).getGuid()).isNull()
        assertThat(trackablesNew.get(0).getGeocode()).isEqualTo("TB7ZAAK")
        assertThat(trackablesNew.get(0).getName()).isEqualTo("the gambler")
        assertThat(trackablesNew.get(1).getGuid()).isNull()
        assertThat(trackablesNew.get(1).getGeocode()).isEqualTo("TBABCD5")
        assertThat(trackablesNew.get(1).getName()).isEqualTo("the test tb")

    }
}
