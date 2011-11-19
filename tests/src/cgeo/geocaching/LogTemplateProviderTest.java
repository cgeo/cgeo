package cgeo.geocaching;

import junit.framework.TestCase;

public class LogTemplateProviderTest extends TestCase {

    public static void testApplyTemplates() {
        final String noTemplates = " no templates ";
        assertEquals(noTemplates, LogTemplateProvider.applyTemplates(noTemplates, true));
        assertTrue(LogTemplateProvider.applyTemplates("[DATE]", true).contains("."));
    }

}
