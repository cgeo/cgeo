package cgeo.geocaching.utils;

import android.app.Activity;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.schedulers.RxThreadFactory;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AndroidRxUtils {

    public static final Scheduler computationScheduler = Schedulers.computation();

    private static final ThreadPoolExecutor.DiscardPolicy DISCARD_POLICY = new ThreadPoolExecutor.DiscardPolicy();

    public static final Scheduler networkScheduler = Schedulers.from(newFixedDiscardingThreadPool(10, "network-"));

    public static final Scheduler refreshScheduler = Schedulers.from(newFixedDiscardingThreadPool(3, "refresh-"));

    private static final HandlerThread looperCallbacksThread =
            new HandlerThread("looper callbacks", Process.THREAD_PRIORITY_DEFAULT);

    static {
        looperCallbacksThread.start();
    }

    public static final Looper looperCallbacksLooper = looperCallbacksThread.getLooper();
    public static final Scheduler looperCallbacksScheduler = AndroidSchedulers.from(looperCallbacksLooper);

    private AndroidRxUtils() {
        // Utility class, not to be instantiated
    }

    public static <T> void andThenOnUi(final Scheduler scheduler, final Callable<T> background, final Consumer<T> foreground) {
        scheduler.createWorker().schedule(() -> {
            try {
                final T value = background.call();
                AndroidSchedulers.mainThread().createWorker().schedule(() -> {
                    try {
                        foreground.accept(value);
                    } catch (final Throwable t) {
                        Log.e("error when running the second part of andThenOnUi");
                    }
                });
            } catch (final Exception e) {
                Log.e("error when running the first part of andThenOnUi", e);
            }
        });
    }

    public static void andThenOnUi(final Scheduler scheduler, final Runnable background, final Runnable foreground) {
        scheduler.createWorker().schedule(() -> {
            background.run();
            AndroidSchedulers.mainThread().createWorker().schedule(foreground);
        });
    }

    public static <T> Observable<T> bindActivity(final Activity activity, final Observable<T> source) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        return source.observeOn(AndroidSchedulers.mainThread()).takeWhile(t -> {
            final Activity a = activityRef.get();
            return a != null && !a.isFinishing();
        });
    }

    public static <T> Maybe<T> bindActivity(final Activity activity, final Single<T> source) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        return source.observeOn(AndroidSchedulers.mainThread()).filter(t -> {
            final Activity a = activityRef.get();
            return a != null && !a.isFinishing();
        });
    }

    public static <T> Maybe<T> bindActivity(final Activity activity, final Maybe<T> source) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        return source.observeOn(AndroidSchedulers.mainThread()).filter(t -> {
            final Activity a = activityRef.get();
            return a != null && !a.isFinishing();
        });
    }

    public static <T> Observable<T> bindFragment(final Fragment fragment, final Observable<T> source) {
        final WeakReference<Fragment> fragmentRef = new WeakReference<>(fragment);
        return source.observeOn(AndroidSchedulers.mainThread()).filter(t -> {
            final Fragment f = fragmentRef.get();
            return f != null && f.isAdded() && !f.getActivity().isFinishing();
        });
    }

    public static Disposable disposeOnCallbacksScheduler(final Runnable runnable) {
        return Disposable.fromRunnable(() -> looperCallbacksScheduler.scheduleDirect(runnable));
    }

    /**
     * Provide an executor service with a fixed number of threads and an unbounded queue. If the
     * service is shutdown while tasks are still queued, jobs will be silently discarded.
     */
    private static ExecutorService newFixedDiscardingThreadPool(final int nThreads, final String prefix) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new RxThreadFactory(prefix),
                DISCARD_POLICY);
    }

}
