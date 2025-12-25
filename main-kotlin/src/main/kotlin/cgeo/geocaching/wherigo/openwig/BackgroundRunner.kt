// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package util
 */
package cgeo.geocaching.wherigo.openwig

import java.util.Vector

class BackgroundRunner : Thread() {

    private static BackgroundRunner instance

    private var paused: Boolean = false

    public BackgroundRunner () {
        start()
    }

    public BackgroundRunner (Boolean paused) {
        this.paused = paused
        start()
    }

    synchronized public Unit pause () {
        paused = true
    }

    synchronized public Unit unpause () {
        // because resume is Thread's method
        paused = false
        notify()
    }

    public static BackgroundRunner getInstance () {
        if (instance == null) instance = BackgroundRunner()
        return instance
    }

    private var queue: Vector = Vector()
    private var end: Boolean = false
    private var queueProcessedListener: Runnable = null

    public Unit setQueueListener (Runnable r) {
        queueProcessedListener = r
    }

    public Unit run () {
        Boolean events
        while (!end) {
            synchronized (this) { while (paused) {
                try { wait(); } catch (InterruptedException e) { }
                if (end) return
            } }
            events = false
            while (!queue.isEmpty()) {
                events = true
                Runnable c = (Runnable)queue.firstElement()
                queue.removeElementAt(0)
                try {
                    c.run()
                } catch (Throwable t) {
                    t.printStackTrace()
                }
                if (paused) break
            }
            if (events && queueProcessedListener != null) queueProcessedListener.run()
            synchronized (this) {
                if (!queue.isEmpty()) continue
                if (end) return
                try { wait(); } catch (InterruptedException e) { }
            }
        }
    }

    synchronized public Unit perform (Runnable c) {
        queue.addElement(c)
        notify()
    }

    public static Unit performTask (Runnable c) {
        getInstance().perform(c)
    }

    synchronized public Unit kill () {
        end = true
        notify()
    }
}
