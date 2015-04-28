package cgeo.geocaching.list;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.DataStore;

import junit.framework.TestCase;

public class StoredListTest extends TestCase {

    public static void testStandardListExists() {
        final StoredList list = getStandardList();
        assertThat(list).isNotNull();
    }

    private static StoredList getStandardList() {
        return DataStore.getList(StoredList.STANDARD_LIST_ID);
    }

    public static void testEquals() {
        final StoredList list1 = getStandardList();
        final StoredList list2 = getStandardList();
        assertThat(list2).isEqualTo(list1);
    }

    public static void testConcrete() {
        assertThat(getStandardList().isConcrete()).isTrue();
    }

    public static void testTitleAndCountContainsTitle() {
        assertThat(getStandardList().getTitleAndCount()).startsWith(getStandardList().getTitle());
    }
}
