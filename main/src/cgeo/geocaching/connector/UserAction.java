package cgeo.geocaching.connector;

import org.eclipse.jdt.annotation.NonNull;

import rx.functions.Action1;

import android.app.Activity;

public class UserAction {

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

    public final int displayResourceId;
    @NonNull private final Action1<Context> runnable;

    public UserAction(final int displayResourceId, final @NonNull Action1<Context> runnable) {
        this.displayResourceId = displayResourceId;
        this.runnable = runnable;
    }

    public void run(@NonNull final Context context) {
        runnable.call(context);
    }
}
