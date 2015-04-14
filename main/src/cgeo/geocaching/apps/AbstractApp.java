package cgeo.geocaching.apps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.utils.ProcessUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.content.Intent;

public abstract class AbstractApp implements App {

    @Nullable private final String packageName;
    @Nullable private final String intent;
    @NonNull
    private final String name;

    protected AbstractApp(@NonNull final String name, @Nullable final String intent,
            @Nullable final String packageName) {
        this.name = name;
        this.intent = intent;
        this.packageName = packageName;
    }

    protected AbstractApp(@NonNull final String name, @Nullable final String intent) {
        this(name, intent, null);
    }

    @Override
    public boolean isInstalled() {
        if (StringUtils.isNotEmpty(packageName) && ProcessUtils.isLaunchable(packageName)) {
            return true;
        }
        if (intent == null) {
            return false;
        }
        assert intent != null; // eclipse issue
        return ProcessUtils.isIntentAvailable(intent);
    }

    @Nullable
    protected Intent getLaunchIntent() {
        return ProcessUtils.getLaunchIntent(packageName);
    }

    @Override
    public boolean isUsableAsDefaultNavigationApp() {
        return true;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    protected static String getString(final int ressourceId) {
        return CgeoApplication.getInstance().getString(ressourceId);
    }

    @Override
    public boolean isEnabled(final @NonNull Geocache cache) {
        return true;
    }
}
