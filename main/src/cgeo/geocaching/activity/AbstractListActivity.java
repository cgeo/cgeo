package cgeo.geocaching.activity;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public abstract class AbstractListActivity extends ListActivity implements
        IAbstractActivity {

    private String helpTopic;

    protected cgeoapplication app = null;
    protected Resources res = null;
    protected cgBase base = null;
    protected SharedPreferences prefs = null;

    protected AbstractListActivity() {
        this(null);
    }

    protected AbstractListActivity(final String helpTopic) {
        this.helpTopic = helpTopic;
    }

    @Override
    final public void goHome(View view) {
        ActivityMixin.goHome(this);
    }

    @Override
    public void goManual(View view) {
        ActivityMixin.goManual(this, helpTopic);
    }

    @Override
    final public void showProgress(final boolean show) {
        ActivityMixin.showProgress(this, show);
    }

    @Override
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

        // init
        res = this.getResources();
        app = (cgeoapplication) this.getApplication();
        prefs = getSharedPreferences(Settings.preferences, Context.MODE_PRIVATE);
        base = new cgBase(app);
    }

    @Override
    final public void setTitle(final String title) {
        ActivityMixin.setTitle(this, title);
    }

    @Override
    public void addVisitMenu(Menu menu, cgCache cache) {
        ActivityMixin.addVisitMenu(this, menu, cache);
    }

}
