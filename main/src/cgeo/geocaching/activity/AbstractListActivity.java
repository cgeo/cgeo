package cgeo.geocaching.activity;

import cgeo.geocaching.cgeoapplication;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentListActivity;
import android.view.View;

public abstract class AbstractListActivity extends FragmentListActivity implements
        IAbstractActivity {

    private boolean keepScreenOn = false;

    protected cgeoapplication app = null;
    protected Resources res = null;

    protected AbstractListActivity() {
        this(false);
    }

    protected AbstractListActivity(final boolean keepScreenOn) {
        this.keepScreenOn = keepScreenOn;
    }

    @Override
    final public void goHome(View view) {
        ActivityMixin.goHome(this);
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
    public final void helpDialog(final String title, final String message) {
        ActivityMixin.helpDialog(this, title, message, null);
    }

    public final void helpDialog(final String title, final String message, final Drawable icon) {
        ActivityMixin.helpDialog(this, title, message, icon);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeCommonFields();
    }

    private void initializeCommonFields() {
        // init
        res = this.getResources();
        app = (cgeoapplication) this.getApplication();

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
