package cgeo.geocaching.concurrent;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Util class to handle working Threads.
 */
public class ThreadPool {
    /** The queue holding the Runnable. **/
    private BlockingQueue<Runnable> queue = null;
    /** The Executor. **/
    private ThreadPoolExecutor executor;

    public static final void main(String[] argv) {
        ThreadPool pool = new ThreadPool(1, new PriorityThreadFactory(Thread.MIN_PRIORITY));
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
            }
            Task currentTask = new Task("Task-" + i) {
                public void run() {
                    for (int n = 0; n < 10; n++) {
                        System.out.println(this.getName() + " Log " + n);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            try {
                pool.add(currentTask, 20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Creates a ThreadPool with the given maximum amount of parallel Threads running.
     *
     * @param maxThreads
     *            Maximum amout of parallel Threads
     * @param queueSize
     *            Size of the queue taking Runnables before blocking further Runnables
     * @param timeoutSecs
     *            Timeout for add operations, 0 waits forever
     */
    public ThreadPool(int poolSize, ThreadFactory threadFactory) {
        this.queue = new ArrayBlockingQueue<Runnable>(poolSize, true);
        this.executor = new ThreadPoolExecutor(0, poolSize, 5, TimeUnit.SECONDS, this.queue);
        this.executor.setThreadFactory(threadFactory);
    }

    /**
     * Add a runnable to the queue. This will wait for timeoutSecs given in the constructor.
     * Please no Threads! ThreadPool itself will created or destroy Threads itself
     *
     * @param action
     *            The object to run.
     * @return true/false successful added
     * @throws InterruptedException
     *             Operation was interrupted
     * @throws TimeoutException
     *             Timeout occurred
     */
    public boolean add(Runnable task, int timeout, TimeUnit unit) throws InterruptedException {
        this.executor.setCorePoolSize(this.executor.getMaximumPoolSize());
        this.executor.prestartAllCoreThreads();
        boolean successfull = this.queue.offer(task, timeout, unit);
        this.executor.setCorePoolSize(0);
        return successfull;
    }
}
