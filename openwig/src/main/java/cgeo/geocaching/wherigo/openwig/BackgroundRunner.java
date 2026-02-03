/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package util
 */
package cgeo.geocaching.wherigo.openwig;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Background thread for executing Lua events and callbacks.
 * <p>
 * BackgroundRunner is a utility thread that processes queued Runnable tasks
 * sequentially in the background. It is used by the Engine to execute all
 * Lua-related operations (events, callbacks, game saves) on a dedicated thread,
 * keeping them separate from the main engine loop and UI thread.
 * <p>
 * Key features:
 * <ul>
 * <li>Processes queued tasks sequentially on a background thread</li>
 * <li>Can be paused and resumed to control execution</li>
 * <li>Ensures Lua state is only accessed from one thread</li>
 * <li>Notifies listeners when queue is empty</li>
 * <li>Singleton pattern for engine-wide use</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> This class is thread-safe. Uses ConcurrentLinkedQueue for
 * task queue management and proper synchronization for state changes.
 * <p>
 * This threading model prevents race conditions in the Lua state and ensures
 * predictable event ordering.
 */
public class BackgroundRunner extends Thread {

    private static BackgroundRunner instance;

    private volatile boolean paused = false;

    public BackgroundRunner () {
        start();
    }

    public BackgroundRunner (boolean paused) {
        this.paused = paused;
        start();
    }

    synchronized public void pause () {
        paused = true;
    }

    synchronized public void unpause () {
        // because resume is Thread's method
        paused = false;
        notify();
    }

    public static synchronized BackgroundRunner getInstance () {
        if (instance == null) instance = new BackgroundRunner();
        return instance;
    }

    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private volatile boolean end = false;
    private volatile Runnable queueProcessedListener = null;

    public void setQueueListener (Runnable r) {
        queueProcessedListener = r;
    }

    public void run () {
        boolean events;
        while (!end) {
            synchronized (this) { while (paused) {
                try { wait(); } catch (InterruptedException ignored) { }
                if (end) return;
            } }
            events = false;
            while (!queue.isEmpty()) {
                events = true;
                Runnable c = queue.poll();
                if (c == null) break; // Queue became empty
                try {
                    c.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                if (paused) break;
            }
            if (events && queueProcessedListener != null) queueProcessedListener.run();
            synchronized (this) {
                if (!queue.isEmpty()) continue;
                if (end) return;
                try { wait(); } catch (InterruptedException ignored) { }
            }
        }
    }

    synchronized public void perform (Runnable c) {
        queue.offer(c);
        notify();
    }

    public static void performTask (Runnable c) {
        getInstance().perform(c);
    }

    synchronized public void kill () {
        end = true;
        notify();
    }
}
