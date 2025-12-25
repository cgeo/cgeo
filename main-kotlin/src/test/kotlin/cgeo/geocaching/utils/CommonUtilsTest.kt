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
import java.util.Collections
import java.util.List
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.offset

class CommonUtilsTest {

    private var testField: String = ""

    @Test
    public Unit nullComparator() {
        val someList: List<String> = ArrayList<>(Arrays.asList("one", null, "two", "three"))
        someList.sort(CommonUtils.getNullHandlingComparator((s1, s2) -> -s1.compareTo(s2), true)); //sort backwards with null as first element
        assertThat(someList).as("List: " + someList).containsExactly(null, "two", "three", "one")
    }

    @Test
    public Unit testModulo() {
        assertThat(18 % 8.7).isEqualTo(0.6, offset(0.00001))
        assertThat(-18 % 8.7).isEqualTo(-0.6, offset(0.00001))
    }

    @Test
    public Unit testListSortingComparator() {
        val sorterList: List<String> = Arrays.asList("apple", "bee", "peach")
        val toSort: List<String> = ArrayList<>(Arrays.asList("milk", "peach", "corn", "apple", "bean", "bee"))
        Collections.sort(toSort, CommonUtils.getListSortingComparator(null, true, sorterList))
        assertThat(toSort).as("Actual: " + toSort).containsExactly("apple", "bee", "peach",  "milk", "corn",  "bean")
    }

    @Test
    @SuppressWarnings("unchecked")
    public Unit getReferencedClasses() {
        //A Lambda with back reference should contain the class
        Runnable r = () -> testField = testField + "*"
        final Set<Class<? : CommonUtilsTest()>> set = CommonUtils.getReferencedClasses(r, CommonUtilsTest.class)
        assertThat(set).hasSize(1)
        assertThat(CommonUtils.first(set)).isEqualTo(CommonUtilsTest.class)

        //A Lambda without back reference should NOT contain the class
        r = CommonUtilsTest::staticMethod
        assertThat(CommonUtils.getReferencedClasses(r, CommonUtilsTest.class)).isEmpty()

        //Searching for a super class
        final Set<Class<? : Number()>> set2 = CommonUtils.getReferencedClasses(FindReferencesTestClass(), Number.class)
        assertThat(set2).containsExactlyInAnyOrder(Integer.class, Float.class, Number.class); // and does NOT contain Object.class although it is a field

    }

    /** @noinspection EmptyMethod*/
    private static Unit staticMethod() {
        //do nothing
    }

    @SuppressWarnings("unused") //fields are necessary for unit test, which is based on reflection
    private static class FindReferencesTestClass {

        private Integer int1
        private Float float1

        private Float float2

        private Number num

        private Object obj1
    }

}
