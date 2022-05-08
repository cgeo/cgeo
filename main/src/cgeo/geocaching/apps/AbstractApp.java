package cgeo.geocaching.apps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.ProcessUtils;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractApp implements App {

    @Nullable private final String packageName;
    @Nullable protected final String intent;
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

    protected static String getString(@StringRes final int resourceId) {
        return CgeoApplication.getInstance().getString(resourceId);
    }

    @Override
    public boolean isEnabled(@NonNull final Geocache cache) {
        return true;
    }
}
