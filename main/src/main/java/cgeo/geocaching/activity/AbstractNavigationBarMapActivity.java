package cgeo.geocaching.activity;

import cgeo.geocaching.maps.MapUtils;

public abstract class AbstractNavigationBarMapActivity extends AbstractNavigationBarActivity {

    @Override
    public void onBackPressed() {
        // try to remove map details fragment first
        if (MapUtils.sheetRemoveFragment(this)) {
            return;
        }
        super.onBackPressed();
    }

    public abstract void clearSheetInfo();
}
