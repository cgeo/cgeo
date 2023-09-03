package cgeo.geocaching.activity;

/**
 * Similar to AbstractNavigationBarActivity, but
 * - placed in custom menu slot of bottom navigation
 * - and defaults to "bottom navigation not shown"
 */

public class CustomMenuEntryActivity extends AbstractNavigationBarActivity {

    @Override
    public int getSelectedBottomItemId() {
        return MENU_CUSTOM;
    }

    @Override
    protected void checkIntentHideNavigationBar() {
        checkIntentHideNavigationBar(true);
    }

}
