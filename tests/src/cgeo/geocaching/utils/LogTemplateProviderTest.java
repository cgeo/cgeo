package cgeo.geocaching.utils;

import cgeo.geocaching.utils.LogTemplateProvider;

import java.util.Calendar;

import junit.framework.TestCase;

public class LogTemplateProviderTest extends TestCase {

    public static void testApplyTemplates() {
        final String noTemplates = " no templates ";
        assertEquals(noTemplates, LogTemplateProvider.applyTemplates(noTemplates, true));

        // This test can occasionally fail if the current year changes right after the next line.
        final String currentYear = Integer.toString(Calendar.YEAR);
        assertTrue(LogTemplateProvider.applyTemplates("[DATE]", true).contains(currentYear));
    }

}
