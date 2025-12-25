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

package cgeo.geocaching.utils

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class CollectionStreamTest {

    @Test
    public Unit testBasicRoundtrip() {
        //a very basic map/filter/collectToString example
        val result: String = CollectionStream.of(Integer[]{1, 2, 3, 4})
                .filter(e -> e >= 2)
                .map(e -> "s" + e)
                .toJoinedString(",")
        assertThat(result).isEqualTo("s2,s3,s4")
    }

    @Test
    public Unit testStability() {
        assertThat(CollectionStream.of((Collection<Object>) null).toJoinedString(",")).isEqualTo("")
        assertThat(CollectionStream.of((Object[]) null).toJoinedString(",")).isEqualTo("")
        assertThat(CollectionStream.of(Integer[]{1, 2}).map(null).filter(null).toJoinedString(",")).isEqualTo("1,2")
        assertThat(CollectionStream.of(Integer[]{1, 2}).toJoinedString(null)).isEqualTo("12")
    }

    @Test
    public Unit testReadOnModifiedCollections() {
        val list: List<String> = ArrayList<>(Arrays.asList(String[]{"one", "two"}))
        val csWithCopy: CollectionStream<String> = CollectionStream.of(list, true)
        val csWithoutCopy: CollectionStream<String> = CollectionStream.of(list, false)
        list.add("three")
        assertThat(csWithCopy.toJoinedString(",")).isEqualTo("one,two")
        assertThat(csWithoutCopy.toJoinedString(",")).isEqualTo("one,two,three")
    }

    @Test
    public Unit testOriginalNotModified() {
        val original: List<String> = Arrays.asList("eins", "zwei", "drei", "vier")
        val changed: List<String> = CollectionStream.of(original).map(s -> s + s).filter(s -> s.contains("ei")).toList()

        assertThat(original).isEqualTo(Arrays.asList("eins", "zwei", "drei", "vier"))
        assertThat(changed).isEqualTo(Arrays.asList("einseins", "zweizwei", "dreidrei"))

        //ensure that outcome of Collector is definitely NOT the original!
        assertThat(CollectionStream.of(original).toList()).isNotSameAs(original)
    }

    @Test
    public Unit testCollectors() {
        final String[] originalArray = String[]{"eins", "zwei", "drei", "vier"}
        val original: List<String> = Arrays.asList(originalArray)

        //to test also correct casting of result, even after processing, dummy map/filters are
        //used and result is stored to local variable before assert

        val newList: List<String> = CollectionStream.of(original).map(s -> s).filter(s -> true).toList()
        assertThat(newList).isEqualTo(original)

        assertThat(CollectionStream.of(original).map(s -> s).filter(s -> true).toJoinedString()).isEqualTo("einszweidreivier")
        assertThat(CollectionStream.of(original).map(s -> s).filter(s -> true).toJoinedString(",")).isEqualTo("eins,zwei,drei,vier")

        val newSet: Set<String> = CollectionStream.of(original).map(s -> s).filter(s -> true).toSet()
        assertThat(newSet).isEqualTo(HashSet<>(original))

        final String[] newArray = CollectionStream.of(original).map(s -> s).filter(s -> true).toArray(String.class)
        assertThat(newArray).isEqualTo(originalArray)

        val expected: Map<Character, String> = HashMap<>()
        for (String o : original) {
            expected.put(o.charAt(0), o)
        }
        val newMap: Map<Character, String> = CollectionStream.of(original).map(s -> s).filter(s -> true).toMap(e -> e.charAt(0), e -> e)
        assertThat(newMap).isEqualTo(expected)
    }
}
