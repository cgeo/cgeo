package cgeo.geocaching.activity;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.compatibility.Compatibility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public abstract class AbstractActivity extends Activity implements IAbstractActivity {

    private String helpTopic;

    protected cgeoapplication app = null;
    protected Resources res = null;
    protected SharedPreferences prefs = null;

    protected AbstractActivity() {
        this(null);
    }

    protected AbstractActivity(final String helpTopic) {
        this.helpTopic = helpTopic;
    }

    final public void goHome(final View view) {
        ActivityMixin.goHome(this);
    }

    public void goManual(final View view) {
        ActivityMixin.goManual(this, helpTopic);
    }

    final public void setTitle(final String title) {
        ActivityMixin.setTitle(this, title);
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
        ActivityMixin.helpDialog(this, title, message);
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

        cgBase.initialize(app);

        // Restore cookie store if needed
        cgBase.restoreCookieStore(Settings.getCookieStore());
    }

    public void addVisitMenu(Menu menu, cgCache cache) {
        ActivityMixin.addVisitMenu(this, menu, cache);
    }

    protected static void disableSuggestions(final EditText edit) {
        Compatibility.disableSuggestions(edit);
    }
}
