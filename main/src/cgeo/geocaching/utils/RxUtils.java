package cgeo.geocaching.utils;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.BlockingObservable;
import rx.observables.ConnectableObservable;
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

    public final static Scheduler computationScheduler = Schedulers.computation();

    public static final Scheduler networkScheduler = Schedulers.from(new ThreadPoolExecutor(10, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));

    public static <T> void waitForCompletion(final BlockingObservable<T> observable) {
        observable.lastOrDefault(null);
    }

    public static void waitForCompletion(final Observable<?>... observables) {
        waitForCompletion(Observable.merge(observables).toBlocking());
    }

    /**
     * ConnectableObservable whose subscription and unsubscription take place on a looper thread.
     *
     * @param <T> the type of the observable
     */
    public static abstract class ConnectableLooperCallbacks<T> extends ConnectableObservable<T> {
        private static final StartableHandlerThread looperCallbacksThread =
                new StartableHandlerThread("Looper callbacks thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        static {
            looperCallbacksThread.start();
        }
        private static final Worker looperCallbacksWorker = AndroidSchedulers.handlerThread(looperCallbacksThread.getHandler()).createWorker();

        final AtomicInteger counter = new AtomicInteger(0);
        final long stopDelay;
        final TimeUnit stopDelayUnit;

        public ConnectableLooperCallbacks(final OnSubscribe<T> onSubscribe, final long stopDelay, final TimeUnit stopDelayUnit) {
            super(onSubscribe);
            this.stopDelay = stopDelay;
            this.stopDelayUnit = stopDelayUnit;
        }

        public ConnectableLooperCallbacks(final OnSubscribe<T> onSubscribe) {
            this(onSubscribe, 0, TimeUnit.SECONDS);
        }

        @Override
        final public void connect(final Action1<? super Subscription> action1) {
            final CompositeSubscription subscription = new CompositeSubscription();
            looperCallbacksWorker.schedule(new Action0() {
                @Override
                public void call() {
                    if (counter.getAndIncrement() == 0) {
                        onStart();
                    }
                    subscription.add(Subscriptions.create(new Action0() {
                        @Override
                        public void call() {
                            looperCallbacksWorker.schedule(new Action0() {
                                @Override
                                public void call() {
                                    if (counter.decrementAndGet() == 0) {
                                        onStop();
                                    }
                                }
                            }, stopDelay, stopDelayUnit);
                        }
                    }));
                }
            });
            action1.call(subscription);
        }

        abstract protected void onStart();
        abstract protected void onStop();
    }
}
