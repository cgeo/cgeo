package cgeo.geocaching.log;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;

import java.util.Calendar;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LogTemplateProviderTest {

    @Test
    public void testApplyTemplatesNone() {
        final String noTemplates = " no templates ";
        final String signature = LogTemplateProvider.applyTemplates(noTemplates, new LogContext(null, null, true));
        assertThat(signature).isEqualTo(noTemplates);
    }

    @Test
    public void testApplyTemplates() {
        // This test can occasionally fail if the current year changes right after the next line.
        final String currentYear = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        final String signature = LogTemplateProvider.applyTemplates("[DATE]", new LogContext(null, null, true));
        assertThat(signature).contains(currentYear);
    }

    /**
     * signature itself can contain templates, therefore nested applying is necessary
     */
    @Test
    public void testApplySignature() {
        final String oldSignature = Settings.getSignature();
        try {
            TestSettings.setSignature("[DATE]");
            final String currentDate = LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(null, null, true));
            final String signatureTemplate = "Signature [SIGNATURE]";
            final String signature = LogTemplateProvider.applyTemplates(signatureTemplate, new LogContext(null, null, true));
            assertThat(signature).isEqualTo("Signature " + currentDate);

            final String currentYear = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
            assertThat(signature).contains(currentYear);
        } finally {
            TestSettings.setSignature(oldSignature);
        }
    }

    /**
     * signature must not contain itself as template
     */
    @Test
    public void testApplyInvalidSignature() {
        final String oldSignature = Settings.getSignature();
        try {
            final String signatureTemplate = "[SIGNATURE]";
            TestSettings.setSignature(signatureTemplate);
            final String signature = LogTemplateProvider.applyTemplates(signatureTemplate, new LogContext(null, null, true));
            assertThat(signature).isEqualTo("invalid signature template");
        } finally {
            TestSettings.setSignature(oldSignature);
        }
    }

    @Test
    public void testNoNumberIncrement() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC45GGA");
        final LogContext context = new LogContext(cache, new LogEntry.Builder().setLogType(LogType.FOUND_IT).build());
        final String template = "[ONLINENUM]";
        final String withIncrement = LogTemplateProvider.applyTemplates(template, context);
        final String withoutIncrement = LogTemplateProvider.applyTemplatesNoIncrement(template, context);

        // both strings represent integers with an offset of one.
        assertThat(Integer.parseInt(withIncrement) - Integer.parseInt(withoutIncrement)).isEqualTo(1);
    }

    @Test
    public void testNumberLogTypeIncrement() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC45GGA");
        final LogContext context = new LogContext(cache, new LogEntry.Builder().setLogType(LogType.FOUND_IT).build());
        final LogContext context2 = new LogContext(cache, new LogEntry.Builder().setLogType(LogType.DIDNT_FIND_IT).build());
        final String template = "[NUMBER]";
        final String withIncrement = LogTemplateProvider.applyTemplates(template, context);
        final String withoutIncrement = LogTemplateProvider.applyTemplates(template, context2);

        // both strings represent integers - the number template should not increase if the log type is not FOUND_IT or something equal
        assertThat(Integer.parseInt(withIncrement) - Integer.parseInt(withoutIncrement)).isEqualTo(1);
    }

    private static LogContext createCache() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC12345");
        return new LogContext(cache, new LogEntry.Builder().build());
    }


    private static LogContext createTrackable() {
        final Trackable trackable = new Trackable();
        return new LogContext(trackable, new LogEntry.Builder().build());
    }

    @Test
    public void testSizeTemplate() {
        final LogContext context = createCache();
        context.getCache().setSize(CacheSize.VERY_LARGE);
        final String log = LogTemplateProvider.applyTemplates("[SIZE]", context);
        assertThat(log).isEqualTo(CacheSize.VERY_LARGE.getL10n());
    }

    @Test
    public void testLocationTemplate() {
        final LogContext context = createCache();
        LocationDataProvider.getInstance().currentGeo().reset();
        final String distance = Units.getDistanceFromMeters(0);
        final String log = LogTemplateProvider.applyTemplates("[LOCATION]", context);
        assertThat(log).isEqualTo("N 00° 00.000' · E 000° 00.000' (±" + distance + ")");
    }

    @Test
    public void testLocationCacheTemplate() {
        final LogContext context = createTrackable();
        final Trackable trackable = context.getTrackable();
        trackable.setSpottedType(Trackable.SPOTTED_CACHE);
        trackable.setSpottedCacheGeocode("GC12345");
        trackable.setSpottedName("My cache");
        final String logCache = LogTemplateProvider.applyTemplates("[TB_LOCATION_GEOCODE]", context);
        final String logName = LogTemplateProvider.applyTemplates("[TB_LOCATION_CACHE]", context);
        assertThat(logCache).isEqualTo("GC12345");
        assertThat(logName).isEqualTo("My cache");
    }

    @Test
    public void testLocationUserTemplate() {
        final LogContext context = createTrackable();
        final Trackable trackable = context.getTrackable();
        trackable.setSpottedType(Trackable.SPOTTED_USER);
        trackable.setSpottedName("username");
        final String log = LogTemplateProvider.applyTemplates("[TB_LOCATION_USER]", context);
        assertThat(log).isEqualTo("username");
    }

    @Test
    public void testLocationOwnerTemplate() {
        final LogContext context = createTrackable();
        final Trackable trackable = context.getTrackable();
        trackable.setSpottedType(Trackable.SPOTTED_OWNER);
        trackable.setOwner("ownername");
        final String log = LogTemplateProvider.applyTemplates("[TB_LOCATION_USER]", context);
        assertThat(log).isEqualTo("ownername");
    }
}
