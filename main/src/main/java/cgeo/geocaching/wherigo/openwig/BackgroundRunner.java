/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package util
 */
package cgeo.geocaching.wherigo.openwig;

import java.util.Vector;

public class BackgroundRunner extends Thread {

    private static BackgroundRunner instance;

    private boolean paused = false;

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

    public static BackgroundRunner getInstance () {
        if (instance == null) instance = new BackgroundRunner();
        return instance;
    }

    private Vector queue = new Vector();
    private boolean end = false;
    private Runnable queueProcessedListener = null;

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
                Runnable c = (Runnable)queue.firstElement();
                queue.removeElementAt(0);
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

    synchronized public void perform (Runnable c) {
        queue.addElement(c);
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
