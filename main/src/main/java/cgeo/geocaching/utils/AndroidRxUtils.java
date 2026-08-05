package cgeo.geocaching.utils;

import android.app.Activity;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
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
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.schedulers.RxThreadFactory;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AndroidRxUtils {

    /**
     *  This Scheduler is intended for computational work.
     * <br>
     *  This can be used for event-loops, processing callbacks and other computational work.
     *  It is not recommended to perform blocking, IO-bound work on this scheduler. Use {@link Schedulers#io()} instead.
     * <br>
     *  The number of threads equals the number of available processors on the device.
     */
    public static final Scheduler computationScheduler = Schedulers.computation();

    private static final ThreadPoolExecutor.DiscardPolicy DISCARD_POLICY = new ThreadPoolExecutor.DiscardPolicy();

    /**
     *  This Scheduler is intended for network requests. Don't use it for offline computation.
     */
    public static final Scheduler networkScheduler = Schedulers.from(newFixedDiscardingThreadPool(10, "network-"));

    /**
     * This Scheduler is intended for queuing Geocache refreshes.
     * <br>
     * Don't use it for anything else than that.
     * It shall SOLELY be used by {@link cgeo.geocaching.CacheDetailActivity} and {@link cgeo.geocaching.service.CacheDownloaderService}!
     */
    public static final Scheduler refreshScheduler = Schedulers.from(newFixedDiscardingThreadPool(3, "refresh-"));

    public static final Scheduler mainThreadScheduler = AndroidSchedulers.mainThread();

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

    public static void runOnUi(final Runnable action) {
        AndroidSchedulers.mainThread().createWorker().schedule(action);
    }

    public static void andThenOnUi(final Scheduler scheduler, final Runnable background, final Runnable foreground) {
        scheduler.createWorker().schedule(() -> {
            background.run();
            AndroidSchedulers.mainThread().createWorker().schedule(foreground);
        });
    }

    public static Disposable runPeriodically(final Scheduler scheduler, final Runnable runnable, final long initialDelayInMs, final long periodInMs) {
        return scheduler.createWorker().schedulePeriodically(runnable, initialDelayInMs, periodInMs, TimeUnit.MILLISECONDS);
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
     * executes a list of tasks in parallel for a list of given items. Warning: function BLOCKS and returns when all tasks are
     * executed. Provides fail-safety for executions: failing (=exceution-throwing) functions lead to ignoring their result.
     *
     * @param items list of values for which to start parallel execution. null values are ignored
     * @param parallelFunction function which is executed in parallel for each value in items. null results are ignored
     * @param initialCombinerValue initial value for combined result. Must be non-null
     * @param combiner function which is executed to combine results with each other
     * @return combined result
     * @param <I> type of initial value
     * @param <R> type of result of parallel execution
     * @param <F> type of result of combining parallel execution result
     */
    @WorkerThread
    @NonNull
    public static <I, R, F> F executeParallelThenCombine(@NonNull final Iterable<I> items, @NonNull final Function<I, R> parallelFunction,
                                                         @NonNull final F initialCombinerValue, @NonNull final BiFunction<F, R, F> combiner) {

        final String logPraefix = "executeParallelThenCombine:";
        return Observable.fromIterable(items).flatMapMaybe((Function<I, Maybe<R>>) item -> {
            try {
                if (item == null) {
                    return Maybe.empty();
                }
                return Maybe.fromCallable(() -> {
                    try {
                        final R result =  parallelFunction.apply(item);
                        if (result == null) {
                            Log.w(logPraefix + "null item returned for function for item " + item);
                        }
                        return result;
                    } catch (final Throwable t) {
                        Log.w(logPraefix + ".function: swallowing error from item " + item, t);
                        return null;
                    }
                }).subscribeOn(AndroidRxUtils.networkScheduler);
            } catch (Throwable t) {
                Log.w(logPraefix + ".maybe-creation: swallowing error from item " + item, t);
                return Maybe.empty();
            }
        }).reduce(initialCombinerValue, (result1, result2) -> {
            try {
                final F combinedResult = combiner.apply(result1, result2);
                if (combinedResult == null) {
                    Log.w(logPraefix + "combining " + result1 + " and " + result2 + " leads to null, ignore combined result");
                }
                return combinedResult == null ? result1 : combinedResult;
            } catch (final Throwable t) {
                Log.w(logPraefix + ".reduce: swallowing error combining " + result1 + " and " + result2, t);
                return result1;
            }
        }).blockingGet();
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
