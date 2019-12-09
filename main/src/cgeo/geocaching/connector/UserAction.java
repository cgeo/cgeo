package cgeo.geocaching.connector;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.lang.ref.WeakReference;

public class UserAction {

    @StringRes public final int displayResourceId;
    @NonNull private final Action1<UAContext> runnable;

    public static class UAContext {
        @NonNull
        public final String displayName;
        public final String userName;
        public WeakReference<Context> contextRef;

        public UAContext(@NonNull final String displayName, @NonNull final String userName) {
            this.displayName = displayName;
            this.userName = userName;
        }

        public void startActivity(final Intent intent) {
            final Context context = contextRef.get();
            if (context == null) {
                return;
            }
            try {
                context.startActivity(intent);
            } catch (final ActivityNotFoundException e) {
                Log.e("Cannot find suitable activity", e);
            }
        }

        public void setContext(final Context context) {
            contextRef = new WeakReference<>(context);
        }

        public Context getContext() {
            return contextRef.get();
        }
    }

    public UserAction(@StringRes final int displayResourceId, @NonNull final Action1<UAContext> runnable) {
        this.displayResourceId = displayResourceId;
        this.runnable = runnable;
    }

    public void run(@NonNull final UAContext context) {
        runnable.call(context);
    }
}
