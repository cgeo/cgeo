package cgeo.geocaching.connector;

import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

public class UserAction {

    @StringRes public final int displayResourceId;
    @NonNull private final Action1<Context> runnable;

    public static class Context {
        @NonNull
        public final String userName;
        @NonNull
        public final Activity activity;

        public Context(@NonNull final String userName, @NonNull final Activity activity) {
            this.userName = userName;
            this.activity = activity;
        }
    }

    public UserAction(@StringRes final int displayResourceId, @NonNull final Action1<Context> runnable) {
        this.displayResourceId = displayResourceId;
        this.runnable = runnable;
    }

    public void run(@NonNull final Context context) {
        runnable.call(context);
    }
}
