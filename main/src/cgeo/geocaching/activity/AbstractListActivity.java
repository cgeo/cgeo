package cgeo.geocaching.activity;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.MainActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentListActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

public abstract class AbstractListActivity extends ActionBarListActivity implements
        IAbstractActivity {

    private boolean keepScreenOn = false;

    protected CgeoApplication app = null;
    protected Resources res = null;

    protected AbstractListActivity() {
        this(false);
    }

    protected AbstractListActivity(final boolean keepScreenOn) {
        this.keepScreenOn = keepScreenOn;
    }

    @Override
    final public void goHome(View view) {
        ActivityMixin.navigateToMain(this);
    }

    final public void showProgress(final boolean show) {
        ActivityMixin.showProgress(this, show);
    }

    final public void setTheme() {
        ActivityMixin.setTheme(this);
    }

    @Override
    public final void showToast(String text) {
        ActivityMixin.showToast(this, text);
    }
    
    @Override
    public final void showShortToast(String text) {
        ActivityMixin.showShortToast(this, text);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        initializeCommonFields();
        initUpAction();
    }

    protected void initUpAction()
    {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()== android.R.id.home) {
            ActivityMixin.navigateToMain(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeCommonFields() {
        // init
        res = this.getResources();
        app = (CgeoApplication) this.getApplication();

        ActivityMixin.keepScreenOn(this, keepScreenOn);
    }

    final protected void setTitle(final String title) {
        ActivityMixin.setTitle(this, title);
    }

    @Override
    public void invalidateOptionsMenuCompatible() {
        ActivityMixin.invalidateOptionsMenu(this);
    }

    public void onCreate(Bundle savedInstanceState, int resourceLayoutID) {
        super.onCreate(savedInstanceState);
        initializeCommonFields();

        setTheme();
        setContentView(resourceLayoutID);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        // initialize action bar title with activity title
        ActivityMixin.setTitle(this, getTitle());
    }
}
