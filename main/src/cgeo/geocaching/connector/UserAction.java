package cgeo.geocaching.connector;

import org.eclipse.jdt.annotation.NonNull;
import rx.util.functions.Action1;

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
    private final @NonNull
    Action1<Context> runnable;

    public UserAction(int displayResourceId, final @NonNull Action1<Context> runnable) {
        this.displayResourceId = displayResourceId;
        this.runnable = runnable;
    }

    public void run(Context context) {
        runnable.call(context);
    }
}
