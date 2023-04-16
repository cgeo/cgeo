package cgeo.geocaching.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;

/** Test Helper class to create and assert lists of actions */
public final class AssertUtils {

    private AssertUtils() {
        //empty on purpose
    }

    @SuppressWarnings("PMD.NPathComplexity") // readability won't be imporved upon split
    public static void  assertGroupedContentList(final String desc, final List<?> list, final Object ... elementGroups) {

        final String description = desc == null ? "" : desc + ": ";
        if (list == null) {
            fail(description + ": list is null");
        }

        int groupIndex = 0;
        Object[] currentGroup = null;
        final List<Object> groupElements = new ArrayList<>();
        for (Object element : list) {
            if (groupIndex >= elementGroups.length) {
                fail(description + "More elements in list than in groups (" + list.size() + "): " + list + " (at element: " + element + ")");
            }
            if (currentGroup == null) {
                final Object o = elementGroups[groupIndex];
                if (o instanceof Collection<?>) {
                    currentGroup = ((Collection<?>) o).toArray(new Object[0]);
                } else if (o != null && o.getClass().isArray()) {
                    currentGroup = (Object[]) o;
                } else {
                    currentGroup = new Object[]{o};
                }
            }
            groupElements.add(element);
            if (groupElements.size() == currentGroup.length) {
                assertThat(groupElements).as(description + "group " + groupIndex + " has not same elements (list: " + list + ")").hasSameElementsAs(Arrays.asList(currentGroup));
                groupElements.clear();
                groupIndex++;
                currentGroup = null;
            }
        }
        if (!groupElements.isEmpty() || groupIndex != elementGroups.length) {
            fail(description + "More elements expected than in list (" + list.size() + "): " + list + "(leftover: " + groupElements + ")");
        }
    }

}
