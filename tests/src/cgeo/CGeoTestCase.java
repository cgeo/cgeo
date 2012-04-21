package cgeo;

import cgeo.geocaching.cgeoapplication;

import android.test.ApplicationTestCase;

public abstract class CGeoTestCase extends ApplicationTestCase<cgeoapplication> {

    public CGeoTestCase() {
        super(cgeoapplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createApplication();
    }

}
