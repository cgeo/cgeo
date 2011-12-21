package cgeo.geocaching.activity;

import cgeo.geocaching.cgCache;

import android.view.Menu;
import android.view.View;

public interface IAbstractActivity {
    static final int MENU_LOG_VISIT = 100;
    static final int MENU_LOG_VISIT_OFFLINE = 101;

    public void goHome(View view);

    public void goManual(View view);

    public void showProgress(final boolean show);

    public void setTheme();

    public void showToast(String text);

    public void showShortToast(String text);

    public void helpDialog(String title, String message);

    public void setTitle(final String title);

    void addVisitMenu(Menu menu, cgCache cache);

}
