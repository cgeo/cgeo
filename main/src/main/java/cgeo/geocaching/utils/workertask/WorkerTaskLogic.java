package cgeo.geocaching.utils.workertask;

import androidx.annotation.WorkerThread;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** to be implemented by tasks which wish to be executed in a worker thread / in the background */
public interface WorkerTaskLogic<I, P, R> {

    /**
     * Runs the worker task. Meant to be executed in the background/on a worker thread
     *
     * @param input input for the worker task
     * @param message this consumer can be used to send progress messages to the GUI thread
     * @param isCancelled this supplier should be checked regularly. If it returns true, the work should be cancelled
     * @return the result of the work
     */
    @WorkerThread
    R run(I input, Consumer<P> message, Supplier<Boolean> isCancelled);

}
