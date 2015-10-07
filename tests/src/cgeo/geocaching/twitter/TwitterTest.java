package cgeo.geocaching.twitter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;

import junit.framework.TestCase;

public class TwitterTest extends TestCase {

    public static void testTrackableMessage() {
        final String oldMessage = Settings.getTrackableTwitterMessage();
        try {
            TestSettings.setTrackableTwitterMessage("I touched [NAME] ([URL]).");
            Trackable tb = new Trackable();
            tb.setName("Travel bug");
            tb.setGeocode("TB1234");
            assertThat(Twitter.getStatusMessage(tb, null)).isEqualTo("I touched Travel bug (http://www.geocaching.com//track/details.aspx?tracker=TB1234). #cgeo #geocaching");
        } finally {
            TestSettings.setTrackableTwitterMessage(oldMessage);
        }
    }

    public static void testCacheMessage() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            TestSettings.setCacheTwitterMessage("I found [NAME] ([URL]).");
            Geocache cache = new Geocache();
            cache.setGeocode("GC1234");
            cache.setName("TwitterTest");
            assertThat(Twitter.getStatusMessage(cache, null)).isEqualTo("I found TwitterTest (http://coord.info/GC1234). #cgeo #geocaching");
        } finally {
            TestSettings.setCacheTwitterMessage(oldMessage);
        }
    }

    public static void testCacheMessageWithLogContent() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            TestSettings.setCacheTwitterMessage("[LOG]");
            Geocache cache = new Geocache();
            LogEntry log = new LogEntry(0, LogType.FOUND_IT, "log text");
            assertThat(Twitter.getStatusMessage(cache, log)).isEqualTo("log text #cgeo #geocaching");
        } finally {
            TestSettings.setCacheTwitterMessage(oldMessage);
        }
    }

    public static void testTrackableMessageWithLogContent() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            TestSettings.setTrackableTwitterMessage("[LOG]");
            Trackable trackable = new Trackable();
            LogEntry log = new LogEntry(0, LogType.FOUND_IT, "trackable log text");
            assertThat(Twitter.getStatusMessage(trackable, log)).isEqualTo("trackable log text #cgeo #geocaching");
        } finally {
            TestSettings.setTrackableTwitterMessage(oldMessage);
        }
    }

    public static void testAvoidDuplicateTags() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            TestSettings.setCacheTwitterMessage("[NAME] #cgeo #mytag");
            Geocache cache = new Geocache();
            cache.setGeocode("GC1234");
            cache.setName("TwitterTest");
            assertThat(Twitter.getStatusMessage(cache, null)).isEqualTo("TwitterTest #cgeo #mytag #geocaching");
        } finally {
            TestSettings.setCacheTwitterMessage(oldMessage);
        }
    }
}
