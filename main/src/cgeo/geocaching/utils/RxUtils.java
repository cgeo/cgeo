package cgeo.geocaching.utils;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class RxUtils {

    // Utility class, not to be instanciated
    private RxUtils() {}

    public final static Scheduler computationScheduler = Schedulers.computation();
}
