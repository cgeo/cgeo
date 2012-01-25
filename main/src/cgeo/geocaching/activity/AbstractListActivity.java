package cgeo.geocaching.activity;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;

import android.app.ListActivity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public abstract class AbstractListActivity extends ListActivity implements
        IAbstractActivity {

    private String helpTopic;
    private boolean keepScreenOn = false;

    protected cgeoapplication app = null;
    protected Resources res = null;

    protected AbstractListActivity() {
        this(null);
    }

    protected AbstractListActivity(final boolean keepScreenOn) {
        this(null);
        this.keepScreenOn = keepScreenOn;
    }

    protected AbstractListActivity(final String helpTopic) {
        this.helpTopic = helpTopic;
    }

    final public void goHome(View view) {
        ActivityMixin.goHome(this);
    }

    public void goManual(View view) {
        ActivityMixin.goManual(this, helpTopic);
    }

    final public void showProgress(final boolean show) {
        ActivityMixin.showProgress(this, show);
    }

    final public void setTheme() {
        ActivityMixin.setTheme(this);
    }

    public final void showToast(String text) {
        ActivityMixin.showToast(this, text);
    }

    public final void showShortToast(String text) {
        ActivityMixin.showShortToast(this, text);
    }

    public final void helpDialog(final String title, final String message) {
        ActivityMixin.helpDialog(this, title, message, null);
    }

    public final void helpDialog(final String title, final String message, final Drawable icon) {
        ActivityMixin.helpDialog(this, title, message, icon);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init
        res = this.getResources();
        app = (cgeoapplication) this.getApplication();
        cgBase.initialize(app);

        ActivityMixin.keepScreenOn(this, keepScreenOn);
    }

    final public void setTitle(final String title) {
        ActivityMixin.setTitle(this, title);
    }

    public void addVisitMenu(Menu menu, cgCache cache) {
        ActivityMixin.addVisitMenu(this, menu, cache);
    }

}
