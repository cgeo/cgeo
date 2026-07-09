package cgeo.geocaching.list;

import cgeo.geocaching.storage.DataStore;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

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

    private static StoredList getIgnoreList() {
        return DataStore.getList(StoredList.IGNORE_LIST_ID);
    }

    @Test
    public void testIgnoreListExists() {
        final StoredList list = getIgnoreList();
        assertThat(list).isNotNull();
        assertThat(list.id).isEqualTo(StoredList.IGNORE_LIST_ID);
    }

    @Test
    public void testIgnoreListIsConcreteAndProtected() {
        final StoredList list = getIgnoreList();
        assertThat(list.isConcrete()).isTrue();
        assertThat(list.preventAskForDeletion).isTrue();
    }

    @Test
    public void testIgnoreListCannotBeDeleted() {
        // reserved list ids are below customListIdOffset and DataStore.removeList only ever deletes
        // custom (user-created) lists
        assertThat(DataStore.removeList(StoredList.IGNORE_LIST_ID)).isFalse();
        assertThat(getIgnoreList()).isNotNull();
    }
}
