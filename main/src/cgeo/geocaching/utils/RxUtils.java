package cgeo.geocaching.utils;

import rx.Observable;
import rx.Scheduler;
import rx.observables.BlockingObservable;
import rx.schedulers.Schedulers;

public class RxUtils {

    // Utility class, not to be instanciated
    private RxUtils() {}

    public final static Scheduler computationScheduler = Schedulers.computation();

    public static <T> void waitForCompletion(final BlockingObservable<T> observable) {
        observable.lastOrDefault(null);
        return;
    }

    public static void waitForCompletion(final Observable<?>... observables) {
        waitForCompletion(Observable.merge(observables).toBlocking());
    }
}
