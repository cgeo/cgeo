package cgeo.geocaching.filter;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgeoapplication;

import android.test.ApplicationTestCase;

public abstract class AbstractFilterTestCase extends ApplicationTestCase<cgeoapplication> {

    public AbstractFilterTestCase() {
        super(cgeoapplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // init environment
        createApplication();
        cgBase.initialize(getApplication());
    }
}
