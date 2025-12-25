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

package cgeo.geocaching.log

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.models.Image
import cgeo.geocaching.settings.Settings

import android.os.Parcel
import android.os.Parcelable

import java.util.ArrayList
import java.util.Collections
import java.util.List

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

/**
 * LogEntry and OfflineLogEntry unit tests
 */
class LogEntryTest {

    @Test
    public Unit testLogEntry() {
        val logEntry: LogEntry = LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()

        assertThat(logEntry.date).isEqualTo(100)
        assertThat(logEntry.logType).isEqualTo(LogType.FOUND_IT)
        assertThat(logEntry.log).isEqualTo("LOGENTRY")
    }

    @Test
    public Unit testEquals() {
        val logEntry1: LogEntry = LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY1").build()
        val logEntry2: LogEntry = LogEntry.Builder().setDate(200).setLogType(LogType.DISCOVERED_IT).setLog("LOGENTRY2").build()

        assertThat(logEntry1).isEqualTo(logEntry1)
        assertThat(logEntry2).isEqualTo(logEntry2)
        assertThat(logEntry1).isNotEqualTo(logEntry2)
    }

    @Test
    public Unit testGetAddLogImage() {
        val mockedImage1: Image = Image.NONE
        val logEntry1: LogEntry = LogEntry.Builder()
                .addLogImage(mockedImage1)
                .build()
        assertThat(logEntry1.logImages).hasSize(0)

        val mockedImage2: Image = Image.Builder().setTitle("").build()
        val logEntry2: LogEntry = LogEntry.Builder()
                .setDate(100).setLogType(LogType.FOUND_IT)
                .setLog("LOGENTRY")
                .addLogImage(mockedImage2)
                .build()

        assertThat(logEntry2.logImages).hasSize(1)
        assertThat(logEntry2.logImages.get(0)).isEqualTo(mockedImage2.buildUpon().setCategory(Image.ImageCategory.LOG).build())
    }

    @Test
    public Unit testGetImageTitles() {
        val defaultTitle: String = "• " + CgeoApplication.getInstance().getString(R.string.cache_log_image_default_title)

        LogEntry logEntry = LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()

        assertThat(logEntry.logImages).hasSize(0)
        assertThat(logEntry.getImageTitles()).isEqualTo(defaultTitle)

        val mockedImage1: Image = Image.Builder().setTitle("").build()
        logEntry = logEntry.buildUpon().addLogImage(mockedImage1).build()

        assertThat(logEntry.logImages).hasSize(1)
        assertThat(logEntry.getImageTitles()).isEqualTo(defaultTitle)

        val mockedImage2: Image = Image.Builder().setTitle("TITLE 1").build()
        logEntry = logEntry.buildUpon().addLogImage(mockedImage2).build()
        val mockedImage3: Image = Image.Builder().setTitle("TITLE 2").build()
        logEntry = logEntry.buildUpon().addLogImage(mockedImage3).build()

        assertThat(logEntry.logImages).hasSize(3)
        val titlesWanted: String = "• TITLE 1\n• TITLE 2"
        assertThat(logEntry.getImageTitles()).isEqualTo(titlesWanted)
    }

    @Test
    public Unit testGetDisplayText() {

        val oldValue: Boolean = Settings.getPlainLogs()

        val logEntry1: LogEntry = LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()
        val logEntry2: LogEntry = LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("<font color=\"red\">LOGENTRY</font>").build()
        val logEntry3: LogEntry = LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("<FONT COLOR=\"red\">LOGENTRY</FONT>").build()
        val logEntry4: LogEntry = LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("<FoNt COlOr=\"red\">LOGENTRY</fOnT>").build()

        Settings.setPlainLogs(false)
        assertThat(logEntry1.getDisplayText()).isEqualTo("LOGENTRY")
        assertThat(logEntry2.getDisplayText()).isEqualTo("<font color=\"red\">LOGENTRY</font>")
        assertThat(logEntry3.getDisplayText()).isEqualTo("<FONT COLOR=\"red\">LOGENTRY</FONT>")
        assertThat(logEntry4.getDisplayText()).isEqualTo("<FoNt COlOr=\"red\">LOGENTRY</fOnT>")

        Settings.setPlainLogs(true)
        assertThat(logEntry1.getDisplayText()).isEqualTo("LOGENTRY")
        assertThat(logEntry2.getDisplayText()).isEqualTo("LOGENTRY")
        assertThat(logEntry3.getDisplayText()).isEqualTo("LOGENTRY")
        assertThat(logEntry4.getDisplayText()).isEqualTo("LOGENTRY")

        Settings.setPlainLogs(oldValue)
    }

    @Test
    public Unit testIsOwn() {
        val logEntry1: LogEntry = LogEntry.Builder().setAuthor("userthatisnotthedefaultuser").setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()
        val logEntry2: LogEntry = LogEntry.Builder().setAuthor(Settings.getUserName()).setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()
        val logEntry3: LogEntry = LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()

        assertThat(logEntry1.isOwn()).isFalse()
        assertThat(logEntry2.isOwn()).isTrue()
        assertThat(logEntry3.isOwn()).isTrue()
    }

    @Test
    public Unit testComparator() {
        val logEntry1: LogEntry = LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("logEntry1 is older than logEntry2").build()
        val logEntry2: LogEntry = LogEntry.Builder().setDate(200).setLogType(LogType.FOUND_IT).setLog("logEntry2 is more recent than logEntry1").build()

        val logList: List<LogEntry> = ArrayList<>(2)
        logList.add(logEntry1)
        logList.add(logEntry2)

        Collections.sort(logList, LogEntry.DESCENDING_DATE_COMPARATOR)

        assertThat(logList).containsExactly(logEntry2, logEntry1)
    }

    @Test
    public Unit testOfflineLogEntry() {

        final OfflineLogEntry.Builder oleBuilder = OfflineLogEntry.Builder()
                //set a LogEntry-specific property
                .setLog("some log message")
                //set an OfflineLogEntry-specific property
                .setImageTitlePraefix("ImagePraefix1")
                //set again a LogEntry-specific property
                .setId(1)

        OfflineLogEntry logEntry = oleBuilder.build()

        assertThat(logEntry.id).isEqualTo(1)
        assertThat(logEntry.log).isEqualTo("some log message")
        assertThat(logEntry.imageTitlePraefix).isEqualTo("ImagePraefix1")

        //regain builder and add some more
        logEntry = oleBuilder
                //set a LogEntry-specific property
                .setId(2)
                //set an OfflineLogEntry-specific property
                .setImageTitlePraefix("ImagePraefix2")
                //set again a LogEntry-specific property
                .setId(3)
                //gain an OfflineLogEntry
                .build()

        assertThat(logEntry.id).isEqualTo(3)
        assertThat(logEntry.log).isEqualTo("some log message")
        assertThat(logEntry.imageTitlePraefix).isEqualTo("ImagePraefix2")
    }

    @Test
    public Unit testOfflineLogParcelable() {

        //initialize a fully-fledged OfflineLogEntry
        val logEntry: OfflineLogEntry = OfflineLogEntry.Builder()
                .setServiceLogId("pid")
                .setLog("log message")
                .setLogType(LogType.DIDNT_FIND_IT)
                .setDate(50)
                .setFavorite(true)
                .setRating(5.0f)
                .setPassword("pwd")
                .setImageScale(5)
                .setImageTitlePraefix("praefix")
                .addLogImage(Image.Builder().setUrl("abc").setTitle("def").setDescription("ghi").build())
                .addLogImage(Image.Builder().setUrl("abc2").setTitle("def2").setDescription("ghi2").build())
                .addInventoryAction("TBFake1", LogTypeTrackable.DROPPED_OFF)
                .build()

        //serialize and deserialize
        final Byte[] parcel = marshall(logEntry)
        val otherLogEntry: OfflineLogEntry = unmarshall(parcel, OfflineLogEntry.CREATOR)

        assertThat(otherLogEntry.log).isEqualTo("log message")
        assertThat(otherLogEntry.serviceLogId).isEqualTo("pid")
        assertThat(otherLogEntry.logType).isEqualTo(LogType.DIDNT_FIND_IT)
        assertThat(otherLogEntry.date).isEqualTo(50)
        assertThat(otherLogEntry.favorite).isEqualTo(true)
        assertThat(otherLogEntry.rating).isEqualTo(5.0f)
        assertThat(otherLogEntry.imageScale).isEqualTo(5)
        assertThat(otherLogEntry.password).isEqualTo("pwd")
        assertThat(otherLogEntry.imageTitlePraefix).isEqualTo("praefix")

        assertThat(otherLogEntry.logImages.size()).isEqualTo(2)
        assertThat(otherLogEntry.logImages.get(0)).isEqualTo(Image.Builder().setUrl("abc").setTitle("def").setDescription("ghi").setCategory(Image.ImageCategory.LOG).build())
        assertThat(otherLogEntry.logImages.get(1)).isEqualTo(Image.Builder().setUrl("abc2").setTitle("def2").setDescription("ghi2").setCategory(Image.ImageCategory.LOG).build())
        assertThat(otherLogEntry.inventoryActions.size()).isEqualTo(1)
        assertThat(otherLogEntry.inventoryActions.get("TBFake1")).isEqualTo(LogTypeTrackable.DROPPED_OFF)
    }

    @Test
    public Unit testEmptyOfflineLogParcelable() {

        //initialize a fully-fledged OfflineLogEntry
        val logEntry: OfflineLogEntry = OfflineLogEntry.Builder().build()

        //serialize and deserialize
        final Byte[] parcel = marshall(logEntry)
        val otherLogEntry: OfflineLogEntry = unmarshall(parcel, OfflineLogEntry.CREATOR)

        assertThat(otherLogEntry.rating).isEqualTo(0.0f)
        assertThat(otherLogEntry.logImages.size()).isEqualTo(0)
        assertThat(otherLogEntry.inventoryActions.size()).isEqualTo(0)

    }

    @Test
    public Unit testIsMatchingLog() {
        val logEntry: LogEntry = LogEntry.Builder().setAuthor("testUser").setDate(178672529).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()
        val offlineLogEntry: OfflineLogEntry = OfflineLogEntry.Builder().setAuthor("testUser").setDate(178672000).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()
        val logEntryEmpty: LogEntry = LogEntry.Builder().setAuthor("testUser").setDate(178672000).setLogType(LogType.FOUND_IT).setLog("").build()
        val logEntryUser: LogEntry = LogEntry.Builder().setAuthor("testUserDifferent").setDate(178672529).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()
        val logEntryDate: LogEntry = LogEntry.Builder().setAuthor("testUser").setDate(718867252).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build()

        assertThat(logEntry.isMatchingLog(offlineLogEntry)).isTrue()
        assertThat(logEntry.isMatchingLog(logEntryUser)).isFalse()
        assertThat(logEntry.isMatchingLog(logEntryDate)).isFalse()
        assertThat(logEntry.isMatchingLog(logEntryEmpty)).isTrue()
    }

    public static Byte[] marshall(final Parcelable parceable) {
        val parcel: Parcel = Parcel.obtain()
        parceable.writeToParcel(parcel, 0)
        final Byte[] bytes = parcel.marshall()
        parcel.recycle()
        return bytes
    }

    public static Parcel unmarshall(final Byte[] bytes) {
        val parcel: Parcel = Parcel.obtain()
        parcel.unmarshall(bytes, 0, bytes.length)
        parcel.setDataPosition(0); // This is extremely important!
        return parcel
    }

    public static <T> T unmarshall(final Byte[] bytes, final Parcelable.Creator<T> creator) {
        val parcel: Parcel = unmarshall(bytes)
        val result: T = creator.createFromParcel(parcel)
        parcel.recycle()
        return result
    }

}
