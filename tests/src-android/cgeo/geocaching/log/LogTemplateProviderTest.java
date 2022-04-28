package cgeo.geocaching.log;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;

import java.util.Calendar;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LogTemplateProviderTest extends TestCase {

    public static void testApplyTemplatesNone() {
        final String noTemplates = " no templates ";
        final String signature = LogTemplateProvider.applyTemplates(noTemplates, new LogContext(null, null, true));
        assertThat(signature).isEqualTo(noTemplates);
    }

    public static void testApplyTemplates() {
        // This test can occasionally fail if the current year changes right after the next line.
        final String currentYear = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        final String signature = LogTemplateProvider.applyTemplates("[DATE]", new LogContext(null, null, true));
        assertThat(signature).contains(currentYear);
    }

    /**
     * signature itself can contain templates, therefore nested applying is necessary
     */
    public static void testApplySignature() {
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
    public static void testApplyInvalidSignature() {
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

    public static void testNoNumberIncrement() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC45GGA");
        final LogContext context = new LogContext(cache, new LogEntry.Builder().setLogType(LogType.FOUND_IT).build());
        final String template = "[ONLINENUM]";
        final String withIncrement = LogTemplateProvider.applyTemplates(template, context);
        final String withoutIncrement = LogTemplateProvider.applyTemplatesNoIncrement(template, context);

        // both strings represent integers with an offset of one.
        assertThat(Integer.parseInt(withIncrement) - Integer.parseInt(withoutIncrement)).isEqualTo(1);
    }

    public static void testNumberLogTypeIncrement() {
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

    public static void testSizeTemplate() {
        final LogContext context = createCache();
        context.getCache().setSize(CacheSize.VERY_LARGE);
        final String log = LogTemplateProvider.applyTemplates("[SIZE]", context);
        assertThat(log).isEqualTo(CacheSize.VERY_LARGE.getL10n());
    }

    public static void testLocationTemplate() {
        final LogContext context = createCache();
        Sensors.getInstance().currentGeo().reset();
        final String distance = Units.getDistanceFromMeters(0);
        final String log = LogTemplateProvider.applyTemplates("[LOCATION]", context);
        assertThat(log).isEqualTo("N 00° 00.000' · E 000° 00.000' (±" + distance + ")");
    }
}
