package cgeo.geocaching.activity;

import android.os.Bundle;
import android.view.Window;

/**
 * Classes actually having an ActionBar (as opposed to the Dialog activities)
 */
public class AbstractActionBarActivity extends AbstractActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState, int resourceLayoutID) {
        initFeatures();
        super.onCreate(savedInstanceState, resourceLayoutID);
        initUpAction();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFeatures();
        initUpAction();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState, int resourceLayoutID, boolean useDialogTheme) {
        initFeatures();
        super.onCreate(savedInstanceState, resourceLayoutID, useDialogTheme);
        initUpAction();
    }

    private void initFeatures() {
        // TODO: This still broken on Android 2.3, no idea why
        try {
            supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    // setTitle is gracefully handled by ActionBarActivity

    private void initUpAction()
    {
       getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
