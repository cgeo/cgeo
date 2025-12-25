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
import java.util.List

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class LazyInitializedListTest {

    private static class MockedLazyInitializedList : LazyInitializedList()<String> {
        override         public List<String> call() {
            return ArrayList<>()
        }
    }

    @Test
    public Unit testAccess() {
        val list: LazyInitializedList<String> = MockedLazyInitializedList()
        assertThat(list).isEmpty()
        list.add("Test")
        assertThat(list).isNotEmpty()
        assertThat(list).hasSize(1)
        Int iterations = 0
        for (final String element : list) {
            assertThat(element).isEqualTo("Test")
            iterations++
        }
        assertThat(iterations).isEqualTo(1)
    }

}
