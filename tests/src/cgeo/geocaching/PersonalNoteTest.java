package cgeo.geocaching;

import junit.framework.TestCase;

public class PersonalNoteTest extends TestCase {

    public static void testParse() {
        final String testString = "cgeo:\nSimple cgeo note\n--\nSimple provider note";
        PersonalNote parsedNote = PersonalNote.parseFrom(testString);
        assertEquals(testString, parsedNote.toString());
    }

    public static void testParseProviderOnly() {
        final String testString = "Simple provider note";
        PersonalNote parsedNote = PersonalNote.parseFrom(testString);
        assertEquals(testString, parsedNote.toString());
    }

    public static void testParseCgeoOnly() {
        final String testString = "cgeo:\nSimple cgeo note";
        PersonalNote parsedNote = PersonalNote.parseFrom(testString);
        assertEquals(testString, parsedNote.toString());
    }

    public static void testSimpleMerge() {
        PersonalNote myNote = PersonalNote.parseFrom("cgeo:\nSimple cgeo note\n--\nSimple provider note");
        PersonalNote otherNote = PersonalNote.parseFrom("cgeo:\ncgeo note\n--\nProvider note");
        PersonalNote result = myNote.mergeWith(otherNote);
        assertEquals("cgeo:\ncgeo note\n--\nSimple provider note", result.toString());
    }

    public static void testMixedMerge() {
        PersonalNote myNote = PersonalNote.parseFrom("cgeo:\nSimple cgeo note\n--\nSimple provider note");
        PersonalNote otherNote = PersonalNote.parseFrom("Provider note");
        PersonalNote result = myNote.mergeWith(otherNote);
        assertEquals("cgeo:\nSimple cgeo note\n--\nSimple provider note", result.toString());
        result = otherNote.mergeWith(myNote);
        assertEquals("cgeo:\nSimple cgeo note\n--\nProvider note", result.toString());
    }
}
