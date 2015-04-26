package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LogEntry unit tests
 */
public class LogEntryTest extends CGeoTestCase {

    public static void testLogEntry() {
        final LogEntry logEntry = new LogEntry(100, LogType.FOUND_IT, "LOGENTRY");

        assertThat(logEntry.date).isEqualTo(100);
        assertThat(logEntry.type).isEqualTo(LogType.FOUND_IT);
        assertThat(logEntry.log).isEqualTo("LOGENTRY");
    }

    public static void testEquals() {
        final LogEntry logEntry1 = new LogEntry(100, LogType.FOUND_IT, "LOGENTRY1");
        final LogEntry logEntry2 = new LogEntry(200, LogType.DISCOVERED_IT, "LOGENTRY2");

        assertThat(logEntry1).isEqualTo(logEntry1);
        assertThat(logEntry2).isEqualTo(logEntry2);
        assertThat(logEntry1).isNotEqualTo(logEntry2);
    }

    public static void testGetAddLogImage() {
        final LogEntry logEntry = new LogEntry(100, LogType.FOUND_IT, "LOGENTRY");
        final Image mockedImage = new Image(null, null, null);
        logEntry.addLogImage(mockedImage);

        assertThat(logEntry.getLogImages()).hasSize(1);
        assertThat(logEntry.getLogImages().get(0)).isEqualTo(mockedImage);
    }

    public static void testGetImageTitles() {
        final String defaultTitle = CgeoApplication.getInstance().getString(R.string.cache_log_image_default_title);

        final LogEntry logEntry = new LogEntry(100, LogType.FOUND_IT, "LOGENTRY");

        assertThat(logEntry.getLogImages()).hasSize(0);
        assertThat(logEntry.getImageTitles()).isEqualTo(defaultTitle);

        final Image mockedImage1 = new Image(null, null, null);
        logEntry.addLogImage(mockedImage1);

        assertThat(logEntry.getLogImages()).hasSize(1);
        assertThat(logEntry.getImageTitles()).isEqualTo(defaultTitle);

        final Image mockedImage2 = new Image(null, "TITLE 1", null);
        logEntry.addLogImage(mockedImage2);
        final Image mockedImage3 = new Image(null, "TITLE 2", null);
        logEntry.addLogImage(mockedImage3);

        assertThat(logEntry.getLogImages()).hasSize(3);
        final String titlesWanted = "TITLE 1, TITLE 2";
        assertThat(logEntry.getImageTitles()).isEqualTo(titlesWanted);
    }

    public static void testGetDisplayText() {

        final Boolean oldValue = Settings.getPlainLogs();

        final LogEntry logEntry1 = new LogEntry(100, LogType.FOUND_IT, "LOGENTRY");
        final LogEntry logEntry2 = new LogEntry(100, LogType.FOUND_IT, "<font color=\"red\">LOGENTRY</font>");
        final LogEntry logEntry3 = new LogEntry(100, LogType.FOUND_IT, "<FONT COLOR=\"red\">LOGENTRY</FONT>");
        final LogEntry logEntry4 = new LogEntry(100, LogType.FOUND_IT, "<FoNt COlOr=\"red\">LOGENTRY</fOnT>");

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

    public static void testIsOwn() {
        final LogEntry logEntry1 = new LogEntry("userthatisnotthedefaultuser", 100, LogType.FOUND_IT, "LOGENTRY");
        final LogEntry logEntry2 = new LogEntry(Settings.getUsername(), 100, LogType.FOUND_IT, "LOGENTRY");
        final LogEntry logEntry3 = new LogEntry(100, LogType.FOUND_IT, "LOGENTRY");

        assertThat(logEntry1.isOwn()).isFalse();
        assertThat(logEntry2.isOwn()).isTrue();
        assertThat(logEntry3.isOwn()).isTrue();
    }

    public static void testComparator() {
        final LogEntry logEntry1 = new LogEntry(100, LogType.FOUND_IT, "logEntry1 is older than logEntry2");
        final LogEntry logEntry2 = new LogEntry(200, LogType.FOUND_IT, "logEntry2 is more recent than logEntry1");

        final List<LogEntry> logList = new ArrayList<>(2);
        logList.add(logEntry1);
        logList.add(logEntry2);

        Collections.sort(logList, LogEntry.DESCENDING_DATE_COMPARATOR);

        assertThat(logList).containsExactly(logEntry2, logEntry1);
    }
}
