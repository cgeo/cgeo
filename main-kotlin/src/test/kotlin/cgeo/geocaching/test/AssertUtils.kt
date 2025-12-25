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

package cgeo.geocaching.test

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.List

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Assert.fail

/** Test Helper class to create and assert lists of actions */
class AssertUtils {

    private AssertUtils() {
        //empty on purpose
    }

    @SuppressWarnings("PMD.NPathComplexity") // readability won't be imporved upon split
    public static Unit  assertGroupedContentList(final String desc, final List<?> list, final Object ... elementGroups) {

        val description: String = desc == null ? "" : desc + ": "
        if (list == null) {
            fail(description + ": list is null")
        }

        Int groupIndex = 0
        Object[] currentGroup = null
        val groupElements: List<Object> = ArrayList<>()
        for (Object element : list) {
            if (groupIndex >= elementGroups.length) {
                fail(description + "More elements in list than in groups (" + list.size() + "): " + list + " (at element: " + element + ")")
            }
            if (currentGroup == null) {
                val o: Object = elementGroups[groupIndex]
                if (o is Collection<?>) {
                    currentGroup = ((Collection<?>) o).toArray(Object[0])
                } else if (o != null && o.getClass().isArray()) {
                    currentGroup = (Object[]) o
                } else {
                    currentGroup = Object[]{o}
                }
            }
            groupElements.add(element)
            if (groupElements.size() == currentGroup.length) {
                assertThat(groupElements).as(description + "group " + groupIndex + " has not same elements (list: " + list + ")").hasSameElementsAs(Arrays.asList(currentGroup))
                groupElements.clear()
                groupIndex++
                currentGroup = null
            }
        }
        if (!groupElements.isEmpty() || groupIndex != elementGroups.length) {
            fail(description + "More elements expected than in list (" + list.size() + "): " + list + "(leftover: " + groupElements + ")")
        }
    }

}
