package cgeo.geocaching.log;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * LogEntry and OfflineLogEntry unit tests
 */
public class LogEntryTest {

    @Test
    public void testLogEntry() {
        final LogEntry logEntry = new LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build();

        assertThat(logEntry.date).isEqualTo(100);
        assertThat(logEntry.logType).isEqualTo(LogType.FOUND_IT);
        assertThat(logEntry.log).isEqualTo("LOGENTRY");
    }

    @Test
    public void testEquals() {
        final LogEntry logEntry1 = new LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY1").build();
        final LogEntry logEntry2 = new LogEntry.Builder().setDate(200).setLogType(LogType.DISCOVERED_IT).setLog("LOGENTRY2").build();

        assertThat(logEntry1).isEqualTo(logEntry1);
        assertThat(logEntry2).isEqualTo(logEntry2);
        assertThat(logEntry1).isNotEqualTo(logEntry2);
    }

    @Test
    public void testGetAddLogImage() {
        final Image mockedImage1 = Image.NONE;
        final LogEntry logEntry1 = new LogEntry.Builder()
                .addLogImage(mockedImage1)
                .build();
        assertThat(logEntry1.logImages).hasSize(0);

        final Image mockedImage2 = new Image.Builder().setTitle("").build();
        final LogEntry logEntry2 = new LogEntry.Builder()
                .setDate(100).setLogType(LogType.FOUND_IT)
                .setLog("LOGENTRY")
                .addLogImage(mockedImage2)
                .build();

        assertThat(logEntry2.logImages).hasSize(1);
        assertThat(logEntry2.logImages.get(0)).isEqualTo(mockedImage2.buildUpon().setCategory(Image.ImageCategory.LOG).build());
    }

    @Test
    public void testGetImageTitles() {
        final String defaultTitle = "• " + CgeoApplication.getInstance().getString(R.string.cache_log_image_default_title);

        LogEntry logEntry = new LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build();

        assertThat(logEntry.logImages).hasSize(0);
        assertThat(logEntry.getImageTitles()).isEqualTo(defaultTitle);

        final Image mockedImage1 = new Image.Builder().setTitle("").build();
        logEntry = logEntry.buildUpon().addLogImage(mockedImage1).build();

        assertThat(logEntry.logImages).hasSize(1);
        assertThat(logEntry.getImageTitles()).isEqualTo(defaultTitle);

        final Image mockedImage2 = new Image.Builder().setTitle("TITLE 1").build();
        logEntry = logEntry.buildUpon().addLogImage(mockedImage2).build();
        final Image mockedImage3 = new Image.Builder().setTitle("TITLE 2").build();
        logEntry = logEntry.buildUpon().addLogImage(mockedImage3).build();

        assertThat(logEntry.logImages).hasSize(3);
        final String titlesWanted = "• TITLE 1\n• TITLE 2";
        assertThat(logEntry.getImageTitles()).isEqualTo(titlesWanted);
    }

    @Test
    public void testGetDisplayText() {

        final Boolean oldValue = Settings.getPlainLogs();

        final LogEntry logEntry1 = new LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build();
        final LogEntry logEntry2 = new LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("<font color=\"red\">LOGENTRY</font>").build();
        final LogEntry logEntry3 = new LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("<FONT COLOR=\"red\">LOGENTRY</FONT>").build();
        final LogEntry logEntry4 = new LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("<FoNt COlOr=\"red\">LOGENTRY</fOnT>").build();

        Settings.setPlainLogs(false);
        assertThat(logEntry1.getDisplayText()).isEqualTo("LOGENTRY");
        assertThat(logEntry2.getDisplayText()).isEqualTo("<font color=\"red\">LOGENTRY</font>");
        assertThat(logEntry3.getDisplayText()).isEqualTo("<FONT COLOR=\"red\">LOGENTRY</FONT>");
        assertThat(logEntry4.getDisplayText()).isEqualTo("<FoNt COlOr=\"red\">LOGENTRY</fOnT>");

        Settings.setPlainLogs(true);
        assertThat(logEntry1.getDisplayText()).isEqualTo("LOGENTRY");
        assertThat(logEntry2.getDisplayText()).isEqualTo("LOGENTRY");
        assertThat(logEntry3.getDisplayText()).isEqualTo("LOGENTRY");
        assertThat(logEntry4.getDisplayText()).isEqualTo("LOGENTRY");

        Settings.setPlainLogs(oldValue);
    }

    @Test
    public void testIsOwn() {
        final LogEntry logEntry1 = new LogEntry.Builder().setAuthor("userthatisnotthedefaultuser").setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build();
        final LogEntry logEntry2 = new LogEntry.Builder().setAuthor(Settings.getUserName()).setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build();
        final LogEntry logEntry3 = new LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("LOGENTRY").build();

        assertThat(logEntry1.isOwn()).isFalse();
        assertThat(logEntry2.isOwn()).isTrue();
        assertThat(logEntry3.isOwn()).isTrue();
    }

    @Test
    public void testComparator() {
        final LogEntry logEntry1 = new LogEntry.Builder().setDate(100).setLogType(LogType.FOUND_IT).setLog("logEntry1 is older than logEntry2").build();
        final LogEntry logEntry2 = new LogEntry.Builder().setDate(200).setLogType(LogType.FOUND_IT).setLog("logEntry2 is more recent than logEntry1").build();

        final List<LogEntry> logList = new ArrayList<>(2);
        logList.add(logEntry1);
        logList.add(logEntry2);

        Collections.sort(logList, LogEntry.DESCENDING_DATE_COMPARATOR);

        assertThat(logList).containsExactly(logEntry2, logEntry1);
    }

    @Test
    public void testOfflineLogEntry() {

        final OfflineLogEntry.Builder<?> oleBuilder = new OfflineLogEntry.Builder<>()
                //set a LogEntry-specific property
                .setLog("some log message")
                //set an OfflineLogEntry-specific property
                .setImageTitlePraefix("ImagePraefix1")
                //set again a LogEntry-specific property
                .setId(1);

        OfflineLogEntry logEntry = oleBuilder.build();

        assertThat(logEntry.id).isEqualTo(1);
        assertThat(logEntry.log).isEqualTo("some log message");
        assertThat(logEntry.imageTitlePraefix).isEqualTo("ImagePraefix1");

        //regain builder and add some more
        logEntry = oleBuilder
                //set a LogEntry-specific property
                .setId(2)
                //set an OfflineLogEntry-specific property
                .setImageTitlePraefix("ImagePraefix2")
                //set again a LogEntry-specific property
                .setId(3)
                //gain an OfflineLogEntry
                .build();

        assertThat(logEntry.id).isEqualTo(3);
        assertThat(logEntry.log).isEqualTo("some log message");
        assertThat(logEntry.imageTitlePraefix).isEqualTo("ImagePraefix2");
    }

    @Test
    public void testOfflineLogParcelable() {

        //initialize a fully-fledged OfflineLogEntry
        final OfflineLogEntry logEntry = new OfflineLogEntry.Builder<>()
                .setServiceLogId("pid")
                .setLog("log message")
                .setLogType(LogType.DIDNT_FIND_IT)
                .setDate(50)
                .setFavorite(true)
                .setTweet(true)
                .setRating(5.0f)
                .setPassword("pwd")
                .setImageScale(5)
                .setImageTitlePraefix("praefix")
                .addLogImage(new Image.Builder().setUrl("abc").setTitle("def").setDescription("ghi").build())
                .addLogImage(new Image.Builder().setUrl("abc2").setTitle("def2").setDescription("ghi2").build())
                .addTrackableAction("TBFake1", LogTypeTrackable.DROPPED_OFF)
                .build();

        //serialize and deserialize;
        final byte[] parcel = marshall(logEntry);
        final OfflineLogEntry otherLogEntry = unmarshall(parcel, OfflineLogEntry.CREATOR);

        assertThat(otherLogEntry.log).isEqualTo("log message");
        assertThat(otherLogEntry.serviceLogId).isEqualTo("pid");
        assertThat(otherLogEntry.logType).isEqualTo(LogType.DIDNT_FIND_IT);
        assertThat(otherLogEntry.date).isEqualTo(50);
        assertThat(otherLogEntry.favorite).isEqualTo(true);
        assertThat(otherLogEntry.tweet).isEqualTo(true);
        assertThat(otherLogEntry.rating).isEqualTo(5.0f);
        assertThat(otherLogEntry.imageScale).isEqualTo(5);
        assertThat(otherLogEntry.password).isEqualTo("pwd");
        assertThat(otherLogEntry.imageTitlePraefix).isEqualTo("praefix");

        assertThat(otherLogEntry.logImages.size()).isEqualTo(2);
        assertThat(otherLogEntry.logImages.get(0)).isEqualTo(new Image.Builder().setUrl("abc").setTitle("def").setDescription("ghi").setCategory(Image.ImageCategory.LOG).build());
        assertThat(otherLogEntry.logImages.get(1)).isEqualTo(new Image.Builder().setUrl("abc2").setTitle("def2").setDescription("ghi2").setCategory(Image.ImageCategory.LOG).build());
        assertThat(otherLogEntry.trackableActions.size()).isEqualTo(1);
        assertThat(otherLogEntry.trackableActions.get("TBFake1")).isEqualTo(LogTypeTrackable.DROPPED_OFF);
    }

    @Test
    public void testEmptyOfflineLogParcelable() {

        //initialize a fully-fledged OfflineLogEntry
        final OfflineLogEntry logEntry = new OfflineLogEntry.Builder<>().build();

        //serialize and deserialize;
        final byte[] parcel = marshall(logEntry);
        final OfflineLogEntry otherLogEntry = unmarshall(parcel, OfflineLogEntry.CREATOR);

        assertThat(otherLogEntry.rating).isNull();
        assertThat(otherLogEntry.logImages.size()).isEqualTo(0);
        assertThat(otherLogEntry.trackableActions.size()).isEqualTo(0);

    }

    public static byte[] marshall(final Parcelable parceable) {
        final Parcel parcel = Parcel.obtain();
        parceable.writeToParcel(parcel, 0);
        final byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public static Parcel unmarshall(final byte[] bytes) {
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0); // This is extremely important!
        return parcel;
    }

    public static <T> T unmarshall(final byte[] bytes, final Parcelable.Creator<T> creator) {
        final Parcel parcel = unmarshall(bytes);
        final T result = creator.createFromParcel(parcel);
        parcel.recycle();
        return result;
    }

}
