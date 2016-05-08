package cgeo.geocaching.utils;

import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.internal.util.RxThreadFactory;
import rx.schedulers.Schedulers;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import java.util.concurrent.Executors;

public class AndroidRxUtils {

    private AndroidRxUtils() {
        // Utility class, not to be instantiated
    }

    public static final Scheduler computationScheduler = Schedulers.computation();

    public static final Scheduler networkScheduler = Schedulers.from(Executors.newFixedThreadPool(10, new RxThreadFactory("network-")));

    public static final Scheduler refreshScheduler = Schedulers.from(Executors.newFixedThreadPool(3, new RxThreadFactory("refresh-")));

    private static final HandlerThread looperCallbacksThread =
            new HandlerThread("looper callbacks", Process.THREAD_PRIORITY_DEFAULT);

    static {
        looperCallbacksThread.start();
    }

    public static final Looper looperCallbacksLooper = looperCallbacksThread.getLooper();
    public static final Scheduler looperCallbacksScheduler = AndroidSchedulers.handlerThread(new Handler(looperCallbacksLooper));
    public static final Worker looperCallbacksWorker = looperCallbacksScheduler.createWorker();

    public static <T> void andThenOnUi(final Scheduler scheduler, final Func0<T> background, final Action1<T> foreground) {
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                final T value = background.call();
                AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        foreground.call(value);
                    }
                });
            }
        });
    }

    public static void andThenOnUi(final Scheduler scheduler, final Action0 background, final Action0 foreground) {
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                background.call();
                AndroidSchedulers.mainThread().createWorker().schedule(foreground);
            }
        });
    }

}
