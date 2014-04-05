package cgeo.geocaching.utils;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RxUtils {

    // Utility class, not to be instanciated
    private RxUtils() {}

    final static private int cores = Runtime.getRuntime().availableProcessors();
    public final static Scheduler computationScheduler = Schedulers.executor(new ThreadPoolExecutor(1, cores, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));

    public static <T> Subscription subscribeThenUI(final Observable<T> observable, final Observer<T> observer) {
        return observable.observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    public static <T> Subscription subscribeThenUI(final Observable<T> observable, final Action1<T> action) {
        return observable.observeOn(AndroidSchedulers.mainThread()).subscribe(action);
    }

    public static <T> Subscription subscribeThenUI(final Observable<T> observable, final Action1<T> action, final Action1<Throwable> onError) {
        return observable.observeOn(AndroidSchedulers.mainThread()).subscribe(action, onError);
    }

    public static <T> Observable<T> onIO(final Observable<T> observable) {
        return observable.subscribeOn(Schedulers.io());
    }

    public static <T> Subscription subscribeOnIOThenUI(final Observable<T> observable, final Observer<T> observer) {
        return subscribeThenUI(onIO(observable), observer);
    }

    public static <T> Subscription subscribeOnIOThenUI(final Observable<T> observable, final Action1<T> action) {
        return subscribeThenUI(onIO(observable), action);
    }

    public static <T> Subscription subscribeOnIOThenUI(final Observable<T> observable, final Action1<T> action, final Action1<Throwable> onError) {
        return subscribeThenUI(onIO(observable), action, onError);
    }

}
