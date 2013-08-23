package cgeo.geocaching.apps.cache;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.ProcessUtils;

import android.content.Intent;

public class GccApp extends AbstractGeneralApp {
    private static final String PACKAGE = "eisbehr.gcc";
    private static final String PACKAGE_PRO = "eisbehr.gcc.pro";

    public GccApp() {
        super(getString(R.string.cache_menu_gcc), null);
    }

    @Override
    public boolean isInstalled() {
        return ProcessUtils.isLaunchable(PACKAGE) || ProcessUtils.isLaunchable(PACKAGE_PRO);
    }

    @Override
    protected Intent getLaunchIntent() {
        if (ProcessUtils.isLaunchable(PACKAGE_PRO)) {
            return ProcessUtils.getLaunchIntent(PACKAGE_PRO);
        }
        return ProcessUtils.getLaunchIntent(PACKAGE);
    }
}
