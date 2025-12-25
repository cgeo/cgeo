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

import cgeo.geocaching.SearchResult
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.storage.DataStore

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.assertj.core.api.Java6Assertions.assertThat

/** Sets up a (temporary) cache list in Cgeo for testing purposes */
class CgeoTemporaryListRule : TestRule {

    private var temporaryListId: Int = Integer.MIN_VALUE

    public Int getListId() {
        return temporaryListId
    }

    override     public Statement apply(final Statement base, final Description description) {
        return Statement() {
            override             public Unit evaluate() throws Throwable {

                try {
                    createList()
                    base.evaluate()
                } finally {
                    deleteList()
                }
            }
        }
    }

    private Unit createList() {
        temporaryListId = DataStore.createList("Temporary unit testing")
        assertThat(temporaryListId).isNotEqualTo(StoredList.TEMPORARY_LIST.id)
        assertThat(temporaryListId).isNotEqualTo(StoredList.STANDARD_LIST_ID)
    }

    private Unit deleteList() {
        if (temporaryListId == Integer.MIN_VALUE) {
            return
        }
        val search: SearchResult = DataStore.getBatchOfStoredCaches(null, temporaryListId)
        assertThat(search).isNotNull()
        DataStore.removeCaches(search.getGeocodes(), LoadFlags.REMOVE_ALL)
        DataStore.removeList(temporaryListId)
    }


}
