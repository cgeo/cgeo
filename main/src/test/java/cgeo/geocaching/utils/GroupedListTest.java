package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GroupedListTest {

    @Test
    public void empty() {
        final GroupedList<String> list = new GroupedList<>();
        assertList(list);
    }

    @Test
    public void noGroupUsage() {
        final GroupedList<String> list = new GroupedList<>();
        list.add("one");
        list.add(0, "two");
        list.add("three");
        assertList(list, "two", "one", "three");

        list.remove("one");
        assertList(list, "two", "three");

        list.clear();
        assertList(list);
    }

    @Test
    public void initalizedList() {
        final GroupedList<String> list = new GroupedList<>(new ArrayList<>(Arrays.asList("one", "two")), 2);
        assertList(list, "2:one", "2:two");

        list.add("three");
        assertList(list, "2:one", "2:two", "2:three");

        list.addToGroup("four", 1);
        assertList(list, "1:four", "2:one", "2:two", "2:three");
    }

    @Test
    public void addIndexedToGroups() {
        final GroupedList<String> list = new GroupedList<>();
        list.addToGroup("one", 1);
        list.addToGroup("two", 2);
        assertList(list, "1:one", "2:two");

        list.add("three");
        assertList(list, "1:one", "2:two", "2:three");

        list.add(0, "four");
        assertList(list, "1:four", "1:one", "2:two", "2:three");

        list.add(2, "five");
        assertList(list, "1:four", "1:one", "2:five", "2:two", "2:three");
    }

    @Test
    public void addCollection() {
        final GroupedList<String> list = new GroupedList<>();
        list.addToGroup("one", 1);
        list.addToGroup("two", 2);
        assertList(list, "1:one", "2:two");

        list.addAllToGroup(Arrays.asList("three", "four"), 2);
        assertList(list, "1:one", "2:three", "2:four", "2:two");

        list.addAllToGroup(Arrays.asList("five", "six"), 3);
        assertList(list, "1:one", "2:three", "2:four", "2:two", "3:five", "3:six");
    }

    @Test
    public void removeGroup() {
        final GroupedList<String> list = new GroupedList<>();
        list.addToGroup("one", 1);
        list.addToGroup("two", 2);
        list.addToGroup("three", 2);
        assertList(list, "1:one", "2:three", "2:two");

        list.removeGroup(2);
        assertList(list, "1:one");

        list.removeGroup(3);
        assertList(list, "1:one");

        list.removeGroup(1);
        assertList(list);
    }

    @Test
    public void remove() {
        final GroupedList<String> list = new GroupedList<>();
        list.add("one");
        assertList(list, "0:one");
        list.remove(0);
        assertList(list);
    }

    @Test
    public void removeFromGroup() {
        final GroupedList<String> list = new GroupedList<>();
        list.addToGroup("one", 2);
        assertList(list, "2:one");
        list.remove(0);
        assertList(list);
    }

    @Test
    public void errorHandling() {
        final GroupedList<String> list = new GroupedList<>();
        list.add("one");
        list.addToGroup("two", 2);
        assertList(list, "0:one", "2:two");

        assertThatThrownBy(() -> list.remove(2))
                .isInstanceOf(IndexOutOfBoundsException.class);

        //check that list is unchanged
        assertList(list, "0:one", "2:two");
    }

    @Test
    public void presentAndNonPresentGroups() {
        final GroupedList<String> list = new GroupedList<>();
        list.add("one");
        list.addToGroup("two", 2);
        list.addToGroup("three", 2);
        assertList(list, "0:one", "2:three", "2:two");

        assertThat(list.groupSize(0)).isEqualTo(1);
        assertThat(list.groupSize(1)).isEqualTo(0);
        assertThat(list.groupSize(2)).isEqualTo(2);

        assertThat(list.groupStart(0)).isEqualTo(0);
        assertThat(list.groupStart(1)).isEqualTo(-1);
        assertThat(list.groupStart(2)).isEqualTo(1);

        assertThat(list.groupFor(0)).isEqualTo(0);
        assertThat(list.groupFor(1)).isEqualTo(2);
        assertThat(list.groupFor(2)).isEqualTo(2);
        assertThat(list.groupFor(3)).isEqualTo(-1);
    }

    @Test
    public void mixedAdd() {
        final GroupedList<String> list = new GroupedList<>(new ArrayList<>(), 5);
        list.add("one"); //added to default group
        list.add("two"); //added to default group
        assertList(list, "5:one", "two");

        list.addToGroup("three", 6);
        list.addToGroup("four", 2);
        assertList(list, "2:four", "5:one", "two", "6:three");
    }

    @Test
    public void mixedAddRemove() {
        final GroupedList<String> list = new GroupedList<>();
        list.add("one");
        assertList(list, "0:one");

        list.addToGroup("two", 2);
        assertList(list, "0:one", "2:two");

        list.addToGroup("three", 3);
        assertList(list, "0:one", "2:two", "3:three");

        list.add(1, "four");
        assertList(list, "0:one", "2:four", "2:two", "3:three");

        list.remove("three");
        assertList(list, "0:one", "2:four", "2:two");

        list.remove(0);
        assertList(list, "2:four", "2:two");
    }

    @Test
    public void groupedIndexOf() {
        final GroupedList<String> list = new GroupedList<>();
        list.add("one");
        list.addToGroup("two", 2);
        list.addToGroup("three", 2);
        assertList(list, "0:one", "2:three", "2:two");

        assertThat(list.groupIndexOf(0, "one")).isEqualTo(0);
        assertThat(list.groupIndexOf(2, "one")).isEqualTo(-1);
        assertThat(list.groupIndexOf(2, "three")).isEqualTo(1);
        assertThat(list.groupIndexOf(2, "two")).isEqualTo(2);
        assertThat(list.groupIndexOf(0, "three")).isEqualTo(-1);
        assertThat(list.groupIndexOf(0, "two")).isEqualTo(-1);

        assertThat(list.groupIndexOf(0, "nonexistingelement")).isEqualTo(-1);
        assertThat(list.groupIndexOf(3, "one")).isEqualTo(-1);
        assertThat(list.groupIndexOf(3, "two")).isEqualTo(-1);
    }

    private void assertList(final GroupedList<String> list, final String ... entry) {
        final String msg = " (Expected: " + Arrays.asList(entry) + ", provided: " + list + ")";
        assertThat(list).as("Size not matching" + msg).hasSameSizeAs(entry);

        final Map<Integer, Integer> expectedGroupStarts = new HashMap<>();
        final Map<Integer, Integer> expectedGroupSizes = new HashMap<>();

        int currentGroup = 0;
        for (int i = 0; i < entry.length; i++) {
            String e = entry[i];
            if (e.contains(":")) {
                final String[] tokens = entry[i].split(":");
                e = tokens[1];
                final int newGroup = Integer.parseInt(tokens[0]);
                assertThat(newGroup).as("new group must be greater than current group").isGreaterThanOrEqualTo(currentGroup);
                currentGroup = newGroup;
            }
            assertThat(list.get(i)).as("Index " + i + " not equal" + msg).isEqualTo(e);
            assertThat(list.groupFor(i)).as("Group on index " + i + " not equal" + msg).isEqualTo(currentGroup);

            if (!expectedGroupStarts.containsKey(currentGroup)) {
                expectedGroupStarts.put(currentGroup, i);
                expectedGroupSizes.put(currentGroup, 0);
            }
            expectedGroupSizes.put(currentGroup, expectedGroupSizes.get(currentGroup) + 1);
        }

        //assert grouplist metadata
        assertThat(list.groups().size()).as("Group size differs from expected" + msg).isEqualTo(expectedGroupSizes.size());
        for (int group : expectedGroupStarts.keySet()) {
            assertThat(list.groupStart(group)).as("Group " + group + " start pos unexpected" + msg).isEqualTo(expectedGroupStarts.get(group));
            assertThat(list.groupSize(group)).as("Group " + group + " size unexpected" + msg).isEqualTo(expectedGroupSizes.get(group));
        }
    }
}
