package cgeo.geocaching.utils;

import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RxUtils {

    // Utility class, not to be instanciated
    private RxUtils() {}

    final static private int cores = Runtime.getRuntime().availableProcessors();
    public final static Scheduler computationScheduler = Schedulers.executor(new ThreadPoolExecutor(1, cores, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));

}
