package cgeo.geocaching.wherigo.openwig.util;

import java.util.concurrent.ConcurrentLinkedQueue;

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

    public synchronized void pause () {
        paused = true;
    }

    public synchronized void unpause () {
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
                try { wait(); } catch (InterruptedException e) { }
                if (end) return;
            } }
            events = false;
            while (!queue.isEmpty()) {
                events = true;
                Runnable c = queue.poll();
                if (c == null) break;
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
                try { wait(); } catch (InterruptedException e) { }
            }
        }
    }

    public synchronized void perform (Runnable c) {
        queue.offer(c);
        notify();
    }

    public static void performTask (Runnable c) {
        getInstance().perform(c);
    }

    public synchronized void kill () {
        end = true;
        notify();
    }
}
