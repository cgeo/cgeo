package cgeo.geocaching.test;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.storage.DataStore;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import static org.assertj.core.api.Java6Assertions.assertThat;

/** Sets up a (temporary) cache list in Cgeo for testing purposes */
public class CgeoTemporaryListRule implements TestRule {

    private int temporaryListId = Integer.MIN_VALUE;

    public int getListId() {
        return temporaryListId;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                try {
                    createList();
                    base.evaluate();
                } finally {
                    deleteList();
                }
            }
        };
    }

    private void createList() {
        temporaryListId = DataStore.createList("Temporary unit testing");
        assertThat(temporaryListId).isNotEqualTo(StoredList.TEMPORARY_LIST.id);
        assertThat(temporaryListId).isNotEqualTo(StoredList.STANDARD_LIST_ID);
    }

    private void deleteList() {
        if (temporaryListId == Integer.MIN_VALUE) {
            return;
        }
        final SearchResult search = DataStore.getBatchOfStoredCaches(null, temporaryListId);
        assertThat(search).isNotNull();
        DataStore.removeCaches(search.getGeocodes(), LoadFlags.REMOVE_ALL);
        DataStore.removeList(temporaryListId);
    }


}
