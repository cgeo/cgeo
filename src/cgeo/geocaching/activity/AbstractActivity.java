package cgeo.geocaching.activity;

import android.app.Activity;
import android.view.View;

public abstract class AbstractActivity extends Activity implements IAbstractActivity {

	private String helpTopic;

	public AbstractActivity() {
		this(null);
	}

	public AbstractActivity(final String helpTopic) {
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
}
