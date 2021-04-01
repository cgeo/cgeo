package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CollectionStreamTest {

    @Test
    public void testBasicRoundtrip() {
        //a very basic map/filter/collectToString example
        final String result = CollectionStream.of(new Integer[]{1, 2, 3, 4})
                .filter(e -> e >= 2)
                .map(e -> "s" + e)
                .toJoinedString(",");
        assertThat(result).isEqualTo("s2,s3,s4");
    }

    @Test
    public void testStability() {
        assertThat(CollectionStream.of((Collection<Object>) null).toJoinedString(",")).isEqualTo("");
        assertThat(CollectionStream.of((Object[]) null).toJoinedString(",")).isEqualTo("");
        assertThat(CollectionStream.of(new Integer[]{1, 2}).map(null).filter(null).toJoinedString(",")).isEqualTo("1,2");
        assertThat(CollectionStream.of(new Integer[]{1, 2}).toJoinedString(null)).isEqualTo("12");
    }

    @Test
    public void testReadOnModifiedCollections() {
        final List<String> list = new ArrayList<>(Arrays.asList(new String[]{"one", "two"}));
        final CollectionStream<String> csWithCopy = CollectionStream.of(list, true);
        final CollectionStream<String> csWithoutCopy = CollectionStream.of(list, false);
        list.add("three");
        assertThat(csWithCopy.toJoinedString(",")).isEqualTo("one,two");
        assertThat(csWithoutCopy.toJoinedString(",")).isEqualTo("one,two,three");
    }

    @Test
    public void testOriginalNotModified() {
        final List<String> original = Arrays.asList("eins", "zwei", "drei", "vier");
        final List<String> changed = CollectionStream.of(original).map(s -> s + s).filter(s -> s.contains("ei")).toList();

        assertThat(original).isEqualTo(Arrays.asList("eins", "zwei", "drei", "vier"));
        assertThat(changed).isEqualTo(Arrays.asList("einseins", "zweizwei", "dreidrei"));

        //ensure that outcome of Collector is definitely NOT the original!
        assertThat(CollectionStream.of(original).toList()).isNotSameAs(original);
    }

    @Test
    public void testCollectors() {
        final String[] originalArray = new String[]{"eins", "zwei", "drei", "vier"};
        final List<String> original = Arrays.asList(originalArray);

        //to test also correct casting of result, even after processing, dummy map/filters are
        //used and result is stored to local variable before assert

        final List<String> newList = CollectionStream.of(original).map(s -> s).filter(s -> true).toList();
        assertThat(newList).isEqualTo(original);

        assertThat(CollectionStream.of(original).map(s -> s).filter(s -> true).toJoinedString()).isEqualTo("einszweidreivier");
        assertThat(CollectionStream.of(original).map(s -> s).filter(s -> true).toJoinedString(",")).isEqualTo("eins,zwei,drei,vier");

        final Set<String> newSet = CollectionStream.of(original).map(s -> s).filter(s -> true).toSet();
        assertThat(newSet).isEqualTo(new HashSet<>(original));

        final String[] newArray = CollectionStream.of(original).map(s -> s).filter(s -> true).toArray(String.class);
        assertThat(newArray).isEqualTo(originalArray);

        final Map<Character, String> expected = new HashMap<>();
        for (String o : original) {
            expected.put(o.charAt(0), o);
        }
        final Map<Character, String> newMap = CollectionStream.of(original).map(s -> s).filter(s -> true).toMap(e -> e.charAt(0), e -> e);
        assertThat(newMap).isEqualTo(expected);
    }
}
