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

package cgeo.geocaching.connector.su

import cgeo.geocaching.connector.UserInfo
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.utils.JsonUtils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.List
import java.util.Locale

import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class SuParserTest {

    private static UserInfo user
    private static val userJson: String = "{\"status\":{\"code\":\"OK\"},\"data\":{\"id\":68451,\"name\":\"lega4\",\"foundCaches\":594,\"hiddenCaches\":28}}"
    private static val emptyUserJson: String = "{\"status\":{\"code\":\"OK\"},\"data\":{}}"
    private static Geocache cache
    private static val cacheJson: String = "{\"status\":{\"code\":\"OK\"}," +
            "\"data\":{" +
            "\"id\":3749," +
            "\"code\":\"TR3749\"," +
            "\"name\":\"Крестовоздвиженский монастырь\"," +
            "\"author\":{\"id\":20,\"name\":\"Инструктор\"}," +
            "\"latitude\":55.51,\"longitude\":37.8533333," +
            "\"type\":2,\"typeString\":\"TraditionalMultistep\"," +
            "\"subtype\":0," +
            "\"attributes\":[{\"id\":9,\"name\":\"scenic\"},{\"id\":14,\"name\":\"parking\"},{\"id\":16,\"name\":\"invalid\"},{\"id\":17,\"name\":\"dogs\"},{\"id\":18,\"name\":\"campfires\"},{\"id\":19,\"name\":\"watch\"},{\"id\":5,\"name\":\"kids\"}]," +
            "\"class\":\"2,6\"," +
            "\"status\":2,\"statusString\":\"Active\"," +
            "\"status2\":2,\"status2String\":\"Active\"," +
            "\"difficulty\":1,\"area\":5," +
            "\"size\":3," +
            "\"founds\":23," +
            "\"notfounds\":12," +
            "\"isFound\":false," +
            "\"is_watched\":true," +
            "\"recommendations\":44," +
            "\"personal_note\":\"My note\"," +
            "\"votes\":23," +
            "\"rating\":4.3," +
            "\"dateHidden\":\"2008-04-05\"," +
            "\"description\":{" +
            "\"container\":\"0. Карандаш, блокнот, толя игроков\"," +
            "\"cache\":\"благодарность .\"," +
            "\"area\":\"Some area description\"," +
            "\"traditionalPart\":\"Спрятан\"," +
            "\"virtualPart\":\"Значение\"," +
            "\"isHtml\":true}," +

            "\"logs\":[" +
            "{\"id\":407324,\"type\":1,\"typeString\":\"found\",\"author\":{\"id\":7255,\"name\":\"LE\"},\"date\":\"2018-10-20 18:12:46\",\"text\":\" Посетил мпан землёй. Рыть глубоко не надо!\"}," +
            "{\"id\":372954,\"type\":1,\"own\":1,\"typeString\":\"found\",\"author\":{\"id\":35161,\"name\":\"Vlad374\"},\"date\":\"2018-03-09 10:53:52\",\"text\":\" Посетили ения монастыря отобедали в монастырской трапезной.\"}," +
            "{\"id\":262481,\"type\":5,\"typeString\":\"repaired\",\"author\":{\"id\":29271,\"name\":\"delia и shimp\"},\"date\":\"2015-08-08 22:46:11\",\"text\":\" 08.08.2015 г. ВосстановВсем успехов! \"}," +
            "{\"id\":162424,\"type\":1,\"typeString\":\"found\",\"author\":{\"id\":29657,\"name\":\"Mariyka1986\"},\"date\":\"2012-11-19 09:47:12\",\"text\":\" Вчера постели даннывиртуальный вопрос. \"}," +
            "{\"id\":136788,\"type\":1,\"typeString\":\"found\",\"author\":{\"id\":21675,\"name\":\"mousquet\"},\"date\":\"2012-02-22 16:38:29\",\"text\":\" Был на месбавил вратарь, попробуйте с ним поговорить...\\r\\n\"}," +
            "{\"id\":130359,\"type\":1,\"typeString\":\"found\",\"author\":{\"id\":25507,\"name\":\"medicus\"},\"date\":\"2011-10-27 18:00:42\",\"text\":\" Сегодня посетили сей монастырь. Жена на территорию не пошла т.к. не было юбки опрос. Спасибо.\"}," +
            "{\"id\":121797,\"type\":1,\"typeString\":\"found\",\"author\":{\"id\":33891,\"name\":\"iog\"},\"date\":\"2011-08-07 16:41:56\",\"text\":\" 7.08.11 около 12.30 тайник разорен, рядом с лежащим стволом лежал разорванный черный пакет.\"}]," +


            "\"waypoints\":[" +
            "{\"type\":3,\"lat\":1.23,\"lon\":2.132,\"name\":\"Имя точки\",\"text\":\"Waypoint some nice description\"}," +
            "{\"type\":2,\"lat\":2,\"lon\":2,\"name\":\"Имя еще одной точки\",\"text\":\"Find something\"}" +
            "]," +

            "\"images\":[" +
            "{\"id\":3749,\"type\":\"cachePhoto\",\"url\":\"https://geocaching.su/photos/caches/3749.jpg\"}," +
            "{\"id\":15090,\"type\":\"areaPhoto\",\"url\":\"https://geocaching.su/photos/areas/15090.jpg\",\"description\":\"Ограда монастыря и собор\"}," +
            "{\"id\":15091,\"type\":\"areaPhoto\",\"url\":\"https://geocaching.su/photos/areas/15091.jpg\",\"description\":\"Временная звонница и &quot;Васильевский&quot; келейный корпус\"}," +
            "{\"id\":15092,\"type\":\"areaPhoto\",\"url\":\"https://geocaching.su/photos/areas/15092.jpg\",\"description\":\"Домик игуменьи\"}," +
            "{\"id\":15093,\"type\":\"areaPhoto\",\"url\":\"https://geocaching.su/photos/areas/15093.jpg\",\"description\":\"Крестовоздвиженская церковь\"}," +
            "{\"id\":15094,\"type\":\"areaPhoto\",\"url\":\"https://geocaching.su/photos/areas/15094.jpg\",\"description\":\"Памятный крест\"}]}}"
    private static val simpleCache: String = "{\"status\":{\"code\":\"OK\"}," +
            "\"data\":{" +
            "\"id\":321," +
            "\"code\":\"TR321\"," +
            "\"name\":\"Крестовоздвиженский монастырь\"," +
            "\"author\":{\"id\":20,\"name\":\"Инструктор\"}," +
            "\"latitude\":55.51,\"longitude\":37.8533333," +
            "\"type\":2,\"typeString\":\"TraditionalMultistep\"," +
            "\"subtype\":0," +
            "\"attributes\":[]," +
            "\"class\":\"2,6\"," +
            "\"status\":2,\"statusString\":\"Active\"," +
            "\"status2\":2,\"status2String\":\"Active\"," +
            "\"difficulty\":1,\"area\":5," +
            "\"size\":3," +
            "\"founds\":23," +
            "\"notfounds\":12," +
            "\"isFound\":true," +
            "\"foundOn\":\"2018-04-05\"," +
            "\"recommendations\":44," +
            "\"votes\":23," +
            "\"rating\":4.3," +
            "\"dateHidden\":\"2008-04-05\"," +
            "\"description\":{" +
            "\"cache\":\"благодарность .\"," +
            "\"area\":\"Some area description\"," +
            "\"isHtml\":true}" +

            "}}"
    private static val cachesListJson: String = "{\"status\":{\"code\":\"OK\"}," +
            "\"data\":[" +
            "{\"id\":6989,\"name\":\"First cache\",\"author\":{\"id\":24219,\"name\":\"QQ\"},\"latitude\":59.9224833,\"longitude\":30.38655,\"type\":3,\"typeString\":\"Virtual\",\"subtype\":0,\"attributes\":[{\"id\":19,\"name\":\"watch\"},{\"id\":1,\"name\":\"hiking\"}],\"class\":\"2,6\",\"status\":1,\"statusString\":\"Active\",\"status2\":1,\"status2String\":\"Active\",\"difficulty\":1,\"area\":5,\"isFound\":false,\"size\":1,\"founds\":null,\"notfounds\":null,\"votes\":null,\"rating\":null,\"recommendations\":null,\"code\":\"VI6989\",\"dateHidden\":\"2009-12-06\",\"waypoints\":[]}," +
            "{\"id\":7582,\"name\":\"Something in 2010\",\"author\":{\"id\":22268,\"name\":\"-nz-\"},\"latitude\":59.9483334,\"longitude\":30.35,\"type\":9,\"typeString\":\"Logical\",\"subtype\":0,\"attributes\":[],\"class\":\"10\",\"status\":1,\"statusString\":\"Active\",\"status2\":1,\"status2String\":\"Active\",\"difficulty\":3,\"area\":3,\"isFound\":false,\"size\":1,\"founds\":null,\"notfounds\":null,\"votes\":null,\"rating\":null,\"recommendations\":null,\"code\":\"LT7582\",\"dateHidden\":\"2010-05-14\",\"waypoints\":[]}," +
            "{\"id\":8849,\"name\":\"SPb-Quest\",\"author\":{\"id\":22268,\"name\":\"-nz-\"},\"latitude\":59.9273834,\"longitude\":30.3193833,\"type\":10,\"typeString\":\"LogicalVirtual\",\"subtype\":0,\"attributes\":[{\"id\":1,\"name\":\"hiking\"},{\"id\":5,\"name\":\"kids\"}],\"class\":\"5,6,10,8,9\",\"status\":1,\"statusString\":\"Active\",\"status2\":1,\"status2String\":\"Active\",\"difficulty\":2,\"area\":5,\"isFound\":false,\"size\":1,\"founds\":null,\"notfounds\":null,\"votes\":null,\"rating\":null,\"recommendations\":null,\"code\":\"LV8849\",\"dateHidden\":\"2010-10-13\",\"waypoints\":[]}" +
            "]}"

    private Unit parseCache(final String jsonData) throws Exception {
        val actualObj: ObjectNode = (ObjectNode) JsonUtils.reader.readTree(jsonData)
        cache = SuParser.parseCache(actualObj)
    }

    private Unit parseUser(final String jsonData) throws Exception {
        val actualObj: ObjectNode = (ObjectNode) JsonUtils.reader.readTree(jsonData)
        user = SuParser.parseUser(actualObj)
    }

    private List<Geocache> parseCaches(final String jsonData) throws Exception {
        val actualObj: ObjectNode = (ObjectNode) JsonUtils.reader.readTree(jsonData)
        return SuParser.parseCaches(actualObj)
    }

    @Test
    public Unit testCanParseCacheJsonCacheId() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getCacheId()).isEqualTo("3749")
    }

    @Test
    public Unit testCanParseCacheJsonCacheName() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getName()).isEqualTo("Крестовоздвиженский монастырь")
    }

    @Test
    public Unit testCanParseCacheJsonCoords() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getCoords()).isEqualTo(Geopoint(55.51, 37.853333))
    }

    @Test
    public Unit testCanParseCacheJsonDescriptionArea() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getDescription()).contains("area description")
    }

    @Test
    public Unit testCanParseCacheJsonDescriptionCache() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getDescription()).contains("благодарность")
    }

    @Test
    public Unit testCanParseCacheJsonDescriptionTrCache() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getDescription()).contains("Спрятан")
    }

    @Test
    public Unit testCanParseCacheJsonDescriptionViCache() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getDescription()).contains("Значение")
    }

    @Test
    public Unit testCanParseCacheJsonDescriptionContains() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getDescription()).contains("Карандаш")
    }

    @Test
    public Unit testCanParseCacheJsonTerrain() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getTerrain()).isEqualTo(5f)
    }

    @Test
    public Unit testCanParseCacheJsonDifficulty() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getDifficulty()).isEqualTo(1f)
    }

    @Test
    public Unit testCanParseCacheJsonAuthorName() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getOwnerDisplayName()).isEqualTo("Инструктор")
    }

    @Test
    public Unit testCanParseCacheJsonAuthorId() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getOwnerUserId()).isEqualTo("20")
    }

    @Test
    public Unit testCanParseCacheJsonCode() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getGeocode()).isEqualTo("TR3749")
    }

    @Test
    public Unit testCanParseCacheJsonSize() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL)
    }

    @Test
    public Unit testCanParseFounds() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getLogCounts().get(LogType.FOUND_IT)).isEqualTo(23)
    }

    @Test
    public Unit testCanParseNotFounds() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getLogCounts().get(LogType.DIDNT_FIND_IT)).isEqualTo(12)
    }

    @Test
    public Unit testCanParseLogs() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getLogs()).hasSize(7)
    }

    @Test
    public Unit testCanParseNoLogs() throws Exception {
        parseCache(simpleCache)
        assertThat(cache.getLogs()).isEmpty()
    }

    @Test
    public Unit testCanParseLogAuthor() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getLogs().get(0).author).isEqualTo("LE")
    }

    @Test
    public Unit testCanParseLogServiceLogId() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getLogs().get(0).serviceLogId).isEqualTo("407324")
    }

    @Test
    public Unit testCanParseLogText() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getLogs().get(0).log).contains("Рыть глубоко")
    }

    @Test
    public Unit testCanParseLogType() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getLogs().get(2).logType).isEqualTo(LogType.OWNER_MAINTENANCE)
    }

    @Test
    public Unit testCanParseLogDateTime() throws Exception {
        parseCache(cacheJson)

        val isoFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        assertThat(Date(cache.getLogs().get(0).date)).isEqualTo(isoFormat.parse("2018-10-20 18:12:46"))
    }

    @Test
    public Unit testCanParseOwnLogs() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getLogs().get(1).friend).isTrue()
    }

    @Test
    public Unit testCanByDefaultLogsNotOwned() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getLogs().get(0).friend).isFalse()
    }

    @Test
    public Unit testCanParseRecommendations() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getFavoritePoints()).isEqualTo(44)
    }

    @Test
    public Unit testCanParseImages() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getImages()).hasSize(6)
    }


    @Test
    public Unit testCanParseImageText() throws Exception {
        parseCache(cacheJson)
        Boolean found = false
        for (final Image img : cache.getImages()) {
            val title: String = img.title
            if (title != null && title.contains("Ограда монастыря")) {
                found = true
                break
            }
        }
        assertThat(found).isTrue()
    }

    @Test
    public Unit testCanParseImageUrl() throws Exception {
        parseCache(cacheJson)
        val imgUrl: String = ((Image) cache.getImages().toArray()[1]).getUrl()
        assertThat(imgUrl).contains("areas/15090.jpg")
    }

    @Test
    public Unit testCacheImageInSpoiler() throws Exception {
        parseCache(cacheJson)

        val imgUrl: String = cache.getSpoilers().get(0).getUrl()
        assertThat(imgUrl).contains("caches/3749.jpg")
    }

    @Test
    public Unit testCanParseStatus() throws Exception {
        parseCache(cacheJson)
        cache.setArchived(false); // make sure the cache is not archived, otherwise disabled will allways return false
        assertThat(cache.isDisabled()).isTrue()
    }

    @Test
    public Unit testCanParseArchivedStatus() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.isArchived()).isTrue()
    }

    @Test
    public Unit testCanParseWaypoints() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getWaypoints()).hasSize(2)
    }

    @Test
    public Unit testCanParseWaypointCoords() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getWaypoints().get(0).getCoords()).isEqualTo(Geopoint(1.23, 2.132))
    }

    @Test
    public Unit testCanParseWaypointName() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getWaypoints().get(0).getName()).isEqualTo("Имя точки")
    }

    @Test
    public Unit testCanParseWaypointType() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getWaypoints().get(0).getWaypointType()).isEqualTo(WaypointType.PUZZLE)
    }

    @Test
    public Unit testCanParseWaypointDescription() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getWaypoints().get(0).getNote()).contains("point some nice descripti")
    }

    @Test
    public Unit testCanParseVotes() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getVotes()).isEqualTo(23)
    }

    @Test
    public Unit testCanParseRating() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getRating()).isEqualTo(4.3f)
    }

    @Test
    public Unit testCanParsePersonalNote() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.getPersonalNote()).isEqualTo("My note")
    }

    @Test
    public Unit testCanParseCachesList() throws Exception {
        val caches: List<Geocache> = parseCaches(cachesListJson)
        assertThat(caches).hasSize(3)
    }

    @Test
    public Unit testCanParseCacheInTheList() throws Exception {
        val caches: List<Geocache> = parseCaches(cachesListJson)
        assertThat(caches.get(0).getGeocode()).isEqualTo("VI6989")
    }

    @Test
    public Unit testCanParseUserName() throws Exception {
        parseUser(userJson)
        assertThat(user.getName()).isEqualTo("lega4")
    }

    @Test
    public Unit testCanParseUserFinds() throws Exception {
        parseUser(userJson)
        assertThat(user.getFinds()).isEqualTo(594)
    }

    @Test
    public Unit testCanParseFindDate() throws Exception {
        parseCache(simpleCache)

        val isoFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        assertThat(Date(cache.getVisitedDate())).isEqualTo(isoFormat.parse("2018-04-05"))
    }

    @Test
    public Unit testCanParseWatchStatus() throws Exception {
        parseCache(cacheJson)
        assertThat(cache.isOnWatchlist()).isTrue()
    }

    @Test
    public Unit testCanHandleWrongUserJson() throws Exception {
        parseUser(emptyUserJson)

        assertThat(user.getStatus()).isEqualTo(UserInfo.UserInfoStatus.FAILED)
    }

    @Test
    public Unit testCanParseUserSuccessfully() throws Exception {
        parseUser(userJson)
        assertThat(user.getStatus()).isEqualTo(UserInfo.UserInfoStatus.SUCCESSFUL)
    }
}
