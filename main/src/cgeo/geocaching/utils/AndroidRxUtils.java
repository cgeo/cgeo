package cgeo.geocaching.utils;

import android.app.Activity;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.schedulers.RxThreadFactory;
import io.reactivex.schedulers.Schedulers;

public class AndroidRxUtils {

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

    private AndroidRxUtils() {
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

    public static <T> Observable<T> bindActivity(final Activity activity, final Observable<T> source) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        return source.observeOn(AndroidSchedulers.mainThread()).takeWhile(new Predicate<T>() {
            @Override
            public boolean test(final T t) throws Exception {
                final Activity a = activityRef.get();
                return a != null && !a.isFinishing();
            }
        });
    }

    public static <T> Maybe<T> bindActivity(final Activity activity, final Single<T> source) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        return source.observeOn(AndroidSchedulers.mainThread()).filter(new Predicate<T>() {
            @Override
            public boolean test(final T t) throws Exception {
                final Activity a = activityRef.get();
                return a != null && !a.isFinishing();
            }
        });
    }

    public static <T> Maybe<T> bindActivity(final Activity activity, final Maybe<T> source) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        return source.observeOn(AndroidSchedulers.mainThread()).filter(new Predicate<T>() {
            @Override
            public boolean test(final T t) throws Exception {
                final Activity a = activityRef.get();
                return a != null && !a.isFinishing();
            }
        });
    }

    public static <T> Observable<T> bindFragment(final Fragment fragment, final Observable<T> source) {
        final WeakReference<Fragment> fragmentRef = new WeakReference<>(fragment);
        return source.observeOn(AndroidSchedulers.mainThread()).filter(new Predicate<T>() {
            @Override
            public boolean test(final T t) throws Exception {
                final Fragment f = fragmentRef.get();
                return f != null && f.isAdded() && !f.getActivity().isFinishing();
            }
        });
    }

    public static Disposable disposeOnCallbacksScheduler(final Runnable runnable) {
        return Disposables.fromRunnable(new Runnable() {
            @Override
            public void run() {
                looperCallbacksScheduler.scheduleDirect(runnable);
            }
        });
    }
}
