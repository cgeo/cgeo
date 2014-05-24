package cgeo.geocaching.activity;

import android.os.Bundle;

/**
 * Classes actually having an ActionBar (as opposed to the Dialog activities)
 */
public class AbstractActionBarActivity extends AbstractActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState, int resourceLayoutID) {
        super.onCreate(savedInstanceState, resourceLayoutID);
        initUpAction();
        showProgress(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUpAction();
        showProgress(false);
    }


    private void initUpAction() {
       getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        // reflect the title in the actionbar
        ActivityMixin.setTitle(this, title);
    }
}
