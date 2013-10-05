package cgeo.geocaching.twitter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.settings.Settings;

import junit.framework.TestCase;

public class TwitterTest extends TestCase {

    public static void testTrackableMessage() {
        final String oldMessage = Settings.getTrackableTwitterMessage();
        try {
            Settings.setTrackableTwitterMessage("I touched [NAME] ([URL]).");
        Trackable tb = new Trackable();
        tb.setName("Travel bug");
        tb.setGeocode("TB1234");
        assertEquals("I touched Travel bug (http://www.geocaching.com//track/details.aspx?tracker=TB1234). #cgeo #geocaching", Twitter.getStatusMessage(tb));
        } finally {
            Settings.setTrackableTwitterMessage(oldMessage);
        }
    }

    public static void testCacheMessage() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            Settings.setCacheTwitterMessage("I found [NAME] ([URL]).");
            Geocache cache = new Geocache();
            cache.setGeocode("GC1234");
            cache.setName("TwitterTest");
            assertEquals("I found TwitterTest (http://coord.info/GC1234). #cgeo #geocaching", Twitter.getStatusMessage(cache));
        } finally {
            Settings.setCacheTwitterMessage(oldMessage);
        }
    }

    public static void testAvoidDuplicateTags() {
        final String oldMessage = Settings.getCacheTwitterMessage();
        try {
            Settings.setCacheTwitterMessage("[NAME] #cgeo");
            Geocache cache = new Geocache();
            cache.setGeocode("GC1234");
            cache.setName("TwitterTest");
            assertEquals("TwitterTest #cgeo #geocaching", Twitter.getStatusMessage(cache));
        } finally {
            Settings.setCacheTwitterMessage(oldMessage);
        }
    }
}
