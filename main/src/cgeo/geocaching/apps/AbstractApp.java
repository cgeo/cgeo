package cgeo.geocaching.apps;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.utils.ProcessUtils;

import org.apache.commons.lang3.StringUtils;

import android.content.Intent;

public abstract class AbstractApp implements App {

    private final String packageName;
    private final String intent;
    private final String name;

    protected AbstractApp(final String name, final String intent,
            final String packageName) {
        this.name = name;
        this.intent = intent;
        this.packageName = packageName;
    }

    protected AbstractApp(final String name, final String intent) {
        this(name, intent, null);
    }

    @Override
    public boolean isInstalled() {
        if (StringUtils.isNotEmpty(packageName) && ProcessUtils.isLaunchable(packageName)) {
            return true;
        }
        return ProcessUtils.isIntentAvailable(intent);
    }

    protected Intent getLaunchIntent() {
        return ProcessUtils.getLaunchIntent(packageName);
    }

    @Override
    public boolean isDefaultNavigationApp() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getId() {
        return getName().hashCode();
    }

    protected static String getString(int ressourceId) {
        return cgeoapplication.getInstance().getString(ressourceId);
    }

    @Override
    public boolean isEnabled(Geocache cache) {
        return cache != null;
    }
}
