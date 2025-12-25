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

package cgeo.geocaching.list

import cgeo.geocaching.storage.DataStore

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class StoredListTest {

    @Test
    public Unit testStandardListExists() {
        val list: StoredList = getStandardList()
        assertThat(list).isNotNull()
    }

    private static StoredList getStandardList() {
        return DataStore.getList(StoredList.STANDARD_LIST_ID)
    }

    @Test
    public Unit testEquals() {
        val list1: StoredList = getStandardList()
        val list2: StoredList = getStandardList()
        assertThat(list2).isEqualTo(list1)
    }

    @Test
    public Unit testConcrete() {
        assertThat(getStandardList().isConcrete()).isTrue()
    }

    @Test
    public Unit testTitleAndCountContainsTitle() {
        assertThat(getStandardList().getTitleAndCount()).startsWith(getStandardList().getTitle())
    }
}
