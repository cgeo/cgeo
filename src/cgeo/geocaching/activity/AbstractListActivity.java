package cgeo.geocaching.activity;

import android.app.ListActivity;
import android.view.View;

public abstract class AbstractListActivity extends ListActivity implements IAbstractActivity {

	private String helpTopic;

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
}
