package cgeo.geocaching.command;

import cgeo.geocaching.models.Geocache;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class RemoveNotUniqueHelperTest extends TestCase {

    private static final Integer LIST_ID1 = 1;
    private static final Integer LIST_ID2 = 2;
    private static final Integer LIST_ID3 = 3;

    private Set<Geocache> geoCaches;
    private Set<Integer> lists1;
    private Set<Integer> lists2;
    private Set<Integer> lists3;
    private Geocache gc1;
    private Geocache gc2;
    private Geocache gc3;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        geoCaches = new HashSet<>();

        gc1 = new Geocache();
        gc2 = new Geocache();
        gc3 = new Geocache();

        gc1.setName("GC1");
        gc2.setName("GC2");
        gc3.setName("GC3");

        geoCaches.add(gc1);
        geoCaches.add(gc2);
        geoCaches.add(gc3);

        lists1 = gc1.getLists();
        lists2 = gc2.getLists();
        lists3 = gc3.getLists();
    }

    public void testRemoveNonUniqueCaches() {
        // GIVEN
        lists1.add(LIST_ID1);
        lists1.add(LIST_ID2);
        lists2.add(LIST_ID1);
        lists3.add(LIST_ID2);
        lists3.add(LIST_ID3);
        // WHEN
        final Set<Geocache> toBeRemoved = RemoveNotUniqueHelper.removeNonUniqueCaches(geoCaches);
        // THEN
        assertThat(toBeRemoved.size()).isEqualTo(2);
        assertThat(toBeRemoved.contains(gc1)).isTrue();
        assertThat(toBeRemoved.contains(gc2)).isFalse();
        assertThat(toBeRemoved.contains(gc3)).isTrue();
    }

    public void testShouldRemoveAllCaches() {
        // GIVEN
        lists1.add(LIST_ID1);
        lists1.add(LIST_ID2);
        lists2.add(LIST_ID1);
        lists2.add(LIST_ID3);
        lists3.add(LIST_ID2);
        lists3.add(LIST_ID3);
        // WHEN
        final Set<Geocache> toBeRemoved = RemoveNotUniqueHelper.removeNonUniqueCaches(geoCaches);
        // THEN
        assertThat(toBeRemoved.size()).isEqualTo(3);
        assertThat(toBeRemoved.contains(gc1)).isTrue();
        assertThat(toBeRemoved.contains(gc2)).isTrue();
        assertThat(toBeRemoved.contains(gc3)).isTrue();
    }

    public void testShouldRemoveNoCache() {
        // GIVEN
        lists1.add(LIST_ID1);
        lists2.add(LIST_ID3);
        lists3.add(LIST_ID3);
        // WHEN
        final Set<Geocache> toBeRemoved = RemoveNotUniqueHelper.removeNonUniqueCaches(geoCaches);
        // THEN
        assertThat(toBeRemoved.size()).isEqualTo(0);
        assertThat(toBeRemoved.contains(gc1)).isFalse();
        assertThat(toBeRemoved.contains(gc2)).isFalse();
        assertThat(toBeRemoved.contains(gc3)).isFalse();
    }
}
