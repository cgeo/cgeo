package cgeo.geocaching.test;

import android.test.InstrumentationTestCase;

import static org.assertj.core.api.Java6Assertions.assertThat;

public abstract class AbstractResourceInstrumentationTestCase extends InstrumentationTestCase {
    //private int temporaryListId;

    //    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        temporaryListId = DataStore.createList("Temporary unit testing");
//        assertThat(temporaryListId).isNotEqualTo(StoredList.TEMPORARY_LIST.id);
//        assertThat(temporaryListId).isNotEqualTo(StoredList.STANDARD_LIST_ID);
//    }
//
//    @Override
//    protected void tearDown() throws Exception {
//        final SearchResult search = DataStore.getBatchOfStoredCaches(null, temporaryListId);
//        assertThat(search).isNotNull();
//        DataStore.removeCaches(search.getGeocodes(), LoadFlags.REMOVE_ALL);
//        DataStore.removeList(temporaryListId);
//        super.tearDown();
//    }
//
//    protected final int getTemporaryListId() {
//        return temporaryListId;
//    }

}
