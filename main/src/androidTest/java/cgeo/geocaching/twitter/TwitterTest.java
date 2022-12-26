package cgeo.geocaching.twitter;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class TwitterTest {

    @Test
    public void testTrackableMessage() {
        final String oldMessage = Settings.getTrackableTwitterMessage();
        try {
            TestSettings.setTrackableTwitterMessage("I touched [NAME] ([URL]).");
            final Trackable tb = new Trackable();
            tb.setName("Travel bug");
            tb.setGeocode("TB1234");
            assertThat(Twitter.getStatusMessage(tb, null)).isEqualTo("I touched Travel bug (https://www.geocaching.com//track/details.aspx?tracker=TB1234). #cgeo #geocaching");
        } finally {
            TestSettings.setTrackableTwitterMessage(oldMessage);
        }
    }

    @Test
    public void testCacheMessage() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            TestSettings.setCacheTwitterMessage("I found [NAME] ([URL]).");
            final Geocache cache = new Geocache();
            cache.setGeocode("GC1234");
            cache.setName("TwitterTest");
            assertThat(Twitter.getStatusMessage(cache, null)).isEqualTo("I found TwitterTest (https://coord.info/GC1234). #cgeo #geocaching");
        } finally {
            TestSettings.setCacheTwitterMessage(oldMessage);
        }
    }

    @Test
    public void testCacheMessageWithLogContent() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            TestSettings.setCacheTwitterMessage("[LOG]");
            final Geocache cache = new Geocache();
            final LogEntry log = new LogEntry.Builder().setDate(0).setLogType(LogType.FOUND_IT).setLog("log text").build();
            assertThat(Twitter.getStatusMessage(cache, log)).isEqualTo("log text #cgeo #geocaching");
        } finally {
            TestSettings.setCacheTwitterMessage(oldMessage);
        }
    }

    @Test
    public void testTrackableMessageWithLogContent() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            TestSettings.setTrackableTwitterMessage("[LOG]");
            final Trackable trackable = new Trackable();
            final LogEntry log = new LogEntry.Builder().setDate(0).setLogType(LogType.FOUND_IT).setLog("trackable log text").build();
            assertThat(Twitter.getStatusMessage(trackable, log)).isEqualTo("trackable log text #cgeo #geocaching");
        } finally {
            TestSettings.setTrackableTwitterMessage(oldMessage);
        }
    }

    @Test
    public void testAvoidDuplicateTags() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            TestSettings.setCacheTwitterMessage("[NAME] #cgeo #mytag");
            final Geocache cache = new Geocache();
            cache.setGeocode("GC1234");
            cache.setName("TwitterTest");
            assertThat(Twitter.getStatusMessage(cache, null)).isEqualTo("TwitterTest #cgeo #mytag #geocaching");
        } finally {
            TestSettings.setCacheTwitterMessage(oldMessage);
        }
    }
}
