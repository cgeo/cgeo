package cgeo.geocaching.activity;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgeoapplication;

public abstract class AbstractListActivity extends ListActivity implements
		IAbstractActivity {

	private String helpTopic;

	protected cgeoapplication app = null;
	protected Resources res = null;
	protected cgSettings settings = null;
	protected cgBase base = null;
	protected SharedPreferences prefs = null;

	public AbstractListActivity() {
		this(null);
	}

	public AbstractListActivity(final String helpTopic) {
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

	public final void helpDialog(String title, String message) {
		ActivityMixin.helpDialog(this, title, message);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		res = this.getResources();
		app = (cgeoapplication) this.getApplication();
		prefs = getSharedPreferences(cgSettings.preferences, 0);
		settings = new cgSettings(this, prefs);
		base = new cgBase(app, settings, prefs);
	}

	final public void setTitle(final String title) {
		ActivityMixin.setTitle(this, title);
	}
	
	final public cgSettings getSettings() {
		return settings;
	}
}
