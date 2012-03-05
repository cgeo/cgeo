package cgeo.geocaching.concurrent;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * BlockingThreadPool restricts the amount of parallel threads executing Runnables.
 */
public class BlockingThreadPool {
    /** The queue holding the Runnable. **/
    private BlockingQueue<Runnable> queue = null;
    /** The Executor. **/
    private ThreadPoolExecutor executor;

    /**
     * Creates a ThreadPool with a given maximum of parallel threads running.
     * Idle threads will be stopped until new threads are added.
     *
     * @param poolSize
     *            Maximum amout of parallel Threads
     * @param priority
     *            The Thread priority e.g. Thread.MIN_PRIORITY
     */
    public BlockingThreadPool(int poolSize, int priority) {
        ThreadFactory threadFactory = new PriorityThreadFactory(priority);
        this.queue = new ArrayBlockingQueue<Runnable>(poolSize, true);
        this.executor = new ThreadPoolExecutor(0, poolSize, 5, TimeUnit.SECONDS, this.queue);
        this.executor.setThreadFactory(threadFactory);
    }

    /**
     * Add a runnable to the pool. This will start the core threads in the underlying
     * executor and try to add the Runnable to the pool. This method waits until timeout
     * if no free thread is available.
     *
     * @param task
     *            The Runnable to add to the pool
     * @param timeout
     *            The timeout to wait for a free thread
     * @param unit
     *            The timeout unit
     * @return true/false successful added
     * @throws InterruptedException
     *             Operation was interrupted
     */
    public boolean add(Runnable task, int timeout, TimeUnit unit) throws InterruptedException {
        this.executor.setCorePoolSize(this.executor.getMaximumPoolSize());
        this.executor.prestartAllCoreThreads();
        boolean successfull = this.queue.offer(task, timeout, unit);
        this.executor.setCorePoolSize(0);
        return successfull;
    }
}
