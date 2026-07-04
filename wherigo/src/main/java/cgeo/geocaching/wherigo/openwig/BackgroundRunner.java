/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package util
 */
package cgeo.geocaching.wherigo.openwig;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BackgroundRunner extends Thread {

    private static BackgroundRunner instance;

    private boolean paused = false;

    public BackgroundRunner () {
        start();
    }

    public BackgroundRunner (final boolean paused) {
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

    public static BackgroundRunner getInstance () {
        if (instance == null) instance = new BackgroundRunner();
        return instance;
    }

    private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private boolean end = false;
    private Runnable queueProcessedListener = null;

    public void setQueueListener (final Runnable r) {
        queueProcessedListener = r;
    }

    public void run () {
        boolean events;
        while (!end) {
            synchronized (this) { while (paused) {
                try { wait(); } catch (final InterruptedException e) { }
                if (end) return;
            } }
            events = false;
            Runnable c;
            while ((c = queue.poll()) != null) {
                events = true;
                try {
                    c.run();
                } catch (final Throwable t) {
                    t.printStackTrace();
                }
                if (paused) break;
            }
            if (events && queueProcessedListener != null) queueProcessedListener.run();
            synchronized (this) {
                if (!queue.isEmpty()) continue;
                if (end) return;
                try { wait(); } catch (final InterruptedException e) { }
            }
        }
    }

    synchronized public void perform (final Runnable c) {
        queue.offer(c);
        notify();
    }

    public static void performTask (final Runnable c) {
        getInstance().perform(c);
    }

    synchronized public void kill () {
        end = true;
        notify();
    }
}
