package cgeo.geocaching.apps;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeo;
import cgeo.geocaching.cgeoapplication;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

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

    protected Intent getLaunchIntent(Context context) {
        if (packageName == null) {
            return null;
        }
        final PackageManager packageManager = context.getPackageManager();
        try {
            // This can throw an exception where the exception type is only defined on API Level > 3
            // therefore surround with try-catch
            return packageManager.getLaunchIntentForPackage(packageName);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isInstalled(final Context context) {
        if (getLaunchIntent(context) != null) {
            return true;
        }
        return cgeo.isIntentAvailable(context, intent);
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
    public boolean isEnabled(cgCache cache) {
        return true;
    }
}
