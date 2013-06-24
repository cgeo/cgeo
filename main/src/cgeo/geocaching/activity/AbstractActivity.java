package cgeo.geocaching.activity;

import butterknife.Views;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.network.Cookies;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.EditText;

public abstract class AbstractActivity extends FragmentActivity implements IAbstractActivity {

    protected cgeoapplication app = null;
    protected Resources res = null;
    private boolean keepScreenOn = false;

    protected AbstractActivity() {
        this(false);
    }

    protected AbstractActivity(final boolean keepScreenOn) {
        this.keepScreenOn = keepScreenOn;
    }

    @Override
    final public void goHome(final View view) {
        ActivityMixin.goHome(this);
    }

    final protected void setTitle(final String title) {
        ActivityMixin.setTitle(this, title);
    }

    final protected void showProgress(final boolean show) {
        ActivityMixin.showProgress(this, show);
    }

    final protected void setTheme() {
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
        ActivityMixin.helpDialog(this, title, message);
    }

    protected final void helpDialog(final String title, final String message, final Drawable icon) {
        ActivityMixin.helpDialog(this, title, message, icon);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeCommonFields();
    }

    protected static void disableSuggestions(final EditText edit) {
        Compatibility.disableSuggestions(edit);
    }

    protected void restartActivity() {
        Compatibility.restartActivity(this);
    }

    @Override
    public void invalidateOptionsMenuCompatible() {
        ActivityMixin.invalidateOptionsMenu(this);
    }

    protected void onCreate(final Bundle savedInstanceState, final int resourceLayoutID) {
        super.onCreate(savedInstanceState);

        initializeCommonFields();

        // non declarative part of layout
        setTheme();
        setContentView(resourceLayoutID);

        // create view variables
        Views.inject(this);
    }

    private void initializeCommonFields() {
        // initialize commonly used members
        res = this.getResources();
        app = (cgeoapplication) this.getApplication();

        // only needed in some activities, but implemented in super class nonetheless
        Cookies.restoreCookieStore(Settings.getCookieStore());
        ActivityMixin.keepScreenOn(this, keepScreenOn);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        // initialize the action bar title with the activity title for single source
        ActivityMixin.setTitle(this, getTitle());
    }
}
