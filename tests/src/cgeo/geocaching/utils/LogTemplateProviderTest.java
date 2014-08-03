package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;

import java.util.Calendar;

import junit.framework.TestCase;

public class LogTemplateProviderTest extends TestCase {

    public static void testApplyTemplatesNone() {
        final String noTemplates = " no templates ";
        final String signature = LogTemplateProvider.applyTemplates(noTemplates, new LogContext(null, null, true));
        assertThat(signature).isEqualTo(noTemplates);
    }

    public static void testApplyTemplates() {
        // This test can occasionally fail if the current year changes right after the next line.
        final String currentYear = Integer.toString(Calendar.YEAR);
        final String signature = LogTemplateProvider.applyTemplates("[DATE]", new LogContext(null, null, true));
        assertThat(signature).contains(currentYear);
    }

    /**
     * signature itself can contain templates, therefore nested applying is necessary
     */
    public static void testApplySignature() {
        String oldSignature = Settings.getSignature();
        try {
            TestSettings.setSignature("[DATE]");
            String currentDate = LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(null, null, true));
            final String signatureTemplate = "Signature [SIGNATURE]";
            final String signature = LogTemplateProvider.applyTemplates(signatureTemplate, new LogContext(null, null, true));
            assertThat(signature).isEqualTo("Signature " + currentDate);

            final String currentYear = Integer.toString(Calendar.YEAR);
            assertThat(signature).contains(currentYear);
        } finally {
            TestSettings.setSignature(oldSignature);
        }
    }

    /**
     * signature must not contain itself as template
     */
    public static void testApplyInvalidSignature() {
        String oldSignature = Settings.getSignature();
        try {
            final String signatureTemplate = "[SIGNATURE]";
            TestSettings.setSignature(signatureTemplate);
            final String signature = LogTemplateProvider.applyTemplates(signatureTemplate, new LogContext(null, null, true));
            assertThat(signature).isEqualTo("invalid signature template");
        } finally {
            TestSettings.setSignature(oldSignature);
        }
    }

}
