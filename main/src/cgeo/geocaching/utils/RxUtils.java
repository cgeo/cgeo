package cgeo.geocaching.utils;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.BlockingObservable;
import rx.schedulers.Schedulers;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RxUtils {

    // Utility class, not to be instanciated
    private RxUtils() {}

    private static final StartableHandlerThread looperCallbacksThread =
            new StartableHandlerThread("GpsStatusProvider thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);

    static {
        looperCallbacksThread.start();
    }

    public static final Worker looperCallbacksWorker = AndroidSchedulers.handlerThread(looperCallbacksThread.getHandler()).createWorker();

    public final static Scheduler computationScheduler = Schedulers.computation();

    public static final Scheduler networkScheduler = Schedulers.from(new ThreadPoolExecutor(10, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));

    public static <T> void waitForCompletion(final BlockingObservable<T> observable) {
        observable.lastOrDefault(null);
    }

    public static void waitForCompletion(final Observable<?>... observables) {
        waitForCompletion(Observable.merge(observables).toBlocking());
    }
}
