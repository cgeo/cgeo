package cgeo.geocaching.activity;

import android.view.View;

public interface IAbstractActivity {
	public void goHome(View view);

	public void goManual(View view);

	public void showProgress(final boolean show);

	public void setTheme();

	public void showToast(String text);

	public void showShortToast(String text);

	public void helpDialog(String title, String message);
}
