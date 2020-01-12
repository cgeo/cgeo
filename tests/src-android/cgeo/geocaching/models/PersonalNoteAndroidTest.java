package cgeo.geocaching.models;

import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.list.StoredList;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class PersonalNoteAndroidTest extends TestCase {

    public static void testLocalNoteExceedsLimit() {
        final Geocache truncCache = new Geocache();
        final String testString = StringUtils.repeat("x", GCConnector.getInstance().getPersonalNoteMaxChars() + 1);
        truncCache.setPersonalNote(testString.substring(0, GCConnector.getInstance().getPersonalNoteMaxChars()));
        final PersonalNote parsedNote = new PersonalNote(truncCache);

        final Geocache exceedingCache = new Geocache();
        exceedingCache.getLists().add(StoredList.STANDARD_LIST_ID); // stored
        exceedingCache.setPersonalNote(testString);
        final PersonalNote otherNote = new PersonalNote(exceedingCache);
        final PersonalNote result = parsedNote.mergeWith(otherNote);
        assertPersonalNote(result, null, testString);
    }

    public static void testSimpleMerge() {
        final Geocache cache1 = new Geocache(); // not stored
        cache1.setPersonalNote("Simple cgeo note\n--\nSimple provider note");
        final PersonalNote myNote = new PersonalNote(cache1);
        final Geocache cache2 = new Geocache();
        cache2.getLists().add(StoredList.STANDARD_LIST_ID); // stored
        cache2.setPersonalNote("cgeo note\n--\nProvider note");
        final PersonalNote otherNote = new PersonalNote(cache2);
        final PersonalNote result = myNote.mergeWith(otherNote);
        assertThat(result.toString()).isEqualTo("cgeo note\n--\nSimple provider note");
        assertPersonalNote(result, "cgeo note", "Simple provider note");
    }

    public static void testMixedMerge() {
        final Geocache cache1 = new Geocache(); // not stored
        cache1.setPersonalNote("Simple cgeo note\n--\nSimple provider note");
        final PersonalNote myNote = new PersonalNote(cache1);
        final Geocache cache2 = new Geocache();
        cache2.getLists().add(StoredList.STANDARD_LIST_ID); // stored
        cache2.setPersonalNote("Provider note");
        final PersonalNote otherNote = new PersonalNote(cache2);
        PersonalNote result = myNote.mergeWith(otherNote);
        assertThat(result.toString()).isEqualTo("Simple cgeo note\n--\nSimple provider note");
        assertPersonalNote(result, "Simple cgeo note", "Simple provider note");
        result = otherNote.mergeWith(myNote);
        assertThat(result.toString()).isEqualTo("Simple cgeo note\n--\nProvider note");
        assertPersonalNote(result, "Simple cgeo note", "Provider note");
    }

    private static void assertPersonalNote(final PersonalNote note, final String cgeoNote, final String providerNote) {
        assertThat(note.getCgeoNote()).isEqualTo(cgeoNote);
        assertThat(note.getProviderNote()).isEqualTo(providerNote);
    }
}
