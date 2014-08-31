package cgeo.geocaching.utils;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.internal.operators.OperatorTakeWhile;
import rx.observables.BlockingObservable;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RxUtils {

    // Utility class, not to be instanciated
    private RxUtils() {
    }

    public final static Scheduler computationScheduler = Schedulers.computation();

    public static final Scheduler networkScheduler = Schedulers.from(new ThreadPoolExecutor(10, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));

    private static final HandlerThread looperCallbacksThread =
            new HandlerThread("Looper callbacks thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);

    static {
        looperCallbacksThread.start();
    }

    public static final Looper looperCallbacksLooper = looperCallbacksThread.getLooper();
    public static final Scheduler looperCallbacksScheduler = AndroidSchedulers.handlerThread(new Handler(looperCallbacksLooper));
    public static final Worker looperCallbacksWorker = looperCallbacksScheduler.createWorker();

    public static <T> void waitForCompletion(final BlockingObservable<T> observable) {
        observable.lastOrDefault(null);
    }

    public static void waitForCompletion(final Observable<?>... observables) {
        waitForCompletion(Observable.merge(observables).toBlocking());
    }

    /**
     * Subscribe function whose subscription and unsubscription take place on a looper thread.
     *
     * @param <T>
     *         the type of the observable
     */
    public static abstract class LooperCallbacks<T> implements OnSubscribe<T> {

        final AtomicInteger counter = new AtomicInteger(0);
        final long stopDelay;
        final TimeUnit stopDelayUnit;
        protected Subscriber<? super T> subscriber;

        public LooperCallbacks(final long stopDelay, final TimeUnit stopDelayUnit) {
            this.stopDelay = stopDelay;
            this.stopDelayUnit = stopDelayUnit;
        }

        public LooperCallbacks() {
            this(0, TimeUnit.SECONDS);
        }

        @Override
        final public void call(final Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
            looperCallbacksWorker.schedule(new Action0() {
                @Override
                public void call() {
                    if (counter.getAndIncrement() == 0) {
                        onStart();
                    }
                    subscriber.add(Subscriptions.create(new Action0() {
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
        }

        abstract protected void onStart();

        abstract protected void onStop();
    }

    public static <T> Operator<T, T> operatorTakeUntil(final Func1<? super T, Boolean> predicate) {
        return new OperatorTakeWhile<T>(new Func1<T, Boolean>() {
            private boolean quitting = false;

            @Override
            public Boolean call(final T item) {
                if (quitting) {
                    return false;
                }
                quitting |= predicate.call(item);
                return true;
            }
        });
    }

}
