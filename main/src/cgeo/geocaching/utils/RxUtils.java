package cgeo.geocaching.utils;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.observables.BlockingObservable;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RxUtils {

    // Utility class, not to be instanciated
    private RxUtils() {}

    private static final StartableHandlerThread looperCallbacksThread =
            new StartableHandlerThread("Looper callbacks thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);

    static {
        looperCallbacksThread.start();
    }

    private static final Worker looperCallbacksWorker = AndroidSchedulers.handlerThread(looperCallbacksThread.getHandler()).createWorker();

    public final static Scheduler computationScheduler = Schedulers.computation();

    public static final Scheduler networkScheduler = Schedulers.from(new ThreadPoolExecutor(10, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));

    public static <T> void waitForCompletion(final BlockingObservable<T> observable) {
        observable.lastOrDefault(null);
    }

    public static void waitForCompletion(final Observable<?>... observables) {
        waitForCompletion(Observable.merge(observables).toBlocking());
    }

    /**
     * Start a job (typically one that register handlers) on a looper thread if the counter goes above 0,
     * and stop it if it comes back to 0 after the job is unsubscribed from.
     *
     * @param counter the counter to use
     * @param onStart the job to launch if the counter goes above 0
     * @param onStop the job to launch when unsubscribing if the counter goes back at 0
     * @param stopDelay the delay before which the unsubscription should take place
     * @param stopDelayUnit the unit of the delay before which the unsubscription should take place
     * @return the subscription allowing to unsubscribe
     */
    public static Subscription looperCallbacksSchedule(final AtomicInteger counter, final Action0 onStart, final Action0 onStop,
                                                       final long stopDelay, final TimeUnit stopDelayUnit) {
        final CompositeSubscription subscription = new CompositeSubscription();
        looperCallbacksWorker.schedule(new Action0() {
            @Override
            public void call() {
                if (counter.getAndIncrement() == 0) {
                    onStart.call();
                }
                subscription.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        looperCallbacksWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                if (counter.decrementAndGet() == 0) {
                                    onStop.call();
                                }
                            }
                        }, stopDelay, stopDelayUnit);
                    }
                }));
            }
        });
        return subscription;
    }

    /**
     * Start a job (typically one that register handlers) on a looper thread if the counter goes above 0,
     * and stop it if it comes back to 0 after the job is unsubscribed from.
     *
     * @param counter the counter to use
     * @param onStart the job to launch if the counter goes above 0
     * @param onStop the job to launch when unsubscribing if the counter goes back at 0
     * @return the subscription allowing to unsubscribe
     */
    public static Subscription looperCallbacksSchedule(final AtomicInteger counter, final Action0 onStart, final Action0 onStop) {
        return looperCallbacksSchedule(counter, onStart, onStop, 0, TimeUnit.SECONDS);
    }
}
