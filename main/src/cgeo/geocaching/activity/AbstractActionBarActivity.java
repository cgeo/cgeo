package cgeo.geocaching.activity;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;

/**
 * Classes actually having an ActionBar (as opposed to the Dialog activities)
 */
public class AbstractActionBarActivity extends AbstractActivity {

    public AbstractActionBarActivity() {
        super(false);
    }
    public AbstractActionBarActivity(final boolean keepScreenOn) {
        super(keepScreenOn);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState, @LayoutRes final int resourceLayoutID) {
        super.onCreate(savedInstanceState, resourceLayoutID);
        initUpAction();
        showProgress(false);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUpAction();
        showProgress(false);
    }


    private void initUpAction() {
       final ActionBar actionBar = getSupportActionBar();
       if (actionBar != null) {
           actionBar.setDisplayHomeAsUpEnabled(true);
       }
    }

    @Override
    public void setTitle(final CharSequence title) {
        super.setTitle(title);
        // reflect the title in the actionbar
        ActivityMixin.setTitle(this, title);
    }

    /**
     * @param view
     *            view to activate the context ActionBar for
     */
    public void addContextMenu(final View view) {
        // placeholder for derived implementations
    }
}
