// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.apps

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.ProcessUtils

import android.content.Intent

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import org.apache.commons.lang3.StringUtils

abstract class AbstractApp : App {

    private final String packageName
    protected final String intent
    private final String name

    protected AbstractApp(final String name, final String intent,
                          final String packageName) {
        this.name = name
        this.intent = intent
        this.packageName = packageName
    }

    protected AbstractApp(final String name, final String intent) {
        this(name, intent, null)
    }

    override     public Boolean isInstalled() {
        if (StringUtils.isNotEmpty(packageName) && ProcessUtils.isLaunchable(packageName)) {
            return true
        }
        if (intent == null) {
            return false
        }
        return ProcessUtils.isIntentAvailable(intent)
    }

    protected Intent getLaunchIntent() {
        return ProcessUtils.getLaunchIntent(packageName)
    }

    override     public Boolean isUsableAsDefaultNavigationApp() {
        return true
    }

    override     public String getName() {
        return name
    }

    protected static String getString(@StringRes final Int resourceId) {
        return CgeoApplication.getInstance().getString(resourceId)
    }

    override     public Boolean isEnabled(final Geocache cache) {
        return true
    }
}
