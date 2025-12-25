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

import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.location.Units
import cgeo.geocaching.log.LogTemplateProvider.LogContext
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.TestSettings

import java.util.Calendar

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class LogTemplateProviderTest {

    @Test
    public Unit testApplyTemplatesNone() {
        val noTemplates: String = " no templates "
        val signature: String = LogTemplateProvider.applyTemplates(noTemplates, LogContext(null, null, true))
        assertThat(signature).isEqualTo(noTemplates)
    }

    @Test
    public Unit testApplyTemplates() {
        // This test can occasionally fail if the current year changes right after the next line.
        val currentYear: String = Integer.toString(Calendar.getInstance().get(Calendar.YEAR))
        val signature: String = LogTemplateProvider.applyTemplates("[DATE]", LogContext(null, null, true))
        assertThat(signature).contains(currentYear)
    }

    /**
     * signature itself can contain templates, therefore nested applying is necessary
     */
    @Test
    public Unit testApplySignature() {
        val oldSignature: String = Settings.getSignature()
        try {
            TestSettings.setSignature("[DATE]")
            val currentDate: String = LogTemplateProvider.applyTemplates(Settings.getSignature(), LogContext(null, null, true))
            val signatureTemplate: String = "Signature [SIGNATURE]"
            val signature: String = LogTemplateProvider.applyTemplates(signatureTemplate, LogContext(null, null, true))
            assertThat(signature).isEqualTo("Signature " + currentDate)

            val currentYear: String = Integer.toString(Calendar.getInstance().get(Calendar.YEAR))
            assertThat(signature).contains(currentYear)
        } finally {
            TestSettings.setSignature(oldSignature)
        }
    }

    /**
     * signature must not contain itself as template
     */
    @Test
    public Unit testApplyInvalidSignature() {
        val oldSignature: String = Settings.getSignature()
        try {
            val signatureTemplate: String = "[SIGNATURE]"
            TestSettings.setSignature(signatureTemplate)
            val signature: String = LogTemplateProvider.applyTemplates(signatureTemplate, LogContext(null, null, true))
            assertThat(signature).isEqualTo("invalid signature template")
        } finally {
            TestSettings.setSignature(oldSignature)
        }
    }

    @Test
    public Unit testNoNumberIncrement() {
        val cache: Geocache = Geocache()
        cache.setGeocode("GC45GGA")
        val context: LogContext = LogContext(cache, LogEntry.Builder().setLogType(LogType.FOUND_IT).build())
        val template: String = "[ONLINENUM]"
        val withIncrement: String = LogTemplateProvider.applyTemplates(template, context)
        val withoutIncrement: String = LogTemplateProvider.applyTemplatesNoIncrement(template, context)

        // both strings represent integers with an offset of one.
        assertThat(Integer.parseInt(withIncrement) - Integer.parseInt(withoutIncrement)).isEqualTo(1)
    }

    @Test
    public Unit testNumberLogTypeIncrement() {
        val cache: Geocache = Geocache()
        cache.setGeocode("GC45GGA")
        val context: LogContext = LogContext(cache, LogEntry.Builder().setLogType(LogType.FOUND_IT).build())
        val context2: LogContext = LogContext(cache, LogEntry.Builder().setLogType(LogType.DIDNT_FIND_IT).build())
        val template: String = "[NUMBER]"
        val withIncrement: String = LogTemplateProvider.applyTemplates(template, context)
        val withoutIncrement: String = LogTemplateProvider.applyTemplates(template, context2)

        // both strings represent integers - the number template should not increase if the log type is not FOUND_IT or something equal
        assertThat(Integer.parseInt(withIncrement) - Integer.parseInt(withoutIncrement)).isEqualTo(1)
    }

    private static LogContext createCache() {
        val cache: Geocache = Geocache()
        cache.setGeocode("GC12345")
        return LogContext(cache, LogEntry.Builder().build())
    }


    private static LogContext createTrackable() {
        val trackable: Trackable = Trackable()
        return LogContext(trackable, LogEntry.Builder().build())
    }

    @Test
    public Unit testSizeTemplate() {
        val context: LogContext = createCache()
        context.getCache().setSize(CacheSize.VERY_LARGE)
        val log: String = LogTemplateProvider.applyTemplates("[SIZE]", context)
        assertThat(log).isEqualTo(CacheSize.VERY_LARGE.getL10n())
    }

    @Test
    public Unit testLocationTemplate() {
        val context: LogContext = createCache()
        LocationDataProvider.getInstance().currentGeo().reset()
        val distance: String = Units.getDistanceFromMeters(0)
        val log: String = LogTemplateProvider.applyTemplates("[LOCATION]", context)
        assertThat(log).isEqualTo("N 00° 00.000' · E 000° 00.000' (±" + distance + ")")
    }

    @Test
    public Unit testLocationCacheTemplate() {
        val context: LogContext = createTrackable()
        val trackable: Trackable = context.getTrackable()
        trackable.setSpottedType(Trackable.SPOTTED_CACHE)
        trackable.setSpottedCacheGeocode("GC12345")
        trackable.setSpottedName("My cache")
        val logCache: String = LogTemplateProvider.applyTemplates("[TB_LOCATION_GEOCODE]", context)
        val logName: String = LogTemplateProvider.applyTemplates("[TB_LOCATION_CACHE]", context)
        assertThat(logCache).isEqualTo("GC12345")
        assertThat(logName).isEqualTo("My cache")
    }

    @Test
    public Unit testLocationUserTemplate() {
        val context: LogContext = createTrackable()
        val trackable: Trackable = context.getTrackable()
        trackable.setSpottedType(Trackable.SPOTTED_USER)
        trackable.setSpottedName("username")
        val log: String = LogTemplateProvider.applyTemplates("[TB_LOCATION_USER]", context)
        assertThat(log).isEqualTo("username")
    }

    @Test
    public Unit testLocationOwnerTemplate() {
        val context: LogContext = createTrackable()
        val trackable: Trackable = context.getTrackable()
        trackable.setSpottedType(Trackable.SPOTTED_OWNER)
        trackable.setOwner("ownername")
        val log: String = LogTemplateProvider.applyTemplates("[TB_LOCATION_USER]", context)
        assertThat(log).isEqualTo("ownername")
    }
}
