package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

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

        try {
            list.remove(2); // expect exception
            Assert.fail("Exception expected");
        } catch (IndexOutOfBoundsException ioobe) {
            //ok, ignore
        }
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

        Assert.assertEquals(1, list.groupSize(0));
        Assert.assertEquals(0, list.groupSize(1));
        Assert.assertEquals(2, list.groupSize(2));

        Assert.assertEquals(0, list.groupStart(0));
        Assert.assertEquals(-1, list.groupStart(1));
        Assert.assertEquals(1, list.groupStart(2));

        Assert.assertEquals(0, list.groupFor(0));
        Assert.assertEquals(2, list.groupFor(1));
        Assert.assertEquals(2, list.groupFor(2));
        Assert.assertEquals(-1, list.groupFor(3));

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


        Assert.assertEquals(0, list.groupIndexOf(0, "one"));
        Assert.assertEquals(-1, list.groupIndexOf(2, "one"));
        Assert.assertEquals(1, list.groupIndexOf(2, "three"));
        Assert.assertEquals(2, list.groupIndexOf(2, "two"));
        Assert.assertEquals(-1, list.groupIndexOf(0, "three"));
        Assert.assertEquals(-1, list.groupIndexOf(0, "two"));

        Assert.assertEquals(-1, list.groupIndexOf(0, "nonexistingelement"));
        Assert.assertEquals(-1, list.groupIndexOf(3, "one"));
        Assert.assertEquals(-1, list.groupIndexOf(3, "two"));
    }

    private void assertList(final GroupedList<String> list, final String ... entry) {
        final String msg = " (Expected: " + Arrays.asList(entry) + ", provided: " + list + ")";
        Assert.assertEquals("Size not matching" + msg, entry.length, list.size());

        final Map<Integer, Integer> expectedGroupStarts = new HashMap<>();
        final Map<Integer, Integer> expectedGroupSizes = new HashMap<>();

        int currentGroup = 0;
        for (int i = 0; i < entry.length; i++) {
            String e = entry[i];
            if (e.contains(":")) {
                final String[] tokens = entry[i].split(":");
                e = tokens[1];
                final int newGroup = Integer.parseInt(tokens[0]);
                Assert.assertTrue("new group must be greater than current group", newGroup >= currentGroup);
                currentGroup = newGroup;
            }
            Assert.assertEquals("Index " + i + " not equal" + msg, e, list.get(i));
            Assert.assertEquals("Group on index " + i + " not equal" + msg, currentGroup, list.groupFor(i));

            if (!expectedGroupStarts.containsKey(currentGroup)) {
                expectedGroupStarts.put(currentGroup, i);
                expectedGroupSizes.put(currentGroup, 0);
            }
            expectedGroupSizes.put(currentGroup, expectedGroupSizes.get(currentGroup) + 1);
        }

        //assert grouplist metadata
        Assert.assertEquals("Group size differs from expected" + msg, expectedGroupSizes.size(), list.groups().size());
        for (int group : expectedGroupStarts.keySet()) {
            Assert.assertEquals("Group " + group + " start pos unexpected" + msg, (int) expectedGroupStarts.get(group), list.groupStart(group));
            Assert.assertEquals("Group " + group + " size unexpected" + msg, (int) expectedGroupSizes.get(group), list.groupSize(group));
        }

    }

}
