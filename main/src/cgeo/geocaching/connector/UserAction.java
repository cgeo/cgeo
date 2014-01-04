package cgeo.geocaching.connector;

import cgeo.geocaching.utils.RunnableWithArgument;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

public class UserAction {

    public static class Context {
        public final String userName;
        public final Activity activity;

        public Context(String userName, Activity activity) {
            this.userName = userName;
            this.activity = activity;
        }
    }

    public final int displayResourceId;
    private final @NonNull RunnableWithArgument<Context> runnable;

    public UserAction(int displayResourceId, final @NonNull RunnableWithArgument<UserAction.Context> runnable) {
        this.displayResourceId = displayResourceId;
        this.runnable = runnable;
    }

    public void run(Context context) {
        runnable.run(context);
    }
}
