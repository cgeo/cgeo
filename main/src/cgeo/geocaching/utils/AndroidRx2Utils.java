package cgeo.geocaching.utils;

import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.schedulers.RxThreadFactory;
import io.reactivex.schedulers.Schedulers;

public class AndroidRx2Utils {

    public static final Scheduler computationScheduler = Schedulers.computation();

    public static final Scheduler networkScheduler = Schedulers.from(Executors.newFixedThreadPool(10, new RxThreadFactory("network-")));

    public static final Scheduler refreshScheduler = Schedulers.from(Executors.newFixedThreadPool(3, new RxThreadFactory("refresh-")));

    private static final HandlerThread looperCallbacksThread =
            new HandlerThread("looper callbacks", Process.THREAD_PRIORITY_DEFAULT);

    static {
        looperCallbacksThread.start();
    }

    public static final Looper looperCallbacksLooper = looperCallbacksThread.getLooper();
    public static final Scheduler looperCallbacksScheduler = AndroidSchedulers.from(looperCallbacksLooper);
    public static final Scheduler.Worker looperCallbacksWorker = looperCallbacksScheduler.createWorker();

    private AndroidRx2Utils() {
        // Utility class, not to be instantiated
    }

    public static <T> void andThenOnUi(final Scheduler scheduler, final Callable<T> background, final Consumer<T> foreground) {
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    final T value = background.call();
                    AndroidSchedulers.mainThread().createWorker().schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                foreground.accept(value);
                            } catch (final Exception e) {
                                Log.e("error when running the second part of andThenOnUi");
                            }
                        }
                    });
                } catch (final Exception e) {
                    Log.e("error when running the first part of andThenOnUi", e);
                }
            }
        });
    }

    public static void andThenOnUi(final Scheduler scheduler, final Runnable background, final Runnable foreground) {
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                background.run();
                AndroidSchedulers.mainThread().createWorker().schedule(foreground);
            }
        });
    }
}
