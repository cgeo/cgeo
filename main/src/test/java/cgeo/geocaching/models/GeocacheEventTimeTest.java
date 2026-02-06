package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CacheType;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocacheEventTimeTest {

    @Test
    public void testEventEndTimeWithGcShortDesc() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setShortDescription("<b>26 January 2024, 14:30 - 16:00</b>");
        cache.setDescription("");
        
        assertThat(cache.getEventStartTimeInMinutes()).isEqualTo(14 * 60 + 30);
        assertThat(cache.getEventEndTimeInMinutes()).isEqualTo(16 * 60);
    }

    @Test
    public void testEventEndTimeWithSimpleTime() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setShortDescription("Event at 17:00");
        cache.setDescription("");
        
        // The pattern "HH:MM" without localized keywords should work
        assertThat(cache.getEventStartTimeInMinutes()).isEqualTo(17 * 60);
        // When only start time is found, end time should default to start + 1 hour
        assertThat(cache.getEventEndTimeInMinutes()).isEqualTo(18 * 60);
    }

    @Test
    public void testEventEndTimeDefaultDuration() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setShortDescription("Event starts at 14:30");
        cache.setDescription("");
        
        assertThat(cache.getEventStartTimeInMinutes()).isEqualTo(14 * 60 + 30);
        // When only start time is found, end time should default to start + 1 hour
        assertThat(cache.getEventEndTimeInMinutes()).isEqualTo(15 * 60 + 30);
    }

    @Test
    public void testEventEndTimeNotSet() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setShortDescription("No time information");
        cache.setDescription("");
        
        assertThat(cache.getEventStartTimeInMinutes()).isEqualTo(-1);
        assertThat(cache.getEventEndTimeInMinutes()).isEqualTo(-1);
    }

    @Test
    public void testNonEventCache() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.TRADITIONAL);
        cache.setShortDescription("Event at 14:30");
        cache.setDescription("");
        
        assertThat(cache.getEventStartTimeInMinutes()).isEqualTo(-1);
        assertThat(cache.getEventEndTimeInMinutes()).isEqualTo(-1);
    }
}
