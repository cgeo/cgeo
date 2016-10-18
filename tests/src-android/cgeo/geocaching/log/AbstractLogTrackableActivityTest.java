package cgeo.geocaching.log;

import cgeo.geocaching.activity.AbstractEspressoTest;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.storage.DataStore;

public abstract class AbstractLogTrackableActivityTest extends AbstractEspressoTest<LogTrackableActivity> {

    private static Trackable trackable;

    public AbstractLogTrackableActivityTest() {
        super(LogTrackableActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTrackable();
        setActivityIntent(LogTrackableActivity.getIntent(getInstrumentation().getContext(), trackable));
        getActivity();
    }

    protected static void createTrackable() {
        trackable = new Trackable();
        trackable.setName("Test trackable");
        trackable.setGeocode("TB4D09K");
        DataStore.saveTrackable(trackable);
    }

}
