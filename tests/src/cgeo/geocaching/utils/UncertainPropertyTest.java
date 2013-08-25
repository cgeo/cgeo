package cgeo.geocaching.utils;

import junit.framework.TestCase;

public class UncertainPropertyTest extends TestCase {

    public static void testHigherCertaintyWins() throws Exception {
        final UncertainProperty<String> prop1 = new UncertainProperty<String>("prop1", 10);
        final UncertainProperty<String> prop2 = new UncertainProperty<String>("prop2", 20);
        assertEquals(prop2, UncertainProperty.getMergedProperty(prop1, prop2));
    }

    public static void testAvoidNull() throws Exception {
        final UncertainProperty<String> prop1 = new UncertainProperty<String>("prop1", 10);
        final UncertainProperty<String> prop2 = new UncertainProperty<String>(null, 20);
        assertEquals(prop1, UncertainProperty.getMergedProperty(prop1, prop2));
        assertEquals(prop1, UncertainProperty.getMergedProperty(prop2, prop1));
    }
}
