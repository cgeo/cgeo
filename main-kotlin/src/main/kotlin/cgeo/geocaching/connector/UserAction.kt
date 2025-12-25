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

package cgeo.geocaching.connector

import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.functions.Action1

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import java.lang.ref.WeakReference

class UserAction {

    @StringRes public final Int displayResourceId
    @DrawableRes public final Int iconId
    private final Action1<UAContext> runnable

    public static class UAContext {
        public final String displayName
        public final String userName
        public final String userGUID
        public final String geocode
        public WeakReference<Context> contextRef

        public UAContext(final String displayName, final String userName, final String userGUID, final String geocode) {
            this.displayName = displayName
            this.userName = userName
            this.userGUID = userGUID
            this.geocode = geocode
        }

        public Unit startActivity(final Intent intent) {
            val context: Context = contextRef.get()
            if (context == null) {
                return
            }
            try {
                context.startActivity(intent)
            } catch (final ActivityNotFoundException e) {
                Log.e("Cannot find suitable activity", e)
            }
        }

        public Unit setContext(final Context context) {
            contextRef = WeakReference<>(context)
        }

        public Context getContext() {
            return contextRef.get()
        }
    }

    public UserAction(@StringRes final Int displayResourceId, final Action1<UAContext> runnable) {
        this(displayResourceId, 0, runnable)
    }

    public UserAction(@StringRes final Int displayResourceId, @DrawableRes final Int iconId, final Action1<UAContext> runnable) {
        this.displayResourceId = displayResourceId
        this.iconId = iconId
        this.runnable = runnable
    }

    public Unit run(final UAContext context) {
        runnable.call(context)
    }
}
