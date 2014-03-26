package cgeo.geocaching.utils;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import org.eclipse.jdt.annotation.NonNull;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Derivated class of {@link android.os.HandlerThread} with an exposed handler and a start/stop mechanism
 * based on subscriptions.
 */

public class StartableHandlerThread extends HandlerThread {

    private final static int START = 1;
    private final static int STOP = 2;

    static public interface Callback {
        public void start(final Context context, final Handler handler);
        public void stop();
    }

    private class StartableHandler extends Handler {
        public StartableHandler() {
            super(StartableHandlerThread.this.getLooper());
        }

        @Override
        public void handleMessage(final Message message) {
            if (callback != null) {
                switch (message.what) {
                    case START:
                        callback.start((Context) message.obj, this);
                        break;
                    case STOP:
                        callback.stop();
                        break;
                }
            }
        }
    }

    private Handler handler;
    private Callback callback;

    public StartableHandlerThread(@NonNull final String name, final int priority, final Callback callback) {
        super(name, priority);
        this.callback = callback;
    }

    public StartableHandlerThread(@NonNull final String name, final int priority) {
        this(name, priority, null);
    }

    public Handler getHandler() {
        if (handler == null) {
            synchronized(this) {
                if (handler == null) {
                    handler = new StartableHandler();
                }
            }
        }
        return handler;
    }

    public void start(final Subscriber<?> subscriber, final Context context) {
        getHandler().obtainMessage(START, context).sendToTarget();
        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                getHandler().sendEmptyMessage(STOP);
            }
        }));
    }

}
