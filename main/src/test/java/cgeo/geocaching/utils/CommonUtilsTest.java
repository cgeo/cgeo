package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CommonUtilsTest {

    @Test
    public void nullComparator() {
        final List<String> someList = new ArrayList<>(Arrays.asList("one", null, "two", "three"));
        someList.sort(CommonUtils.getNullHandlingComparator((s1, s2) -> -s1.compareTo(s2), true)); //sort backwards with null as first element
        assertThat(someList).containsExactly(null, "two", "three", "one");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void groupList() {
        //Original Index of test data:          0       1      2        3        4         5       6        7
        final List<String> data = Arrays.asList("blue", "red", "green", "x-red", "yellow", "gray", "brown", "x-pink");
        //for test, group list after first letter with standard group order. "x" is not part of a group
        final List<List<Object>> groupedList = new ArrayList<>();
        CommonUtils.groupList(data, (s, idx) -> s.startsWith("x-") ? null : s.substring(0, 1), null,
                (group, firstIdx, size) -> groupedList.add(Arrays.asList(group, true, firstIdx, size)),
                (item, originalIdx, group, groupIndex) -> groupedList.add(Arrays.asList(item, false, originalIdx, group, groupIndex)));

        assertThat(groupedList).containsExactly(
                //non-grouped items come first. They appear in original order
                Arrays.asList("x-red", false, 3, null, -1),
                Arrays.asList("x-pink", false, 7, null, -1),
                //groups. Groups are sorted alphabetically, items inside group are sorted in original order
                Arrays.asList("b", true, 3, 2),
                Arrays.asList("blue", false, 0, "b", 2),
                Arrays.asList("brown", false, 6, "b", 2),
                Arrays.asList("g", true, 6, 2),
                Arrays.asList("green", false, 2, "g", 5),
                Arrays.asList("gray", false, 5, "g", 5),
                Arrays.asList("r", true, 9, 1),
                Arrays.asList("red", false, 1, "r", 8),
                Arrays.asList("y", true, 11, 1),
                Arrays.asList("yellow", false, 4, "y", 10)
                );
    }
}
