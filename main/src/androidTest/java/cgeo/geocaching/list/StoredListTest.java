package cgeo.geocaching.list;

import cgeo.geocaching.storage.DataStore;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class StoredListTest {

    @Test
    public void testStandardListExists() {
        final StoredList list = getStandardList();
        assertThat(list).isNotNull();
    }

    private static StoredList getStandardList() {
        return DataStore.getList(StoredList.STANDARD_LIST_ID);
    }

    @Test
    public void testEquals() {
        final StoredList list1 = getStandardList();
        final StoredList list2 = getStandardList();
        assertThat(list2).isEqualTo(list1);
    }

    @Test
    public void testConcrete() {
        assertThat(getStandardList().isConcrete()).isTrue();
    }

    @Test
    public void testTitleAndCountContainsTitle() {
        assertThat(getStandardList().getTitleAndCount()).startsWith(getStandardList().getTitle());
    }
}
